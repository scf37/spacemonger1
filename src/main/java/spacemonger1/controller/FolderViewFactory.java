package spacemonger1.controller;

import spacemonger1.service.ColorService;
import spacemonger1.service.FormatService;

import java.awt.Container;

public class FolderViewFactory {
    private final FormatService formatService;
    private final ColorService colorService;

    public FolderViewFactory(FormatService formatService, ColorService colorService) {
        this.formatService = formatService;
        this.colorService = colorService;
    }

    public FolderView newInstance(FolderView.AppCommands commands, Container parent) {
        return new FolderView(commands, formatService, colorService, parent);
    }
}
