package io.github.vgrytsenko2022.plugin.pubspec;

import java.util.List;
import java.util.Optional;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;

/** Adds pubspec-aware proposals alongside NetBeans' bundled YAML completion. */
@MimeRegistration(
        mimeType = "text/x-yaml",
        service = CompletionProvider.class,
        position = 200)
public final class PubspecCompletionProvider implements CompletionProvider {
    private final PubspecCompletionEngine engine = new PubspecCompletionEngine();

    @Override
    public CompletionTask createTask(int queryType, JTextComponent component) {
        if (component == null
                || (queryType != COMPLETION_QUERY_TYPE
                && queryType != COMPLETION_ALL_QUERY_TYPE)) {
            return null;
        }
        Optional<PubspecFiles.Context> context = PubspecFiles.from(component.getDocument());
        return context
                .<CompletionTask>map(value -> new AsyncCompletionTask(
                        new Query(engine, value), component))
                .orElse(null);
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        // The MVP is deliberately explicit: show proposals only for Ctrl+Space.
        return 0;
    }

    private static final class Query extends AsyncCompletionQuery {
        private final PubspecCompletionEngine engine;
        private final PubspecFiles.Context context;

        Query(PubspecCompletionEngine engine, PubspecFiles.Context context) {
            this.engine = engine;
            this.context = context;
        }

        @Override
        protected void query(
                CompletionResultSet resultSet,
                Document document,
                int caretOffset) {
            try {
                if (isTaskCancelled()) {
                    return;
                }
                String source = document.getText(0, document.getLength());
                List<PubspecCompletionEngine.Suggestion> suggestions = engine.complete(
                        source,
                        caretOffset,
                        context.projectRoot(),
                        context.packageRoot(),
                        this::isTaskCancelled);
                if (suggestions.isEmpty() || isTaskCancelled()) {
                    return;
                }
                resultSet.setAnchorOffset(suggestions.get(0).replaceStart());
                for (PubspecCompletionEngine.Suggestion suggestion : suggestions) {
                    if (isTaskCancelled()) {
                        return;
                    }
                    resultSet.addItem(CompletionUtilities
                            .newCompletionItemBuilder(suggestion.label())
                            .insertText(suggestion.insertText())
                            .startOffset(suggestion.replaceStart())
                            .endOffset(suggestion.replaceEnd())
                            .leftHtmlText(html(suggestion.label()))
                            .rightHtmlText(html(suggestion.detail()))
                            .sortPriority(suggestion.priority())
                            .sortText(suggestion.label())
                            .build());
                }
            } catch (BadLocationException ex) {
                // The document changed while the asynchronous query was reading it.
            } finally {
                resultSet.finish();
            }
        }

        private static String html(String value) {
            return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }
    }
}
