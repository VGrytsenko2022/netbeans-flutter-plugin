package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.core.spi.multiview.CloseOperationHandler;

class FlutterDesignerAsyncCloseOperationHandlerRegistrationTest {

    @Test
    void productionMimeLookupKeepsAsyncHandlerDormantUntilSplitGateExists() {
        var handlers = MimeLookup.getLookup(FlutterDesignerMime.MIME_TYPE)
                .lookupAll(CloseOperationHandler.class);

        assertFalse(handlers.stream().anyMatch(
                FlutterDesignerAsyncCloseOperationHandler.class::isInstance),
                "the static MIME lookup must not change native close policy before "
                + "split, clone-close and direct-close paths have a peer-removal gate");
    }
}
