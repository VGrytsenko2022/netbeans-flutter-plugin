package dev.flutter.netbeans.plugin.project;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class FlutterRunControllerMilestoneTest {

    @Test
    void failurePreventsLaterSuccess() {
        FlutterRunController.ActionMilestone milestone =
                new FlutterRunController.ActionMilestone();

        assertTrue(milestone.fail());
        assertFalse(milestone.succeed());
        assertFalse(milestone.isPending());
    }

    @Test
    void successPreventsLaterFailure() {
        FlutterRunController.ActionMilestone milestone =
                new FlutterRunController.ActionMilestone();

        assertTrue(milestone.succeed());
        assertFalse(milestone.fail());
        assertFalse(milestone.isPending());
    }

    @Test
    void concurrentSuccessAndFailureHaveExactlyOneWinner() throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            FlutterRunController.ActionMilestone milestone =
                    new FlutterRunController.ActionMilestone();
            CountDownLatch start = new CountDownLatch(1);
            AtomicBoolean successWon = new AtomicBoolean();
            AtomicBoolean failureWon = new AtomicBoolean();

            Thread success = Thread.ofVirtual().start(() -> {
                await(start);
                successWon.set(milestone.succeed());
            });
            Thread failure = Thread.ofVirtual().start(() -> {
                await(start);
                failureWon.set(milestone.fail());
            });

            start.countDown();
            success.join();
            failure.join();

            assertTrue(successWon.get() ^ failureWon.get());
            assertFalse(milestone.isPending());
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("milestone race test was interrupted", ex);
        }
    }
}
