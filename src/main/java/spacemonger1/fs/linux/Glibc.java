package spacemonger1.fs.linux;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

public class Glibc {
    private static Linker linker = Linker.nativeLinker();
    private static SymbolLookup glibc = SymbolLookup.loaderLookup()
        .or(Linker.nativeLinker().defaultLookup());
    private static FunctionDescriptor statDesc = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
    private static MemorySegment statAddr = glibc.find("__lxstat").orElseThrow(() -> new RuntimeException("Could not find __lxstat address"));
    private static MethodHandle statMH = linker.downcallHandle(statAddr, statDesc);

    private static FunctionDescriptor gnuDevMajorDesc = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT);
    private static FunctionDescriptor gnuDevMinorDesc = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT);
    private static MemorySegment gnuDevMajorAddr = glibc.find("gnu_dev_major").orElseThrow(() -> new RuntimeException("Could not find gnu_dev_major address"));
    private static MemorySegment gnuDevMinorAddr = glibc.find("gnu_dev_minor").orElseThrow(() -> new RuntimeException("Could not find gnu_dev_minor address"));
    private static MethodHandle gnuDevMajorMH = linker.downcallHandle(gnuDevMajorAddr, gnuDevMajorDesc);
    private static MethodHandle gnuDevMinorMH = linker.downcallHandle(gnuDevMinorAddr, gnuDevMinorDesc);

    private static final GroupLayout TIMESPEC = MemoryLayout.structLayout(
        ValueLayout.JAVA_LONG.withName("tv_sec"),
        ValueLayout.JAVA_LONG.withName("tv_nsec")
    );
    private static final GroupLayout STAT = MemoryLayout.structLayout(
        ValueLayout.JAVA_LONG.withName("st_dev"),
        ValueLayout.JAVA_LONG.withName("st_ino"),
        ValueLayout.JAVA_LONG.withName("st_nlink"),
        ValueLayout.JAVA_INT.withName("st_mode"),
        ValueLayout.JAVA_INT.withName("st_uid"),
        ValueLayout.JAVA_INT.withName("st_gid"),
        ValueLayout.JAVA_INT.withName("__pad0"),
        ValueLayout.JAVA_INT.withName("st_rdev"),
        MemoryLayout.paddingLayout(4),  // 4 bytes padding
        ValueLayout.JAVA_LONG.withName("st_size"),
        ValueLayout.JAVA_LONG.withName("st_blksize"),
        ValueLayout.JAVA_LONG.withName("st_blocks"),
        TIMESPEC.withName("st_atim"),   // access time
        TIMESPEC.withName("st_mtim"),   // modification time
        TIMESPEC.withName("st_ctim"),   // change time
        MemoryLayout.sequenceLayout(128, ValueLayout.JAVA_BYTE.withName("__unused"))
    );

    private static final VarHandle STAT_st_dev = STAT.varHandle(MemoryLayout.PathElement.groupElement("st_dev"));
    private static final VarHandle STAT_st_ino = STAT.varHandle(MemoryLayout.PathElement.groupElement("st_ino"));
    private static final VarHandle STAT_st_nlink = STAT.varHandle(MemoryLayout.PathElement.groupElement("st_nlink"));
    private static final VarHandle STAT_st_mode = STAT.varHandle(MemoryLayout.PathElement.groupElement("st_mode"));
    private static final VarHandle STAT_st_uid = STAT.varHandle(MemoryLayout.PathElement.groupElement("st_uid"));
    private static final VarHandle STAT_st_gid = STAT.varHandle(MemoryLayout.PathElement.groupElement("st_gid"));
    private static final VarHandle STAT_st_rdev = STAT.varHandle(MemoryLayout.PathElement.groupElement("st_rdev"));
    private static final VarHandle STAT_st_size = STAT.varHandle(MemoryLayout.PathElement.groupElement("st_size"));
    private static final VarHandle STAT_st_blksize = STAT.varHandle(MemoryLayout.PathElement.groupElement("st_blksize"));
    private static final VarHandle STAT_st_blocks = STAT.varHandle(MemoryLayout.PathElement.groupElement("st_blocks"));
    private static final VarHandle STAT_st_mtim_tv_sec = STAT.varHandle(
        MemoryLayout.PathElement.groupElement("st_mtim"),
        MemoryLayout.PathElement.groupElement("tv_sec")
    );
    private static final VarHandle STAT_st_ctim_tv_sec = STAT.varHandle(
        MemoryLayout.PathElement.groupElement("st_ctim"),
        MemoryLayout.PathElement.groupElement("tv_sec")
    );

    record Stat(
        int devMajor,
        int devMinor,
        long inode,
        long size,
        long physicalSize,
        boolean isDir,
        boolean isFile,
        long updateTimeSec
    ) { }

    public static Stat stat(String path) {
        try (var arena = Arena.ofConfined()) {
            var cPath = arena.allocateFrom(path);
            var statBuf = arena.allocate(STAT);
            int result = (int) statMH.invokeExact(1, cPath, statBuf);
            if (result != 0) return null;

            long dev = (long) STAT_st_dev.get(statBuf, 0L);
            int mode = (int) STAT_st_mode.get(statBuf, 0L);

            return new Stat(
                (int)gnuDevMajorMH.invokeExact((int)dev),
                (int)gnuDevMinorMH.invokeExact((int)dev),
                (long) STAT_st_ino.get(statBuf, 0L),
                (long) STAT_st_size.get(statBuf, 0L),
                (long) STAT_st_blocks.get(statBuf, 0L) * 512,
                (mode & 0170000) == 0040000, // man 7 inode
                (mode & 0170000) == 0100000,
                (long)STAT_st_mtim_tv_sec.get(statBuf, 0L)
            );

        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
}
