package io.github.vgrytsenko2022.plugin.project;

import io.github.vgrytsenko2022.plugin.options.FlutterOptionsPanelController;
import java.awt.EventQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.awt.Notification;
import org.openide.awt.NotificationDisplayer;
import org.openide.awt.StatusDisplayer;

/** Presents Dart analysis lifecycle events through standard NetBeans UI surfaces. */
final class NetBeansDartAnalysisStatusReporter implements DartAnalysisStatusReporter {
    private static final int RUNNING_MESSAGE_MILLIS = 5_000;
    private static final int FAILURE_MESSAGE_MILLIS = 10_000;

    private final AtomicLong revision = new AtomicLong();
    private StatusDisplayer.Message statusMessage;
    private Notification failureNotification;

    @Override
    public void starting(String projectName, boolean restarting) {
        dispatch(() -> {
            clearFailureNotification();
            showStatus((restarting ? "Restarting" : "Starting")
                    + " Dart Analysis Server for " + projectName + "...", 0);
        });
    }

    @Override
    public void running(String projectName, boolean restarted) {
        dispatch(() -> {
            clearFailureNotification();
            showStatus("Dart Analysis Server process "
                    + (restarted ? "restarted" : "started")
                    + " for " + projectName + ".", RUNNING_MESSAGE_MILLIS);
        });
    }

    @Override
    public void failed(String projectName, String reason, boolean notifyUser) {
        String detail = normalizedReason(reason)
                + " Click to open Tools > Options > Flutter.";
        dispatch(() -> {
            showStatus("Dart Analysis Server failed for " + projectName + ": "
                    + normalizedReason(reason), FAILURE_MESSAGE_MILLIS);
            clearFailureNotification();
            if (notifyUser) {
                failureNotification = NotificationDisplayer.getDefault().notify(
                        "Dart Analysis Server unavailable: " + projectName,
                        FlutterProjectFactory.projectIcon(),
                        detail,
                        event -> OptionsDisplayer.getDefault().open(
                                FlutterOptionsPanelController.ID),
                        NotificationDisplayer.Priority.HIGH,
                        NotificationDisplayer.Category.ERROR);
            }
        });
    }

    @Override
    public void clear() {
        dispatch(() -> {
            clearStatusMessage();
            clearFailureNotification();
        });
    }

    private void dispatch(Runnable update) {
        long requestedRevision = revision.incrementAndGet();
        EventQueue.invokeLater(() -> {
            if (revision.get() == requestedRevision) {
                update.run();
            }
        });
    }

    private void showStatus(String message, int clearAfterMillis) {
        clearStatusMessage();
        statusMessage = StatusDisplayer.getDefault().setStatusText(
                message,
                StatusDisplayer.IMPORTANCE_ANNOTATION);
        if (clearAfterMillis > 0) {
            statusMessage.clear(clearAfterMillis);
        }
    }

    private void clearStatusMessage() {
        if (statusMessage != null) {
            statusMessage.clear(0);
            statusMessage = null;
        }
    }

    private void clearFailureNotification() {
        if (failureNotification != null) {
            failureNotification.clear();
            failureNotification = null;
        }
    }

    private static String normalizedReason(String reason) {
        return reason == null || reason.isBlank()
                ? "The Dart Analysis Server process could not be started."
                : reason.strip();
    }
}
