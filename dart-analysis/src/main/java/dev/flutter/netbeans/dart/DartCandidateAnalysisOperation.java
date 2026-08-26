package dev.flutter.netbeans.dart;

import java.util.concurrent.CompletionStage;

/** Cancellable asynchronous validation of one exact candidate snapshot. */
public interface DartCandidateAnalysisOperation {
    CompletionStage<DartCandidateAnalysisResult> result();

    /** Requests cancellation. A successfully cancelled operation resolves as STALE. */
    boolean cancel();
}
