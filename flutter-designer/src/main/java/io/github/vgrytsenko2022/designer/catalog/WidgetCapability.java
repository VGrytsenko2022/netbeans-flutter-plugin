package io.github.vgrytsenko2022.designer.catalog;

/**
 * One independently reviewed Designer capability for a widget definition.
 *
 * <p>Capabilities are deliberately finer grained than "supported" and cover
 * only the interactive Designer surfaces enforced by this gate. Catalog read
 * and deterministic generation remain separate generic contracts; creation,
 * editing and native Canvas participation stay fail-closed until their whole
 * vertical slice has been reviewed.</p>
 */
public enum WidgetCapability {
    /** Exposes typed read/write entries in the NetBeans Properties window. */
    PROPERTIES,
    /** Projects the exact reviewed schema to the isolated Flutter renderer. */
    CANVAS,
    /** Exposes the widget as a creation candidate in Designer UI. */
    CREATE,
    /**
     * Allows Palette-prototype insertion through tree or Canvas DnD. Existing
     * widget moves inside the tree remain generic catalog-planned operations.
     */
    DND
}
