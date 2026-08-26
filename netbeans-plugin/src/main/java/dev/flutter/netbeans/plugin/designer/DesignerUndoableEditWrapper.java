package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.plugin.dart.DartTokenId;
import javax.swing.text.Document;
import javax.swing.undo.UndoableEdit;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.document.UndoableEditWrapper;

/** Dart MIME hook which wraps only an exact active Designer atomic token. */
@MimeRegistration(
        mimeType = DartTokenId.MIME_TYPE,
        service = UndoableEditWrapper.class,
        position = 100_000)
public final class DesignerUndoableEditWrapper implements UndoableEditWrapper {
    @Override
    public UndoableEdit wrap(UndoableEdit edit, Document document) {
        return DesignerAtomicEditCapture.wrapActive(edit, document);
    }
}
