package io.github.vgrytsenko2022.plugin.tooling;

import io.github.vgrytsenko2022.run.FlutterAnalyzeParser;
import java.nio.file.Path;
import java.util.List;
import org.netbeans.api.extexecution.print.ConvertedLine;
import org.netbeans.api.extexecution.print.LineConvertor;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

/** Makes source locations emitted by {@code flutter analyze} clickable. */
final class FlutterAnalyzeLineConvertor implements LineConvertor {
    private final Path projectRoot;
    private final FlutterAnalyzeParser parser = new FlutterAnalyzeParser();

    FlutterAnalyzeLineConvertor(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    @Override
    public List<ConvertedLine> convert(String line) {
        return parser.parseLine(line)
                .<List<ConvertedLine>>map(issue -> {
                    DartSourceLocation location = new DartSourceLocation(
                            projectRoot,
                            issue.file(),
                            issue.line(),
                            issue.column());
                    return List.of(ConvertedLine.forText(line, listener(location)));
                })
                .orElseGet(() -> List.of(ConvertedLine.forText(line, null)));
    }

    private static OutputListener listener(DartSourceLocation location) {
        return new OutputListener() {
            @Override
            public void outputLineSelected(OutputEvent event) {
            }

            @Override
            public void outputLineAction(OutputEvent event) {
                location.open();
            }

            @Override
            public void outputLineCleared(OutputEvent event) {
            }
        };
    }
}
