package io.github.vgrytsenko2022.plugin.dart.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DartClassNamingTest {
    @Test
    void convertsUpperCamelCaseToDartFileName() {
        assertEquals("order_repository.dart", DartClassNaming.fileName("OrderRepository"));
        assertEquals("http_client.dart", DartClassNaming.fileName("HTTPClient"));
        assertEquals("_private_service.dart", DartClassNaming.fileName("_PrivateService"));
    }

    @Test
    void validatesStyleAndGeneratesClass() {
        assertTrue(DartClassNaming.isValidClassName("OrderRepository"));
        assertTrue(DartClassNaming.isValidClassName("_OrderRepository2"));
        assertFalse(DartClassNaming.isValidClassName("order_repository"));
        assertFalse(DartClassNaming.isValidClassName("Order_Repository"));
        assertEquals("""
                class OrderRepository {
                  const OrderRepository();
                }
                """, DartClassNaming.source("OrderRepository"));
        assertThrows(IllegalArgumentException.class, () -> DartClassNaming.source("bad-name"));
    }

    @Test
    void acceptsOnlySafeProjectRelativeLocations() throws Exception {
        assertEquals("lib/models", DartClassWizardIterator.normalizeLocation("./lib\\models"));
        assertThrows(IOException.class, () -> DartClassWizardIterator.normalizeLocation("lib//models"));
        assertThrows(IOException.class, () -> DartClassWizardIterator.normalizeLocation("lib/./models"));
        assertThrows(IOException.class, () -> DartClassWizardIterator.normalizeLocation("../lib"));
        assertThrows(IOException.class, () -> DartClassWizardIterator.normalizeLocation("C:\\tmp"));
        assertThrows(IOException.class, () -> DartClassWizardIterator.normalizeLocation("build/generated"));
        assertThrows(IOException.class, () -> DartClassWizardIterator.normalizeLocation(".dart_tool"));
    }

    @Test
    void refusesAFileWhereAParentFolderIsRequired(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("lib"), "not a directory");

        assertThrows(
                IOException.class,
                () -> DartClassWizardIterator.resolveLocation(root, "lib/models"));
    }
}
