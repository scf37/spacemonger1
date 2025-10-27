package spacemonger1.service;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class DialogUnitsService {
    private static final BufferedImage DUMMY_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    /**
     * Computes dialog unit conversion scales based on the given dialog font.
     */
    public DialogUnits compute(Font font) {
        if (font == null) {
            throw new IllegalArgumentException("Font must not be null");
        }

        Graphics2D g2d = DUMMY_IMAGE.createGraphics();
        try {
            FontMetrics fm = g2d.getFontMetrics(font);
            int avgCharWidth = (fm.stringWidth("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz") / 26 + 1) / 2;

            int charHeight = fm.getHeight();

            return new DialogUnits(avgCharWidth, charHeight);
        } finally {
            g2d.dispose();
        }
    }
}