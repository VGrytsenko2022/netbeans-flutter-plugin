package dev.flutter.netbeans.plugin.dart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.text.BadLocationException;
import org.junit.jupiter.api.Test;
import org.netbeans.api.editor.document.AtomicLockDocument;
import org.netbeans.api.editor.document.AtomicLockEvent;
import org.netbeans.api.editor.document.AtomicLockListener;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.editor.BaseDocument;

class DartTypingHooksTest {
    @Test
    void runsEditorChangesInsideAnAtomicUserEdit() {
        BaseDocument document = new BaseDocument(false, DartTokenId.MIME_TYPE);
        AtomicLockDocument atomicDocument = LineDocumentUtils.asRequired(
                document, AtomicLockDocument.class);
        AtomicBoolean locked = new AtomicBoolean();
        AtomicBoolean editObservedLock = new AtomicBoolean();
        AtomicInteger lockEvents = new AtomicInteger();
        AtomicInteger unlockEvents = new AtomicInteger();
        AtomicLockListener listener = new AtomicLockListener() {
            @Override
            public void atomicLock(AtomicLockEvent event) {
                locked.set(true);
                lockEvents.incrementAndGet();
            }

            @Override
            public void atomicUnlock(AtomicLockEvent event) {
                locked.set(false);
                unlockEvents.incrementAndGet();
            }
        };
        atomicDocument.addAtomicLockListener(listener);
        try {
            DartTypingHooks.runAtomicUserEdit(document, () -> {
                editObservedLock.set(locked.get());
                try {
                    document.insertString(0, "  }", null);
                } catch (BadLocationException ex) {
                    throw new AssertionError(ex);
                }
            });
        } finally {
            atomicDocument.removeAtomicLockListener(listener);
        }

        assertTrue(editObservedLock.get());
        assertFalse(locked.get());
        assertEquals(1, lockEvents.get());
        assertEquals(1, unlockEvents.get());
        assertEquals(3, document.getLength());
    }
}
