package spacemonger1;

import javax.swing.Icon;
import java.nio.file.Path;

public class Utils {
    public static Icon iconByPath(Path path) {
//        final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
//        Icon icon = fc.getUI().getFileView(fc).getIcon(path.toFile());
        javax.swing.filechooser.FileSystemView fsv = javax.swing.filechooser.FileSystemView.getFileSystemView();
        javax.swing.Icon icon = fsv.getSystemIcon(path.toFile());

        return icon;
    }
}
