package spacemonger1.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class LangService {
    private static final Pattern LANG_PATTERN = Pattern.compile("lang_([a-z]{2})\\.properties");
    private final List<Lang> languages;

    public LangService() {
        Properties props = new Properties();
        try (InputStream is = LangService.class.getClassLoader().getResourceAsStream("spacemonger1.properties")) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: /spacemonger1.properties");
            }
            props.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load properties file /spacemonger1.properties", e);
        }
        String langsStr = props.getProperty("languages");
        List<String> langs = new ArrayList<>();
        for (String lang : langsStr.split(",")) {
            langs.add(lang.trim());
        }

        List<Lang> result = new ArrayList<>();
        for (String lang : langs) {
            try {
                result.add(Lang.load(lang));
            } catch (Exception e) {
                System.err.println("Failed to load lang " + lang + ": " + e.getMessage());
            }
        }

        languages = result;
    }

    public List<Lang> languages() {
        return languages;
    }

    public Lang byCode(String lang) {
        for (Lang l : languages) {
           if (l.lang_code.equals(lang)) return l;
        }
        return languages.getFirst();
    }
}
