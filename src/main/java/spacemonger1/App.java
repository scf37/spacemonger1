package spacemonger1;

import spacemonger1.controller.AppController;
import spacemonger1.controller.DriveDialogControllerFactory;
import spacemonger1.controller.FolderDialogControllerFactory;
import spacemonger1.controller.FolderViewFactory;
import spacemonger1.controller.SettingsDialogControllerFactory;
import spacemonger1.fs.FileSystems;
import spacemonger1.service.DialogUnitsService;
import spacemonger1.service.DrivesService;
import spacemonger1.service.FormatService;
import spacemonger1.service.LangService;
import spacemonger1.service.SettingsService;
import spacemonger1.service.ToolbarIconsService;

public class App {

    public static void main(String[] args) {
//        FlatLightLaf.setup();

        FileSystems fileSystems = FileSystems.instance();
        ToolbarIconsService toolbarIconsService = new ToolbarIconsService();
        DrivesService drivesService = new DrivesService(fileSystems);
        FormatService formatService = new FormatService();
        LangService langService = new LangService();
        SettingsService settingsService = new SettingsService();
        DialogUnitsService dialogUnitsService = new DialogUnitsService();

        DriveDialogControllerFactory driveDialogControllerFactory = new DriveDialogControllerFactory(drivesService, dialogUnitsService);
        FolderDialogControllerFactory folderDialogControllerFactory = new FolderDialogControllerFactory(dialogUnitsService);
        FolderViewFactory folderViewFactory = new FolderViewFactory(formatService);
        SettingsDialogControllerFactory settingsDialogControllerFactory = new SettingsDialogControllerFactory(langService, dialogUnitsService);

        AppController appController = new AppController(
            toolbarIconsService,
            driveDialogControllerFactory,
            folderDialogControllerFactory,
            fileSystems,
            folderViewFactory,
            settingsService,
            langService,
                settingsDialogControllerFactory
        );

        appController.init();

        if (args.length == 1) appController.openFolder(args[0]);
    }
}