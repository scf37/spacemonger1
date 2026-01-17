package spacemonger1.service;

import spacemonger1.fs.FileInfo;
import spacemonger1.fs.FileSystems;
import spacemonger1.fs.Volume;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

public class CFolder {
    public CFolder() {
        cur = 0;
        max = 2;
        names = new String[max];
        sizes = new long[max];
        actualsizes = new long[max];
        children = new CFolder[max];
        times = new long[max];
        sizeSelf = sizeChildren = 0;
        parent = null;
        parentindex = 0;
    }

    public void AddFile(String name, long size, long actualSize, long time) {
        if (cur >= max) MoreEntries();
        names[cur] = name;
        sizes[cur] = size;
        actualsizes[cur] = actualSize;
        times[cur] = time;
        sizeSelf += size;
        cur++;
    }

    public void AddFolder(String name, CFolder folder, long time) {
        if (cur >= max) MoreEntries();
        names[cur] = name;
        sizes[cur] = folder.SizeTotal();
        sizeChildren += folder.SizeTotal();
        actualsizes[cur] = sizes[cur];
        times[cur] = time;
        children[cur] = folder;
        folder.parent = this;
        folder.parentindex = cur;
        cur++;
    }

    public void Finalize() {
        if (cur <= 1) return;

        // Allocate temporary arrays
        String[] newnames = new String[cur];
        long[] newsizes = new long[cur];
        long[] newactualsizes = new long[cur];
        CFolder[] newkids = new CFolder[cur];
        long[] newtimes = new long[cur];

        // Perform 8 passes (for 64-bit values, 8 bits per pass)
        EightBitCountingSort(newsizes, sizes, cur, 0,  newnames, names, newkids, children, newactualsizes, actualsizes, newtimes, times);
        EightBitCountingSort(sizes, newsizes, cur, 8,  names, newnames, children, newkids, actualsizes, newactualsizes, times, newtimes);
        EightBitCountingSort(newsizes, sizes, cur, 16, newnames, names, newkids, children, newactualsizes, actualsizes, newtimes, times);
        EightBitCountingSort(sizes, newsizes, cur, 24, names, newnames, children, newkids, actualsizes, newactualsizes, times, newtimes);
        EightBitCountingSort(newsizes, sizes, cur, 32, newnames, names, newkids, children, newactualsizes, actualsizes, newtimes, times);
        EightBitCountingSort(sizes, newsizes, cur, 40, names, newnames, children, newkids, actualsizes, newactualsizes, times, newtimes);
        EightBitCountingSort(newsizes, sizes, cur, 48, newnames, names, newkids, children, newactualsizes, actualsizes, newtimes, times);
        EightBitCountingSort(sizes, newsizes, cur, 56, names, newnames, children, newkids, actualsizes, newactualsizes, times, newtimes);
    }

    public static CFolder LoadFolderInitial(String name, FileSystems fileSystems, Control control) {
        Path path = Paths.get(name).toAbsolutePath();
        var info = fileSystems.fileInfo(path);
        if (info == null) return new CFolder();
        return new CFolderLoader(path, info.volumeId(), fileSystems, control).invoke();
    }

    public long SizeFiles() {
        return sizeSelf;
    }

    public long SizeSub() {
        return sizeChildren;
    }

    public long SizeTotal() {
        return sizeSelf + sizeChildren;
    }

    private void MoreEntries() {
        int newmax = max * 2;
        var newnames = new String[newmax];
        var newsizes = new long[newmax];
        var newactualsizes = new long[newmax];
        var newchildren = new CFolder[newmax];
        var newtimes = new long[newmax];

        System.arraycopy(names, 0, newnames, 0, max);
        System.arraycopy(sizes, 0, newsizes, 0, max);
        System.arraycopy(actualsizes, 0, newactualsizes, 0, max);
        System.arraycopy(children, 0, newchildren, 0, max);
        System.arraycopy(times, 0, newtimes, 0, max);

        names = newnames;
        sizes = newsizes;
        actualsizes = newactualsizes;
        children = newchildren;
        times = newtimes;
        max = newmax;
    }

    private void EightBitCountingSort(
        long[] dsize, long[] ssize, int count, int bitpos,
        String[] dnames, String[] snames,
        CFolder[] dkids, CFolder[] skids,
        long[] dasize, long[] sasize,
        long[] dtimes, long[] stimes) {
        final int[] countarray = new int[257]; // indices 0..256

        // VALUE macro as lambda-style inline
        // Note: Java >> is signed; use >>> for logical shift if needed.
        // Since we're masking with 0xFF, >> is fine for non-negative values.
        // Assuming sizes are non-negative.
        // VALUE = 0xFF - ((size >> bitpos) & 0xFF)
        for (int i = 0; i < count; i++) {
            int value = 0xFF - (int)((ssize[i] >>> bitpos) & 0xFF);
            countarray[value + 1]++;
        }

        // Prefix sum to get starting indices
        for (int i = 1; i < 256; i++) {
            countarray[i] += countarray[i - 1];
        }

        // Scatter elements into destination
        for (int i = 0; i < count; i++) {
            int value = 0xFF - (int)((ssize[i] >>> bitpos) & 0xFF);
            int dest = countarray[value]++;
            dsize[dest] = ssize[i];
            dnames[dest] = snames[i];
            dkids[dest] = skids[i];
            dasize[dest] = sasize[i];
            dtimes[dest] = stimes[i];
            if (dkids[dest] != null) {
                dkids[dest].parentindex = i;
            }
        }
    }

    // Public fields (Java style typically uses private + getters/setters, but matching C++ public access)
    public CFolder parent;
    public int parentindex;

    public String[] names;
    public CFolder[] children;
    public long[] sizes;
    public long[] actualsizes;
    public long[] times;
    public long sizeSelf;
    public long sizeChildren;

    public int cur;
    public int max;

    public interface Control {
        boolean cancelled();
        /// @return false if file is already known
        boolean addKnownFile(FileInfo.Id fileId);
        void addFile(long size);
        void addFolder();
        void updateStatus(Path path);
    }

    static class CFolderLoader extends RecursiveTask<CFolder> {
        private final Path path;
        private final Volume.Id volumeId;
        private final FileSystems fileSystems;
        private final Control control;

        CFolderLoader(Path path, Volume.Id volumeId, FileSystems fileSystems, Control control) {
            this.path = path;
            this.volumeId = volumeId;
            this.fileSystems = fileSystems;
            this.control = control;
        }

        @Override
        protected CFolder compute() {
            CFolder folder = new CFolder();
            if (control.cancelled()) return folder;
            control.updateStatus(path);
            try {
                List<Path> list;
                try (var stream = Files.list(path)) {
                    list = stream.collect(Collectors.toList());
                }
                Collections.sort(list);

                record LoaderTask(CFolderLoader loader, String fileName, long updateTimeMs) { }
                List<LoaderTask> loaderTasks = new ArrayList<>();
                for (var file: list) {
                    if (control.cancelled()) throw new CancellationException();
                    String fileName = file.getFileName().toString();
                    if (fileName.equals(".") || fileName.equals("..")) continue;
                    FileInfo info = fileSystems.fileInfo(file);
                    if (info == null) continue;

                    // deduplicate hardlinks
                    if (!control.addKnownFile(info.id())) {
                        continue;
                    }

                    if (info.isDirectory()) {
                        if (!info.volumeId().equals(volumeId)) continue;
                        CFolderLoader newFolderLoader = new CFolderLoader(file, volumeId, fileSystems, control);
                        newFolderLoader.fork();
                        loaderTasks.add(new LoaderTask(newFolderLoader, fileName, info.updateTimeMs()));
                    } else if (info.isFile()) {
                        long actualsize = info.logicalSize();
                        long size = info.physicalSize();
                        folder.AddFile(fileName, size, actualsize, info.updateTimeMs());
                        control.addFile(size);
                    }
                }

                for (var loaderTask: loaderTasks) {
                    folder.AddFolder(loaderTask.fileName(), loaderTask.loader.join(), loaderTask.updateTimeMs());
                    control.addFolder();
                }
            } catch (IOException e) {
            } catch (CancellationException e) {
            } finally {
                folder.Finalize();
            }
            return folder;
        }
    }
}