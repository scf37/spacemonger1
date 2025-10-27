package spacemonger1.controller;

import spacemonger1.service.FormatService;

import java.awt.Container;

public class FolderViewFactory {
    private final FormatService formatService;

    public FolderViewFactory(FormatService formatService) {
        this.formatService = formatService;
    }

    public FolderView newInstance(FolderView.AppCommands commands, Container parent) {
        return new FolderView(commands, formatService, parent);
    }
}
