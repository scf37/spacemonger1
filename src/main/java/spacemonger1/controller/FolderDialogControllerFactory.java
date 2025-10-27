package spacemonger1.controller;

import spacemonger1.service.DialogUnitsService;
import spacemonger1.service.Lang;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import java.awt.Image;
import java.io.IOException;
import java.util.List;

public class FolderDialogControllerFactory {
    private final DialogUnitsService dialogUnitsService;
    private final List<Image> animationFrames;

    public FolderDialogControllerFactory(DialogUnitsService dialogUnitsService) {
        this.dialogUnitsService = dialogUnitsService;
        try {
            animationFrames = List.of(
                ImageIO.read(getClass().getClassLoader().getResourceAsStream("scan1.png")),
                ImageIO.read(getClass().getClassLoader().getResourceAsStream("scan2.png")),
                ImageIO.read(getClass().getClassLoader().getResourceAsStream("scan3.png")),
                ImageIO.read(getClass().getClassLoader().getResourceAsStream("scan4.png"))
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FolderDialogController newInstance(JFrame owner, Lang lang, Runnable onCancel) {
        return new FolderDialogController(dialogUnitsService, owner, animationFrames, lang, onCancel);
    }
}
