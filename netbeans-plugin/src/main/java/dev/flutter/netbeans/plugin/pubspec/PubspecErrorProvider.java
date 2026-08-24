package dev.flutter.netbeans.plugin.pubspec;

import java.util.List;
import java.util.Optional;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lsp.Diagnostic;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.spi.lsp.ErrorProvider;

/** Publishes pubspec semantic problems through NetBeans' standard diagnostics API. */
@MimeRegistration(
        mimeType = "text/x-yaml",
        service = ErrorProvider.class,
        position = 200)
public final class PubspecErrorProvider implements ErrorProvider {
    private final PubspecValidator validator = new PubspecValidator();

    @Override
    public List<? extends Diagnostic> computeErrors(Context context) {
        if (context == null
                || context.errorKind() != Kind.ERRORS
                || context.isCancelled()) {
            return List.of();
        }
        Optional<PubspecFiles.Context> pubspec = PubspecFiles.from(context.file());
        if (pubspec.isEmpty() || context.isCancelled()) {
            return List.of();
        }
        Source source = Source.create(context.file());
        if (source == null || context.isCancelled()) {
            return List.of();
        }
        String text = source.createSnapshot().getText().toString();
        List<PubspecDiagnostic> diagnostics = validator.validate(
                text,
                pubspec.get().packageRoot(),
                context::isCancelled);
        if (context.isCancelled()) {
            return List.of();
        }
        return diagnostics.stream()
                .map(PubspecErrorProvider::toNetBeansDiagnostic)
                .toList();
    }

    private static Diagnostic toNetBeansDiagnostic(PubspecDiagnostic diagnostic) {
        return Diagnostic.Builder.create(
                        diagnostic::startOffset,
                        diagnostic::endOffset,
                        diagnostic.message())
                .setSeverity(diagnostic.severity() == PubspecDiagnostic.Severity.ERROR
                        ? Diagnostic.Severity.Error
                        : Diagnostic.Severity.Warning)
                .setCode(diagnostic.code())
                .build();
    }
}
