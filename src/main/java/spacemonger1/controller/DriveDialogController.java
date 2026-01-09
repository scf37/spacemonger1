package spacemonger1.controller;

import spacemonger1.Utils;
import spacemonger1.component.ListView;
import spacemonger1.service.DialogUnitsService;
import spacemonger1.service.Lang;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DriveDialogController {
    private final DialogUnitsService dialogUnitsService;
    private final JFrame owner;
    private final List<Drive> drives;
    private final Lang lang;

    private JDialog dialog;

    public DriveDialogController(DialogUnitsService dialogUnitsService, JFrame owner, List<Drive> drives, Lang lang) {
        this.dialogUnitsService = dialogUnitsService;
        this.owner = owner;
        this.drives = drives;
        this.lang = lang;
    }

    public CompletableFuture<Optional<Drive>> show() {
        CompletableFuture<Optional<Drive>> result = new  CompletableFuture<>();
        dialog = new JDialog(owner, true);
        var u = dialogUnitsService.compute(new JLabel().getFont());

        result.whenComplete((r, e) -> {
            dialog.dispose();
        });

        ListView<Drive> listView = ListView.newInstance(
            drives.stream().map(drive -> new ListView.Item<>(drive, Utils.iconByPath(drive.rootPath()), drive.name())).toList(),
            u.w(60), u.h(40),
            drive -> {
                result.complete(Optional.of(drive.id()));
            }
        );
        JPanel content = new JPanel(null);
        content.setPreferredSize(new Dimension(u.w(221), u.h(180)));
        dialog.setContentPane(content);
        dialog.setTitle(lang.selectdrive);
        listView.component().setBounds(u.w(7), u.h(5), u.w(207), u.h(139));
        content.add(listView.component());
        JButton okButton = new JButton(lang.ok);
        JButton cancelButton = new JButton(lang.cancel);
        JButton browseButton = new JButton("Browse...");
        content.add(okButton);
        content.add(cancelButton);
        content.add(browseButton);

        okButton.setBounds(u.w(140),u.h(151),u.w(60),u.h(14));
        okButton.addActionListener(e -> {
            if (listView.getSelectedItem() != null) {
                result.complete(Optional.of(listView.getSelectedItem().id()));
            }
        });
        cancelButton.setBounds(u.w(75),u.h(151),u.w(60),u.h(14));
        cancelButton.addActionListener(e -> result.complete(Optional.empty()));
        browseButton.setBounds(u.w(10), u.h(151), u.w(60), u.h(14));
        browseButton.addActionListener(e -> browseForFolder().ifPresent(d -> result.complete(Optional.of(d))));

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                result.complete(Optional.empty());
            }
        });

        dialog.pack();
        dialog.setLocation(owner.getLocation());
        dialog.setVisible(true);

        return result;
    }

    private Optional<Drive> browseForFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(lang.selectdrive);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        int choice = chooser.showOpenDialog(dialog);
        if (choice == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            try {
                FileStore store = Files.getFileStore(path);
                long total = store.getTotalSpace();
                long used = store.getTotalSpace() - store.getUsableSpace();
                String name = path.getFileName() != null ? path.getFileName().toString() : path.toString();
                return Optional.of(new Drive(name, path, total, used));
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }
}
