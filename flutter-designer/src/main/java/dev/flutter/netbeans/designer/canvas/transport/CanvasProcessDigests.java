package dev.flutter.netbeans.designer.canvas.transport;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class CanvasProcessDigests {
    private CanvasProcessDigests() {
    }

    static byte[] sha256(byte[] payload) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(payload);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(
                    "The Java runtime does not provide SHA-256", impossible);
        }
    }
}
