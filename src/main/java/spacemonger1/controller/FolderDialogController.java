package spacemonger1.controller;

import spacemonger1.service.DialogUnitsService;
import spacemonger1.service.Lang;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FolderDialogController {
    private final DialogUnitsService dialogUnitsService;
    private final List<Image> animationFrames;
    private final JFrame owner;

    private final ScheduledExecutorService animationScheduler;
    private int frame = 0;
    private final Runnable onCancel;
    private final Lang lang;

    private JPanel content;
    private JLabel path;
    private JLabel files;
    private JLabel folders;
    private JProgressBar progressBar;
    private JDialog dialog;

    public FolderDialogController(
        DialogUnitsService dialogUnitsService, JFrame owner,
        List<Image> animationFrames,
        Lang lang,
        Runnable onCancel
    ) {
        this.dialogUnitsService = dialogUnitsService;
        this.owner = owner;
        this.animationFrames = animationFrames;
        this.onCancel = onCancel;
        this.lang = lang;
        this.animationScheduler = Executors.newScheduledThreadPool(1);
    }

    public void show() {
        dialog = new JDialog(owner, true);
        var u = dialogUnitsService.compute(new JLabel().getFont());
        content = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // This clears the background (important!)
                g.drawImage(animationFrames.get(frame), u.w(7), u.h(13), null);
            }
        };
        content.setPreferredSize(new Dimension(u.w(250), u.h(54)));
        dialog.setContentPane(content);
        dialog.setTitle(lang.scanning);

        JButton cancelButton = new JButton(lang.cancel);
        cancelButton.setBounds(u.w(203),u.h(24),u.w(40),u.h(14));
        cancelButton.addActionListener(e -> onCancel.run());
        content.add(cancelButton);

        JLabel filesFound = new JLabel(lang.files_found);
        filesFound.setBounds(u.w(103),u.h(21),u.w(66),u.h(8));
        content.add(filesFound);

        JLabel foldersFound = new JLabel(lang.folders_found);
        foldersFound.setBounds(u.w(103),u.h(30),u.w(66),u.h(8));
        content.add(foldersFound);

        path = new JLabel("");
        path.setBounds(u.w(7),u.h(4),u.w(236),u.h(9));
        content.add(path);

        files = new JLabel("0");
        files.setBounds(u.w(171),u.h(21),u.w(30),u.h(8));
        content.add(files);

        folders = new JLabel("0");
        folders.setBounds(u.w(171),u.h(30),u.w(30),u.h(8));
        content.add(folders);

        progressBar = new JProgressBar(0, 1000);
        progressBar.setBounds(u.w(7),u.h(44),u.w(236),u.h(6));
        content.add(progressBar);


        animationScheduler.scheduleAtFixedRate(() -> {
            SwingUtilities.invokeLater(() -> {
                content.repaint();
                frame++;
                if  (frame >= animationFrames.size()) frame = 0;
            });
        }, 0, 400, TimeUnit.MILLISECONDS);

        dialog.pack();
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                animationScheduler.close();
                onCancel.run();
            }
        });
        dialog.setLocation(owner.getLocation());
        dialog.setVisible(true);
    }

    public void update(String path, long files, long folders, double progress) {
        if (path == null) return;
        this.path.setText(path);
        this.files.setText(String.valueOf(files));
        this.folders.setText(String.valueOf(folders));
        this.progressBar.setValue((int) (progress * 1000));
        content.repaint();

    }

    public void hide() {
        animationScheduler.shutdownNow();
        dialog.dispose();
    }
}
