package spacemonger1.service;

import spacemonger1.controller.Drive;
import spacemonger1.fs.FileInfo;
import spacemonger1.fs.FileSystems;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class CFolderTree {
    private final FileSystems fileSystems;
    private Set<FileInfo.Id> knownFiles = ConcurrentHashMap.newKeySet();

    private volatile Status status = new Status(0, 0, "", null, 0);
    private volatile boolean cancelled;


    private CFolder.Control control = new CFolder.Control() {
        @Override
        public boolean cancelled() {
            return cancelled;
        }

        @Override
        public boolean addKnownFile(FileInfo.Id fileId) {
            return knownFiles.add(fileId);
        }

        @Override
        public void addFile(long size) {
            numfiles.add(1);
            filespace.add(size);
        }

        @Override
        public void addFolder() {
            numfolders.add(1);
        }

        @Override
        public void updateStatus(Path path) {
            CFolderTree.this.updateStatus(path.toString(), false);
        }
    };

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
        cur = root;
        filespace.reset();
        totalspace = drive.totalspace();
        usedspace = drive.usedspace();
        freespace = totalspace - usedspace;

        root = CFolder.LoadFolderInitial(mPath, fileSystems, control);

        root.AddFile("<<<<<<<<<<<<<<<<<<<<", freespace, freespace, 0);
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
        long now = System.nanoTime();
        long elapsedMs = (now - lastStatus) / 1_000_000;
        if (elapsedMs > 100 || done) {
            lastStatus = now;
            status = new Status(numfiles.longValue(), numfolders.longValue(), path, done?this:null, Double.valueOf(filespace.longValue()) / totalspace);
        }
    }

    private volatile long lastStatus = 0;
    protected CFolder root;
    protected CFolder cur;

    public String mPath;
    public long freespace;
    public long usedspace;
    public long totalspace;
    public LongAdder numfiles = new LongAdder();
    public LongAdder numfolders = new LongAdder();
    public LongAdder filespace = new LongAdder();
}