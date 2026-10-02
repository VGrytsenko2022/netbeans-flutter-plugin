package io.github.vgrytsenko2022.plugin.device;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FlutterDeviceManagerRegistrationTest {

    @Test
    void registersDeviceManagerWindowAndFlutterMenuAction() throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");

        assertTrue(layer.contains("FlutterDeviceManagerTopComponent"));
        assertTrue(layer.contains("CTL_FlutterDeviceManagerAction"));
        assertTrue(layer.contains("<folder name=\"Flutter\">"));
        assertTrue(layer.contains("intvalue=\"130\" name=\"position\""));
    }

    private static String readResource(String name) throws IOException {
        try (InputStream input = FlutterDeviceManagerRegistrationTest.class
                .getClassLoader()
                .getResourceAsStream(name)) {
            assertNotNull(input, "missing generated registration: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
