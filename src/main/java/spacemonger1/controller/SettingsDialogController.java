package spacemonger1.controller;

import spacemonger1.service.DialogUnits;
import spacemonger1.service.DialogUnitsService;
import spacemonger1.service.Lang;
import spacemonger1.service.LangService;
import spacemonger1.service.Settings;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import static spacemonger1.service.TipSettings.TIP_ATTRIB;
import static spacemonger1.service.TipSettings.TIP_DATE;
import static spacemonger1.service.TipSettings.TIP_ICON;
import static spacemonger1.service.TipSettings.TIP_NAME;
import static spacemonger1.service.TipSettings.TIP_PATH;
import static spacemonger1.service.TipSettings.TIP_SIZE;

public class SettingsDialogController {
    private final LangService langService;
    private final DialogUnitsService dialogUnitsService;

    private final JFrame owner;
    private final Lang lang;
    private final Settings initialSettings;
    private final Consumer<Settings> onOk;

    private JDialog dialog;
    private JPanel content;

    // Static labels
    private JLabel densityLabel;
    private JLabel filesLabel;
    private JLabel foldersLabel;
    private JLabel biasLabel;
    private JLabel delayLabel1;
    private JLabel delayLabel2;
    private JLabel msecLabel1;
    private JLabel msecLabel2;
    private JLabel horzLabel;
    private JLabel vertLabel;
    private JLabel equalLabel;

    private JPanel layoutGroupBox;
    private JPanel displayColorsGroupBox;
    private JPanel tooltipsGroupBox;
    private JPanel miscOptionsGroupBox;


    // Controls
    private JComboBox<String> langCombo;
    private JComboBox<String> densityCombo;
    private JSlider biasSlider;
    private JComboBox<String> fileColorCombo;
    private JComboBox<String> folderColorCombo;
    private JCheckBox showNameTipsCheck;
    private JTextField nameTipDelayField;
    private JCheckBox showRolloverBoxCheck;
    private JCheckBox showInfoTipsCheck;
    private JCheckBox infoTipPathCheck;
    private JCheckBox infoTipDateCheck;
    private JCheckBox infoTipNameCheck;
    private JCheckBox infoTipSizeCheck;
    private JCheckBox infoTipIconCheck;
    private JCheckBox infoTipAttribCheck;
    private JTextField infoTipDelayField;
    private JCheckBox autoRescanCheck;
    private JCheckBox disableDeleteCheck;
    private JCheckBox animatedCheck;
    private JCheckBox savePosCheck;
    private JButton okButton;
    private JButton cancelButton;

    public SettingsDialogController(
        LangService langService, DialogUnitsService dialogUnitsService,
        JFrame owner,
        Lang lang,
        Settings initialSettings,
        Consumer<Settings> onOk
    ) {
        this.langService = langService;
        this.dialogUnitsService = dialogUnitsService;
        this.owner = owner;
        this.lang = lang;
        this.initialSettings = initialSettings;
        this.onOk = onOk;
    }

    public void show() {
        dialog = new JDialog(owner, "", true);

        var u = dialogUnitsService.compute(new JLabel().getFont());

        content = new JPanel(null);
        content.setPreferredSize(new Dimension(u.w(280), u.h(213)));
        dialog.setContentPane(content);

        // --- Group Boxes (with titles) ---
        layoutGroupBox = createTitledGroupBox(u.w(7), u.h(4), u.w(139), u.h(68));
        displayColorsGroupBox = createTitledGroupBox(u.w(153), u.h(29), u.w(120), u.h(43));
        tooltipsGroupBox = createTitledGroupBox(u.w(7), u.h(74), u.w(266), u.h(73));
        miscOptionsGroupBox = createTitledGroupBox(u.w(6), u.h(149), u.w(267), u.h(38));

        content.add(layoutGroupBox);
        content.add(displayColorsGroupBox);
        content.add(tooltipsGroupBox);
        content.add(miscOptionsGroupBox);

        // --- Initialize Controls ---
        initCombos(u);
        initCheckboxes(u);
        initTextFields(u);
        initSlider(u);
        initButtons(u);
        initLang(lang);

        // --- Populate from initial settings ---
        loadSettings(initialSettings);


        // Finalize
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private void initCombos(DialogUnits u) {
        // Language
        langCombo = new JComboBox<>(langService.languages().stream().map(it -> it.lang_name).toArray(String[]::new));
        langCombo.setBounds(u.w(191), u.h(8), u.w(82), u.h(14));
        langCombo.addActionListener(e -> {
            initLang(langService.languages().get(langCombo.getSelectedIndex()));
            dialog.repaint();
        });
        content.add(langCombo);

        // Density
        densityCombo = new JComboBox<>();
        densityCombo.setBounds(u.w(42), u.h(18), u.w(90), u.h(14));
        content.add(densityCombo);

        // Colors
        fileColorCombo = new JComboBox<>();
        fileColorCombo.setBounds(u.w(191), u.h(40), u.w(72), u.h(14));
        content.add(fileColorCombo);

        folderColorCombo = new JComboBox<>();
        folderColorCombo.setBounds(u.w(191), u.h(55), u.w(72), u.h(14));
        content.add(folderColorCombo);
    }

    private void initCheckboxes(DialogUnits u) {
        showNameTipsCheck = new JCheckBox();
        showNameTipsCheck.setBounds(u.w(24), u.h(85), u.w(104), u.h(10));
        content.add(showNameTipsCheck);

        showRolloverBoxCheck = new JCheckBox();
        showRolloverBoxCheck.setBounds(u.w(24), u.h(126), u.w(119), u.h(8));
        content.add(showRolloverBoxCheck);

        showInfoTipsCheck = new JCheckBox();
        showInfoTipsCheck.setBounds(u.w(144), u.h(85), u.w(104), u.h(10));
        content.add(showInfoTipsCheck);

        infoTipPathCheck = new JCheckBox();
        infoTipPathCheck.setBounds(u.w(153), u.h(96), u.w(50), u.h(10));
        content.add(infoTipPathCheck);

        infoTipDateCheck = new JCheckBox();
        infoTipDateCheck.setBounds(u.w(205), u.h(96), u.w(50), u.h(10));
        content.add(infoTipDateCheck);

        infoTipNameCheck = new JCheckBox();
        infoTipNameCheck.setBounds(u.w(153), u.h(106), u.w(50), u.h(10));
        content.add(infoTipNameCheck);

        infoTipSizeCheck = new JCheckBox();
        infoTipSizeCheck.setBounds(u.w(205), u.h(106), u.w(50), u.h(10));
        content.add(infoTipSizeCheck);

        infoTipIconCheck = new JCheckBox();
        infoTipIconCheck.setBounds(u.w(153), u.h(116), u.w(50), u.h(10));
        content.add(infoTipIconCheck);

        infoTipAttribCheck = new JCheckBox();
        infoTipAttribCheck.setBounds(u.w(205), u.h(116), u.w(50), u.h(10));
        content.add(infoTipAttribCheck);

        autoRescanCheck = new JCheckBox();
        autoRescanCheck.setBounds(u.w(24), u.h(160), u.w(120), u.h(10));
        content.add(autoRescanCheck);

        disableDeleteCheck = new JCheckBox();
        disableDeleteCheck.setBounds(u.w(24), u.h(171), u.w(120), u.h(10));
        content.add(disableDeleteCheck);

        animatedCheck = new JCheckBox();
        animatedCheck.setBounds(u.w(144), u.h(160), u.w(120), u.h(10));
        content.add(animatedCheck);

        savePosCheck = new JCheckBox();
        savePosCheck.setBounds(u.w(144), u.h(171), u.w(120), u.h(10));
        content.add(savePosCheck);
    }

    private void initTextFields(DialogUnits u) {
        nameTipDelayField = new JTextField();
        nameTipDelayField.setHorizontalAlignment(JTextField.RIGHT);
        nameTipDelayField.setBounds(u.w(59), u.h(96), u.w(27), u.h(12));
        content.add(nameTipDelayField);

        infoTipDelayField = new JTextField();
        infoTipDelayField.setHorizontalAlignment(JTextField.RIGHT);
        infoTipDelayField.setBounds(u.w(182), u.h(129), u.w(27), u.h(12));
        content.add(infoTipDelayField);
    }

    private void initSlider(DialogUnits u) {
        biasSlider = new JSlider(-20, 20, 0);
        biasSlider.setMajorTickSpacing(5);
        biasSlider.setMinorTickSpacing(5);
        biasSlider.setPaintTicks(true);
        biasSlider.setBounds(u.w(39), u.h(49), u.w(95), u.h(17));
        content.add(biasSlider);
    }

    private void initButtons(DialogUnits u) {
        cancelButton = new JButton();
        cancelButton.setBounds(u.w(86), u.h(194), u.w(50), u.h(14));
        cancelButton.addActionListener(e -> dialog.dispose());
        content.add(cancelButton);

        okButton = new JButton();
        okButton.setBounds(u.w(142), u.h(194), u.w(50), u.h(14));
        okButton.addActionListener(this::onOkPressed);
        dialog.getRootPane().setDefaultButton(okButton);
        content.add(okButton);

        densityLabel = addStaticLabel(u, u.w(14), u.h(20));
        filesLabel = addStaticLabel(u, u.w(160), u.h(43));
        biasLabel = addStaticLabel(u, u.w(14), u.h(55));
        horzLabel = addCenteredLabel(u, u.w(35), u.h(40));
        vertLabel = addCenteredLabel(u, u.w(118), u.h(40));
        equalLabel = addCenteredLabel(u, u.w(76), u.h(40));
        foldersLabel =addStaticLabel(u, u.w(160), u.h(58));
        delayLabel1 = addStaticLabel(u, u.w(153), u.h(131));
        msecLabel1 = addStaticLabel(u, u.w(212), u.h(131));
        delayLabel2 = addStaticLabel(u, u.w(30), u.h(98));
        msecLabel2 = addStaticLabel(u, u.w(89), u.h(98));
    }

    private void initLang(Lang lang) {
        dialog.setTitle(lang.settings);

        cancelButton.setText(lang.cancel);
        okButton.setText(lang.ok);

        densityLabel.setText(lang.density);
        filesLabel.setText(lang.files);
        biasLabel.setText(lang.bias);
        horzLabel.setText(lang.horz);
        vertLabel.setText(lang.vert);
        equalLabel.setText(lang.equal);
        foldersLabel.setText(lang.folders);
        delayLabel1.setText(lang.delay);
        msecLabel1.setText(lang.msec);
        delayLabel2.setText(lang.delay);
        msecLabel2.setText(lang.msec);

        ((TitledBorder)layoutGroupBox.getBorder()).setTitle(lang.layout);
        ((TitledBorder)displayColorsGroupBox.getBorder()).setTitle(lang.displaycolors);
        ((TitledBorder)tooltipsGroupBox.getBorder()).setTitle(lang.tooltips);
        ((TitledBorder)miscOptionsGroupBox.getBorder()).setTitle(lang.miscoptions);

        showNameTipsCheck.setText(lang.shownametips);
        showRolloverBoxCheck.setText(lang.showrolloverbox);
        showInfoTipsCheck.setText(lang.showinfotips);
        infoTipPathCheck.setText(lang.fullpath);
        infoTipDateCheck.setText(lang.datetime);
        infoTipNameCheck.setText(lang.filename);
        infoTipSizeCheck.setText(lang.filesize);
        infoTipIconCheck.setText(lang.smallicon);
        infoTipAttribCheck.setText(lang.attrib);
        autoRescanCheck.setText(lang.autorescan);
        disableDeleteCheck.setText(lang.disabledelete);
        animatedCheck.setText(lang.animatedzoom);
        savePosCheck.setText(lang.savepos);

        int sel = densityCombo.getSelectedIndex();
        densityCombo.setModel(new DefaultComboBoxModel<>(lang.densitynames));
        densityCombo.setSelectedIndex(sel);

        sel = fileColorCombo.getSelectedIndex();
        fileColorCombo.setModel(new DefaultComboBoxModel<>(lang.colornames));
        fileColorCombo.setSelectedIndex(sel);

        sel = folderColorCombo.getSelectedIndex();
        folderColorCombo.setModel(new DefaultComboBoxModel<>(lang.colornames));
        folderColorCombo.setSelectedIndex(sel);
    }

    private JLabel addStaticLabel(DialogUnits u, int x, int y) {
        JLabel label = new JLabel();
        label.setBounds(x, y, u.w(30), u.h(8));
        content.add(label);
        return label;
    }

    private JLabel addCenteredLabel(DialogUnits u, int x, int y) {
        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setBounds(x, y, u.w(21), u.h(8));
        content.add(label);
        return label;
    }

    private void loadSettings(Settings s) {
        // Combos
        densityCombo.setSelectedIndex(clamp(s.density() + 3, 0, lang.densitynames.length - 1));
        fileColorCombo.setSelectedIndex(clamp(s.file_color(), 0, lang.colornames.length - 1));
        folderColorCombo.setSelectedIndex(clamp(s.folder_color(), 0, lang.colornames.length - 1));
        int i = 0;
        for (Lang l: langService.languages()) {
            if (l.lang_code.equals(s.lang())) {
                langCombo.setSelectedIndex(i);
                break;
            }
            i++;
        }

        // Slider
        int sliderValue = s.bias(); // bias -20..+20
        biasSlider.setValue(clamp(sliderValue, -20, 20));

        // Checkboxes
        showNameTipsCheck.setSelected(s.show_name_tips());
        showRolloverBoxCheck.setSelected(s.rollover_box());
        showInfoTipsCheck.setSelected(s.show_info_tips());
        autoRescanCheck.setSelected(s.auto_rescan());
        disableDeleteCheck.setSelected(s.disable_delete());
        animatedCheck.setSelected(s.animated_zoom());
        savePosCheck.setSelected(s.save_pos());

        // Info tip flags
        int flags = s.infotip_flags();
        infoTipPathCheck.setSelected((flags & TIP_PATH.code) != 0);
        infoTipDateCheck.setSelected((flags & TIP_DATE.code) != 0);
        infoTipNameCheck.setSelected((flags & TIP_NAME.code) != 0);
        infoTipSizeCheck.setSelected((flags & TIP_SIZE.code) != 0);
        infoTipIconCheck.setSelected((flags & TIP_ICON.code) != 0);
        infoTipAttribCheck.setSelected((flags & TIP_ATTRIB.code) != 0);

        // Text fields
        nameTipDelayField.setText(String.valueOf(s.nametip_delay()));
        infoTipDelayField.setText(String.valueOf(s.infotip_delay()));
    }

    private void onOkPressed(ActionEvent e) {
        try {
            // Parse numeric fields
            int nameDelay = parseInt(nameTipDelayField.getText(), initialSettings.nametip_delay());
            int infoDelay = parseInt(infoTipDelayField.getText(), initialSettings.infotip_delay());

            // Build infotip flags
            int flags = 0;
            if (infoTipPathCheck.isSelected()) flags |= TIP_PATH.code;
            if (infoTipDateCheck.isSelected()) flags |= TIP_DATE.code;
            if (infoTipNameCheck.isSelected()) flags |= TIP_NAME.code;
            if (infoTipSizeCheck.isSelected()) flags |= TIP_SIZE.code;
            if (infoTipIconCheck.isSelected()) flags |= TIP_ICON.code;
            if (infoTipAttribCheck.isSelected()) flags |= TIP_ATTRIB.code;

            int bias = biasSlider.getValue();
            bias = clamp(bias, -20, 20);

            Settings updated = new Settings(
                densityCombo.getSelectedIndex() - 3,
                fileColorCombo.getSelectedIndex(),
                folderColorCombo.getSelectedIndex(),
                autoRescanCheck.isSelected(),
                animatedCheck.isSelected(),
                disableDeleteCheck.isSelected(),
                showRolloverBoxCheck.isSelected(),
                bias,
                savePosCheck.isSelected(),
                initialSettings.rect(),      // unchanged
                initialSettings.showcmd(),   // unchanged
                showNameTipsCheck.isSelected(),
                nameDelay,
                showInfoTipsCheck.isSelected(),
                flags,
                infoDelay,
                langService.languages().get(langCombo.getSelectedIndex()).lang_code
            );

            onOk.accept(updated);
            dialog.dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialog, "Invalid input", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private JPanel createTitledGroupBox(int x, int y, int width, int height) {
        JPanel panel = new JPanel(null);
        Border etched = BorderFactory.createEtchedBorder();
        Border titled = BorderFactory.createTitledBorder(etched, "");
        panel.setBorder(titled);
        panel.setOpaque(false);
        panel.setBounds(x, y, width, height);
        return panel;
    }
}