package dev.flutter.netbeans.designer.codec;

import java.io.InputStream;

/** Runtime locations of bundled, non-networked Flutter Designer schemas. */
final class FdSchemas {
    static final String V1_RESOURCE =
            "META-INF/netbeans-flutter-designer/schema/fd-v1.schema.json";
    static final String V2_RESOURCE =
            "META-INF/netbeans-flutter-designer/schema/fd-v2.schema.json";
    static final String V3_RESOURCE =
            "META-INF/netbeans-flutter-designer/schema/fd-v3.schema.json";
    static final String V4_RESOURCE =
            "META-INF/netbeans-flutter-designer/schema/fd-v4.schema.json";
    static final String CURRENT_RESOURCE = V4_RESOURCE;

    private FdSchemas() {
    }

    static InputStream openV1() {
        return open(V1_RESOURCE);
    }

    static InputStream openV2() {
        return open(V2_RESOURCE);
    }

    static InputStream openV3() {
        return open(V3_RESOURCE);
    }

    static InputStream openV4() {
        return open(V4_RESOURCE);
    }

    static InputStream openCurrent() {
        return open(CURRENT_RESOURCE);
    }

    private static InputStream open(String resource) {
        InputStream input = FdSchemas.class.getClassLoader().getResourceAsStream(resource);
        if (input == null) {
            throw new IllegalStateException("Missing bundled Flutter Designer schema: " + resource);
        }
        return input;
    }
}
