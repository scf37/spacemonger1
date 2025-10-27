package spacemonger1.service;

import spacemonger1.App;

import java.awt.Rectangle;
import java.util.prefs.Preferences;

import static spacemonger1.service.TipSettings.TIP_DATE;
import static spacemonger1.service.TipSettings.TIP_ICON;
import static spacemonger1.service.TipSettings.TIP_SIZE;

public class SettingsService {
    private static final Settings DefaultSettings = new Settings(
        0,                                  // density
        0,                                  // file_color
        0,                                  // folder_color
        false,                              // auto_rescan (0 → false)
        true,                               // animated_zoom (1 → true)
        false,                              // disable_delete (0 → false)
        false,                              // rollover_box (0 → false)
        0,                                  // bias

        false,                              // save_pos (0 → false)
        new Rectangle(
            /*CW_USEDEFAULT*/0,
            /*CW_USEDEFAULT*/0,
            /*CW_USEDEFAULT*/0,
            /*CW_USEDEFAULT*/0
        ),                                  // rect (left/right/top/bottom = CW_USEDEFAULT)
        0/*SW_SHOWNORMAL*/,                      // showcmd

        true,                               // show_name_tips (1 → true)
        125,                                // nametip_delay

        true,                               // show_info_tips (1 → true)
        TIP_DATE.code | TIP_SIZE.code | TIP_ICON.code,     // infotip_flags
        250,                                // infotip_delay

        "en_US"                                // lang
    );

    private static final String KEY_DENSITY = "density";
    private static final String KEY_FILE_COLOR = "file_color";
    private static final String KEY_FOLDER_COLOR = "folder_color";
    private static final String KEY_AUTO_RESCAN = "auto_rescan";
    private static final String KEY_ANIMATED_ZOOM = "animated_zoom";
    private static final String KEY_DISABLE_DELETE = "disable_delete";
    private static final String KEY_ROLLOVER_BOX = "rollover_box";
    private static final String KEY_BIAS = "bias";

    private static final String KEY_SAVE_POS = "save_pos";
    private static final String KEY_RECT_X = "rect_x";
    private static final String KEY_RECT_Y = "rect_y";
    private static final String KEY_RECT_WIDTH = "rect_width";
    private static final String KEY_RECT_HEIGHT = "rect_height";
    private static final String KEY_SHOWCMD = "showcmd";

    private static final String KEY_SHOW_NAME_TIPS = "show_name_tips";
    private static final String KEY_NAMETIP_DELAY = "nametip_delay";

    private static final String KEY_SHOW_INFO_TIPS = "show_info_tips";
    private static final String KEY_INFOTIP_FLAGS = "infotip_flags";
    private static final String KEY_INFOTIP_DELAY = "infotip_delay";

    private static final String KEY_LANG = "lang";

    public Settings load() {
        Preferences pref = Preferences.userNodeForPackage(App.class);

        try {
            int density = pref.getInt(KEY_DENSITY, DefaultSettings.density());
            int fileColor = pref.getInt(KEY_FILE_COLOR, DefaultSettings.file_color());
            int folderColor = pref.getInt(KEY_FOLDER_COLOR, DefaultSettings.folder_color());
            boolean autoRescan = pref.getBoolean(KEY_AUTO_RESCAN, DefaultSettings.auto_rescan());
            boolean animatedZoom = pref.getBoolean(KEY_ANIMATED_ZOOM, DefaultSettings.animated_zoom());
            boolean disableDelete = pref.getBoolean(KEY_DISABLE_DELETE, DefaultSettings.disable_delete());
            boolean rolloverBox = pref.getBoolean(KEY_ROLLOVER_BOX, DefaultSettings.rollover_box());
            int bias = pref.getInt(KEY_BIAS, DefaultSettings.bias());

            boolean savePos = pref.getBoolean(KEY_SAVE_POS, DefaultSettings.save_pos());
            int x = pref.getInt(KEY_RECT_X, DefaultSettings.rect().x);
            int y = pref.getInt(KEY_RECT_Y, DefaultSettings.rect().y);
            int width = pref.getInt(KEY_RECT_WIDTH, DefaultSettings.rect().width);
            int height = pref.getInt(KEY_RECT_HEIGHT, DefaultSettings.rect().height);
            Rectangle rect = new Rectangle(x, y, width, height);
            int showcmd = pref.getInt(KEY_SHOWCMD, DefaultSettings.showcmd());

            boolean showNameTips = pref.getBoolean(KEY_SHOW_NAME_TIPS, DefaultSettings.show_name_tips());
            int nameTipDelay = pref.getInt(KEY_NAMETIP_DELAY, DefaultSettings.nametip_delay());

            boolean showInfoTips = pref.getBoolean(KEY_SHOW_INFO_TIPS, DefaultSettings.show_info_tips());
            int infoTipFlags = pref.getInt(KEY_INFOTIP_FLAGS, DefaultSettings.infotip_flags());
            int infoTipDelay = pref.getInt(KEY_INFOTIP_DELAY, DefaultSettings.infotip_delay());

            String lang = pref.get(KEY_LANG, DefaultSettings.lang());

            return new Settings(
                density, fileColor, folderColor, autoRescan, animatedZoom,
                disableDelete, rolloverBox, bias, savePos, rect, showcmd,
                showNameTips, nameTipDelay, showInfoTips, infoTipFlags,
                infoTipDelay, lang
            );
        } catch (Exception e) {
            return DefaultSettings;
        }
    }

    public void save(Settings settings) {
        Preferences pref = Preferences.userNodeForPackage(App.class);

        pref.putInt(KEY_DENSITY, settings.density());
        pref.putInt(KEY_FILE_COLOR, settings.file_color());
        pref.putInt(KEY_FOLDER_COLOR, settings.folder_color());
        pref.putBoolean(KEY_AUTO_RESCAN, settings.auto_rescan());
        pref.putBoolean(KEY_ANIMATED_ZOOM, settings.animated_zoom());
        pref.putBoolean(KEY_DISABLE_DELETE, settings.disable_delete());
        pref.putBoolean(KEY_ROLLOVER_BOX, settings.rollover_box());
        pref.putInt(KEY_BIAS, settings.bias());

        pref.putBoolean(KEY_SAVE_POS, settings.save_pos());
        Rectangle r = settings.rect();
        pref.putInt(KEY_RECT_X, r.x);
        pref.putInt(KEY_RECT_Y, r.y);
        pref.putInt(KEY_RECT_WIDTH, r.width);
        pref.putInt(KEY_RECT_HEIGHT, r.height);
        pref.putInt(KEY_SHOWCMD, settings.showcmd());

        pref.putBoolean(KEY_SHOW_NAME_TIPS, settings.show_name_tips());
        pref.putInt(KEY_NAMETIP_DELAY, settings.nametip_delay());

        pref.putBoolean(KEY_SHOW_INFO_TIPS, settings.show_info_tips());
        pref.putInt(KEY_INFOTIP_FLAGS, settings.infotip_flags());
        pref.putInt(KEY_INFOTIP_DELAY, settings.infotip_delay());

        pref.put(KEY_LANG, settings.lang());

        // Optional: flush to disk immediately (usually not needed)
        // try { pref.flush(); } catch (Exception ignored) {}
    }
}
