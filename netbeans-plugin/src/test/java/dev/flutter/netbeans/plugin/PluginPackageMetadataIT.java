package dev.flutter.netbeans.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

class PluginPackageMetadataIT {
    private static final String NAME = "Flutter and Dart Support";
    private static final String CATEGORY = "Flutter";
    private static final String SHORT_DESCRIPTION =
            "Develop Dart and Flutter applications in Apache NetBeans.";
    private static final String LONG_DESCRIPTION =
            "Adds Dart editing, analysis, completion, navigation and formatting together with "
                    + "Flutter project creation, execution, device management, testing and debugging "
                    + "to Apache NetBeans 30.";

    @Test
    void exposesCompletePluginManagerMetadata() throws Exception {
        Path nbm = requiredPath("nbm.file");
        String mavenVersion = requiredProperty("maven.version");
        Document info = readInfo(nbm);
        Element module = info.getDocumentElement();
        Element manifest = firstElement(module, "manifest");
        Element license = firstElement(module, "license");

        assertEquals("netbeans-plugin-" + mavenVersion + ".nbm", module.getAttribute("distribution"));
        assertEquals(NAME, manifest.getAttribute("OpenIDE-Module-Name"));
        assertEquals(CATEGORY, manifest.getAttribute("OpenIDE-Module-Display-Category"));
        assertEquals(SHORT_DESCRIPTION,
                manifest.getAttribute("OpenIDE-Module-Short-Description"));
        assertEquals(LONG_DESCRIPTION,
                manifest.getAttribute("OpenIDE-Module-Long-Description"));

        String specificationVersion = mavenVersion.replaceFirst("-SNAPSHOT$", "");
        assertEquals(specificationVersion,
                manifest.getAttribute("OpenIDE-Module-Specification-Version"));
        String implementationVersion = manifest.getAttribute("OpenIDE-Module-Implementation-Version");
        if (mavenVersion.endsWith("-SNAPSHOT")) {
            assertTrue(Pattern.matches(
                    Pattern.quote(specificationVersion) + "-\\d{8}", implementationVersion),
                    () -> "unexpected snapshot implementation version: " + implementationVersion);
        } else {
            assertEquals(specificationVersion, implementationVersion);
        }

        assertEquals(module.getAttribute("license"), license.getAttribute("name"));
        String packagedLicense = normalizeNewlines(license.getTextContent());
        String sourceLicense = normalizeNewlines(Files.readString(
                requiredPath("source.license"), StandardCharsets.UTF_8)).stripTrailing();
        assertTrue(packagedLicense.contains(sourceLicense), "NBM must contain the complete LICENSE text");
        assertTrue(packagedLicense.contains("Apache License"));
        assertTrue(packagedLicense.contains("Version 2.0"));
        assertFalse(packagedLicense.contains("Unknown"));
        var attributes = manifest.getAttributes();
        for (int index = 0; index < attributes.getLength(); index++) {
            String attributeName = attributes.item(index).getNodeName();
            String attributeValue = attributes.item(index).getNodeValue();
            assertFalse(
                    attributeValue.contains("<undefined>"),
                    () -> "undefined manifest metadata: " + attributeName);
        }
    }

    private static Document readInfo(Path nbm) throws Exception {
        try (ZipFile zip = new ZipFile(nbm.toFile())) {
            ZipEntry entry = zip.getEntry("Info/info.xml");
            assertNotNull(entry, "NBM is missing Info/info.xml");
            try (InputStream input = zip.getInputStream(entry)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                var builder = factory.newDocumentBuilder();
                builder.setEntityResolver((publicId, systemId) ->
                        new InputSource(new StringReader("")));
                return builder.parse(input);
            }
        }
    }

    private static Element firstElement(Element parent, String tagName) {
        Element element = (Element) parent.getElementsByTagName(tagName).item(0);
        assertNotNull(element, "Info/info.xml is missing <" + tagName + ">");
        return element;
    }

    private static Path requiredPath(String name) throws IOException {
        Path path = Path.of(requiredProperty(name));
        assertTrue(Files.isRegularFile(path), () -> name + " does not point to a file: " + path);
        return path;
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        assertNotNull(value, "missing system property: " + name);
        assertFalse(value.isBlank(), "blank system property: " + name);
        return value;
    }

    private static String normalizeNewlines(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }
}
