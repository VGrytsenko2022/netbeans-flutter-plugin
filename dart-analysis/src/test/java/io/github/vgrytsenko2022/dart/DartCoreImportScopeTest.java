package io.github.vgrytsenko2022.dart;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DartCoreImportScopeTest {
    @Test
    void detectsCoreDirectivesWithCombinatorsCommentsAndDecodedUriLiterals() {
        for (String source : List.of(
                "import 'dart:core';",
                "import \"dart:core\" as core show bool;",
                "import /* nested /* comment */ valid */ r'dart:core' hide String;",
                "import 'dart:\\u0063ore';",
                "import 'dart:\\u{63}ore';",
                "import 'dart:\\x63ore';",
                "import '''dart:core''';",
                "import '''\ndart:core''' as core;",
                "import r''' \t\r\ndart:core''' as core;",
                "import '''\\\ndart:core''' as core;",
                "import 'dart:' /* split */ r'core' as core;",
                "import 'dart:' '''\ncore''' as core;",
                "import 'dart:core' if (dart.library.io) 'dart:math' as selected;",
                "#!/usr/bin/dart\nlibrary example;\nimport 'dart:core' as core;")) {
            assertTrue(DartCoreImportScope.hasExplicitCoreImport(source), source);
        }
    }

    @Test
    void ignoresCommentsStringsExportsAndCoreLikeLibraryNames() {
        for (String source : List.of(
                "// import 'dart:core';\nvoid main() {}",
                "/* outer /* import 'dart:core'; */ end */ void main() {}",
                "const value = \"import 'dart:core';\";",
                "const value = r\"import 'dart:core';\";",
                "const value = '''import 'dart:core';''';",
                "export 'dart:core';",
                "part 'dart:core';",
                "import 'dart:core_extra';",
                "import 'dart:core' '_extra';",
                "import ''' dart:core''';",
                "import '''\n dart:core''';",
                "import 'dart:math' if (dart.library.io) 'dart:core' as selected;",
                "const value = '$' + \"{import 'dart:core';}\";")) {
            assertFalse(DartCoreImportScope.hasExplicitCoreImport(source), source);
        }
        String interpolation = "const value = '$" + "{[\"import 'dart:core';\", {'nested': r\"import 'dart:core';\"}]}';";
        assertFalse(DartCoreImportScope.hasExplicitCoreImport(interpolation), interpolation);
        assertTrue(DartCoreImportScope.hasExplicitCoreImport(interpolation + "\nimport 'dart:core';"));
    }

    @Test
    void refusesAmbiguousUnterminatedSource() {
        for (String source : List.of("/* unclosed", "const value = 'unclosed", "import 'dart:\\uZZZZore';")) {
            assertThrows(IllegalArgumentException.class, () -> DartCoreImportScope.hasExplicitCoreImport(source));
        }
    }
}
