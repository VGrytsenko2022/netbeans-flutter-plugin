package io.github.vgrytsenko2022.plugin.designer;

import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;

/** Standard NetBeans Dart editor exposed as the Source designer perspective. */
@MultiViewElement.Registration(
        mimeType = FlutterDesignerMime.MIME_TYPE,
        persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
        displayName = "Source",
        preferredID = "flutter.designer.source",
        position = 200)
public final class FlutterDesignerMultiViewSource extends MultiViewEditorElement {
    private final UndoRedo undoRedo;

    public FlutterDesignerMultiViewSource(Lookup context) {
        super(context);
        undoRedo = resolveUndoRedo(context);
        FlutterDesignerEditorSupport editor = context.lookup(
                FlutterDesignerEditorSupport.class);
        if (editor != null) {
            editor.prepareDocument();
        }
    }

    @Override
    public UndoRedo getUndoRedo() {
        return undoRedo == null ? super.getUndoRedo() : undoRedo;
    }

    static UndoRedo resolveUndoRedo(Lookup context) {
        FlutterDesignerDataObject dataObject = context.lookup(
                FlutterDesignerDataObject.class);
        return dataObject == null
                ? null : dataObject.getCombinedUndoRedo();
    }
}
