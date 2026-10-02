package io.github.vgrytsenko2022.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import static org.junit.jupiter.api.Assertions.*;

class ProjectNamespaceContractTest {
    private static final String GROUP = "io.github.vgrytsenko2022";

    @Test void allModuleCoordinatesAndJavaSourcePathsAgreeWithTheRootNamespace() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        if (Files.isRegularFile(root.resolve("../pom.xml"))) { root = root.getParent(); }
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element parent = factory.newDocumentBuilder().parse(root.resolve("pom.xml").toFile()).getDocumentElement();
        assertEquals(GROUP, child(parent, "groupId"));
        String artifact = child(parent, "artifactId"), version = child(parent, "version");
        assertEquals("netbeans-flutter-plugin", artifact);
        var modules = parent.getElementsByTagName("module");
        for (int i = 0; i < modules.getLength(); i++) {
            Path module = root.resolve(modules.item(i).getTextContent().strip());
            Element pom = factory.newDocumentBuilder().parse(module.resolve("pom.xml").toFile()).getDocumentElement();
            Element inherited = (Element) pom.getElementsByTagName("parent").item(0);
            assertEquals(GROUP, child(inherited, "groupId"), module.toString());
            assertEquals(artifact, child(inherited, "artifactId"), module.toString());
            assertEquals(version, child(inherited, "version"), module.toString());
            if (child(pom, "groupId") != null) { assertEquals(GROUP, child(pom, "groupId")); }
            if (child(pom, "version") != null) { assertEquals(version, child(pom, "version")); }
            for (String source : new String[]{"src/main/java", "src/test/java"}) {
                Path sources = module.resolve(source);
                if (!Files.isDirectory(sources)) { continue; }
                try (var paths = Files.walk(sources)) {
                    for (Path file : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                        String text = Files.readString(file);
                        var declaration = java.util.regex.Pattern.compile("(?m)^package\\s+([\\w.]+)\\s*;").matcher(text);
                        assertTrue(declaration.find(), file.toString());
                        String name = declaration.group(1);
                        assertTrue(name.startsWith(GROUP + "."), file.toString());
                        assertEquals(sources.resolve(name.replace('.', '/')), file.getParent(), file.toString());
                    }
                }
            }
        }
    }

    private static String child(Element element, String name) {
        for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element value && name.equals(value.getTagName())) {
                return value.getTextContent().strip();
            }
        }
        return null;
    }
}
