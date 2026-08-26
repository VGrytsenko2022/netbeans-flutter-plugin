package dev.flutter.netbeans.designer.canvas.protocol;

/** Stable constants shared by the version 1 Canvas control-message codec. */
public final class CanvasWireProtocol {
    public static final String FORMAT = "netbeans-flutter-canvas-wire";
    public static final int VERSION = 1;

    /** Largest interoperable exact integer in common JSON implementations. */
    public static final long MAX_SEQUENCE = 9_007_199_254_740_991L;

    private CanvasWireProtocol() {
    }

    static long requireSequence(long value, String name) {
        if (value < 0 || value > MAX_SEQUENCE) {
            throw new IllegalArgumentException(
                    name + " must be between zero and " + MAX_SEQUENCE);
        }
        return value;
    }
}
