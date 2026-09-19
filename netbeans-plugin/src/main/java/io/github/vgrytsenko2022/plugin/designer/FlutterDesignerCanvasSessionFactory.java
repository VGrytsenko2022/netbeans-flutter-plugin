package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerCanvasBackendSelector.Backend;
import io.github.vgrytsenko2022.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
import io.github.vgrytsenko2022.plugin.designer.canvas.NativeCanvasPlatformProviders;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasHost;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasPlatformProvider;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Creates one fully owned backend-neutral Canvas lifecycle envelope. */
@FunctionalInterface
interface FlutterDesignerCanvasSessionFactory {
    FlutterDesignerCanvasOwner create(Backend backend, Callbacks callbacks)
            throws CreationException;

    static FlutterDesignerCanvasSessionFactory production() {
        return FlutterDesignerRoutedCanvasSessionFactory.production();
    }

    record Callbacks(
            Consumer<FlutterDesignerNativeCanvasStatus> statusListener,
            Consumer<StableId> selectionListener,
            Function<String, Optional<WidgetTypeId>> paletteDropTokenResolver,
            Consumer<FlutterDesignerCanvasSession.AdmittedPaletteDrop>
                    paletteDropListener,
            Consumer<CanvasRunnerRuntimeEvent.DeleteSelection>
                    deleteSelectionListener) {
        public Callbacks {
            Objects.requireNonNull(statusListener, "statusListener");
            Objects.requireNonNull(selectionListener, "selectionListener");
            Objects.requireNonNull(
                    paletteDropTokenResolver, "paletteDropTokenResolver");
            Objects.requireNonNull(paletteDropListener, "paletteDropListener");
            Objects.requireNonNull(deleteSelectionListener,
                    "deleteSelectionListener");
        }
    }

    /** A fail-closed creation result carrying the exact attempted target. */
    final class CreationException extends IOException {
        private final String target;

        CreationException(String target, String reason) {
            super(reason);
            this.target = requireText(target, "target");
        }

        CreationException(String target, String reason, Throwable cause) {
            super(reason, cause);
            this.target = requireText(target, "target");
        }

        String target() {
            return target;
        }

        private static String requireText(String value, String label) {
            String accepted = Objects.requireNonNull(value, label).trim();
            if (accepted.isEmpty()) {
                throw new IllegalArgumentException(label + " must not be blank");
            }
            return accepted;
        }
    }
}

/**
 * Backend-neutral production factory. Exact Web construction is admitted here
 * while product routing remains independently gated by the MultiView close and
 * physical Windows/WebView2 verification barrier.
 */
final class FlutterDesignerRoutedCanvasSessionFactory
        implements FlutterDesignerCanvasSessionFactory {
    @FunctionalInterface
    interface WebSessionCreator {
        FlutterDesignerCanvasSession create(Callbacks callbacks)
                throws IOException;
    }

    private final FlutterDesignerCanvasSessionFactory nativeFactory;
    private final WebSessionCreator webSessionCreator;

    private FlutterDesignerRoutedCanvasSessionFactory(
            FlutterDesignerCanvasSessionFactory nativeFactory,
            WebSessionCreator webSessionCreator) {
        this.nativeFactory = Objects.requireNonNull(
                nativeFactory, "nativeFactory");
        this.webSessionCreator = Objects.requireNonNull(
                webSessionCreator, "webSessionCreator");
    }

    static FlutterDesignerCanvasSessionFactory production() {
        return new FlutterDesignerRoutedCanvasSessionFactory(
                FlutterDesignerNativeCanvasSessionFactory.production(),
                callbacks -> FlutterDesignerWebCanvasSession.createDefault(
                        callbacks.statusListener(),
                        callbacks.selectionListener(),
                        callbacks.paletteDropListener(),
                        callbacks.deleteSelectionListener()));
    }

    static FlutterDesignerCanvasSessionFactory forTests(
            FlutterDesignerCanvasSessionFactory nativeFactory,
            WebSessionCreator webSessionCreator) {
        return new FlutterDesignerRoutedCanvasSessionFactory(
                nativeFactory, webSessionCreator);
    }

    @Override
    public FlutterDesignerCanvasOwner create(
            Backend backend,
            Callbacks callbacks) throws CreationException {
        Backend acceptedBackend = Objects.requireNonNull(backend, "backend");
        Callbacks acceptedCallbacks = Objects.requireNonNull(
                callbacks, "callbacks");
        if (acceptedBackend == Backend.NATIVE) {
            return nativeFactory.create(acceptedBackend, acceptedCallbacks);
        }
        try {
            FlutterDesignerCanvasSession session = Objects.requireNonNull(
                    webSessionCreator.create(acceptedCallbacks),
                    "Web Canvas session factory returned no session");
            return FlutterDesignerCanvasOwner.adopt(acceptedBackend, session);
        } catch (IOException | RuntimeException | LinkageError failure) {
            if (failure instanceof CreationException creationFailure) {
                throw creationFailure;
            }
            throw new CreationException(
                    "exact Flutter Web Canvas",
                    failureReason(failure),
                    failure);
        }
    }

    private static String failureReason(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message;
    }
}

/** Production adapter that keeps all NativeCanvasHost ownership out of MultiView. */
final class FlutterDesignerNativeCanvasSessionFactory
        implements FlutterDesignerCanvasSessionFactory {
    private static final String FALLBACK_TARGET =
            "provider-owned native Flutter surface";

    @FunctionalInterface
    interface NativeSessionCreator {
        FlutterDesignerCanvasSession create(
                NativeCanvasPlatformProvider provider,
                NativeCanvasHost host,
                Callbacks callbacks) throws IOException;
    }

    private final Supplier<NativeCanvasPlatformProvider> providerSelector;
    private final NativeSessionCreator sessionCreator;

    private FlutterDesignerNativeCanvasSessionFactory(
            Supplier<NativeCanvasPlatformProvider> providerSelector,
            NativeSessionCreator sessionCreator) {
        this.providerSelector = Objects.requireNonNull(
                providerSelector, "providerSelector");
        this.sessionCreator = Objects.requireNonNull(
                sessionCreator, "sessionCreator");
    }

    static FlutterDesignerCanvasSessionFactory production() {
        return new FlutterDesignerNativeCanvasSessionFactory(
                NativeCanvasPlatformProviders::current,
                (provider, host, callbacks) ->
                        FlutterDesignerNativeCanvasSession.createDefault(
                                provider,
                                host,
                                callbacks.statusListener(),
                                callbacks.selectionListener(),
                                callbacks.paletteDropTokenResolver(),
                                callbacks.paletteDropListener(),
                                callbacks.deleteSelectionListener()));
    }

    static FlutterDesignerNativeCanvasSessionFactory forTests(
            Supplier<NativeCanvasPlatformProvider> providerSelector,
            NativeSessionCreator sessionCreator) {
        return new FlutterDesignerNativeCanvasSessionFactory(
                providerSelector, sessionCreator);
    }

    @Override
    public FlutterDesignerCanvasOwner create(
            Backend backend,
            Callbacks callbacks) throws CreationException {
        Backend acceptedBackend = Objects.requireNonNull(backend, "backend");
        Callbacks acceptedCallbacks = Objects.requireNonNull(
                callbacks, "callbacks");
        if (acceptedBackend != Backend.NATIVE) {
            throw new CreationException(
                    "exact Flutter Web Canvas",
                    "No admitted exact Flutter Web Canvas session factory is installed");
        }

        NativeCanvasPlatformProvider provider = null;
        NativeCanvasHost host = null;
        try {
            provider = Objects.requireNonNull(
                    providerSelector.get(),
                    "Native Canvas provider selector returned no provider");
            if (!provider.isSupported()) {
                throw new CreationException(
                        safeTarget(provider),
                        safeReason(provider.availabilityReason(),
                                "Native Canvas provider is unavailable"));
            }
            host = Objects.requireNonNull(
                    provider.createHost(),
                    "Native Canvas provider returned no host");
            FlutterDesignerCanvasSession session = Objects.requireNonNull(
                    sessionCreator.create(provider, host, acceptedCallbacks),
                    "Canvas session factory returned no session");
            // The returned session now owns the provider host. Owner adoption
            // closes that session itself if component validation fails.
            host = null;
            return FlutterDesignerCanvasOwner.adopt(acceptedBackend, session);
        } catch (IOException | RuntimeException | LinkageError failure) {
            String reason = failureReason(failure);
            boolean cleanupFailed = false;
            if (host != null) {
                try {
                    host.close();
                } catch (RuntimeException | LinkageError cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                    reason += "; host cleanup failed: "
                            + failureReason(cleanupFailure);
                    cleanupFailed = true;
                }
            }
            if (failure instanceof CreationException creationFailure
                    && !cleanupFailed) {
                throw creationFailure;
            }
            throw new CreationException(
                    safeTarget(provider),
                    reason,
                    failure);
        }
    }

    private static String safeTarget(NativeCanvasPlatformProvider provider) {
        if (provider != null) {
            try {
                String description = provider.targetDescription();
                if (description != null && !description.isBlank()) {
                    return description;
                }
            } catch (RuntimeException | LinkageError ignored) {
                // A provider diagnostic must not break fail-closed creation.
            }
        }
        return FALLBACK_TARGET;
    }

    private static String safeReason(String reason, String fallback) {
        return reason == null || reason.isBlank() ? fallback : reason;
    }

    private static String failureReason(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message;
    }
}
