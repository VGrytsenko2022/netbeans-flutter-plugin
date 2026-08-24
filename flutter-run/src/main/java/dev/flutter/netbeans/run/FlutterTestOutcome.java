package dev.flutter.netbeans.run;

/** Effective state of one test, including late error events. */
public enum FlutterTestOutcome {
    RUNNING,
    SUCCESS,
    SKIPPED,
    FAILURE,
    ERROR,
    UNKNOWN
}
