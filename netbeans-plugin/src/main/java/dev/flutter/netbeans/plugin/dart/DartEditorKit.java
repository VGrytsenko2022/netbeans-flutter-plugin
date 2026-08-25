package dev.flutter.netbeans.plugin.dart;

import javax.swing.text.EditorKit;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.editor.NbEditorKit;

/** NetBeans editor kit for Dart source files. */
@MimeRegistration(
        mimeType = DartTokenId.MIME_TYPE,
        service = EditorKit.class)
public final class DartEditorKit extends NbEditorKit {
    public DartEditorKit() {
    }

    @Override
    public String getContentType() {
        return DartTokenId.MIME_TYPE;
    }
}
