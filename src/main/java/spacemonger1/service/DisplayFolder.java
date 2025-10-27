package spacemonger1.service;

public class DisplayFolder {
    public String name;
    public int depth;
    public int flags;          // 1 = folder
    public CFolder source;
    public int index;
    public short x; public short y;
    public short w; public short h;
    public DisplayFolder next;
}