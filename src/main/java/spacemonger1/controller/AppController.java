package spacemonger1.controller;

import spacemonger1.component.Toolbar;
import spacemonger1.fs.FileSystems;
import spacemonger1.service.CFolderTree;
import spacemonger1.service.Lang;
import spacemonger1.service.LangService;
import spacemonger1.service.Settings;
import spacemonger1.service.SettingsService;
import spacemonger1.service.ToolbarIconsService;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AppController {
    private final ToolbarIconsService toolbarIconsService;
    private final DriveDialogControllerFactory driveDialogFactory;
    private final FolderDialogControllerFactory folderDialogControllerFactory;
    private final FileSystems fileSystems;
    private final FolderViewFactory folderViewFactory;
    private final SettingsService settingsService;
    private final LangService langService;
    private final SettingsDialogControllerFactory settingsDialogControllerFactory;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private JFrame frame;
    private JPanel content;
    private Toolbar<ToolbarButtons> toolbar;
    private FolderView folderView;

    private boolean showFreeSpace = true;
    private Lang lang;
    private CFolderTree tree;
    private Drive selectedDrive;
    private Settings settings;

    public AppController(ToolbarIconsService toolbarIconsService, DriveDialogControllerFactory driveDialogFactory, FolderDialogControllerFactory folderDialogControllerFactory, FileSystems fileSystems, FolderViewFactory folderViewFactory, SettingsService settingsService, LangService langService, SettingsDialogControllerFactory settingsDialogControllerFactory) {
        this.toolbarIconsService = toolbarIconsService;
        this.driveDialogFactory = driveDialogFactory;
        this.folderDialogControllerFactory = folderDialogControllerFactory;
        this.fileSystems = fileSystems;
        this.folderViewFactory = folderViewFactory;
        this.settingsService = settingsService;
        this.langService = langService;
        this.settingsDialogControllerFactory = settingsDialogControllerFactory;
    }

    public void init() {
        settings = settingsService.load();
        lang = langService.byCode(settings.lang());
        frame = new JFrame();
        frame.setTitle("SpaceMonger One");
        if (settings.save_pos()) {
            frame.setLocation(new Point(settings.rect().x, settings.rect().y));
        }
        try {
            frame.setIconImage(ImageIO.read(getClass().getClassLoader().getResourceAsStream("SpaceMonger_hres.png")));
        } catch (IOException e) {
        }

        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        content = new JPanel(null);
        if (settings.save_pos()) {
            content.setPreferredSize(new Dimension(settings.rect().width, settings.rect().height));
        } else {
            content.setPreferredSize(new Dimension(1200, 800));
        }
        content.setLayout(null);
        frame.setContentPane(content);

        var icons = toolbarIconsService;
        toolbar = Toolbar.<ToolbarButtons>newBuilder()
            .button(ToolbarButtons.Open, icons.at(0), () -> lang.toolbar_open, this::onOpen)
            .button(ToolbarButtons.Reload, icons.at(42), () -> lang.toolbar_reload, this::onReload)
            .separator()
            .button(ToolbarButtons.ZoomFull, icons.at(89), () -> lang.toolbar_zoomfull, this::onZoomFull)
            .button(ToolbarButtons.ZoomIn, icons.at(133), () -> lang.toolbar_zoomin, this::onZoomIn)
            .button(ToolbarButtons.ZoomOut, icons.at(176), () -> lang.toolbar_zoomout, this::onZoomOut)
            .separator()
            .toggle(ToolbarButtons.FreeSpace, icons.at(216), () -> lang.toolbar_freespace, this::onFreeSpace)
            .separator()
            .button(ToolbarButtons.RunOrOpen, icons.at(262), () -> lang.toolbar_runoropen, this::onRunOrOpen)
            .button(ToolbarButtons.Delete, icons.at(306), () -> lang.toolbar_delete, this::onDelete)
            .separator()
            .button(ToolbarButtons.Setup, icons.at(348), () -> lang.toolbar_setup, this::onSetup)
            .separator()
            .button(ToolbarButtons.About, icons.at(392), () -> lang.toolbar_about, this::onAbout)
            .build();

        toolbar.toggle(ToolbarButtons.FreeSpace, showFreeSpace);
        content.add(toolbar.component());

        toolbar.component().setBounds(0, 0, 1200, toolbar.component().getPreferredSize().height);

        var self = this;
        folderView = folderViewFactory.newInstance(new FolderView.AppCommands() {
            @Override
            public void setTitle(String title) {
                frame.setTitle(title);
            }

            @Override
            public void updated() {
                updateToolbarState();
            }

            @Override
            public Settings settings() {
                return settings;
            }

            @Override
            public Lang lang() {
                return lang;
            }

            @Override
            public boolean showFreeSpace() {
                return showFreeSpace;
            }

            @Override
            public void zoomIn() {
                self.onZoomIn();
            }

            @Override
            public void zoomOut() {
                self.onZoomOut();
            }

            @Override
            public void zoomFull() {
                self.onZoomFull();
            }

            @Override
            public void runOrOpen() {
                self.onRunOrOpen();
            }

            @Override
            public void delete() {
                self.onDelete();
            }

            @Override
            public void openDrive() {
                self.onOpen();
            }

            @Override
            public void reload() {
                self.onReload();
            }

            @Override
            public void toggleFreeSpace() {
                showFreeSpace = !showFreeSpace;
                folderView.onUpdate();
            }
        }, frame);

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                doLayout();
                saveWindowPos();
            }
            @Override
            public void componentMoved(ComponentEvent e) {
                saveWindowPos();
            }
        });


        updateToolbarState();
        doLayout();

        frame.pack();
    }

    private void saveWindowPos() {
        var newLoc = frame.getLocationOnScreen();
        var newSize = content.getSize();
        var newPos = new Rectangle(newLoc.x, newLoc.y, newSize.width, newSize.height);
        var oldPos = settings.rect();
        if (!newPos.equals(oldPos)) {
            settings = settings.withRect(newPos);
            settingsService.save(settings);
        }
    }

    private void onAbout() {
        new AboutDialog(frame).setVisible(true);
    }

    private void onSetup() {
        settingsDialogControllerFactory.newInstance(frame, settings, lang, s -> {
            settings = s;
            settingsService.save(settings);
            lang = langService.byCode(settings.lang());
            toolbar.reloadText();
            folderView.onUpdate();
            updateToolbarState();
            frame.repaint();
        }).show();
    }

    private void onDelete() {
        try {
            var selection = folderView.selection();
            if (selection == null) return;
            fileSystems.moveToTrash(selection.path());
            folderView.deleteSelected();
            if (settings.auto_rescan()) {
                onReload();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onRunOrOpen() {
        var selection = folderView.selection();
        if (selection == null) return;
        try {
            Desktop.getDesktop().open(selection.path().toFile());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onFreeSpace(boolean aBoolean) {
        showFreeSpace = aBoolean;
        updateToolbarState();
        folderView.onUpdate();
    }

    private void onZoomOut() {
        folderView.zoomOut();
    }

    private void onZoomIn() {
        folderView.zoomIn();
    }

    private void onZoomFull() {
        folderView.ZoomFull();
    }

    private void onReload() {
        setSelectedDrive(selectedDrive);
    }

    private void onOpen() {
        driveDialogFactory.newInstance(frame, lang).show().thenAccept(driveOpt -> {
            if (driveOpt.isPresent()) {
                onDriveSelected(driveOpt.get());
            }
        });
    }

    private void onDriveSelected(DriveDialogController.DriveDialogResult selectionResult) {
        if (!selectionResult.isDriveSelected()) {
            onFreeSpace(false);
        }
        setSelectedDrive(selectionResult.drive());
    }

    private void setSelectedDrive(Drive drive) {
        selectedDrive = drive;
        tree = null;
        updateToolbarState();

        AtomicReference<CFolderTree> folderTree = new AtomicReference<>();
        FolderDialogController folderDialog = folderDialogControllerFactory.newInstance(frame, lang, () -> {
            var ft = folderTree.get();
            if (ft != null) ft.cancel();
        });

        Thread.ofPlatform().start(() -> {
            CFolderTree ft = new CFolderTree(fileSystems);
            folderTree.set(ft);
            ft.LoadTree(drive);
        });

        AtomicReference<ScheduledFuture<?>> scheduledFuture = new AtomicReference<>();
        scheduledFuture.set(scheduler.scheduleAtFixedRate(() -> {
            SwingUtilities.invokeLater(() -> {
                CFolderTree ft = folderTree.get();
                if (ft == null) return;
                if (ft.isCancelled()) {
                    folderDialog.hide();
                    scheduledFuture.get().cancel(false);
                    return;
                }
                var status = ft.status();
                folderDialog.update(status.currentPath(), status.files(), status.folders(), status.progress());
                if (status.tree() != null) {
                    tree = status.tree();
                    folderDialog.hide();
                    updateToolbarState();
                    folderView.setDocument(tree);
                    scheduledFuture.get().cancel(false);
                }

            });
        }, 0, 200, TimeUnit.MILLISECONDS));

        folderDialog.show();

    }

    private void doLayout() {
        toolbar.component().setSize(content.getWidth(), toolbar.component().getHeight());
        folderView.component().setBounds(0, toolbar.component().getHeight(), content.getWidth(), content.getHeight() - toolbar.component().getHeight());
    }

    private void updateToolbarState() {
        FolderView.Selection selection = folderView.selection();

        toolbar.enable(ToolbarButtons.Reload, tree != null);
        toolbar.enable(ToolbarButtons.ZoomFull, tree != null && !folderView.isZoomFull());
        toolbar.enable(ToolbarButtons.ZoomIn, tree != null && selection != null && selection.isDirectory());
        toolbar.enable(ToolbarButtons.ZoomOut, tree != null && !folderView.isZoomFull());
        toolbar.enable(ToolbarButtons.FreeSpace, tree != null);
        toolbar.enable(ToolbarButtons.RunOrOpen,  selection != null);
        toolbar.enable(ToolbarButtons.Delete, selection != null && !settings.disable_delete());

        toolbar.toggle(ToolbarButtons.FreeSpace, showFreeSpace);
    }

    private enum ToolbarButtons {
        Open, Reload, ZoomFull, ZoomIn, ZoomOut, FreeSpace, RunOrOpen, Delete, Setup, About
    }
}
