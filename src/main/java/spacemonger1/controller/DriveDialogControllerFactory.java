package spacemonger1.controller;

import spacemonger1.service.DialogUnitsService;
import spacemonger1.service.DrivesService;
import spacemonger1.service.Lang;

import javax.swing.JFrame;

public class DriveDialogControllerFactory {
    private final DrivesService drivesService;
    private final DialogUnitsService dialogUnitsService;

    public DriveDialogControllerFactory(DrivesService drivesService, DialogUnitsService dialogUnitsService) {
        this.drivesService = drivesService;
        this.dialogUnitsService = dialogUnitsService;
    }

    public DriveDialogController newInstance(JFrame owner, Lang lang) {
        var drives = drivesService.enumerateDrives();
        return new DriveDialogController(dialogUnitsService, owner, drives, lang);
    }
}
