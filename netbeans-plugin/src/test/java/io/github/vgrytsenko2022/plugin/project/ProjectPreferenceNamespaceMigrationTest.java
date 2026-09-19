package io.github.vgrytsenko2022.plugin.project;

import java.util.UUID;
import java.util.prefs.Preferences;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openide.filesystems.LocalFileSystem;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Element;
import static org.junit.jupiter.api.Assertions.*;

class ProjectPreferenceNamespaceMigrationTest {
    private final Preferences current = Preferences.userRoot().node("/namespace-test/" + UUID.randomUUID());
    @TempDir Path directory;
    private FlutterProjectMetadata metadata;

    @BeforeEach void setup() throws Exception {
        LocalFileSystem filesystem = new LocalFileSystem();
        filesystem.setRootDirectory(directory.toFile());
        metadata = new FlutterProjectMetadata(filesystem.getRoot());
    }

    @AfterEach void cleanup() throws Exception { current.removeNode(); }

    @Test void importsPrivatePropertiesOnceWithoutRemovingOldValues() {
        String key = ProjectPreferenceNamespaceMigration.LEGACY_MODULE + ".selectedDeviceId";
        metadata.put(key, "windows", false);
        assertEquals("windows", metadata.get(key, false));
        ProjectPreferenceNamespaceMigration.migrate(current, metadata, metadata);
        assertEquals("windows", current.get("selectedDeviceId", null));
        assertEquals("windows", metadata.get(key, false));
        current.remove("selectedDeviceId");
        ProjectPreferenceNamespaceMigration.migrate(current, metadata, metadata);
        assertNull(current.get("selectedDeviceId", null));
    }

    @Test void readsLegacyAuxiliaryConfigurationAndPrivatePropertiesTakePrecedence() {
        String ns = ProjectPreferenceNamespaceMigration.XML_NAMESPACE;
        var document = XMLUtil.createDocument("preferences", ns, null, null);
        Element module = document.createElementNS(ns, "module");
        module.setAttribute("name", ProjectPreferenceNamespaceMigration.LEGACY_MODULE);
        document.getDocumentElement().appendChild(module);
        for (String key : new String[]{"selectedDeviceId", "selectedDeviceName"}) {
            Element value = document.createElementNS(ns, "property");
            value.setAttribute("name", key);
            value.setAttribute("value", "xml-value");
            module.appendChild(value);
        }
        metadata.putConfigurationFragment(document.getDocumentElement(), false);
        metadata.put(ProjectPreferenceNamespaceMigration.LEGACY_MODULE + ".selectedDeviceId", "private-value", false);
        ProjectPreferenceNamespaceMigration.migrate(current, metadata, metadata);
        assertEquals("private-value", current.get("selectedDeviceId", null));
        assertEquals("xml-value", current.get("selectedDeviceName", null));
    }

    @Test void preservesNewChoicesAndNeverImportsSharedSettings() {
        metadata.put(ProjectPreferenceNamespaceMigration.LEGACY_MODULE + ".selectedDeviceId", "shared", true);
        ProjectPreferenceNamespaceMigration.migrate(current, metadata, metadata);
        assertNull(current.get("selectedDeviceId", null));
        current.remove("packageNamespaceMigrated");
        current.put("selectedDeviceId", "new-choice");
        metadata.put(ProjectPreferenceNamespaceMigration.LEGACY_MODULE + ".selectedDeviceName", "old-name", false);
        ProjectPreferenceNamespaceMigration.migrate(current, metadata, metadata);
        assertEquals("new-choice", current.get("selectedDeviceId", null));
        assertNull(current.get("selectedDeviceName", null));
    }
}
