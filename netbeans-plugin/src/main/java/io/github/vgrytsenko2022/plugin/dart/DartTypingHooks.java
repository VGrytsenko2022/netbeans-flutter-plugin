package io.github.vgrytsenko2022.plugin.dart;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import org.netbeans.api.editor.document.AtomicLockDocument;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.editor.indent.api.Indent;
import org.netbeans.spi.editor.typinghooks.TypedBreakInterceptor;
import org.netbeans.spi.editor.typinghooks.TypedTextInterceptor;

/** Dart-specific Enter and closing-brace behavior built on NetBeans typing hooks. */
public final class DartTypingHooks {
    private static final Logger LOGGER = Logger.getLogger(DartTypingHooks.class.getName());

    private DartTypingHooks() {
    }

    private static final class BreakHook implements TypedBreakInterceptor {
        @Override
        public boolean beforeInsert(Context context) {
            return false;
        }

        @Override
        public void insert(MutableContext context) throws BadLocationException {
            Document document = context.getDocument();
            if (DartIndentation.shouldExpandBreak(
                    document, context.getBreakInsertOffset())) {
                context.setText("\n\n", 1, 1, 0, 2);
            }
        }

        @Override
        public void afterInsert(Context context) {
        }

        @Override
        public void cancelled(Context context) {
        }
    }

    private static final class TextHook implements TypedTextInterceptor {
        @Override
        public boolean beforeInsert(Context context) {
            return false;
        }

        @Override
        public void insert(MutableContext context) {
        }

        @Override
        public void afterInsert(Context context) throws BadLocationException {
            if (!"}".equals(context.getText())) {
                return;
            }
            Document document = context.getDocument();
            int offset = context.getOffset();
            Position position = document.createPosition(offset);
            SwingUtilities.invokeLater(() -> reindentClosingBrace(document, position));
        }

        @Override
        public void cancelled(Context context) {
        }
    }

    private static void reindentClosingBrace(Document document, Position position) {
        try {
            int offset = position.getOffset();
            if (!DartIndentation.isCodeSeparatorAt(document, offset, '}')
                    || !DartIndentation.isFirstNonWhitespaceOnLine(document, offset)) {
                return;
            }
            int lineStart = DartIndentation.lineStart(document, offset);
            Indent indent = Indent.get(document);
            BadLocationException[] failure = new BadLocationException[1];
            indent.lock();
            try {
                runAtomicUserEdit(document, () -> {
                    try {
                        indent.reindent(lineStart);
                    } catch (BadLocationException ex) {
                        failure[0] = ex;
                    }
                });
            } finally {
                indent.unlock();
            }
            if (failure[0] != null) {
                throw failure[0];
            }
        } catch (BadLocationException ex) {
            LOGGER.log(Level.FINE, "Could not reindent a Dart closing brace", ex);
        }
    }

    static void runAtomicUserEdit(Document document, Runnable operation) {
        AtomicLockDocument atomicDocument = LineDocumentUtils.asRequired(
                document, AtomicLockDocument.class);
        atomicDocument.runAtomicAsUser(operation);
    }

    @MimeRegistration(
            mimeType = DartTokenId.MIME_TYPE,
            service = TypedBreakInterceptor.Factory.class)
    public static final class BreakFactory implements TypedBreakInterceptor.Factory {
        @Override
        public TypedBreakInterceptor createTypedBreakInterceptor(MimePath mimePath) {
            return new BreakHook();
        }
    }

    @MimeRegistration(
            mimeType = DartTokenId.MIME_TYPE,
            service = TypedTextInterceptor.Factory.class)
    public static final class TextFactory implements TypedTextInterceptor.Factory {
        @Override
        public TypedTextInterceptor createTypedTextInterceptor(MimePath mimePath) {
            return new TextHook();
        }
    }
}
