package spacemonger1.fs.java;

import spacemonger1.fs.Common;
import spacemonger1.fs.FileInfo;
import spacemonger1.fs.FileSystems;
import spacemonger1.fs.Volume;

import java.awt.Desktop;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JavaFileSystems implements FileSystems {
    private static final Set<Path> systemRoots = Set.of(
        Paths.get("/dev"),
        Paths.get("/proc")
    );
    private List<RootInfo> roots = new ArrayList<>();

    @Override
    public List<Volume> volumes() {
        List<Path> roots = new ArrayList<>();
        java.nio.file.FileSystems.getDefault().getRootDirectories().forEach(roots::add);

        if (roots.equals(List.of(Paths.get("/")))) {
            roots.add(Paths.get(System.getProperty("user.home")));
        }
        List<Volume> result = new ArrayList<Volume>();

        boolean fillRoots = this.roots.isEmpty();

        for (Path root : roots) {
            try {
                FileStore store = Files.getFileStore(root);
                var id = new JavaVolumeId(root);

                if (fillRoots) this.roots.add(new RootInfo(root.toString(), id, store.getBlockSize()));
                result.add(new Volume(
                    id,
                    root.toString(),
                    root,
                    store.getTotalSpace(),
                    store.getTotalSpace() - store.getUsableSpace(),
                    false
                ));
            } catch (Exception e) {
                System.err.println("Error reading file store " + root + ": " + e);
            }
        }
        if (fillRoots) {
            Collections.sort(this.roots, (e1, e2) -> e2.path().length() - e1.path().length());
        }

        return result;
    }

    @Override
    public FileInfo fileInfo(Path path) {
        if (systemRoots.contains(path)) return null;

        RootInfo root = null;
        String strPath = path.toString();
        for (var r: this.roots) {
            if (strPath.startsWith(r.path())) {
                root = r;
                break;
            }
        }
        if (root == null) return null;

        try {
            var attributes = Files.getFileAttributeView(path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).readAttributes();
            long fileSize = attributes.size();
            return new FileInfo(
                root.id,
                new JavaFileId(attributes.fileKey() != null ? attributes.fileKey() : path.toRealPath(LinkOption.NOFOLLOW_LINKS)),
                fileSize,
                (fileSize + root.blockSize - 1) / root.blockSize * root.blockSize,
                attributes.lastModifiedTime().toMillis(),
                attributes.isDirectory() && !attributes.isOther() && !attributes.isSymbolicLink(),
                attributes.isRegularFile() && !attributes.isOther() && !attributes.isSymbolicLink()
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

    private record RootInfo(
        String path,
        JavaVolumeId id,
        long blockSize
    ) { }

    private record JavaVolumeId(Path path) implements Volume.Id { }
    private record JavaFileId(Object key) implements FileInfo.Id { }
}
