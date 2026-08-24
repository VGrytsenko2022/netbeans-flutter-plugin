package dev.flutter.netbeans.plugin.tooling.test;

import org.netbeans.modules.gsf.testrunner.api.Report;
import org.netbeans.modules.gsf.testrunner.api.TestSession;

/** Narrow seam around the public Test Results core manager API. */
interface FlutterTestResultsSink {
    void start(TestSession session);

    void output(TestSession session, String text, boolean error);

    void report(TestSession session, Report report, boolean completed);

    void finish(TestSession session);
}
