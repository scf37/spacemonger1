package spacemonger1.service;

import spacemonger1.fs.FileInfo;
import spacemonger1.fs.Volume;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
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

    public void AddFile(CFolderTree tree, String name, long size, long actualSize, long time) {
        if (cur >= max) MoreEntries();
        names[cur] = name;
        sizes[cur] = size;
        actualsizes[cur] = actualSize;
        times[cur] = time;
        sizeSelf += size;
        if (!name.startsWith(">>>>")) { // 'free space' file
            tree.filespace += size;
        }

        cur++;
        tree.numfiles++;
        if (tree.filespace > tree.totalspace) {
            System.out.println(name);
        }
    }

    public void AddFolder(CFolderTree tree, String name, CFolder folder, long time) {
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
        tree.numfolders++;
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

    public void LoadFolderInitial(CFolderTree tree, String name, long clusterSize) {
        name = Paths.get(name).toAbsolutePath().toString();
        var info = tree.fileSystems.fileInfo(Paths.get(name));
        if (info == null) return;
        LoadFolder(tree, name, info.volumeId());
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

    private void LoadFolder(CFolderTree tree, String name, Volume.Id volumeId) {
        if (tree.cancelled) return;
        updateStatus(tree, Paths.get(name));
        try {
            List<Path> list;
            try (var stream = Files.list(Paths.get(name))) {
                list = stream.collect(Collectors.toList());
            }
            Collections.sort(list);

            record ChildJob(String name, ForkJoinTask<CFolder> task, long modified) { }
            List<ChildJob> childJobs = new ArrayList<>();

            list.forEach(file -> {
                if (tree.cancelled) throw new CancellationException();
                String fileName = file.getFileName().toString();
                if (fileName.equals(".") || fileName.equals("..")) return;
                FileInfo info = tree.fileSystems.fileInfo(file);
                if (info == null) return;

                // deduplicate hardlinks
                if (!tree.knownFiles.add(info.id())) {
                    return;
                }

                if (info.isDirectory()) {
                    if (!info.volumeId().equals(volumeId)) return;

                    CFolder newFolder = new CFolder();
                    ForkJoinTask<CFolder> task = ForkJoinTask.adapt(() -> {
                        newFolder.LoadFolder(tree, file.toString(), volumeId);
                        return newFolder;
                    });
                    tree.pool().execute(task);
                    childJobs.add(new ChildJob(fileName, task, info.updateTimeMs()));
                } else if (info.isFile()) {
                    long actualsize = info.logicalSize();
                    long size = info.physicalSize();
                    AddFile(tree, fileName, size, actualsize, info.updateTimeMs());
                }
            });

            for (ChildJob job : childJobs) {
                if (tree.cancelled) break;
                CFolder loaded = job.task().join();
                if (loaded != null) {
                    AddFolder(tree, job.name(), loaded, job.modified());
                }
            }
        } catch (IOException e) {
        } catch (CancellationException e) {
        } finally {
            Finalize();
        }
    }

    private void updateStatus(CFolderTree tree, Path path) {
        long now = System.nanoTime();
        long elapsedMs = (now - lastStatus) / 1_000_000;
        if (elapsedMs > 100) {
            lastStatus = now;
            tree.updateStatus(path.toString(), false);
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

    private volatile long lastStatus = System.nanoTime();
}
