package spacemonger1.fs.linux;

import spacemonger1.fs.Common;
import spacemonger1.fs.FileInfo;
import spacemonger1.fs.Volume;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
// --- Implementations below ---

public final class LinuxFileSystems implements spacemonger1.fs.FileSystems {

    private static final Set<String> KNOWN_PHYSICAL_FS = Set.of(
        "ext2", "ext3", "ext4",
        "xfs",
        "btrfs",
        "jfs",
        "reiserfs",
        "f2fs",
        "ntfs", "ntfs3",
        "vfat", "msdos",
        "exfat", "fuseblk",
        "hfs", "hfsplus",
        "ufs",
        "zfs",        // (if using ZFS on Linux)
        "apfs",       // (rare on Linux, but possible)
        "bcachefs"    // (new, but disk-backed)
    ).stream().map(s -> " " + s + " ").collect(Collectors.toSet());

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
                boolean isVirtual = isVirtualFs(line);

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

    private static boolean isVirtualFs(String line) {
        for (String fs: KNOWN_PHYSICAL_FS) {
            if (line.contains(fs)) return false;
        }
        return true;
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
        Common.moveToTrash(path);
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