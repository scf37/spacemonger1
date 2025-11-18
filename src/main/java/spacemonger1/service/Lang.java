package spacemonger1.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class Lang {
    public String lang_code;
    public String lang_name;
    public String toolbar_open;
    public String toolbar_reload;
    public String toolbar_zoomfull;
    public String toolbar_zoomin;
    public String toolbar_zoomout;
    public String toolbar_freespace;
    public String toolbar_runoropen;
    public String toolbar_delete;
    public String toolbar_setup;
    public String toolbar_about;
    public String about_spacemonger;
    public String freeware;
    public String warranty;
    public String email;
    public String ok, cancel;
    public String bytes, kb, mb, gb;
    public String freespace_format;
    public String zoomin, zoomout, zoomfull, run, del;
    public String opendrive, rescandrive, showfreespace;
    public String percent_format, size_format;
    public String total, free, files_total, folders_total, files_found, folders_found;
    public char digitpt, digitcomma;
    public String[] densitynames;
    public String[] colornames;
    public String[] monthnames;
    public String[] attribnames;
    public String deleting;
    public String selectdrive;
    public String scanning;
    public String settings;
    public String layout, density, bias, horz, equal, vert;
    public String displaycolors, files, folders;
    public String tooltips, shownametips, showinfotips, delay, msec;
    public String fullpath, filename, smallicon, icon, datetime, filesize, attrib;
    public String miscoptions;
    public String autorescan, disabledelete, animatedzoom, savepos;
    public String showrolloverbox;
    public String properties;

    public static Lang load(String langCode) {
        Properties props = new Properties();
        String resourceName = "lang_" + langCode + ".properties";
        try (InputStream is = Lang.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new RuntimeException("Language file not found: " + resourceName);
            }
            props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load language: " + langCode, e);
        }

        Lang lang = new Lang();

        // Simple scalar fields
        lang.lang_code = langCode;
        lang.lang_name = props.getProperty("lang_name");
        lang.about_spacemonger = props.getProperty("about_spacemonger");
        lang.freeware = props.getProperty("freeware");
        lang.warranty = props.getProperty("warranty");
        lang.email = props.getProperty("email");
        lang.ok = props.getProperty("ok");
        lang.cancel = props.getProperty("cancel");
        lang.bytes = props.getProperty("bytes");
        lang.kb = props.getProperty("kb");
        lang.mb = props.getProperty("mb");
        lang.gb = props.getProperty("gb");
        lang.freespace_format = props.getProperty("freespace_format");
        lang.zoomin = props.getProperty("zoomin");
        lang.zoomout = props.getProperty("zoomout");
        lang.zoomfull = props.getProperty("zoomfull");
        lang.run = props.getProperty("run");
        lang.del = props.getProperty("del");
        lang.opendrive = props.getProperty("opendrive");
        lang.rescandrive = props.getProperty("rescandrive");
        lang.showfreespace = props.getProperty("showfreespace");
        lang.percent_format = props.getProperty("percent_format");
        lang.size_format = props.getProperty("size_format");
        lang.total = props.getProperty("total");
        lang.free = props.getProperty("free");
        lang.files_total = props.getProperty("files_total");
        lang.folders_total = props.getProperty("folders_total");
        lang.files_found = props.getProperty("files_found");
        lang.folders_found = props.getProperty("folders_found");
        lang.digitpt = props.getProperty("digitpt").charAt(0);
        lang.digitcomma = props.getProperty("digitcomma").charAt(0);
        lang.deleting = props.getProperty("deleting");
        lang.selectdrive = props.getProperty("selectdrive");
        lang.scanning = props.getProperty("scanning");
        lang.settings = props.getProperty("settings");
        lang.layout = props.getProperty("layout");
        lang.density = props.getProperty("density");
        lang.bias = props.getProperty("bias");
        lang.horz = props.getProperty("horz");
        lang.equal = props.getProperty("equal");
        lang.vert = props.getProperty("vert");
        lang.displaycolors = props.getProperty("displaycolors");
        lang.files = props.getProperty("files");
        lang.folders = props.getProperty("folders");
        lang.tooltips = props.getProperty("tooltips");
        lang.shownametips = props.getProperty("shownametips");
        lang.showinfotips = props.getProperty("showinfotips");
        lang.delay = props.getProperty("delay");
        lang.msec = props.getProperty("msec");
        lang.fullpath = props.getProperty("fullpath");
        lang.filename = props.getProperty("filename");
        lang.smallicon = props.getProperty("smallicon");
        lang.icon = props.getProperty("icon");
        lang.datetime = props.getProperty("datetime");
        lang.filesize = props.getProperty("filesize");
        lang.attrib = props.getProperty("attrib");
        lang.miscoptions = props.getProperty("miscoptions");
        lang.autorescan = props.getProperty("autorescan");
        lang.disabledelete = props.getProperty("disabledelete");
        lang.animatedzoom = props.getProperty("animatedzoom");
        lang.savepos = props.getProperty("savepos");
        lang.showrolloverbox = props.getProperty("showrolloverbox");
        lang.properties = props.getProperty("properties");
        lang.toolbar_open=props.getProperty("toolbar_open");
        lang.toolbar_reload=props.getProperty("toolbar_reload");
        lang.toolbar_zoomfull=props.getProperty("toolbar_zoomfull");
        lang.toolbar_zoomin=props.getProperty("toolbar_zoomin");
        lang.toolbar_zoomout=props.getProperty("toolbar_zoomout");
        lang.toolbar_freespace=props.getProperty("toolbar_freespace");
        lang.toolbar_runoropen=props.getProperty("toolbar_runoropen");
        lang.toolbar_delete=props.getProperty("toolbar_delete");
        lang.toolbar_setup=props.getProperty("toolbar_setup");
        lang.toolbar_about=props.getProperty("toolbar_about");

        // Array fields — split by | (pipe) delimiter
        lang.densitynames = props.getProperty("densitynames").split("\\|");
        lang.colornames = props.getProperty("colornames").split("\\|");
        lang.monthnames = props.getProperty("monthnames").split("\\|");
        lang.attribnames = props.getProperty("attribnames").split("\\|");

        return lang;
    }
}