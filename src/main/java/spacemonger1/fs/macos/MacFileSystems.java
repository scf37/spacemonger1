package spacemonger1.fs.macos;

import spacemonger1.fs.Common;
import spacemonger1.fs.FileInfo;
import spacemonger1.fs.Volume;
import spacemonger1.fs.FileSystems;

import java.awt.Desktop;
import java.nio.file.FileStore;
import java.nio.file.LinkOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
// --- Implementations below ---

public final class MacFileSystems implements FileSystems {

    @Override
    public List<Volume> volumes() {
        List<Volume> result = new ArrayList<>();
        List<Path> paths = new ArrayList<>();

        paths.add(Paths.get("/"));
        paths.add(Paths.get(System.getProperty("user.home")));
        try (var stream = Files.list(Path.of("/Volumes"))) {
            stream.forEach(paths::add);
        } catch (Exception ignored) { }

        for (Path path : paths) {
            try {
                if (!Files.isDirectory(path) || !Files.isReadable(path)) continue;
                long dev = (long) Files.getAttribute(path, "unix:dev", LinkOption.NOFOLLOW_LINKS);
                FileStore store = Files.getFileStore(path);
                long total = store.getTotalSpace();
                long usable = store.getUsableSpace();
                long used = total - usable;
                String name = path.getFileName() == null ? path.toString() : path.getFileName().toString();

                result.add(new Volume(
                    new MacVolumeId(dev),
                    name,
                    path,
                    total,
                    used,
                    false
                ));
            } catch (Exception ignored) {
                // Skip inaccessible mounts
            }
        }

        return result;
    }

    @Override
    public FileInfo fileInfo(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            long dev = (long) Files.getAttribute(path, "unix:dev", LinkOption.NOFOLLOW_LINKS);
            long inode = (long) Files.getAttribute(path, "unix:ino", LinkOption.NOFOLLOW_LINKS);

            long logicalSize = attrs.size();
            long physicalSize = logicalSize; // Mac can't get block size? Todo find alternate.

            return new FileInfo(
                new MacVolumeId(dev),
                new MacFileId(new MacVolumeId(dev), inode),
                logicalSize,
                physicalSize,
                attrs.lastModifiedTime().toMillis(),
                attrs.isDirectory() && !attrs.isSymbolicLink() && !attrs.isOther(),
                attrs.isRegularFile() && !attrs.isSymbolicLink() && !attrs.isOther()
            );
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void moveToTrash(Path path) {
        if (Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            Desktop.getDesktop().moveToTrash(path.toFile());
            return;
        }
        throw new RuntimeException("Move to trash is not supported in this Java version. Use latest Oracle Java.");
    }

}

record MacVolumeId(
    long dev
) implements Volume.Id { }

record MacFileId(
    MacVolumeId volumeId,
    long inode
) implements FileInfo.Id { }
