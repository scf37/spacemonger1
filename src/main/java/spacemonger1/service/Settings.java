package spacemonger1.service;

import java.awt.Rectangle;

public record Settings(
    int density,            // Density of filenames (-3..3)
    int file_color,            // Displayed color of files
    int folder_color,        // Displayed color of folders
    boolean auto_rescan,        // Auto-rescan when a file is deleted
    boolean animated_zoom,        // Use animated zoom in/out
    boolean disable_delete,    // Disallow the deletion of files
    boolean rollover_box,        // Show rollover box
    int bias,                // Display bias, from -20 (vert) to +20 (horz)

    boolean save_pos,            // Save this window's position?
    Rectangle rect,                // Pluralized window rectangle
    int showcmd,            // Current show-command

    boolean show_name_tips,    // Show tooltips?
    int nametip_delay,        // Tip delay, in milliseconds

    boolean show_info_tips,    // Show tooltips?
    int infotip_flags,        // Tooltip flags
    int infotip_delay,        // Tip delay, in milliseconds

    String lang            // Language, by two-letter code)
) {

    public Settings withRect(Rectangle rect) {
        return new Settings(
            density,
            file_color,
            folder_color,
            auto_rescan,
            animated_zoom,
            disable_delete,
            rollover_box,
            bias,
            save_pos,
            rect,
            showcmd,
            show_name_tips,
            nametip_delay,
            show_info_tips,
            infotip_flags,
            infotip_delay,
            lang
        );
    }
}
