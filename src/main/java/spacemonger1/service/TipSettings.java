package spacemonger1.service;

public enum TipSettings {
        TIP_PATH(1),			// Display full path in tooltip
        TIP_NAME(2),			// Display filename in tooltip
        TIP_ICON(4),			// Display suitable icon in tooltip
        TIP_DATE(8),			// Display file date in tooltip
        TIP_SIZE(16),			// Display file size in tooltip
        TIP_ATTRIB(32);		// Display file attributes in tooltip

    TipSettings(int code) {
        this.code = code;
    }
    public final int code;
}
