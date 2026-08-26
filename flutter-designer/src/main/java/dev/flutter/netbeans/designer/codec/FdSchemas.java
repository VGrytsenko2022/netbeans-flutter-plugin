package dev.flutter.netbeans.designer.codec;

import java.io.InputStream;

/** Runtime locations of bundled, non-networked Flutter Designer schemas. */
final class FdSchemas {
    static final String V1_RESOURCE =
            "META-INF/netbeans-flutter-designer/schema/fd-v1.schema.json";

    private FdSchemas() {
    }

    static InputStream openV1() {
        InputStream input = FdSchemas.class.getClassLoader().getResourceAsStream(V1_RESOURCE);
        if (input == null) {
            throw new IllegalStateException("Missing bundled Flutter Designer schema: " + V1_RESOURCE);
        }
        return input;
    }
}
