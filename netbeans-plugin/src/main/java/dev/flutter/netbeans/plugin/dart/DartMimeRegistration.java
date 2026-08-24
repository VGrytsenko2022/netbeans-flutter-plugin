package dev.flutter.netbeans.plugin.dart;

import org.openide.filesystems.MIMEResolver;

/** Registers Dart sources with the MIME type used by the NetBeans DAP debugger. */
@MIMEResolver.ExtensionRegistration(
        displayName = "Dart Source Files",
        extension = {"dart"},
        mimeType = DartTokenId.MIME_TYPE,
        position = 351)
public final class DartMimeRegistration {
    private DartMimeRegistration() {
    }
}
