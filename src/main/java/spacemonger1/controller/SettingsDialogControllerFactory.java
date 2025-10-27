package spacemonger1.controller;

import spacemonger1.service.DialogUnitsService;
import spacemonger1.service.Lang;
import spacemonger1.service.LangService;
import spacemonger1.service.Settings;

import javax.swing.JFrame;
import java.util.function.Consumer;

public class SettingsDialogControllerFactory {
    private final LangService langService;
    private final DialogUnitsService dialogUnitsService;

    public SettingsDialogControllerFactory(LangService langService, DialogUnitsService dialogUnitsService) {
        this.langService = langService;
        this.dialogUnitsService = dialogUnitsService;
    }

    public SettingsDialogController newInstance(JFrame owner, Settings settings, Lang lang, Consumer<Settings> onOk) {
        return new SettingsDialogController(langService, dialogUnitsService, owner, lang, settings, onOk);
    }
}
