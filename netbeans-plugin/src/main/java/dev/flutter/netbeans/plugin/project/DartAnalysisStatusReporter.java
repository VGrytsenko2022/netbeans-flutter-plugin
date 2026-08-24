package dev.flutter.netbeans.plugin.project;

/** Receives user-visible state changes for one project's Dart analysis process. */
interface DartAnalysisStatusReporter {
    DartAnalysisStatusReporter NONE = new DartAnalysisStatusReporter() {
        @Override
        public void starting(String projectName, boolean restarting) {
        }

        @Override
        public void running(String projectName, boolean restarted) {
        }

        @Override
        public void failed(String projectName, String reason, boolean notifyUser) {
        }

        @Override
        public void clear() {
        }
    };

    void starting(String projectName, boolean restarting);

    void running(String projectName, boolean restarted);

    void failed(String projectName, String reason, boolean notifyUser);

    void clear();
}
