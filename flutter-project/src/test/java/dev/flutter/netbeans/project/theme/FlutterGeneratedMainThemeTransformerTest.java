package dev.flutter.netbeans.project.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class FlutterGeneratedMainThemeTransformerTest {
    private final FlutterGeneratedMainThemeTransformer transformer =
            new FlutterGeneratedMainThemeTransformer();

    @Test
    void wiresOnlyTheRootMaterialThemeAndPreservesCounterApplication() throws Exception {
        String source = generatedCounterMain("\n");

        String transformed = transformer.transform(source);

        assertTrue(transformed.startsWith("import 'package:flutter/material.dart';\n"
                + "import 'theme/app_theme.dart';\n"));
        assertTrue(transformed.contains("      theme: AppTheme.light,\n"
                + "      darkTheme: AppTheme.dark,\n"
                + "      themeMode: AppTheme.mode,\n"
                + "      home: const MyHomePage(title: 'Flutter Demo Home Page'),"));
        assertTrue(transformed.contains("class _MyHomePageState extends State<MyHomePage>"));
        assertTrue(transformed.contains("_counter++;"));
        assertEquals(1, occurrences(transformed, "theme: AppTheme.light"));
        assertEquals(1, occurrences(transformed, "darkTheme: AppTheme.dark"));
        assertEquals(1, occurrences(transformed, "themeMode: AppTheme.mode"));
    }

    @Test
    void preservesCrLfAndIgnoresCommentAndStringLookalikes() throws Exception {
        String source = generatedCounterMain("\r\n")
                .replace("  Widget build(BuildContext context) {",
                        "  Widget build(BuildContext context) {\r\n"
                        + "    // return MaterialApp(theme: ThemeData());\r\n"
                        + "    const marker = 'return MaterialApp(theme: ThemeData())';");

        String transformed = transformer.transform(source);

        assertTrue(transformed.contains("import 'theme/app_theme.dart';\r\n"));
        assertTrue(transformed.contains("darkTheme: AppTheme.dark,\r\n"));
        assertTrue(transformed.contains("const marker = 'return MaterialApp(theme: ThemeData())';"));
        assertEquals(-1, transformed.replace("\r\n", "").indexOf('\n'));
    }

    @Test
    void rejectsMissingOrAmbiguousRootWithoutChangingInput() {
        String missing = "import 'package:flutter/material.dart';\nvoid main() {}\n";
        IOException missingFailure = assertThrows(IOException.class,
                () -> transformer.transform(missing));
        assertTrue(missingFailure.getMessage().contains("returned MaterialApp"));

        String duplicate = generatedCounterMain("\n")
                + "Widget other() { return MaterialApp(theme: ThemeData()); }\n";
        IOException duplicateFailure = assertThrows(IOException.class,
                () -> transformer.transform(duplicate));
        assertTrue(duplicateFailure.getMessage().contains("more than one"));
    }

    @Test
    void rejectsAlreadyWiredOrNonThemeDataRoot() {
        String already = generatedCounterMain("\n")
                .replace("import 'package:flutter/material.dart';\n",
                        "import 'package:flutter/material.dart';\n"
                        + "import 'theme/app_theme.dart';\n");
        assertThrows(IOException.class, () -> transformer.transform(already));

        String custom = generatedCounterMain("\n")
                .replace("ThemeData(\n        colorScheme: .fromSeed(seedColor: Colors.deepPurple),\n      )",
                        "existingTheme");
        IOException customFailure = assertThrows(IOException.class,
                () -> transformer.transform(custom));
        assertTrue(customFailure.getMessage().contains("not a ThemeData expression"));
    }

    static String generatedCounterMain(String newline) {
        return String.join(newline, new String[]{
            "import 'package:flutter/material.dart';",
            "",
            "void main() {",
            "  runApp(const MyApp());",
            "}",
            "",
            "class MyApp extends StatelessWidget {",
            "  const MyApp({super.key});",
            "",
            "  @override",
            "  Widget build(BuildContext context) {",
            "    return MaterialApp(",
            "      title: 'Flutter Demo',",
            "      theme: ThemeData(",
            "        colorScheme: .fromSeed(seedColor: Colors.deepPurple),",
            "      ),",
            "      home: const MyHomePage(title: 'Flutter Demo Home Page'),",
            "    );",
            "  }",
            "}",
            "",
            "class MyHomePage extends StatefulWidget {",
            "  const MyHomePage({super.key, required this.title});",
            "  final String title;",
            "  @override",
            "  State<MyHomePage> createState() => _MyHomePageState();",
            "}",
            "",
            "class _MyHomePageState extends State<MyHomePage> {",
            "  int _counter = 0;",
            "  void _incrementCounter() {",
            "    setState(() {",
            "      _counter++;",
            "    });",
            "  }",
            "  @override",
            "  Widget build(BuildContext context) => const Placeholder();",
            "}",
            ""
        });
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int position = 0;
        while ((position = value.indexOf(needle, position)) >= 0) {
            count++;
            position += needle.length();
        }
        return count;
    }
}
