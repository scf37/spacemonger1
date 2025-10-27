package spacemonger1.service;

import spacemonger1.controller.Drive;
import spacemonger1.fs.FileInfo;
import spacemonger1.fs.FileSystems;

import java.util.HashSet;
import java.util.Set;

public class CFolderTree {
    public final FileSystems fileSystems;
    private volatile Status status = new Status(0, 0, "", null, 0);
    volatile boolean cancelled;

    public CFolderTree(FileSystems fileSystems) {
        this.fileSystems = fileSystems;
    }

    public record Status(
        long files,
        long folders,
        String currentPath,
        CFolderTree tree, // only filled in on completion
        double progress
    ) { }

    public boolean LoadTree(Drive drive) {
        mPath = drive.rootPath().toString();
        root = new CFolder();
        cur = root;
        filespace = 0;
        totalspace = drive.totalspace();
        usedspace = drive.usedspace();
        freespace = totalspace - usedspace;

        root.LoadFolderInitial(this, mPath, clustersize); // TODO if-else ?

        root.AddFile(this, "<<<<<<<<<<<<<<<<<<<<", freespace, freespace, 0);
        root.Finalize();
        updateStatus(status.currentPath, true);
        knownFiles = null;
        return false;
    }

    public Status status() {
        return status;
    }
    public void cancel() {
        cancelled = true;
    }
    public boolean isCancelled() {
        return cancelled;
    }

    public CFolder GetRoot() { return root; }

    public CFolder SetCur(String path) {
        return cur;
    }

    public CFolder GetCur() { return cur; }

    public CFolder Down(int index) {
        if (index < cur.cur) {
            var newfolder = cur.children[index];
            if (newfolder != null) {
                cur = newfolder;
            }
            return cur;

        }
        return null;
    }
    public CFolder Up() {
        if (cur != root) {
            cur = cur.parent;
            return cur;
        }
        return null;
    }

    void updateStatus(String path, boolean done) {
        status = new Status(numfiles, numfolders, path, done?this:null, Double.valueOf(filespace) / totalspace);
    }

    protected CFolder root;
    protected CFolder cur;

    public String mPath;
    public long freespace;
    public long usedspace;
    public long totalspace;
    public long clustersize;
    public long numfiles; // TODO set from the dialog
    public long numfolders; // TODO set from the dialog
    public long filespace;

    public Set<FileInfo.Id> knownFiles = new HashSet<>();
}