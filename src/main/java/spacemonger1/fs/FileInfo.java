package spacemonger1.fs;

public record FileInfo(
    // volume this files belongs to
    Volume.Id volumeId,
    // unique file id within that volume
    Id id,
    long logicalSize,
    long physicalSize,
    long updateTimeMs,
    // ordinary directory, nor soft link, nor device
    boolean isDirectory,
    // ordinary file, nor soft link, nor device
    boolean isFile
) {
    public interface Id {}
}