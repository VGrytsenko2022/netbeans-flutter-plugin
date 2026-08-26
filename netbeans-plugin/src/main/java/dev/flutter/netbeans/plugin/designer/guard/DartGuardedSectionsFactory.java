package dev.flutter.netbeans.plugin.designer.guard;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.guards.GuardedEditorSupport;
import org.netbeans.spi.editor.guards.GuardedSectionsFactory;
import org.netbeans.spi.editor.guards.GuardedSectionsProvider;

/** Creates guarded-section persistence for generated Dart designer regions. */
@MimeRegistration(
        mimeType = DartGuardedSectionsFactory.DART_MIME_TYPE,
        service = GuardedSectionsFactory.class)
public final class DartGuardedSectionsFactory extends GuardedSectionsFactory {

    public static final String DART_MIME_TYPE = "text/x-dart";

    @Override
    public GuardedSectionsProvider create(GuardedEditorSupport editor) {
        return new DartGuardedSectionsProvider(editor);
    }
}
