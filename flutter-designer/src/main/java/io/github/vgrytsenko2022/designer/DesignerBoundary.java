package io.github.vgrytsenko2022.designer;

/** Marks the NetBeans-independent boundary of the Flutter Designer domain. */
public final class DesignerBoundary {
    private DesignerBoundary() { }

    public static String milestone() {
        return "M4: prepared pairs and a fail-closed NetBeans pair-save edge";
    }
}
