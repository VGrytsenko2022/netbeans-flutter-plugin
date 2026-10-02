package io.github.vgrytsenko2022.plugin.pubspec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;

/** Small, package-private facade over SnakeYAML's representation tree. */
final class PubspecYaml {
    private static final LoadSettings SETTINGS = LoadSettings.builder()
            .setLabel("pubspec.yaml")
            .setAllowDuplicateKeys(false)
            .setAllowRecursiveKeys(false)
            .setMaxAliasesForCollections(32)
            .setCodePointLimit(PubspecValidator.MAX_DOCUMENT_LENGTH)
            .setUseMarks(true)
            .build();

    private PubspecYaml() {
    }

    static Optional<Node> compose(String source) {
        return new Compose(SETTINGS).composeString(source);
    }

    static Map<String, Entry> entries(MappingNode mapping) {
        Map<String, Entry> entries = new LinkedHashMap<>();
        for (NodeTuple tuple : mapping.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode key) {
                entries.put(key.getValue(), new Entry(key, tuple.getValueNode()));
            }
        }
        return entries;
    }

    static String scalar(Node node) {
        return node instanceof ScalarNode scalar ? scalar.getValue() : null;
    }

    record Entry(ScalarNode key, Node value) {
    }
}
