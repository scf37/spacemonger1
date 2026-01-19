package spacemonger1.controller;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;

public class AboutDialog extends JDialog {
    public AboutDialog(Window owner) {
        super(owner, "About SpaceMonger One", ModalityType.MODELESS);
        setLayout(new BorderLayout());

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        Icon icon = null;
        try {
            icon = new ImageIcon(ImageIO.read(getClass().getClassLoader().getResourceAsStream("SpaceMonger_hres.png")).getScaledInstance(32, 32, Image.SCALE_SMOOTH));
        } catch (IOException e) {
        }
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel title = new JLabel("SpaceMonger One 1.0.0");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 2));
        JLabel author = new JLabel("Scf37 (C) 2025-2026");

        JLabel link = new JLabel("<html><a href=''>https://github.com/scf37/spacemonger1</a></html>");
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/scf37/spacemonger1"));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AboutDialog.this,
                        "Could not open browser: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JLabel licence = new JLabel("Distributed under the MIT License.");

        JLabel copyright = new JLabel("Original work by Sean Werkema (C) 1998, 1999, 2000");

        // Add vertical glue for spacing
        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(author);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(link);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(licence);
        textPanel.add(Box.createVerticalStrut(24));
        textPanel.add(copyright);

        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.add(iconLabel);
        leftColumn.add(Box.createVerticalGlue());

        JPanel leftRight = new JPanel(new BorderLayout());
        leftRight.add(leftColumn, BorderLayout.WEST);
        leftRight.add(textPanel, BorderLayout.CENTER);

        content.add(leftRight, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(okButton);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }
}