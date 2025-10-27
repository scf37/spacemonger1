package spacemonger1.service;

public final class DialogUnits {
    private final int width;
    private final int height; // pixels per vertical dialog unit

    DialogUnits(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int w(int units) {
        // 10% on top to properly show in Swing
        return units * width / 4 * 10 / 9;
    }

    public int h(int units) {
        return units * height / 8 * 10 / 9;
    }
}
