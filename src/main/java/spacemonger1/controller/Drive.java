package spacemonger1.controller;

import java.nio.file.Path;

public record Drive(
    String name,
    Path rootPath,
    long totalspace,
    long usedspace
) {
}
