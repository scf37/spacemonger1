package spacemonger1.service;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ToolbarIconsService {
    final BufferedImage fullImage;

    {
        try {
            fullImage = ImageIO.read(getClass().getClassLoader().getResourceAsStream("toolbar_en.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Icon at(int x) {
        BufferedImage squareImage = fullImage.getSubimage(x, 0, 18, 15);
        return new ImageIcon(squareImage);
    }
}