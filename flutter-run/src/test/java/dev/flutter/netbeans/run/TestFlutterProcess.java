package dev.flutter.netbeans.run;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class TestFlutterProcess extends Process {
    private final PipedInputStream stdoutInput = new PipedInputStream(256 * 1024);
    private final PipedOutputStream stdoutOutput;
    private final PipedInputStream stderrInput = new PipedInputStream(64 * 1024);
    private final PipedOutputStream stderrOutput;
    private final ByteArrayOutputStream stdin = new ByteArrayOutputStream();
    private final OutputStream stdinSink = new OutputStream() {
        @Override
        public void write(int value) {
            synchronized (stdin) {
                stdin.write(value);
            }
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            synchronized (stdin) {
                stdin.write(bytes, offset, length);
            }
        }
    };
    private final AtomicBoolean alive = new AtomicBoolean(true);
    private final CountDownLatch exited = new CountDownLatch(1);
    private volatile int code;

    TestFlutterProcess() throws IOException {
        stdoutOutput = new PipedOutputStream(stdoutInput);
        stderrOutput = new PipedOutputStream(stderrInput);
    }

    void emitStdout(String line) throws IOException {
        writeLine(stdoutOutput, line);
    }

    void emitStderr(String line) throws IOException {
        writeLine(stderrOutput, line);
    }

    String stdinText() {
        synchronized (stdin) {
            return stdin.toString(StandardCharsets.UTF_8);
        }
    }

    void finish(int exitCode) {
        if (!alive.compareAndSet(true, false)) {
            return;
        }
        code = exitCode;
        try {
            stdoutOutput.close();
        } catch (IOException ignored) {
        }
        try {
            stderrOutput.close();
        } catch (IOException ignored) {
        }
        exited.countDown();
    }

    @Override
    public OutputStream getOutputStream() {
        return stdinSink;
    }

    @Override
    public InputStream getInputStream() {
        return stdoutInput;
    }

    @Override
    public InputStream getErrorStream() {
        return stderrInput;
    }

    @Override
    public int waitFor() throws InterruptedException {
        exited.await();
        return code;
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        return exited.await(timeout, unit);
    }

    @Override
    public int exitValue() {
        if (alive.get()) {
            throw new IllegalThreadStateException("Process is still running");
        }
        return code;
    }

    @Override
    public void destroy() {
        finish(143);
    }

    @Override
    public Process destroyForcibly() {
        finish(137);
        return this;
    }

    @Override
    public boolean isAlive() {
        return alive.get();
    }

    private static void writeLine(OutputStream output, String line) throws IOException {
        output.write(line.getBytes(StandardCharsets.UTF_8));
        output.write('\n');
        output.flush();
    }
}
