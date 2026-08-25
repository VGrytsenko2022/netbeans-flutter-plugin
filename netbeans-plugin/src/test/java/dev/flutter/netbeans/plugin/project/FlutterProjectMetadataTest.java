package dev.flutter.netbeans.plugin.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.FlutterProjectInfo;
import dev.flutter.netbeans.plugin.tooling.FlutterToolingController;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.netbeans.spi.project.AuxiliaryProperties;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.LocalFileSystem;
import org.openide.loaders.DataFolder;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class FlutterProjectMetadataTest {
    private static final String OPEN_FILES_NAMESPACE
            = "http://www.netbeans.org/ns/projectui-open-files/2";
    private static final String PREFERENCES_NAMESPACE
            = "http://www.netbeans.org/ns/auxiliary-configuration-preferences/1";
    private static final String BOOKMARKS_NAMESPACE
            = "http://www.netbeans.org/ns/editor-bookmarks/2";
    private static final String ORDERING_LOGGER = "org.openide.filesystems.Ordering";
    private static final String ORDERING_WARNING
            = "Encountered non-boolean relative ordering attribute";
    private static final int MAX_TEST_METADATA_BYTES = 4 * 1024 * 1024;

    @TempDir
    Path temporaryDirectory;

    @Test
    void roundTripsIsolatedFragmentsAndProperties() throws Exception {
        FileObject projectDirectory = createProjectDirectory("metadata_roundtrip");
        FlutterProjectMetadata metadata = new FlutterProjectMetadata(projectDirectory);

        Element privateFragment = fragment(
                "run-state",
                "urn:test:flutter:private",
                "target",
                "windows");
        metadata.putConfigurationFragment(privateFragment, false);

        Element storedPrivate = metadata.getConfigurationFragment(
                "run-state",
                "urn:test:flutter:private",
                false);
        assertNotNull(storedPrivate);
        assertNull(storedPrivate.getParentNode());
        assertEquals("windows", storedPrivate.getAttribute("target"));
        storedPrivate.setAttribute("target", "mutated-copy");
        assertEquals(
                "windows",
                metadata.getConfigurationFragment(
                        "run-state",
                        "urn:test:flutter:private",
                        false).getAttribute("target"));
        assertNull(projectDirectory.getFileObject(".netbeans"),
                "Private metadata must not create files in the Flutter project");
        assertFalse(FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE.contains("/"),
                "The private metadata attribute must not look like a relative ordering key");
        assertNotNull(projectDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));

        Element sharedFragment = fragment(
                "run-state",
                "urn:test:flutter:private",
                "target",
                "chrome");
        metadata.putConfigurationFragment(sharedFragment, true);
        assertEquals(
                "windows",
                metadata.getConfigurationFragment(
                        "run-state",
                        "urn:test:flutter:private",
                        false).getAttribute("target"));
        assertEquals(
                "chrome",
                metadata.getConfigurationFragment(
                        "run-state",
                        "urn:test:flutter:private",
                        true).getAttribute("target"));
        FileObject sharedMetadata = projectDirectory.getFileObject(
                FlutterProjectMetadata.SHARED_METADATA_PATH);
        assertNotNull(sharedMetadata);
        assertFalse(sharedMetadata.asText().contains(projectDirectory.toURI().toASCIIString()),
                "Shared metadata must not expose a machine-local project path");

        metadata.put("selected/device.$", "Pixel 9 — API 36", false);
        metadata.put("selectedTargetKind", "MOBILE", false);
        metadata.put("selectedTargetKind", "WEB", true);
        assertEquals("Pixel 9 — API 36", metadata.get("selected/device.$", false));
        assertEquals("MOBILE", metadata.get("selectedTargetKind", false));
        assertEquals("WEB", metadata.get("selectedTargetKind", true));
        assertEquals(
                List.of("selected/device.$", "selectedTargetKind"),
                iterableToList(metadata.listKeys(false)));

        metadata.put("selected/device.$", null, false);
        assertNull(metadata.get("selected/device.$", false));
        assertEquals(List.of("selectedTargetKind"), iterableToList(metadata.listKeys(false)));
        assertTrue(metadata.removeConfigurationFragment(
                "run-state",
                "urn:test:flutter:private",
                false));
        assertFalse(metadata.removeConfigurationFragment(
                "run-state",
                "urn:test:flutter:private",
                false));
    }

    @Test
    void migratesAllLegacyPrivateFragmentsBeforeLogicalViewEnumeration() throws Exception {
        FileObject projectDirectory = createProjectDirectory("legacy_migration");
        String openFilesAttribute = legacyAttribute(OPEN_FILES_NAMESPACE, "open-files");
        String preferencesAttribute = legacyAttribute(PREFERENCES_NAMESPACE, "preferences");
        String bookmarksAttribute = legacyAttribute(BOOKMARKS_NAMESPACE, "editor-bookmarks");
        String preferencesXml = """
                <preferences xmlns="http://www.netbeans.org/ns/auxiliary-configuration-preferences/1">
                  <module name="dev-flutter-netbeans-netbeans-plugin">
                    <property name="selectedDeviceId" value="windows"/>
                    <property name="selectedTargetKind" value="DESKTOP"/>
                  </module>
                </preferences>
                """;

        projectDirectory.setAttribute(openFilesAttribute, """
                <open-files xmlns="http://www.netbeans.org/ns/projectui-open-files/2">
                  <group><file>file:/sample/lib/main.dart</file></group>
                </open-files>
                """);
        projectDirectory.setAttribute(preferencesAttribute, preferencesXml);
        projectDirectory.setAttribute(bookmarksAttribute, """
                <editor-bookmarks xmlns="http://www.netbeans.org/ns/editor-bookmarks/2"
                                  lastBookmarkId="0"/>
                """);

        FlutterProjectMetadata metadata = new FlutterProjectMetadata(projectDirectory);
        metadata.putConfigurationFragment(fragment(
                "preferences",
                PREFERENCES_NAMESPACE,
                "migrationFixture",
                "stale-private-store-value"), false);
        Path rootPath = FileUtil.toFile(projectDirectory).toPath();
        FlutterProject project = new FlutterProject(
                projectDirectory,
                new TestProjectState(),
                new FlutterProjectInfo(
                        rootPath,
                        "legacy_migration",
                        rootPath.resolve("pubspec.yaml")),
                metadata);
        metadata.migrateLegacyPrivateConfiguration();

        assertNotNull(project.getLookup().lookup(AuxiliaryConfiguration.class));
        assertNotNull(project.getLookup().lookup(AuxiliaryProperties.class));
        assertNull(projectDirectory.getAttribute(openFilesAttribute));
        assertNull(projectDirectory.getAttribute(preferencesAttribute));
        assertNull(projectDirectory.getAttribute(bookmarksAttribute));

        AuxiliaryConfiguration configuration = ProjectUtils.getAuxiliaryConfiguration(project);
        assertNotNull(configuration.getConfigurationFragment(
                "open-files",
                OPEN_FILES_NAMESPACE,
                false));
        Element preferences = configuration.getConfigurationFragment(
                "preferences",
                PREFERENCES_NAMESPACE,
                false);
        assertNotNull(preferences);
        assertEquals(
                "windows",
                ((Element) preferences.getElementsByTagNameNS(
                        PREFERENCES_NAMESPACE,
                        "property").item(0)).getAttribute("value"));
        assertNotNull(configuration.getConfigurationFragment(
                "editor-bookmarks",
                BOOKMARKS_NAMESPACE,
                false));

        preferences.getElementsByTagNameNS(PREFERENCES_NAMESPACE, "property")
                .item(0).getAttributes().getNamedItem("value").setNodeValue("chrome");
        metadata.putConfigurationFragment(preferences, false);
        projectDirectory.setAttribute(preferencesAttribute, preferencesXml);
        metadata.migrateLegacyPrivateConfiguration();
        assertEquals("chrome", selectedDeviceId(metadata),
                "An unchanged leftover fallback must not overwrite newer private state");

        String downgradedPreferences = preferencesXml.replace(
                "value=\"windows\"",
                "value=\"android-emulator\"");
        projectDirectory.setAttribute(preferencesAttribute, downgradedPreferences);
        metadata.migrateLegacyPrivateConfiguration();
        assertEquals("android-emulator", selectedDeviceId(metadata),
                "A fallback changed by an older plugin must win after an upgrade");
        assertNull(projectDirectory.getAttribute(preferencesAttribute));

        List<LogRecord> orderingWarnings = captureOrderingWarnings(() -> {
            LogicalViewProvider logicalView = project.getLookup().lookup(LogicalViewProvider.class);
            assertNotNull(logicalView);
            logicalView.createLogicalView().getChildren().getNodes(true);
        });
        assertTrue(orderingWarnings.isEmpty(), () -> "Unexpected Ordering warnings: "
                + orderingWarnings.stream().map(LogRecord::getMessage).toList());

        FlutterProjectMetadata reloaded = new FlutterProjectMetadata(projectDirectory);
        assertNotNull(reloaded.getConfigurationFragment(
                "preferences",
                PREFERENCES_NAMESPACE,
                false));
        closeProjectServices(project);
    }

    @Test
    void quarantinesMalformedAndUnsupportedLegacyAttributes() throws Exception {
        FileObject projectDirectory = createProjectDirectory("malformed_legacy");
        String attribute = legacyAttribute("urn:test:malformed", "broken");
        String malformedXml = "<broken xmlns=\"urn:test:malformed\">";
        String unsupportedAttribute = legacyAttribute("urn:test:unsupported", "state");
        projectDirectory.setAttribute(attribute, malformedXml);
        projectDirectory.setAttribute(unsupportedAttribute, 42);
        FlutterProjectMetadata metadata = new FlutterProjectMetadata(projectDirectory);

        Logger logger = Logger.getLogger(FlutterProjectMetadata.class.getName());
        boolean previousUseParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        try {
            metadata.migrateLegacyPrivateConfiguration();
        } finally {
            logger.setUseParentHandlers(previousUseParentHandlers);
        }

        assertNull(projectDirectory.getAttribute(attribute));
        String privateMetadata = (String) projectDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE);
        assertNotNull(privateMetadata);
        assertTrue(privateMetadata.contains(encoded(malformedXml)),
                "Malformed legacy XML must remain recoverable in private quarantine");

        assertNull(projectDirectory.getAttribute(unsupportedAttribute));
        String safeQuarantineAttribute = FlutterProjectMetadata.LEGACY_VALUE_QUARANTINE_PREFIX
                + encoded(unsupportedAttribute);
        assertFalse(safeQuarantineAttribute.contains("/"));
        assertEquals(42, projectDirectory.getAttribute(safeQuarantineAttribute));

        List<LogRecord> orderingWarnings = captureOrderingWarnings(() ->
                DataFolder.findFolder(projectDirectory)
                        .getNodeDelegate()
                        .getChildren()
                        .getNodes(true));
        assertTrue(orderingWarnings.isEmpty(), () -> "Quarantined legacy metadata still "
                + "triggered Ordering warnings: "
                + orderingWarnings.stream().map(LogRecord::getMessage).toList());
    }

    @Test
    void quarantinesLargeMalformedLegacyPayloadOutsideTheBoundedContainer()
            throws Exception {
        FileObject projectDirectory = createProjectDirectory("large_malformed_legacy");
        String attribute = legacyAttribute("urn:test:large-malformed", "broken");
        String malformedXml = "<broken xmlns=\"urn:test:large-malformed\">"
                + "x".repeat(MAX_TEST_METADATA_BYTES - 4096);
        assertTrue(malformedXml.getBytes(StandardCharsets.UTF_8).length
                < MAX_TEST_METADATA_BYTES);
        projectDirectory.setAttribute(attribute, malformedXml);
        FlutterProjectMetadata metadata = new FlutterProjectMetadata(projectDirectory);

        Logger logger = Logger.getLogger(FlutterProjectMetadata.class.getName());
        boolean previousUseParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        try {
            metadata.migrateLegacyPrivateConfiguration();
        } finally {
            logger.setUseParentHandlers(previousUseParentHandlers);
        }

        assertNull(projectDirectory.getAttribute(attribute));
        assertEquals(
                List.of(malformedXml),
                externalPayloadQuarantineValues(projectDirectory),
                "A near-limit malformed fallback must remain recoverable without "
                        + "Base64 expansion inside the bounded metadata document");
        assertNull(projectDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));
    }

    @Test
    void failedWritePreservesStoredDataAndLegacyFallback() throws Exception {
        FileObject projectDirectory = createProjectDirectory("failed_write");
        FlutterProjectMetadata metadata = new FlutterProjectMetadata(projectDirectory);
        metadata.put("selectedDeviceId", "windows", false);
        Object originalAttribute = projectDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE);
        assertNotNull(originalAttribute);

        FlutterProjectMetadata failing = new FlutterProjectMetadata(
                projectDirectory,
                (directory, shared, bytes) -> {
                    throw new IOException("simulated metadata write failure");
                });
        assertThrows(
                UncheckedIOException.class,
                () -> failing.put("selectedDeviceId", "chrome", false));
        assertEquals(originalAttribute, projectDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));
        assertEquals("windows", metadata.get("selectedDeviceId", false));

        String attribute = legacyAttribute("urn:test:write-failure", "state");
        String xml = "<state xmlns=\"urn:test:write-failure\" target=\"mobile\"/>";
        projectDirectory.setAttribute(attribute, xml);
        Logger logger = Logger.getLogger(FlutterProjectMetadata.class.getName());
        boolean previousUseParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        try {
            failing.migrateLegacyPrivateConfiguration();
        } finally {
            logger.setUseParentHandlers(previousUseParentHandlers);
        }
        assertEquals(xml, projectDirectory.getAttribute(attribute),
                "A failed destination write must not delete the fallback attribute");
        assertEquals(originalAttribute, projectDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE),
                "A failed migration must preserve the original private metadata attribute");
    }

    @Test
    void corruptPrivateMetadataIsQuarantinedBeforeTheNextWrite() throws Exception {
        FileObject projectDirectory = createProjectDirectory("corrupt_private_metadata");
        String corrupt = "<unexpected-root xmlns=\"urn:test:corrupt\"/>";
        projectDirectory.setAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE,
                corrupt);
        FlutterProjectMetadata metadata = new FlutterProjectMetadata(projectDirectory);

        Logger logger = Logger.getLogger(FlutterProjectMetadata.class.getName());
        boolean previousUseParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        try {
            assertNull(metadata.get("selectedDeviceId", false));
            assertEquals(List.of(), iterableToList(metadata.listKeys(false)));
            metadata.put("selectedDeviceId", "windows", false);
        } finally {
            logger.setUseParentHandlers(previousUseParentHandlers);
        }

        assertEquals("windows", metadata.get("selectedDeviceId", false));
        String recovered = (String) projectDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE);
        assertNotNull(recovered);
        assertFalse(corrupt.equals(recovered));
        assertTrue(recovered.contains(encoded(corrupt)),
                "The corrupt original container must remain recoverable after repair");
    }

    @Test
    void largeCorruptPrivateMetadataIsQuarantinedOutsideTheBoundedContainer()
            throws Exception {
        FileObject projectDirectory = createProjectDirectory("large_corrupt_private_metadata");
        String corrupt = "<unexpected-root xmlns=\"urn:test:large-corrupt\">"
                + "x".repeat(MAX_TEST_METADATA_BYTES - 4096)
                + "</unexpected-root>";
        assertTrue(corrupt.getBytes(StandardCharsets.UTF_8).length
                < MAX_TEST_METADATA_BYTES);
        projectDirectory.setAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE,
                corrupt);
        FlutterProjectMetadata metadata = new FlutterProjectMetadata(projectDirectory);

        Logger logger = Logger.getLogger(FlutterProjectMetadata.class.getName());
        boolean previousUseParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        try {
            metadata.put("selectedDeviceId", "windows", false);
        } finally {
            logger.setUseParentHandlers(previousUseParentHandlers);
        }

        assertEquals("windows", metadata.get("selectedDeviceId", false));
        assertEquals(
                List.of(corrupt),
                externalPayloadQuarantineValues(projectDirectory),
                "The near-limit corrupt primary value must remain recoverable outside "
                        + "the bounded metadata document");
        Object repaired = projectDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE);
        assertTrue(repaired instanceof String);
        assertFalse(corrupt.equals(repaired));
    }

    @Test
    void nonStringCorruptPrivateMetadataDoesNotPermanentlyBlockWrites()
            throws Exception {
        FileObject projectDirectory = createProjectDirectory("non_string_private_metadata");
        projectDirectory.setAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE,
                42);
        FlutterProjectMetadata failing = new FlutterProjectMetadata(
                projectDirectory,
                (directory, shared, bytes) -> {
                    throw new IOException("simulated repair write failure");
                });

        Logger logger = Logger.getLogger(FlutterProjectMetadata.class.getName());
        boolean previousUseParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        try {
            assertThrows(
                    UncheckedIOException.class,
                    () -> failing.put("selectedDeviceId", "windows", false));
        } finally {
            logger.setUseParentHandlers(previousUseParentHandlers);
        }

        assertEquals(42, projectDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE),
                "A failed repair write must not replace the corrupt primary value");
        assertEquals(
                List.of(42),
                externalPayloadQuarantineValues(projectDirectory),
                "The raw value must be durable in quarantine before replacement starts");

        FlutterProjectMetadata repaired = new FlutterProjectMetadata(projectDirectory);
        logger.setUseParentHandlers(false);
        try {
            repaired.put("selectedDeviceId", "windows", false);
        } finally {
            logger.setUseParentHandlers(previousUseParentHandlers);
        }
        assertEquals("windows", repaired.get("selectedDeviceId", false));
        assertTrue(projectDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE) instanceof String);
        assertEquals(List.of(42), externalPayloadQuarantineValues(projectDirectory));
    }

    @Test
    void sharedWriteRejectsMetadataDirectorySymlinkOutsideProject() throws Exception {
        FileObject projectDirectory = createProjectDirectory("shared_symlink_escape");
        Path projectPath = FileUtil.toFile(projectDirectory).toPath();
        Path outside = Files.createDirectory(temporaryDirectory.resolve("outside-shared-store"));
        Path link = projectPath.resolve(".netbeans");
        boolean symlinkCreated;
        try {
            Files.createSymbolicLink(link, outside);
            symlinkCreated = true;
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            symlinkCreated = false;
        }
        Assumptions.assumeTrue(
                symlinkCreated,
                "Symbolic-link creation is unavailable on this Windows installation");

        FlutterProjectMetadata metadata = new FlutterProjectMetadata(projectDirectory);
        assertThrows(
                UncheckedIOException.class,
                () -> metadata.put("selectedTargetKind", "WEB", true));
        assertFalse(Files.exists(outside.resolve("flutter-metadata.xml")),
                "A shared metadata write must not escape the Flutter project root");
    }

    @Test
    void removingTheLastPrivateValueClearsTheRootAttribute() throws Exception {
        FileObject projectDirectory = createProjectDirectory("empty_private_metadata");
        FlutterProjectMetadata metadata = new FlutterProjectMetadata(projectDirectory);
        metadata.put("selectedDeviceId", "windows", false);
        assertNotNull(projectDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));

        metadata.put("selectedDeviceId", null, false);

        assertNull(projectDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));
        List<LogRecord> orderingWarnings = captureOrderingWarnings(() ->
                DataFolder.findFolder(projectDirectory)
                        .getNodeDelegate()
                        .getChildren()
                        .getNodes(true));
        assertTrue(orderingWarnings.isEmpty());
    }

    @Test
    void privateStateFollowsRenameAndMoveButNotDeleteAndRecreate() throws Exception {
        FileObject projectDirectory = createProjectDirectory("identity_lifecycle");
        FlutterProjectMetadata metadata = new FlutterProjectMetadata(projectDirectory);
        metadata.put("selectedDeviceId", "windows", false);
        metadata.putConfigurationFragment(fragment(
                "run-state",
                "urn:test:flutter:identity",
                "target",
                "windows"), false);
        Object originalAttribute = projectDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE);
        assertNotNull(originalAttribute);

        FileObject parent = projectDirectory.getParent();
        try (FileLock lock = projectDirectory.lock()) {
            projectDirectory.rename(lock, "renamed_flutter_app", null);
        }
        FileObject renamed = parent.getFileObject("renamed_flutter_app");
        assertNotNull(renamed);
        assertEquals(originalAttribute, renamed.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));
        FlutterProjectMetadata renamedMetadata = new FlutterProjectMetadata(renamed);
        assertEquals("windows", renamedMetadata.get("selectedDeviceId", false));

        FileObject destination = FileUtil.createFolder(parent, "moved_projects");
        FileObject moved;
        try (FileLock lock = renamed.lock()) {
            moved = renamed.move(lock, destination, "moved_flutter_app", null);
        }
        assertEquals(originalAttribute, moved.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));
        FlutterProjectMetadata movedMetadata = new FlutterProjectMetadata(moved);
        assertEquals("windows", movedMetadata.get("selectedDeviceId", false));
        assertEquals(
                "windows",
                movedMetadata.getConfigurationFragment(
                        "run-state",
                        "urn:test:flutter:identity",
                        false).getAttribute("target"));

        String deletedPath = moved.getPath();
        moved.delete();
        FileObject recreated = FileUtil.createFolder(destination, "moved_flutter_app");
        assertEquals(deletedPath, recreated.getPath());
        assertNull(recreated.getAttribute(FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));
        FlutterProjectMetadata recreatedMetadata = new FlutterProjectMetadata(recreated);
        assertNull(recreatedMetadata.get("selectedDeviceId", false));
        assertNull(recreatedMetadata.getConfigurationFragment(
                "run-state",
                "urn:test:flutter:identity",
                false));
    }

    private FileObject createProjectDirectory(String name) throws Exception {
        Path root = temporaryDirectory.resolve(name);
        Files.createDirectories(root.resolve("lib"));
        Files.writeString(root.resolve("pubspec.yaml"), """
                name: %s
                environment:
                  sdk: '>=3.0.0 <4.0.0'
                flutter:
                """.formatted(name));
        Files.writeString(root.resolve("lib/main.dart"), "void main() {}\n");
        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(temporaryDirectory.toFile());
        return fileSystem.getRoot().getFileObject(name);
    }

    private static Element fragment(
            String elementName,
            String namespace,
            String attributeName,
            String attributeValue) {
        Document document = XMLUtil.createDocument(elementName, namespace, null, null);
        Element fragment = document.getDocumentElement();
        fragment.setAttribute(attributeName, attributeValue);
        return fragment;
    }

    private static String legacyAttribute(String namespace, String elementName) {
        return FlutterProjectMetadata.LEGACY_ATTRIBUTE_PREFIX
                + namespace + "#" + elementName;
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> iterableToList(Iterable<String> values) {
        List<String> result = new ArrayList<>();
        values.forEach(result::add);
        return result;
    }

    private static List<Object> externalPayloadQuarantineValues(FileObject directory) {
        List<Object> result = new ArrayList<>();
        var attributes = directory.getAttributes();
        while (attributes.hasMoreElements()) {
            String attribute = attributes.nextElement();
            if (attribute.startsWith(
                    FlutterProjectMetadata.EXTERNAL_PAYLOAD_QUARANTINE_PREFIX)) {
                result.add(directory.getAttribute(attribute));
            }
        }
        return result;
    }

    private static String selectedDeviceId(FlutterProjectMetadata metadata) {
        Element preferences = metadata.getConfigurationFragment(
                "preferences",
                PREFERENCES_NAMESPACE,
                false);
        var properties = preferences.getElementsByTagNameNS(
                PREFERENCES_NAMESPACE,
                "property");
        for (int index = 0; index < properties.getLength(); index++) {
            Element property = (Element) properties.item(index);
            if ("selectedDeviceId".equals(property.getAttribute("name"))) {
                return property.getAttribute("value");
            }
        }
        return null;
    }

    private static List<LogRecord> captureOrderingWarnings(CheckedRunnable action)
            throws Exception {
        Logger logger = Logger.getLogger(ORDERING_LOGGER);
        Level previousLevel = logger.getLevel();
        boolean previousUseParentHandlers = logger.getUseParentHandlers();
        List<LogRecord> warnings = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getMessage() != null
                        && record.getMessage().startsWith(ORDERING_WARNING)) {
                    warnings.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(Level.ALL);
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        try {
            action.run();
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(previousUseParentHandlers);
            logger.setLevel(previousLevel);
        }
        return warnings;
    }

    private static void closeProjectServices(FlutterProject project) {
        FlutterRunController runController = project.getLookup().lookup(FlutterRunController.class);
        if (runController != null) {
            runController.close();
        }
        FlutterToolingController toolingController = project.getLookup()
                .lookup(FlutterToolingController.class);
        if (toolingController != null) {
            toolingController.close();
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }

    private static final class TestProjectState implements ProjectState {
        @Override
        public void markModified() {
        }

        @Override
        public void notifyDeleted() {
        }
    }
}
