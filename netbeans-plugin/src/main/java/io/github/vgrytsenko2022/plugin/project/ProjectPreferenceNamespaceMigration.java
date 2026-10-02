package io.github.vgrytsenko2022.plugin.project;

import java.util.List;
import java.util.prefs.Preferences;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.netbeans.spi.project.AuxiliaryProperties;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Retains private run-target choices when the owning NetBeans module is renamed. */
final class ProjectPreferenceNamespaceMigration {
    static final String LEGACY_MODULE = "dev-flutter-netbeans-netbeans-plugin";
    static final String XML_NAMESPACE = "http://www.netbeans.org/ns/auxiliary-configuration-preferences/1";
    private static final String MIGRATED = "packageNamespaceMigrated";
    private static final List<String> KEYS = List.of("selectedDeviceId", "selectedDeviceName",
            "selectedTargetKind", "selectedTargetPreferencesMigrated");

    private ProjectPreferenceNamespaceMigration() { }

    static void migrate(Project project, Preferences current) {
        migrate(current, project.getLookup().lookup(AuxiliaryProperties.class),
                project.getLookup().lookup(AuxiliaryConfiguration.class));
    }

    static void migrate(Preferences current, AuxiliaryProperties properties,
            AuxiliaryConfiguration configuration) {
        if (current.getBoolean(MIGRATED, false)) {
            return;
        }
        if (KEYS.stream().noneMatch(key -> current.get(key, null) != null)) {
            Element fragment = configuration == null ? null
                    : configuration.getConfigurationFragment("preferences", XML_NAMESPACE, false);
            for (String key : KEYS) {
                String value = properties == null ? null
                        : properties.get(LEGACY_MODULE + "." + key, false);
                if (value == null) {
                    value = xmlValue(fragment, key);
                }
                if (value != null) {
                    current.put(key, value);
                }
            }
        }
        current.putBoolean(MIGRATED, true);
    }

    private static String xmlValue(Element root, String key) {
        if (root == null) {
            return null;
        }
        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element module) || !"module".equals(module.getLocalName())
                    || !XML_NAMESPACE.equals(module.getNamespaceURI())
                    || !LEGACY_MODULE.equals(module.getAttribute("name"))) {
                continue;
            }
            for (Node child = module.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child instanceof Element property && "property".equals(property.getLocalName())
                        && XML_NAMESPACE.equals(property.getNamespaceURI())
                        && key.equals(property.getAttribute("name")) && property.hasAttribute("value")) {
                    return property.getAttribute("value");
                }
            }
        }
        return null;
    }
}
