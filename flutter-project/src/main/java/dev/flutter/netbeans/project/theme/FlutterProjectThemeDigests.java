package dev.flutter.netbeans.project.theme;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Canonical digest operations shared by provisioning and theme consumers. */
public final class FlutterProjectThemeDigests {
    private static final HexFormat UPPERCASE_HEX = HexFormat.of().withUpperCase();

    private FlutterProjectThemeDigests() {
    }

    public static String sha256(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        try {
            return UPPERCASE_HEX.formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError("The Java runtime does not provide SHA-256", ex);
        }
    }
}
