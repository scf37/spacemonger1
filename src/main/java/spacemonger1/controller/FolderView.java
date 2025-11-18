package spacemonger1.controller;

import spacemonger1.Utils;
import spacemonger1.component.Tooltip;
import spacemonger1.service.CFolder;
import spacemonger1.service.CFolderTree;
import spacemonger1.service.DisplayFolder;
import spacemonger1.service.FormatService;
import spacemonger1.service.Lang;
import spacemonger1.service.Settings;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.SystemColor;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static spacemonger1.service.TipSettings.TIP_ATTRIB;
import static spacemonger1.service.TipSettings.TIP_DATE;
import static spacemonger1.service.TipSettings.TIP_ICON;
import static spacemonger1.service.TipSettings.TIP_NAME;
import static spacemonger1.service.TipSettings.TIP_PATH;
import static spacemonger1.service.TipSettings.TIP_SIZE;

public class FolderView {
    private static final Color SYS_COLOR_3DFACE = SystemColor.control;
    private static final Color SYS_COLOR_3DHILIGHT = SystemColor.controlHighlight; // or brighter
    private static final Color SYS_COLOR_3DSHADOW  = SystemColor.controlShadow;

    public interface AppCommands {
        void setTitle(String title);
        void updated();
        Settings settings();

        Lang lang();

        boolean showFreeSpace();

        void zoomIn();

        void zoomOut();

        void zoomFull();

        void runOrOpen();

        void delete();

        void openDrive();

        void reload();

        void toggleFreeSpace();
    }

    public record Selection(
        Path path,
        boolean isDirectory,
        long size
    ) { }

    private static final Color[] BoxColors = {
        new Color(0xFF, 0x7F, 0x7F),
        new Color(0xFF, 0xBF, 0x7F),
        new Color(0xFF, 0xFF, 0x00),
        new Color(0x7F, 0xFF, 0x7F),
        new Color(0x7F, 0xFF, 0xFF),
        new Color(0xBF, 0xBF, 0xFF),
        new Color(0xBF, 0xBF, 0xBF),
        new Color(0xFF, 0x7F, 0xFF),

        new Color(0xFF, 0xBF, 0xBF),
        new Color(0xFF, 0xDF, 0xBF),
        new Color(0xFF, 0xFF, 0xBF),
        new Color(0xBF, 0xFF, 0xBF),
        new Color(0xDF, 0xFF, 0xFF),
        new Color(0xDF, 0xDF, 0xFF),
        new Color(0xDF, 0xDF, 0xDF),
        new Color(0xFF, 0xBF, 0xFF),

        new Color(0xBF, 0x7F, 0x7F),
        new Color(0xBF, 0x9F, 0x5F),
        new Color(0xBF, 0xBF, 0x3F),
        new Color(0x7F, 0xBF, 0x7F),
        new Color(0x7F, 0xBF, 0xBF),
        new Color(0x9F, 0x9F, 0xFF),
        new Color(0x9F, 0x9F, 0x9F),
        new Color(0xBF, 0x7F, 0xBF),

        new Color(0x00, 0x00, 0x00),
        new Color(0xFF, 0xFF, 0xFF),
    };

    // These colors are used for specifically-chosen folder colors.
    private static final  Color[] FixedColors = {
        new Color(0xFF, 0xFF, 0xFF),
        new Color(0xBF, 0xBF, 0xBF),
        new Color(0x7F, 0x7F, 0x7F),

        new Color(0xFF, 0x7F, 0x7F),
        new Color(0xFF, 0xBF, 0x7F),
        new Color(0xFF, 0xFF, 0x00),
        new Color(0x7F, 0xFF, 0x7F),
        new Color(0x7F, 0xFF, 0xFF),
        new Color(0xBF, 0xBF, 0xFF),
        new Color(0xFF, 0x7F, 0xFF),

        new Color(0xFF, 0xFF, 0xFF),
        new Color(0xFF, 0xFF, 0xFF),
        new Color(0xBF, 0xBF, 0xBF),

        new Color(0xFF, 0x9F, 0x9F),
        new Color(0xFF, 0xDF, 0xBF),
        new Color(0xFF, 0xFF, 0xBF),
        new Color(0xBF, 0xFF, 0xBF),
        new Color(0xDF, 0xFF, 0xFF),
        new Color(0xDF, 0xDF, 0xFF),
        new Color(0xFF, 0xBF, 0xFF),

        new Color(0xBF, 0xBF, 0xBF),
        new Color(0x7F, 0x7F, 0x7F),
        new Color(0x3F, 0x3F, 0x3F),

        new Color(0xBF, 0x7F, 0x7F),
        new Color(0xBF, 0x9F, 0x9F),
        new Color(0xBF, 0xBF, 0x3F),
        new Color(0x7F, 0xBF, 0x7F),
        new Color(0x7F, 0xBF, 0xBF),
        new Color(0x9F, 0x9F, 0xFF),
        new Color(0xBF, 0x7F, 0xBF),
    };

    public FolderView(AppCommands appCommands, FormatService formatService, Container parent) {
        this.appCommands = appCommands;
        this.formatService = formatService;
        this.component = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                FolderView.this.paintComponent(g);
            }
        };

        parent.add(component);

        rootfolder = null;
        selected = null;
        displayfolders = displayend = null;
        zoomlevel = 0;
        lastcur = null;
        m_infotipwnd = Tooltip.create(component, minifont);
        m_nametipwnd = Tooltip.create(component, minifont);

        component.setLayout(null);
        component.setBackground(Color.WHITE);

        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    onLButtonDown(e);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    onLButtonDblClk(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    onRButtonUp(e);
                }
            }
        });

        component.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                onMouseMove(e);
            }
        });

        // --- Resize (OnSize) ---
        component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension size = component.getSize();
                onSize(0, size.width, size.height);
            }
        });

        component.setVisible(true);
    }
    public JComponent component() {
        return component;
    }

    public Selection selection() {
        if (selected == null) return null;
        return new Selection(Paths.get(document.mPath, folderPath(selected.source), selected.source.names[selected.index]), (selected.flags & 1) != 0, selected.source.actualsizes[selected.index]);
    }

    public void deleteSelected() {
        final boolean isfolder = (selected.flags & 1) != 0;

        if (isfolder) {
            selected.source.children[selected.index] = null;
        }
        long subsize = selected.source.sizes[selected.index];
        CFolder folder = selected.source;
        if (isfolder) folder.sizeChildren -= subsize;
        else folder.sizeSelf -= subsize;
        folder = folder.parent;
        while (folder != null) {
            folder.sizeChildren -= subsize;
            folder = folder.parent;
        }
        selected.source.sizes[selected.index] = 0;
        selected = null;
        appCommands.updated();
        onSize(0, getWidth(), getHeight());
    }

    public void setDocument(CFolderTree doc) {
        document = doc;
        if (doc != null) {
            rootfolder = doc.GetRoot();
        } else {
            rootfolder = null;
        }
        zoomlevel = 0;
        onUpdate(null);
        UpdateTitleBar();
        onSize(0, getWidth(), getHeight());
        repaint();
    }

    private String folderPath(CFolder folder) {
        StringBuilder sb = new StringBuilder();
        BuildTitleReverse(folder, sb);
        return sb.toString();
    }

    private void BuildTitleReverse(CFolder folder, StringBuilder string) {
        if (folder.parent != null) {
            BuildTitleReverse(folder.parent, string);
        }
        if (folder.parent == null) {
            return;
        }

        string.append(folder.parent.names[folder.parentindex]);
        string.append(File.separator);
    }

    private DisplayFolder GetDisplayFolderFromPoint(Point point) {
        DisplayFolder cur = displayfolders;

        while (cur != null) {
            if (point.x > cur.x && point.y > cur.y
                && point.x < cur.x + cur.w && point.y < cur.y + cur.h) {

                if ((cur.flags & 1) != 0) {
                    if (point.x < cur.x + 3 || point.y < cur.y + 12
                        || point.x > cur.x + cur.w - 3 || point.y > cur.y + cur.h - 3) {
                        break;
                    }
                } else {
                    break;
                }
            }
            cur = cur.next;
        }

        if (cur != null) {
            if (cur.name == null) {
                cur = null;
            } else if (cur.name.length() > 0 && cur.name.charAt(0) == '<') {
                cur = null;
            }
        }

        return cur;
    }

    private void HighlightPathAtPoint(Graphics2D g, Point point) {
        DisplayFolder cur = displayfolders;
        while (cur != null) {
            if (cur.name != null && !cur.name.isEmpty() && cur.name.charAt(0) != '<') {
                boolean inside = (point.x > cur.x && point.y > cur.y
                    && point.x < cur.x + cur.w && point.y < cur.y + cur.h);

                if (inside) {
                    if ((cur.flags & 4) == 0) {
                        cur.flags |= 4;
                        minimalDrawDisplayFolder(g, cur, selected == cur);
                    }
                } else {
                    if ((cur.flags & 4) != 0) {
                        cur.flags &= ~4;
                        minimalDrawDisplayFolder(g, cur, selected == cur);
                    }
                }
            }
            cur = cur.next;
        }
    }

    public void zoomIn() {
        if (selected == null) return;
        ZoomIn(selected);
    }

    private void ZoomIn(DisplayFolder folder) {
        CFolderTree doc = document;
        CFolder oldroot = rootfolder;

        if (folder != null && folder.source.children[folder.index] != null) {
            rootfolder = folder.source.children[folder.index];
        }

        if (rootfolder == oldroot) {
            return;
        }

        Rectangle start = new Rectangle(
            folder.x,
            folder.y,
            folder.w,
            folder.h
        );

        Rectangle end = new Rectangle(0, 0, getWidth(), getHeight());

        AnimateBox(start, end, () -> {
            CFolder parent = rootfolder.parent;
            zoomlevel = 0;
            while (parent != null) {
                zoomlevel++;
                parent = parent.parent;
            }

            onUpdate(doc);
            UpdateTitleBar();
        });

    }

    private void AnimateBox(Rectangle start, Rectangle end, Runnable onComplete) {
        if (!appCommands.settings().animated_zoom()) {
            onComplete.run();
            return;
        }
        // Cancel any ongoing animation
        animStep = -1;

        this.animStart = start;
        this.animEnd = end;
        this.animStep = 0;
        if (this.animOnComplete != null) {
            var old = this.animOnComplete;
            this.animOnComplete = () -> {
                old.run();
                onComplete.run();
            };
        } else {
            this.animOnComplete = onComplete;
        }

        // Start animation loop
        scheduleAnimationFrame();
    }

    private void scheduleAnimationFrame() {
        if (animStep == -1) return;

        Timer timer = new Timer(25, e -> {
            if (animStep == -1) return;

            repaint();

            animStep++;
            if (animStep >= 16) {
                animStep = -1;
                animOnComplete.run();
                animStart = null;
                animEnd = null;
                animOnComplete = null;
                repaint();
            } else {
                scheduleAnimationFrame();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void drawAnimateBox(Graphics2D g) {
        if (animStep >= 0 && animStep < 16) {
            Graphics2D g2d = (Graphics2D) g.create();

            g2d.setXORMode(Color.WHITE);

            Rectangle current = computeNewRect(animStart, animEnd, 8, animStep % 8);

            int x1 = current.x;
            int y1 = current.y;
            int x2 = x1 + current.width;
            int y2 = y1 + current.height;

            g2d.drawLine(x1, y1, x2, y1); // top
            g2d.drawLine(x2, y1, x2, y2); // right
            g2d.drawLine(x2, y2, x1, y2); // bottom
            g2d.drawLine(x1, y2, x1, y1); // left

            g2d.dispose();
        }
    }

    private Rectangle computeNewRect(Rectangle start, Rectangle end, int max, int step) {
        if (max <= 0) max = 1;

        int left   = ((start.x          * (max - step)) + (end.x          * step)) / max;
        int top    = ((start.y          * (max - step)) + (end.y          * step)) / max;
        int right  = ((start.x + start.width) * (max - step) + (end.x + end.width) * step) / max;
        int bottom = ((start.y + start.height) * (max - step) + (end.y + end.height) * step) / max;

        return new Rectangle(left, top, right - left, bottom - top);
    }

    public void zoomOut() {
            CFolderTree doc = document;
            CFolder oldroot = rootfolder;

            if (rootfolder != null && rootfolder.parent != null) {
                rootfolder = rootfolder.parent;
            }

            if (rootfolder == oldroot) {
                return;
            }

            Rectangle start = new Rectangle(0, 0, getWidth(), getHeight());

            int centerX = start.width / 2;
            int centerY = start.height / 2;
            Rectangle end = new Rectangle(centerX, centerY, 0, 0);

            AnimateBox(start, end, () -> {
                zoomlevel--;
                onUpdate(doc);
                UpdateTitleBar();
            });

    }
    public void ZoomFull() {
        CFolderTree doc = document;
        CFolder oldroot = rootfolder;

        if (doc != null) {
            rootfolder = doc.GetRoot();
        } else {
            rootfolder = null;
        }

        if (rootfolder == oldroot) {
            return;
        }

        Rectangle start = new Rectangle(0, 0, getWidth(), getHeight());

        int centerX = start.width / 2;
        int centerY = start.height / 2;
        Rectangle end = new Rectangle(centerX, centerY, 0, 0);

        AnimateBox(start, end, () -> {
            zoomlevel = 0;
            onUpdate(doc);
            UpdateTitleBar();
        });
    }

    public boolean isZoomFull() { return zoomlevel == 0; }

    private void UpdateTitleBar() {
        CFolderTree ft = document;
        StringBuilder title = new StringBuilder();
        long size;

        appCommands.updated();
        if (ft == null) {
            appCommands.setTitle("");
            return;
        }

        if (selected != null && selected.name != null) {
            title.append(Paths.get(ft.mPath, folderPath(selected.source), selected.source.names[selected.index]));
            size = selected.source.sizes[selected.index];
            title.append("  -  ")
                .append(formatService.getSizeString(appCommands.lang(), size, ft.totalspace, true))
                .append("  -  ")
                .append(formatService.getSizeString(appCommands.lang(), size, ft.totalspace, false))
                .append("  -  ");
        }
        else if (rootfolder != null && ft != null) {
            title.append(Paths.get(ft.mPath, folderPath(rootfolder)));
            if (rootfolder.parent == null) {
                size = ft.totalspace; // Kludge
            } else {
                size = rootfolder.SizeTotal();
            }
            title.append("  -  ")
                .append(formatService.getSizeString(appCommands.lang(), size, ft.totalspace, false))
                .append(" ")
                .append(appCommands.lang().total)
                .append("  -  ")
                .append(formatService.getSizeString(appCommands.lang(), ft.freespace, ft.freespace, false))
                .append(" ")
                .append(appCommands.lang().free)
                .append("  -  ");
        }

        title.append("SpaceMonger One");
        appCommands.setTitle(title.toString());
    }

    private void selectFolder(DisplayFolder cur) {
        if (cur == selected) return;

        selected = cur;

        UpdateTitleBar();
    }

    private void paintComponent(Graphics g) {
        Graphics2D gg = (Graphics2D) g;
        DisplayFolder cur = displayfolders;

        g.setFont(minifont);

        drawBox(gg, SYS_COLOR_3DFACE, 0, 0, getWidth(), getHeight());
        if (cur == null)
            fillBox(gg, SYS_COLOR_3DFACE, 1, 1, getWidth()-2, getHeight()-2);

        while (cur != null) {
            minimalDrawDisplayFolder(gg, cur, selected == cur);
            cur = cur.next;
        }

        if (selected != null)
            drawDisplayFolder(gg, selected, true);


        if (hightlightPathPoint != null) {
            HighlightPathAtPoint(gg, hightlightPathPoint);
        }

        drawAnimateBox(gg);
    }

    private void onSize(int type, int width, int height) {
        clearDisplayFolders();

        if (rootfolder == null) {
            if (document == null) return;
            rootfolder = document.GetRoot();
            zoomlevel = 0;
            if (rootfolder == null) return;
        }

        buildFolderLayout(0, 0, width - 1, height - 1, rootfolder, zoomlevel);
        repaint();
    }
    private void onLButtonDown(MouseEvent e) {
        selectFolder(GetDisplayFolderFromPoint(e.getPoint()));
        lastcur = null;
        onMouseMove(e);
    }

    private void onLButtonDblClk(MouseEvent e) {
        Point point = e.getPoint();
        DisplayFolder cur = GetDisplayFolderFromPoint(point);
        selectFolder(cur);

        if (cur != null) {
            if ((cur.flags & 1) != 0) {
                ZoomIn(cur);
            } else {
                StringBuilder title = new StringBuilder();
                CFolderTree doc = document;
                title.append(Paths.get(doc.mPath, folderPath(selected.source), selected.source.names[selected.index]));

                // Convert to File and open with default app
                File fileToOpen = new File(title.toString());
                if (fileToOpen.exists()) {
                    try {
                        Desktop.getDesktop().open(fileToOpen);
                    } catch (IOException ex) {
                    }
                }
            }
        }

        lastcur = null;
        onMouseMove(e);
    }

    private void onRButtonUp(MouseEvent e) {
        Point point = e.getPoint();
        DisplayFolder cur = GetDisplayFolderFromPoint(point);
        selectFolder(cur);

        // ClientToScreen: convert to screen coordinates
        Point screenPoint = new Point(point);
        SwingUtilities.convertPointToScreen(screenPoint, component);

        m_infotipwnd.enableWindow(false);
        m_nametipwnd.enableWindow(false);

        // Build popup menu
        menu = new JPopupMenu();

        // Zoom In
        JMenuItem zoomIn = new JMenuItem(appCommands.lang().zoomin);
        zoomIn.addActionListener(_ -> appCommands.zoomIn());
        zoomIn.setEnabled(cur != null && (cur.flags & 1) != 0);
        menu.add(zoomIn);

        // Zoom Out
        JMenuItem zoomOut = new JMenuItem(appCommands.lang().zoomout);
        zoomOut.addActionListener(_ -> appCommands.zoomOut());
        zoomOut.setEnabled(zoomlevel != 0);
        menu.add(zoomOut);

        // Zoom Full
        JMenuItem zoomFull = new JMenuItem(appCommands.lang().zoomfull);
        zoomFull.addActionListener(_ -> appCommands.zoomFull());
        zoomFull.setEnabled(zoomlevel != 0);
        menu.add(zoomFull);

        menu.addSeparator();

        // Run (open file)
        JMenuItem run = new JMenuItem(appCommands.lang().run);
        run.addActionListener(_ -> appCommands.runOrOpen());
        run.setEnabled(cur != null);
        menu.add(run);

        // Delete
        JMenuItem delete = new JMenuItem(appCommands.lang().del);
        delete.addActionListener(_ -> appCommands.delete());
        boolean deleteEnabled = !appCommands.settings().disable_delete() && cur != null;
        delete.setEnabled(deleteEnabled);
        menu.add(delete);

        menu.addSeparator();

        // Open Drive
        JMenuItem openDrive = new JMenuItem(appCommands.lang().opendrive);
        openDrive.addActionListener(_ -> appCommands.openDrive());
        menu.add(openDrive);

        // Refresh
        JMenuItem refresh = new JMenuItem(appCommands.lang().rescandrive);
        refresh.addActionListener(_ -> appCommands.reload());
        menu.add(refresh);

        // Show Free Space (checkmark if active)
        JCheckBoxMenuItem showFree = new JCheckBoxMenuItem(appCommands.lang().showfreespace, appCommands.showFreeSpace());
        showFree.addActionListener(_ -> appCommands.toggleFreeSpace());
        menu.add(showFree);
//
//        menu.addSeparator();
//
//        // Properties
//        JMenuItem properties = new JMenuItem(appCommands.lang().properties);
//        properties.setActionCommand("ID_FILE_PROPERTIES");
//        menu.add(properties);

        // Show menu at screen coordinates
        menu.show(component, screenPoint.x - component.getLocationOnScreen().x, screenPoint.y - component.getLocationOnScreen().y);

        repaint();
    }

    private void onMouseMove(MouseEvent e) {
        Point point = e.getPoint();
        DisplayFolder cur = GetDisplayFolderFromPoint(point);

        if (appCommands.settings().rollover_box()) {
            hightlightPathPoint = point;

            // Swing can produce hundreds mouse events per sec, lets limit repaint rate to 30 fps.
            if (repaintLimitTimer == null) {
                repaintLimitTimer = new Timer(1000 / 30, _ -> {
                    repaint();
                    repaintLimitTimer = null;
                });
                repaintLimitTimer.setRepeats(false);
                repaintLimitTimer.start();
            }
        } else {
            hightlightPathPoint = null;
        }

        if (appCommands.settings().show_info_tips()) {
            if (cur == null) {
                m_infotipwnd.enableWindow(false);
            } else if (lastcur != cur) {
                SetupInfoTip(cur);
            }
        } else {
            m_infotipwnd.enableWindow(false);
        }

        if (appCommands.settings().show_name_tips()) {
            if (cur == null) {
                m_nametipwnd.enableWindow(false);
            } else if (lastcur != cur) {
                SetupNameTip(cur);
            }
        } else {
            m_nametipwnd.enableWindow(false);
        }

        lastcur = cur;
    }


    public void onUpdate(CFolderTree doc) {
        UpdateTitleBar();

        onSize(0, getWidth(), getHeight());

        repaint();

        m_infotipwnd.SetShowDelay(appCommands.settings().infotip_delay());
        m_nametipwnd.SetShowDelay(appCommands.settings().nametip_delay());
    }

    private void buildFolderLayout(int x, int y, int w, int h, CFolder folder, int depth) {
        if (folder == null) return;

        int[] indices = new int[folder.cur];
        for (int i = 0; i < folder.cur; i++) {
            indices[i] = i;
        }

        final int[][] minsizes = {
            { 96, 64 },
            { 64, 48 },
            { 48, 32 },
            { 32, 24 },
            { 24, 16 },
            { 16, 12 },
            { 8, 6 }
        };

        int idx = appCommands.settings().density() + 3;

        hmin = minsizes[idx][0];
        vmin = minsizes[idx][1];

        sizeFolders(x, y, w, h, folder, indices, folder.cur, depth);
    }

    private void sizeFolders(int x, int y, int w, int h, CFolder folder, int[] index, int numindices, int depth) {
        if (folder == null) return;

        long totalspace = folder.SizeTotal();

        int[] list1 = new int[numindices];
        int[] list2 = new int[numindices];
        int numlist1, numlist2;
        long list1sum, list2sum;
        int x1, y1, w1, h1;
        int x2, y2, w2, h2;
        int split;

        // Split the lists evenly.  We assume the sizes are sorted
        // in descending order.  Overall, this is a greedy algorithm,
        // so it should produce fairly good results.

        numlist1 = numlist2 = 0;
        list1sum = list2sum = 0;

        for (int largest = 0; largest < numindices; largest++) {
            long bignum = folder.sizes[index[largest]];
            if (folder.names[index[largest]].charAt(0) == '<' && !appCommands.showFreeSpace()) {
                bignum = 0;
            }
            if (bignum != 0) {
                if (list1sum <= list2sum) {
                    list1[numlist1++] = index[largest];
                    list1sum += bignum;
                } else {
                    list2[numlist2++] = index[largest];
                    list2sum += bignum;
                }
            }
        }

        // Don't bother if the files have no space
        if (list1sum + list2sum <= 0) {
            return;
        }

        // Okay, we're now as even as we can safely get.  Now we know how to
        // split up the space.
        int wbias, hbias;
        if (appCommands.settings().bias() > 0) {
            wbias = appCommands.settings().bias() + 8;
            hbias = 8;
        }
        else if (appCommands.settings().bias() < 0) {
            hbias = -appCommands.settings().bias() + 8;
            wbias = 8;
        } else {
            wbias = 8;
            hbias = 8;
        }

        if (((w * wbias) / 8) > ((h * hbias) / 8)) {
            split = (int) ((w * list1sum) / (list1sum + list2sum));
            x1 = x; y1 = y; w1 = split; h1 = h;
            x2 = x + split; y2 = y; w2 = w - split; h2 = h;
        } else {
            split = (int) ((h * list1sum) / (list1sum + list2sum));
            x1 = x; y1 = y; w1 = w; h1 = split;
            x2 = x; y2 = y + split; w2 = w; h2 = h - split;
        }

        // Now if a given rectangle has more than one file and is
        // large enough to be subdivided again, subdivide again
        if (numlist1 > 1 && w1 > hmin && h1 > vmin)
            sizeFolders(x1, y1, w1, h1, folder, list1, numlist1, depth);
        else if (numlist1 > 0) {
            if (w1 > hmin && h1 > vmin) {
                addDisplayFolder(folder, list1[0],
                                 depth, (short) x1, (short) y1, (short) w1, (short) h1,
                                 (folder.children[list1[0]] != null)? 1:0);
                if (folder.children[list1[0]] != null) {
                    if (w1 > hmin && h1 > vmin) {
                        buildFolderLayout(x1 + 3, y1 + 12, w1 - 6, h1 - 15,
                                          folder.children[list1[0]], depth + 1);
                    } else {
                        addDisplayFolder(folder, 0xFFFFFFFF, depth + 1,
                                         (short) (x2 + 3), (short) (y2 + 12), (short) (w2 - 6), (short) (h2 - 15), 0);
                    }
                }
            } else {
                addDisplayFolder(folder, 0xFFFFFFFF, depth, (short) x1, (short) y1, (short) w1, (short) h1, 0);
            }
        }

        if (numlist2 > 1 && w2 > hmin && h2 > vmin)
            sizeFolders(x2, y2, w2, h2, folder, list2, numlist2, depth);
        else if (numlist2 > 0) {
            if (w2 > hmin && h2 > vmin) {
                addDisplayFolder(folder, list2[0],
                                 depth, (short) x2, (short) y2, (short) w2, (short) h2,
                                 (folder.children[list2[0]] != null)?1:0);
                if (folder.children[list2[0]] != null) {
                    if (w2 > hmin && h2 > vmin) {
                        buildFolderLayout(x2 + 3, y2 + 12, w2 - 6, h2 - 15,
                                          folder.children[list2[0]], depth + 1);
                    } else {
                        addDisplayFolder(folder, 0xFFFFFFFF, depth + 1,
                                         (short) (x2 + 3), (short) (y2 + 12), (short) (w2 - 6), (short) (h2 - 15), 0);
                    }
                }
            } else {
                addDisplayFolder(folder, 0xFFFFFFFF, depth, (short) x2, (short) y2, (short) w2, (short) h2, 0);
            }
        }
    }

    private DisplayFolder addDisplayFolder(CFolder source, int index, int depth, short x, short y, short w, short h, int flags) {
        DisplayFolder newfolder = new DisplayFolder();
        char c;

        if (source != null && index != 0xFFFFFFFF) {
            newfolder.name = source.names[index];
            c = newfolder.name.charAt(0);
        } else {
            newfolder.name = null;
            c = 0;
        }

        if (c == '*' || c == '<' || c == '>' || c == '?' || c == '|') {
            newfolder.depth = -1;
            flags |= 2;
        } else {
            newfolder.depth = depth;
        }

        newfolder.flags = flags;
        newfolder.source = source;
        newfolder.index = index;
        newfolder.x = x;
        newfolder.y = y;
        newfolder.w = w;
        newfolder.h = h;
        newfolder.next = null;

        if (displayend == null) {
            displayfolders = displayend = newfolder;
        } else {
            displayend = (displayend.next = newfolder);
        }

        return newfolder;
    }

    private void clearDisplayFolders() {
        displayfolders = null;
        displayend = null;
        selected = null;
    }

    private void drawDisplayFolder(Graphics2D g, DisplayFolder cur, boolean selected) {
        minimalDrawDisplayFolder(g, cur, selected);

    }

    private void minimalDrawDisplayFolder(Graphics2D g, DisplayFolder cur, boolean sel) {
        int x, y, w, h;

        x = cur.x;
        y = cur.y;
        w = cur.w + 1;
        h = cur.h + 1;

        // Draw outer black box if depth != -1
        if (cur.depth != -1) {
            drawBox(g, Color.BLACK, x, y, w, h);
        }

        if (w > 2 && h > 2) {
            Color color, bright, dark;
            int colortype = ((cur.flags & 1) != 0)
                ? appCommands.settings().folder_color()
                : appCommands.settings().file_color();

            if (sel && (cur.flags & 2) == 0) {
                color = bright = dark = Color.BLACK;
            } else if (cur.depth != -1) {
                if (colortype == 0) {
                    int idx = cur.depth & 7;
                    color = BoxColors[idx];
                    bright = BoxColors[idx + 8];
                    dark = BoxColors[idx + 16];
                } else if (colortype == 1) {
                    color = SYS_COLOR_3DFACE;
                    bright = SYS_COLOR_3DHILIGHT;
                    dark = SYS_COLOR_3DSHADOW;
                } else {
                    int base = colortype - 2;
                    color = FixedColors[base];
                    bright = FixedColors[base + 10];
                    dark = FixedColors[base + 20];
                }

                if (appCommands.settings().rollover_box()) {
                    if ((cur.flags & 4) != 0) {
                        dark = color;
                        color = bright;
                        bright = Color.WHITE;
                    } else {
                        bright = color;
                        color = dark;
                    }
                }
            } else {
                color = bright = dark = SYS_COLOR_3DFACE;
            }

            // Draw inner dual-color frame
            drawDualBox(g, bright, dark, x + 1, y + 1, w - 2, h - 2);

            if ((cur.flags & 1) != 0) {
                // Folder: draw inner border and top bar
                drawBox(g, color, x + 2, y + 2, w - 4, h - 4);
                fillBox(g, color, x + 3, y + 3, w - 6, 9);
            } else {
                fillBox(g, color, x + 2, y + 2, w - 4, h - 4);
            }
        }

        if (cur.name != null) {
            // Save current clip
            Shape originalClip = g.getClip();

            // Set clipping to folder bounds
            g.setClip(x, y, w, h);

            FontMetrics fm = g.getFontMetrics();
            Rectangle2D nameBounds = fm.getStringBounds(cur.name, g);
            int textWidth = (int) Math.ceil(nameBounds.getWidth());
            int textHeight = fm.getHeight();

            int tx, ty;

            if (textWidth > w - 2 || (cur.flags & 1) != 0) {
                tx = x + 2;
            } else {
                tx = x + (w - textWidth) / 2;
            }

            if (textHeight > h - 2 || (cur.flags & 1) != 0) {
                ty = y + 1;
            } else {
                ty = y + (h - textHeight) / 2;
            }

            if ((cur.flags & 2) != 0) {
                // Free-space block
                CFolderTree ft = document;
                long ts = ft.totalspace;
                if (ts <= 1) ts = 1;
                long freepercent = ft.freespace * 1000L / ts;
                int fp = (int) freepercent;

                // Format freespace string (e.g., "45.6%")
                String freespaceStr = String.format(appCommands.lang().freespace_format, fp / 10, fp % 10);
                Rectangle2D fsBounds = fm.getStringBounds(freespaceStr, g);
                int fsWidth = (int) Math.ceil(fsBounds.getWidth());
                if (fsWidth > w - 2) {
                    tx = x + 2;
                } else {
                    tx = x + (w - fsWidth) / 2;
                }

                // GDI TextOut draws with top-left at (x,y)
                // Java drawString uses baseline → we simulate top alignment by using ty directly
                // But: to match GDI's y+1, y-18, etc., we treat ty as **top coordinate**
                // So we compute baseline = ty + ascent
                int ascent = fm.getAscent();

                g.setColor(sel ? Color.WHITE : Color.BLACK);
                g.drawString(freespaceStr, tx, ty - 18 + ascent);

                String freeLine = formatService.getSizeString(appCommands.lang(), ft.freespace, ft.totalspace, false) + " " + appCommands.lang().free;
                g.drawString(freeLine, tx, ty - 6 + ascent);

                String filesLine = appCommands.lang().files_total + "  " + ft.numfiles;
                g.drawString(filesLine, tx, ty + 6 + ascent);

                String foldersLine = appCommands.lang().folders_total + "  " + ft.numfolders;
                g.drawString(foldersLine, tx, ty + 15 + ascent);
            } else {
                if (sel) {
                    g.setColor(Color.WHITE);
                } else g.setColor(Color.BLACK);

                if ((cur.flags & 1) == 0 && h >= 36 && w >= 48) {
                    // Render file size
                    StringBuilder sb = new StringBuilder();
                    formatService.printFileSize(appCommands.lang(), sb, cur.source.actualsizes[cur.index]);
                    String sizeStr = sb.toString();
                    Rectangle2D sizeBounds = fm.getStringBounds(sizeStr, g);
                    int sizeW = (int) Math.ceil(sizeBounds.getWidth());
                    int sizeAscent = fm.getAscent();
                    if (sizeW > w - 2) {
                        g.drawString(sizeStr, x + 2, ty + 1 + sizeAscent);
                    } else {
                        g.drawString(sizeStr, x + (w - sizeW) / 2, ty + 1 + sizeAscent);
                    }

                    // Render date
                    sb.setLength(0);
                    formatService.printDate(appCommands.lang(), sb, cur.source.times[cur.index]);
                    String dateStr = sb.toString();
                    Rectangle2D dateBounds = fm.getStringBounds(dateStr, g);
                    int dateW = (int) Math.ceil(dateBounds.getWidth());
                    if (dateW > w - 2) {
                        g.drawString(dateStr, x + 2, ty + 11 + fm.getAscent());
                    } else {
                        g.drawString(dateStr, x + (w - dateW) / 2, ty + 11 + fm.getAscent());
                    }

                    ty -= 12;
                }

                g.drawString(cur.name, tx, ty + fm.getAscent());

                if (sel) {
                    g.setColor(Color.BLACK);
                } else g.setColor(Color.WHITE);
            }

            // Restore original clip
            g.setClip(originalClip);
        }
    }

    void SetupInfoTip(DisplayFolder cur) {
        if (menu != null && menu.isVisible()) return;

        if (cur == null) {
            m_infotipwnd.enableWindow(false);
            return;
        }

        StringBuilder string = new StringBuilder();
        CFolderTree ft = document;

        // Build full path
        String fullPath = Paths.get(ft.mPath, folderPath(cur.source), cur.source.names[cur.index]).toString();

        // Get file attributes (for TIP_ATTRIB)
        java.nio.file.Path filePath = java.nio.file.Paths.get(fullPath);
        java.nio.file.attribute.BasicFileAttributes attrs = null;
        boolean fileExists = false;
        try {
            attrs = java.nio.file.Files.readAttributes(filePath, java.nio.file.attribute.BasicFileAttributes.class);
            fileExists = true;
        } catch (Exception e) {
            // File not found or inaccessible — proceed with empty attrs
            attrs = null;
        }

        int flags = appCommands.settings().infotip_flags();

        // TIP_PATH
        if ((flags & TIP_PATH.code) != 0) {
            string.append(Paths.get(ft.mPath, folderPath(cur.source)));
        }

        // TIP_NAME
        if ((flags & TIP_NAME.code) != 0) {
            string.append(cur.source.names[cur.index]);
        }

        if ((flags & (TIP_NAME.code | TIP_PATH.code)) != 0) {
            string.append('\n');
        }

        // TIP_SIZE
        if ((flags & TIP_SIZE.code) != 0) {
            formatService.printFileSize(appCommands.lang(), string, cur.source.actualsizes[cur.index]);
        }

        // TIP_ATTRIB
        if ((flags & TIP_ATTRIB.code) != 0) {
            if ((flags & TIP_SIZE.code) != 0 && fileExists) {
                string.append("  /  ");
            }

            // Map Windows attributes to Java (approximate)
            if (fileExists && attrs != null) {
                java.nio.file.attribute.DosFileAttributes dosAttrs = null;
                try {
                    dosAttrs = java.nio.file.Files.readAttributes(filePath, java.nio.file.attribute.DosFileAttributes.class);
                } catch (Exception ex) {
                    // Not on Windows or no DOS attrs
                }

                if (dosAttrs != null) {
                    if (dosAttrs.isArchive())     { string.append(" ").append(appCommands.lang().attribnames[0]); }
                    if (dosAttrs.isHidden())      { string.append(" ").append(appCommands.lang().attribnames[4]); }
                    if (dosAttrs.isReadOnly())    { string.append(" ").append(appCommands.lang().attribnames[6]); }
                    if (dosAttrs.isSystem())      { string.append(" ").append(appCommands.lang().attribnames[9]); }
                }

                // DIRECTORY
                if (attrs.isDirectory())      { string.append(" ").append(appCommands.lang().attribnames[2]); }

                // Others: not directly available in Java — skip (as in original, they'd be 0 if not set)
                // COMPRESSED, ENCRYPTED, OFFLINE, REPARSE_POINT, SPARSE_FILE, TEMPORARY
                // → Java has no standard API for these → leave out (matches behavior if file system doesn't report them)
            }
        }

        if ((flags & (TIP_ATTRIB.code | TIP_SIZE.code)) != 0) {
            string.append('\n');
        }

        // TIP_DATE
        if ((flags & TIP_DATE.code) != 0) {
            formatService.printDate(appCommands.lang(), string, cur.source.times[cur.index]);
            string.append('\n');
        }

        // TIP_ICON
        if ((flags & TIP_ICON.code) != 0) {
            // Java: get system icon via FileSystemView (Swing)
            try {
                javax.swing.Icon icon = Utils.iconByPath(Paths.get(fullPath));
                m_infotipwnd.setIcon(icon);
                m_infotipwnd.setIconPos(Tooltip.TW_LEFT);
            } catch (Exception e) {
                // Icon not available — skip
            }
        }

        m_infotipwnd.setAutoPos(true);
        m_infotipwnd.setWindowText(string.toString());
        m_infotipwnd.autoSize();
        m_infotipwnd.enableWindow(true);
        m_infotipwnd.redrawWindow();
    }

    private void SetupNameTip(DisplayFolder cur) {
        if (menu != null && menu.isVisible()) return;
        m_nametipwnd.enableWindow(false);

        if (cur == null) return;

        // Get text metrics using current mini font
        FontMetrics fm = component.getFontMetrics(minifont);
        Rectangle2D bounds = fm.getStringBounds(cur.name, null);
        int textWidth = (int) Math.ceil(bounds.getWidth());
        int textHeight = fm.getHeight();

        int x = cur.x;
        int y = cur.y;
        int w = cur.w + 1;
        int h = cur.h + 1;

        int tx, ty;
        int failed = 0;

        if (textWidth > w - 2) {
            tx = x + 2;
        } else if ((cur.flags & 1) != 0) {
            tx = x + 2;
            failed++;
        } else {
            tx = x + (w - textWidth) / 2;
            failed++;
        }

        if (textHeight > h - 2) {
            ty = y + 1;
        } else if ((cur.flags & 1) != 0) {
            ty = y + 1;
            failed++;
        } else {
            ty = y + (h - textHeight) / 2;
            failed++;
        }

        if (failed == 2) {
            return;
        }

        if ((cur.flags & 1) == 0 && h >= 36 && w >= 48) {
            ty -= 12;
        }

        // Set tooltip colors
        if (selected == cur) {
            m_nametipwnd.setBgColor(Color.BLACK);
            m_nametipwnd.setTextColor(Color.WHITE);
        } else if ((cur.flags & 4) != 0) {
            m_nametipwnd.setBgColor(BoxColors[(cur.depth & 7) + 8]);
            m_nametipwnd.setTextColor(Color.BLACK);
        } else {
            m_nametipwnd.setBgColor(BoxColors[cur.depth & 7]);
            m_nametipwnd.setTextColor(Color.BLACK);
        }

        m_nametipwnd.setWindowText(cur.name);
        m_nametipwnd.autoSize();
        m_nametipwnd.moveWindow(this.component, tx - 2, ty - 1);
        m_nametipwnd.pushOnScreen();

        m_nametipwnd.enableWindow(true);
        m_nametipwnd.redrawWindow();
    }

    private static void fillBox(Graphics2D g, Color brush, int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            g.setColor(brush);
            g.fillRect(x, y, width, height);
        }
    }

    private static void drawBox(Graphics2D g, Color brush, int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            g.setColor(brush);
            g.drawRect(x, y, width - 1, height - 1);
        }
    }

    private static void drawDualBox(Graphics2D g, Color topLeft, Color bottomRight,
                                    int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        g.setColor(topLeft);
        g.fillRect(x, y, width, 1);      // top
        g.fillRect(x, y, 1, height);     // left
        g.setColor(bottomRight);
        g.fillRect(x, y + height - 1, width, 1);        // bottom
        g.fillRect(x + width - 1, y, 1, height);        // right
    }

    private int getWidth() {
        return component.getWidth();
    }
    private int getHeight() {
        return component.getHeight();
    }

    private void repaint() {
        component.repaint();
    }

    private JComponent component;

    private CFolderTree document;
    private CFolder rootfolder;
    private DisplayFolder displayfolders, displayend;
    private int hmin, vmin;
    private Tooltip m_infotipwnd, m_nametipwnd;
    private DisplayFolder lastcur;

    private int zoomlevel;
    private DisplayFolder selected;
    private JPopupMenu menu;

    private Point hightlightPathPoint;
    // Animation state
    private Rectangle animStart;
    private Rectangle animEnd;
    private int animStep = -1; // -1 = inactive, 0–15 = active
    private Runnable animOnComplete;

    // timer to limit repaint rate
    private Timer repaintLimitTimer = null;

    private final Font minifont = new Font("SansSerif", Font.PLAIN, 9);
    private final AppCommands appCommands;
    private final FormatService formatService;
}