package spacemonger1.fs;

import java.nio.file.Path;

public record Volume(
    Id id,
    String name,
    Path mount,
    long totalSize,
    long usedSize,
    // links to virtual filesystem/not of interest to the user
    boolean virtual
) {
    public interface Id {}
}
