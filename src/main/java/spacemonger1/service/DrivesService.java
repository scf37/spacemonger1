package spacemonger1.service;

import spacemonger1.controller.Drive;
import spacemonger1.fs.FileSystems;

import java.util.ArrayList;
import java.util.List;

public class DrivesService {
    private final FileSystems fileSystems;;

    public DrivesService(FileSystems fileSystems) {
        this.fileSystems = fileSystems;
    }

    public List<Drive> enumerateDrives() {
        List<Drive> drives = new ArrayList<>();
        for (var v: fileSystems.volumes()) {
            if (!v.virtual()) {
                drives.add(new Drive(
                    v.name(),
                    v.mount(),
                    v.totalSize(),
                    v.usedSize()
                ));
            }
        }

        return drives;
    }
}
