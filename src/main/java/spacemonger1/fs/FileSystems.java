package spacemonger1.fs;

import spacemonger1.fs.java.JavaFileSystems;
import spacemonger1.fs.linux.LinuxFileSystems;
import spacemonger1.fs.windows.WindowsFileSystems;

import java.nio.file.Path;
import java.util.List;

public interface FileSystems {
    List<Volume> volumes();
    FileInfo fileInfo(Path path);
    void moveToTrash(Path path);
    
    static FileSystems instance() {
        String osName = System.getProperty("os.name").toLowerCase();
        FileSystems fs = null;

        if (osName.contains("linux"))
            fs = new LinuxFileSystems();
        else if (osName.contains("windows")) {
            fs = new WindowsFileSystems();
        }

        if (fs != null && checkHealth(fs)) {
            return fs;
        }

        return new JavaFileSystems();
    }

    private static boolean checkHealth(FileSystems fs) {
        try {
            for (var v: fs.volumes()) {
                fs.fileInfo(v.mount());
            }
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }
}
