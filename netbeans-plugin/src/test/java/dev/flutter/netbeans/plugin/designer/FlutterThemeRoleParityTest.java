package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.flutter.netbeans.canvas.runner.CanvasRunnerBundle;
import dev.flutter.netbeans.designer.catalog.MaterialThemeTokenCatalog;
import dev.flutter.netbeans.project.theme.FlutterMaterialColorRole;
import dev.flutter.netbeans.project.theme.FlutterMaterialTextStyleRole;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Prevents project schema, .fd semantic tokens, and native Canvas from drifting. */
class FlutterThemeRoleParityTest {
    @Test
    void colorSchemeRolesMatchProjectDesignerAndDartAllowlists() throws Exception {
        Set<String> projectTokens = Arrays.stream(FlutterMaterialColorRole.values())
                .map(FlutterMaterialColorRole::themeTokenWireId)
                .collect(Collectors.toUnmodifiableSet());
        Set<String> projectRoles = Arrays.stream(FlutterMaterialColorRole.values())
                .map(FlutterMaterialColorRole::wireName)
                .collect(Collectors.toUnmodifiableSet());
        String dart = runnerModelSource();

        assertEquals(projectTokens, MaterialThemeTokenCatalog.colorRoles().keySet());
        assertEquals(projectTokens, quotedValues(constBlock(
                dart, "canvasColorSchemeThemeTokens", "canvasTextThemeTokens")));
        assertEquals(projectRoles, quotedValues(constBlock(
                dart, "canvasColorSchemeRoles", "canvasTextThemeRoles")));
    }

    @Test
    void textThemeRolesMatchProjectDesignerAndDartAllowlists() throws Exception {
        Set<String> projectTokens = Arrays.stream(FlutterMaterialTextStyleRole.values())
                .map(FlutterMaterialTextStyleRole::themeTokenWireId)
                .collect(Collectors.toUnmodifiableSet());
        Set<String> projectRoles = Arrays.stream(FlutterMaterialTextStyleRole.values())
                .map(FlutterMaterialTextStyleRole::wireName)
                .collect(Collectors.toUnmodifiableSet());
        String dart = runnerModelSource();

        assertEquals(projectTokens, MaterialThemeTokenCatalog.textStyleRoles().keySet());
        assertEquals(projectTokens, quotedValues(constBlock(
                dart, "canvasTextThemeTokens", "canvasColorSchemeRoles")));
        assertEquals(projectRoles, quotedValues(constBlock(
                dart, "canvasTextThemeRoles", "final _stableIdPattern")));
    }

    private static String runnerModelSource() throws IOException {
        try (var input = CanvasRunnerBundle.openSource("lib/src/canvas_model.dart")) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String constBlock(String source, String startName, String endName) {
        int start = source.indexOf(startName);
        int end = source.indexOf(endName, start + startName.length());
        if (start < 0 || end < 0) {
            throw new AssertionError("Missing Dart allowlist block " + startName);
        }
        return source.substring(start, end);
    }

    private static Set<String> quotedValues(String block) {
        Matcher matcher = Pattern.compile("'([^']+)'").matcher(block);
        java.util.HashSet<String> values = new java.util.HashSet<>();
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return Set.copyOf(values);
    }
}
