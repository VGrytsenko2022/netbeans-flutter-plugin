package dev.flutter.netbeans.plugin.dart;

import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import org.netbeans.modules.editor.indent.spi.Context;
import org.netbeans.modules.editor.indent.spi.ExtraLock;
import org.netbeans.modules.editor.indent.spi.IndentTask;

/** Applies lightweight lexer-aware indentation while the user types Dart code. */
public final class DartIndentTask implements IndentTask {
    private final Context context;

    private DartIndentTask(Context context) {
        this.context = context;
    }

    @Override
    public void reindent() throws BadLocationException {
        if (!context.isIndent()) {
            return;
        }
        Document document = context.document();
        List<DartIndentation.Decision> decisions = DartIndentation.plan(
                document,
                context.startOffset(),
                context.endOffset(),
                context.caretOffset(),
                Math.max(1, IndentUtils.indentLevelSize(document)));
        for (int index = decisions.size() - 1; index >= 0; index--) {
            DartIndentation.Decision decision = decisions.get(index);
            if (decision.indent() != DartIndentation.KEEP) {
                context.modifyIndent(decision.lineStart(), decision.indent());
            }
        }
    }

    @Override
    public ExtraLock indentLock() {
        return null;
    }

    @MimeRegistration(
            mimeType = DartTokenId.MIME_TYPE,
            service = IndentTask.Factory.class)
    public static final class Factory implements IndentTask.Factory {
        @Override
        public IndentTask createTask(Context context) {
            return new DartIndentTask(context);
        }
    }
}
