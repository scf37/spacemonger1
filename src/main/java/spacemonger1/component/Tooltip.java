package spacemonger1.component;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.geom.Rectangle2D;

public interface Tooltip {
    void destroy();

    static Tooltip create(
        JComponent parent,
        Font font
    ) {
        return new TipWnd(parent, font);
    }

    boolean isWindowEnabled();

    void enableWindow(boolean enable);

    void SetShowDelay(int delayMs);

    void setIcon(Icon imageIcon);

    void setIconPos(int iconPos);

    void setAutoPos(boolean autoPos);

    void setWindowText(String string);

    void autoSize();

    void redrawWindow();

    void setBgColor(Color black);

    void setTextColor(Color white);

    void moveWindow(JComponent component, int x, int y);

    void pushOnScreen();


    // Constants from C++
    int TW_LEFT = 0;
    int TW_TOP = 1;
    int TW_RIGHT = 2;
    int TW_BOTTOM = 3;


}

class TipWnd implements Tooltip {
    private final JWindow window;
    private final Font font;
    private String text = "";
    private Icon icon;
    private Color bgColor = SystemColor.control;          // COLOR_INFOBK
    private Color textColor = SystemColor.infoText;    // COLOR_INFOTEXT
    private Color borderColor = Color.BLACK;
    private int hPadding = 2;
    private int vPadding = 2;
    private int iconPos = TW_LEFT; // 0=left, 1=top, 2=right, 3=bottom
    private int showDelayMs = 0;
    private Timer timer;
    private boolean autoPos;

    public TipWnd(JComponent parent, Font font) {
        Window parentWindow = SwingUtilities.getWindowAncestor(parent);
        parentWindow.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                enableWindow(false);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                enableWindow(false);
            }
        });
        parentWindow.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                enableWindow(false);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                enableWindow(false);
            }
        });

        this.window = new JWindow(parentWindow);
        window.setType(Window.Type.POPUP);
        this.font = font;
        window.setFont(font);
        this.window.setBackground(new Color(0, 0, 0, 0)); // transparent background
        this.window.setAlwaysOnTop(true);

        window.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                forward(e);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                forward(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                forward(e);
            }

            private void forward(MouseEvent e) {
                Point parentPoint = SwingUtilities.convertPoint(
                    window, e.getX(), e.getY(), parent
                );
                MouseEvent forwarded = new MouseEvent(
                    parent,
                    e.getID(),
                    e.getWhen(),
                    e.getModifiersEx(),
                    parentPoint.x,
                    parentPoint.y,
                    e.getClickCount(),
                    e.isPopupTrigger(),
                    e.getButton()
                );
                parent.dispatchEvent(forwarded);
            }

        });

        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();

                int w = getWidth();
                int h = getHeight();

                // Draw border
                g2d.setColor(borderColor);
                g2d.drawRect(0, 0, w - 1, h - 1);

                // Draw background
                g2d.setColor(bgColor);
                g2d.fillRect(1, 1, w - 2, h - 2);

                // Draw icon
                if (icon != null) {
                    int iconX = 0, iconY = 0;
                    int iconSize = Math.max(icon.getIconWidth(), icon.getIconHeight());

                    switch (iconPos) {
                        case TW_LEFT:
                            iconX = 1 + hPadding;
                            iconY = 1 + vPadding + Math.max(0, (h - 2 - vPadding * 2 - iconSize) / 2);
                            break;
                        case TW_RIGHT:
                            iconX = w - 1 - hPadding - iconSize;
                            iconY = 1 + vPadding + Math.max(0, (h - 2 - vPadding * 2 - iconSize) / 2);
                            break;
                        case TW_TOP:
                            iconX = 1 + hPadding + Math.max(0, (w - 2 - hPadding * 2 - iconSize) / 2);
                            iconY = 1 + vPadding;
                            break;
                        case TW_BOTTOM:
                            iconX = 1 + hPadding + Math.max(0, (w - 2 - hPadding * 2 - iconSize) / 2);
                            iconY = h - 1 - vPadding - iconSize;
                            break;
                    }
                    icon.paintIcon(this, g2d, iconX, iconY);
                }

                // Draw text
                g2d.setColor(textColor);
                g2d.setFont(font);

                FontMetrics fm = g2d.getFontMetrics();
                int textX, textY;

                if (icon != null) {
                    int iconSize = Math.max(icon.getIconWidth(), icon.getIconHeight());;

                    switch (iconPos) {
                        case TW_LEFT:
                            textX = 1 + hPadding + iconSize + 4;
                            textY = 1 + vPadding;
                            break;
                        case TW_RIGHT:
                            textX = 1 + hPadding;
                            textY = 1 + vPadding;
                            break;
                        case TW_TOP:
                            textX = 1 + hPadding;
                            textY = 1 + vPadding + iconSize + 2;
                            break;
                        case TW_BOTTOM:
                            textX = 1 + hPadding;
                            textY = 1 + vPadding;
                            break;
                        default:
                            textX = 1 + hPadding;
                            textY = 1 + vPadding;
                    }
                } else {
                    textX = 1 + hPadding;
                    textY = 1 + vPadding;
                }

                if (text != null && !text.isEmpty()) {
                    String[] lines = text.split("\n", -1);
                    int lineHeight = fm.getHeight();
                    for (String line : lines) {
                        g2d.drawString(line, textX, textY + fm.getAscent());
                        textY += lineHeight;
                    }
                }

                g2d.dispose();
            }
        };
        content.setOpaque(false);
        this.window.getContentPane().add(content);
    }

    @Override
    public void destroy() {
        window.dispose();
    }

    @Override
    public boolean isWindowEnabled() {
        return window.isVisible();
    }

    @Override
    public void enableWindow(boolean enable) {
        if (enable) {
            if (timer == null) {
                timer = new Timer(showDelayMs, e -> {
                    timer = null;
                    if (autoPos) {
                        pushOnScreen();
                    }
                    window.setVisible(true);
                });
                timer.setRepeats(false);
                timer.start();

            }
        } else {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
            window.setVisible(false);
        }
    }

    @Override
    public void SetShowDelay(int delayMs) {
        this.showDelayMs = delayMs;
    }

    @Override
    public void setIcon(Icon imageIcon) {
        this.icon = imageIcon;
        autoSize();
    }

    @Override
    public void setIconPos(int iconPos) {
        this.iconPos = iconPos;
        autoSize();
    }

    @Override
    public void setWindowText(String string) {
        this.text = string != null ? string : "";
        autoSize();
    }

    @Override
    public void autoSize() {
        if (window == null) return;

        FontMetrics fm = window.getFontMetrics(font);
        int maxWidth = 0;
        int totalHeight = 0;

        if (text != null && !text.isEmpty()) {
            String[] lines = text.split("\n", -1);
            for (String line : lines) {
                Rectangle2D bounds = fm.getStringBounds(line, null);
                int width = (int) Math.ceil(bounds.getWidth());
                if (width > maxWidth) maxWidth = width;
            }
            totalHeight = fm.getHeight() * lines.length;
        }

        int iconSize = 0;
        int iconExtra = 0;
        if (icon != null) {
            iconSize = Math.max(icon.getIconWidth(), icon.getIconHeight());
            switch (iconPos) {
                case TW_LEFT:
                case TW_RIGHT:
                    iconExtra = iconSize + 4;
                    if (totalHeight < iconSize) totalHeight = iconSize;
                    break;
                case TW_TOP:
                case TW_BOTTOM:
                    iconExtra = iconSize + 2;
                    if (maxWidth < iconSize) maxWidth = iconSize;
                    break;
            }
        }

        int width = maxWidth + iconExtra + hPadding * 2 + 2; // +2 for border
        int height = totalHeight + vPadding * 2 + 2;         // +2 for border

        window.setSize(width, height);
    }

    @Override
    public void redrawWindow() {
        window.repaint();
    }

    @Override
    public void setBgColor(Color color) {
        this.bgColor = color;
        redrawWindow();
    }

    @Override
    public void setTextColor(Color color) {
        this.textColor = color;
        redrawWindow();
    }

    @Override
    public void moveWindow(JComponent component, int x, int y) {
        Point screenPoint = new Point(x, y);
        SwingUtilities.convertPointToScreen(screenPoint, component);
        window.setLocation(screenPoint);
    }

    @Override
    public void pushOnScreen() {
        if (autoPos) {
            var pi = MouseInfo.getPointerInfo();
            if (pi == null) return;
            var mouseLoc = pi.getLocation();
            window.setLocation(mouseLoc.x + 16, mouseLoc.y + 16);
        }
        Rectangle bounds = window.getBounds();
        Dimension screenSize = window.getGraphicsConfiguration().getBounds().getSize();

        if (bounds.x + bounds.width > screenSize.width) {
            bounds.x = screenSize.width - bounds.width;
        }
        if (bounds.y + bounds.height > screenSize.height) {
            bounds.y = screenSize.height - bounds.height;
        }
        if (bounds.x < 0) bounds.x = 0;
        if (bounds.y < 0) bounds.y = 0;

        window.setBounds(bounds);
    }

    @Override
    public void setAutoPos(boolean autoPos) {
        this.autoPos = autoPos;
    }

}