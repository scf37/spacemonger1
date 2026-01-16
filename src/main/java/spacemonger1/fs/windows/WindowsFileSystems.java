package spacemonger1.fs.windows;

import spacemonger1.fs.Common;
import spacemonger1.fs.FileInfo;
import spacemonger1.fs.FileSystems;
import spacemonger1.fs.Volume;

import java.awt.Desktop;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WindowsFileSystems implements FileSystems {

    private Map<WindowsVolumeId, WindowsVolumeId> volumeIdDedup = new HashMap<>();

    @Override
    public List<Volume> volumes() {
        try (Arena a = Arena.ofConfined()) {
            List<String> volumeGuids = new ArrayList<>();
            MemorySegment nameBuf = a.allocate(ValueLayout.JAVA_CHAR, 256);
            MemorySegment h = Kernel32.FindFirstVolumeW(nameBuf, (int)nameBuf.address());
            if (h.address() != -1) {
                do {
                    volumeGuids.add(nameBuf.getString(0, StandardCharsets.UTF_16LE));
                } while (Kernel32.FindNextVolumeW(h, nameBuf, (int)nameBuf.byteSize()));
                Kernel32.FindVolumeClose(h);
            }

            List<Volume> result = new ArrayList<>();
            for (String volumeGuid: volumeGuids) {
                try {
                    List<String> mountPoints = Kernel32.getVolumeMountPoints(volumeGuid);
                    if (mountPoints.isEmpty()) continue;
                    var volumeInfo = Kernel32.getVolumeInformation(volumeGuid);
                    if (volumeInfo == null) continue;

                    var mountPoint = Paths.get(mountPoints.getFirst());
                    var store = Files.getFileStore(mountPoint);

                    result.add(new Volume(
                            newVolumeId(volumeInfo.volumeSerialNumber()),
                            mountPoint.toString(),
                            Paths.get(mountPoints.getFirst()),
                            store.getTotalSpace(),
                            store.getTotalSpace() - store.getUsableSpace(),
                            false
                    ));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return result;
        }
    }

    @Override
    public FileInfo fileInfo(Path path) {
        MemorySegment h = Kernel32.CreateFileW(
                path.toString(),
                0,
                Kernel32.FILE_SHARE_READ|Kernel32.FILE_SHARE_WRITE,
                MemorySegment.NULL,
                Kernel32.OPEN_EXISTING,
                Kernel32.FILE_ATTRIBUTE_NORMAL|Kernel32.FILE_FLAG_BACKUP_SEMANTICS|Kernel32.FILE_FLAG_OPEN_REPARSE_POINT,
                MemorySegment.NULL
        );
        if (h.address() == -1) return null;
        var info = Kernel32.GetFileInformationByHandleEx(h);

        Kernel32.CloseHandle(h);
        if (info == null) return null;
        var volumeId = newVolumeId(info.volumeId());
        return new FileInfo(
                volumeId,
                new WindowsFileId(volumeId, info.fileIdHigh(), info.fileIdLow()),
                info.size(),
                info.physicalSize(),
                info.lastModified(),
                info.isDirectory(),
                info.isFile()
        );
    }

    @Override
    public void moveToTrash(Path path) {
        if (Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            Desktop.getDesktop().moveToTrash(path.toFile());
            return;
        }
        throw new RuntimeException("Move to trash is not supported in this Java version. Use latest Oracle Java.");
    }

    private WindowsVolumeId newVolumeId(int serial) {
        var volumeId = new WindowsVolumeId(serial);
        return volumeIdDedup.computeIfAbsent(volumeId, x -> x);
    }

    private record WindowsVolumeId(
            int serial
    ) implements Volume.Id { }

    private record WindowsFileId(
            WindowsVolumeId volumeId,
            long idHigh,
            long idLow
    ) implements FileInfo.Id { }

}
