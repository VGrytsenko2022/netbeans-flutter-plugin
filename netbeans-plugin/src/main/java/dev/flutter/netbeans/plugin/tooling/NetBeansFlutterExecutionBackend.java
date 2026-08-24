package dev.flutter.netbeans.plugin.tooling;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import javax.swing.event.ChangeListener;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.api.extexecution.base.input.LineProcessor;
import dev.flutter.netbeans.plugin.options.FlutterOptionsPanelController;

/** NetBeans-native Output/Stop/Progress backend for Flutter tooling commands. */
final class NetBeansFlutterExecutionBackend implements FlutterExecutionBackend {
    private static final ExecutionDescriptor.RerunCondition NO_BUILT_IN_RERUN =
            new ExecutionDescriptor.RerunCondition() {
                @Override
                public void addChangeListener(ChangeListener listener) {
                }

                @Override
                public void removeChangeListener(ChangeListener listener) {
                }

                @Override
                public boolean isRerunPossible() {
                    return false;
                }
            };

    @Override
    public Future<Integer> start(FlutterExecutionRequest request) {
        org.netbeans.api.extexecution.base.ProcessBuilder builder =
                org.netbeans.api.extexecution.base.ProcessBuilder.getLocal();
        builder.setExecutable(request.executable().toString());
        builder.setWorkingDirectory(request.workingDirectory().toString());
        builder.setArguments(request.arguments());
        builder.setRedirectErrorStream(false);

        ExecutionDescriptor descriptor = new ExecutionDescriptor()
                .frontWindow(true)
                .frontWindowOnError(true)
                .controllable(true)
                .rerunCondition(NO_BUILT_IN_RERUN)
                .showProgress(true)
                .charset(StandardCharsets.UTF_8)
                .outLineBased(true)
                .errLineBased(true)
                .optionsPath(FlutterOptionsPanelController.ID);

        if (request.outputConvertor() != null) {
            descriptor = descriptor.outConvertorFactory(request::outputConvertor);
        } else {
            descriptor = descriptor.outProcessorFactory(
                    (ExecutionDescriptor.InputProcessorFactory2) defaultProcessor -> processors(
                            request.echoStandardOutput() ? defaultProcessor : null,
                            request.standardOutput()));
        }
        descriptor = descriptor.errProcessorFactory(
                (ExecutionDescriptor.InputProcessorFactory2) defaultProcessor -> processors(
                        defaultProcessor,
                        request.standardError()));

        return ExecutionService.newService(builder, descriptor, request.displayName()).run();
    }

    private static InputProcessor processors(
            InputProcessor defaultProcessor,
            Consumer<String> lineConsumer) {
        List<InputProcessor> processors = new ArrayList<>(2);
        if (defaultProcessor != null) {
            processors.add(defaultProcessor);
        }
        processors.add(InputProcessors.bridge(new ConsumerLineProcessor(lineConsumer)));
        return InputProcessors.proxy(processors.toArray(InputProcessor[]::new));
    }

    private static final class ConsumerLineProcessor implements LineProcessor {
        private final Consumer<String> consumer;

        ConsumerLineProcessor(Consumer<String> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void processLine(String line) {
            consumer.accept(line);
        }

        @Override
        public void reset() {
        }

        @Override
        public void close() {
        }
    }
}
