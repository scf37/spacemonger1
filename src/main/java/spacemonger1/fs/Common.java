package spacemonger1.fs;

import java.awt.Desktop;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Common {
    public static void moveToTrash(Path path) {
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
}
