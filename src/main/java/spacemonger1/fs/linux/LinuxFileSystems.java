package spacemonger1.fs.linux;

import spacemonger1.fs.Common;
import spacemonger1.fs.FileInfo;
import spacemonger1.fs.FileSystems;
import spacemonger1.fs.Volume;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

// --- Implementations below ---

public final class LinuxFileSystems implements FileSystems {

    private Map<LinuxVolumeId, LinuxVolumeId> volumeIdDedup = new HashMap<>();

    @Override
    public List<Volume> volumes() {
        List<Volume> result = new ArrayList<>();
        Set<String> seenDevices = new HashSet<>();

        // Use /proc/mounts as the source of truth for mounted filesystems
        try (Scanner scanner = new Scanner(Paths.get("/proc/self/mountinfo"), "UTF-8")) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(" ");
                if (parts.length < 4) continue;
                String device = parts[2]; // major:minor
                String mountPoint =  parts[4]; // mount point

                String[] deviceParts = device.split(":");
                int devMajor = Integer.parseInt(deviceParts[0]);
                int devMinor = Integer.parseInt(deviceParts[1]);

                // Skip pseudo filesystems that are not useful to users
                boolean isVirtual = Common.hasVirtualFs(line);

                if (!seenDevices.add(device)) {
                    // Already processed this device
                    continue;
                }

                Path mountPath = Paths.get(mountPoint);
                try {

                    FileStore store = Files.getFileStore(mountPath);
                    long total = store.getTotalSpace();
                    long usable = store.getUsableSpace();
                    long used = total - usable; // approximation

                    LinuxVolumeId volumeId = newVolumeId(devMajor, devMinor);
                    Volume vol = new Volume(volumeId, mountPath.toString(), mountPath, total, used, isVirtual);
                    result.add(vol);
                } catch (IOException ignored) {
                    // Skip inaccessible mounts
                }
            }
        } catch (IOException ignored) {
            // Fallback: try File.listRoots() — though not reliable on Linux
        }

        return result;
    }

    @Override
    public FileInfo fileInfo(Path path) {
        Objects.requireNonNull(path);
        Glibc.Stat stat = Glibc.stat(path.toString());
        if (stat == null) return null; // file not found?

        var volumeId = newVolumeId(stat.devMajor(), stat.devMinor());
        return new FileInfo(
            volumeId,
            new LinuxFileId(volumeId, stat.inode()),
            stat.size(),
            stat.physicalSize(),
            stat.updateTimeSec() * 1000,
            stat.isDir(),
            stat.isFile()
        );
    }

    @Override
    public void moveToTrash(Path path) {
        if (Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            Desktop.getDesktop().moveToTrash(path.toFile());
            return;
        }

        try {
            if (new File("/usr/bin/gio").canExecute()) {
                int res = new ProcessBuilder("gio", "trash", "--", path.toAbsolutePath().toString()).inheritIO().start().waitFor();
                if (res != 0) throw new RuntimeException("Failed to delete file: " + path);
                return;
            }

            Path home = Paths.get(System.getProperty("user.home"));
            Path trash = home.resolve(".local/share/Trash");
            Path filesDir = trash.resolve("files");
            Path infoDir = trash.resolve("info");

            Files.createDirectories(filesDir);
            Files.createDirectories(infoDir);

            String name = path.getFileName().toString();
            Path destFile = filesDir.resolve(name);
            Path infoFile = infoDir.resolve(name + ".trashinfo");

            // Handle name collisions
            int counter = 1;
            while (Files.exists(destFile)) {
                String base = stripExtension(name);
                String ext = getExtension(name);
                destFile = filesDir.resolve(base + "." + (counter++) + (ext.isEmpty() ? "" : "." + ext));
                infoFile = infoDir.resolve(destFile.getFileName() + ".trashinfo");
            }

            Files.move(path, destFile);

            // Create .trashinfo
            String deletionDate = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String originalPath = path.toAbsolutePath().toString();
            String infoContent = "[Trash Info]\n" +
                "Path=" + originalPath + "\n" +
                "DeletionDate=" + deletionDate + "\n";

            Files.write(infoFile, infoContent.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String stripExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex == -1) ? name : name.substring(0, dotIndex);
    }

    private static String getExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex == -1) ? "" : name.substring(dotIndex + 1);
    }

    private LinuxVolumeId newVolumeId(int devMajor, int devMinor) {
        var volumeId = new LinuxVolumeId(devMajor, devMinor);
        return volumeIdDedup.computeIfAbsent(volumeId, x -> x);
    }
}

record LinuxVolumeId(
    int major,
    int minor
) implements Volume.Id { }

record LinuxFileId(
    LinuxVolumeId volumeId,
    long inode
) implements FileInfo.Id { }