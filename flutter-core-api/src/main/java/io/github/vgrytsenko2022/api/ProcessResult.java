package io.github.vgrytsenko2022.api;

public record ProcessResult(int exitCode, String stdout, String stderr) {
    public boolean success() { return exitCode == 0; }
}
