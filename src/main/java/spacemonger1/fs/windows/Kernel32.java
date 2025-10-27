package spacemonger1.fs.windows;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.ValueLayout.*;

public class Kernel32 {
    private static Linker linker = Linker.nativeLinker();
    private static SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", Arena.global());

    // CreateFileW
    private static final MemorySegment CreateFileWAddress = kernel32.findOrThrow("CreateFileW");
    private static final FunctionDescriptor CreateFileWDescriptor = FunctionDescriptor.of(
            ADDRESS,                // HANDLE return
            ADDRESS,                // LPCWSTR lpFileName
            JAVA_INT,               // DWORD dwDesiredAccess
            JAVA_INT,               // DWORD dwShareMode
            ADDRESS,                // LPSECURITY_ATTRIBUTES lpSecurityAttributes
            JAVA_INT,               // DWORD dwCreationDisposition
            JAVA_INT,               // DWORD dwFlagsAndAttributes
            ADDRESS                 // HANDLE hTemplateFile
    );
    private static final MethodHandle CreateFileW = linker.downcallHandle(CreateFileWAddress, CreateFileWDescriptor);

    // GetFileInformationByHandleEx
    //    BOOL GetFileInformationByHandleEx(
    //  [in]  HANDLE                    hFile,
    //  [in]  FILE_INFO_BY_HANDLE_CLASS FileInformationClass,
    //  [out] LPVOID                    lpFileInformation,
    //  [in]  DWORD                     dwBufferSize
    //    );
    private static final MemorySegment GetFileInformationByHandleExAddress = kernel32.findOrThrow("GetFileInformationByHandleEx");
    private static final FunctionDescriptor GetFileInformationByHandleExDescriptor = FunctionDescriptor.of(
            JAVA_BOOLEAN,
            ADDRESS,
            JAVA_INT,
            ADDRESS,
            JAVA_INT
    );
    private static final MethodHandle GetFileInformationByHandleEx = linker.downcallHandle(GetFileInformationByHandleExAddress, GetFileInformationByHandleExDescriptor);


    // CloseHandle
    private static final MemorySegment CloseHandleAddress = kernel32.findOrThrow("CloseHandle");
    private static final FunctionDescriptor CloseHandleDescriptor = FunctionDescriptor.of(
            JAVA_BOOLEAN,
            ADDRESS
    );
    private static final MethodHandle CloseHandle = linker.downcallHandle(CloseHandleAddress, CloseHandleDescriptor);

    // Struct: FILE_STANDARD_INFO
// typedef struct _FILE_STANDARD_INFO {
//   LARGE_INTEGER AllocationSize;
//   LARGE_INTEGER EndOfFile;
//   DWORD         NumberOfLinks;
//   BOOLEAN       DeletePending;
//   BOOLEAN       Directory;
// } FILE_STANDARD_INFO, *PFILE_STANDARD_INFO;

    public static final GroupLayout FILE_STANDARD_INFO_LAYOUT = MemoryLayout.structLayout(
            JAVA_LONG.withName("AllocationSize"),
            JAVA_LONG.withName("EndOfFile"),
            JAVA_INT.withName("NumberOfLinks"),
            JAVA_INT.withName("DeletePending"),   // BOOLEAN is 1 byte
            JAVA_INT.withName("Directory")        // BOOLEAN is 1 byte
    );

    // VarHandles for top-level fields
    private static final VarHandle AllocationSize = FILE_STANDARD_INFO_LAYOUT.varHandle(PathElement.groupElement("AllocationSize"));
    private static final VarHandle EndOfFile = FILE_STANDARD_INFO_LAYOUT.varHandle(PathElement.groupElement("EndOfFile"));
    private static final VarHandle NumberOfLinks = FILE_STANDARD_INFO_LAYOUT.varHandle(PathElement.groupElement("NumberOfLinks"));
    private static final VarHandle DeletePending = FILE_STANDARD_INFO_LAYOUT.varHandle(PathElement.groupElement("DeletePending"));
    private static final VarHandle Directory = FILE_STANDARD_INFO_LAYOUT.varHandle(PathElement.groupElement("Directory"));

    // Struct: FILE_BASIC_INFO
// typedef struct _FILE_BASIC_INFO {
//   LARGE_INTEGER CreationTime;
//   LARGE_INTEGER LastAccessTime;
//   LARGE_INTEGER LastWriteTime;
//   LARGE_INTEGER ChangeTime;
//   DWORD         FileAttributes;
// } FILE_BASIC_INFO, *PFILE_BASIC_INFO;
    public static final GroupLayout FILE_BASIC_INFO_LAYOUT = MemoryLayout.structLayout(
            JAVA_LONG.withName("CreationTime"),
            JAVA_LONG.withName("LastAccessTime"),
            JAVA_LONG.withName("LastWriteTime"),
            JAVA_LONG.withName("ChangeTime"),
            JAVA_INT.withName("FileAttributes"),
            MemoryLayout.paddingLayout(4)
    );

    // VarHandles
    private static final VarHandle CreationTime = FILE_BASIC_INFO_LAYOUT.varHandle(PathElement.groupElement("CreationTime"));
    private static final VarHandle LastAccessTime = FILE_BASIC_INFO_LAYOUT.varHandle(PathElement.groupElement("LastAccessTime"));
    private static final VarHandle LastWriteTime = FILE_BASIC_INFO_LAYOUT.varHandle(PathElement.groupElement("LastWriteTime"));
    private static final VarHandle ChangeTime = FILE_BASIC_INFO_LAYOUT.varHandle(PathElement.groupElement("ChangeTime"));
    private static final VarHandle FileAttributes = FILE_BASIC_INFO_LAYOUT.varHandle(PathElement.groupElement("FileAttributes"));

    // Struct: FILE_ID_INFO
    // typedef struct _FILE_ID_INFO {
    //   ULONGLONG   VolumeSerialNumber;
    //   FILE_ID_128 FileId;
    // } FILE_ID_INFO, *PFILE_ID_INFO;

    // FILE_ID_128 is a 128-bit (16-byte) opaque identifier
    public static final SequenceLayout FILE_ID_128_LAYOUT = MemoryLayout.sequenceLayout(16, JAVA_BYTE);

    public static final GroupLayout FILE_ID_INFO_LAYOUT = MemoryLayout.structLayout(
            JAVA_LONG.withName("VolumeSerialNumber"),
            JAVA_LONG.withName("FileIdLow"),
            JAVA_LONG.withName("FileIdHigh")
    );

    // VarHandles
    private static final VarHandle VolumeSerialNumber = FILE_ID_INFO_LAYOUT.varHandle(PathElement.groupElement("VolumeSerialNumber"));
    private static final VarHandle FileIdLow = FILE_ID_INFO_LAYOUT.varHandle(PathElement.groupElement("FileIdLow"));
    private static final VarHandle FileIdHigh = FILE_ID_INFO_LAYOUT.varHandle(PathElement.groupElement("FileIdHigh"));

    public static final int FILE_SHARE_READ = 0x00000001;
    public static final int FILE_SHARE_WRITE = 0x00000002;
    public static final int OPEN_EXISTING = 3;
    public static final int FILE_ATTRIBUTE_NORMAL = 0x80;
    public static final int FILE_FLAG_BACKUP_SEMANTICS = 0x02000000;
    public static final int FILE_FLAG_OPEN_REPARSE_POINT = 0x00200000;
    private static final int FILE_ATTRIBUTE_DIRECTORY = 0x10;
    private static final int FILE_ATTRIBUTE_NOFILE =
        0x40 // FILE_ATTRIBUTE_DEVICE
        | 0x400 // FILE_ATTRIBUTE_REPARSE_POINT
        | 0x10000 // FILE_ATTRIBUTE_VIRTUAL
        ;

    private static final MethodHandle GetLastError = linker.downcallHandle(kernel32.findOrThrow("GetLastError"), FunctionDescriptor.of(JAVA_INT));

    public static MemorySegment CreateFileW(
            String lpFileName,
            int dwDesiredAccess,
            int dwShareMode,
            MemorySegment lpSecurityAttributes,
            int dwCreationDisposition,
            int dwFlagsAndAttributes,
            MemorySegment hTemplateFile
    ) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment fileNameAddr = arena.allocateFrom(lpFileName, StandardCharsets.UTF_16LE);
            return (MemorySegment) CreateFileW.invoke(
                    fileNameAddr,
                    dwDesiredAccess,
                    dwShareMode,
                    lpSecurityAttributes,
                    dwCreationDisposition,
                    dwFlagsAndAttributes,
                    hTemplateFile
            );
        } catch (Throwable e) {
            e.printStackTrace();
            return MemorySegment.ofAddress(-1);
        }
    }

    public static boolean CloseHandle(MemorySegment handle) {
        try {
            return (boolean)CloseHandle.invoke(handle);
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    // FindFirstVolumeW
    private static final MemorySegment FindFirstVolumeWAddress = kernel32.findOrThrow("FindFirstVolumeW");
    private static final FunctionDescriptor FindFirstVolumeWDescriptor = FunctionDescriptor.of(
            ADDRESS,                // HANDLE return
            ADDRESS,                // LPWSTR lpszVolumeName
            JAVA_INT                // DWORD cchBufferLength
    );
    private static final MethodHandle FindFirstVolumeW = linker.downcallHandle(FindFirstVolumeWAddress, FindFirstVolumeWDescriptor);

    public static MemorySegment FindFirstVolumeW(MemorySegment lpszVolumeName, int cchBufferLength) {
        try {
            return (MemorySegment) FindFirstVolumeW.invoke(lpszVolumeName, cchBufferLength);
        } catch (Throwable e) {
            e.printStackTrace();
            return MemorySegment.ofAddress(-1); // INVALID_HANDLE_VALUE
        }
    }

    // FindNextVolumeW
    private static final MemorySegment FindNextVolumeWAddress = kernel32.findOrThrow("FindNextVolumeW");
    private static final FunctionDescriptor FindNextVolumeWDescriptor = FunctionDescriptor.of(
            JAVA_BOOLEAN,           // BOOL return
            ADDRESS,                // HANDLE hFindVolume
            ADDRESS,                // LPWSTR lpszVolumeName
            JAVA_INT                // DWORD cchBufferLength
    );
    private static final MethodHandle FindNextVolumeW = linker.downcallHandle(FindNextVolumeWAddress, FindNextVolumeWDescriptor);

    public static boolean FindNextVolumeW(MemorySegment hFindVolume, MemorySegment lpszVolumeName, int cchBufferLength) {
        try {
            return (boolean) FindNextVolumeW.invoke(hFindVolume, lpszVolumeName, cchBufferLength);
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    // FindVolumeClose
    private static final MemorySegment FindVolumeCloseAddress = kernel32.findOrThrow("FindVolumeClose");
    private static final FunctionDescriptor FindVolumeCloseDescriptor = FunctionDescriptor.of(
            JAVA_BOOLEAN,           // BOOL return
            ADDRESS                 // HANDLE hFindVolume
    );
    private static final MethodHandle FindVolumeClose = linker.downcallHandle(FindVolumeCloseAddress, FindVolumeCloseDescriptor);

    public static boolean FindVolumeClose(MemorySegment hFindVolume) {
        try {
            return (boolean) FindVolumeClose.invoke(hFindVolume);
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- Low-level FFM binding ---

    private static final MemorySegment GetVolumePathNamesForVolumeNameWAddress =
            kernel32.findOrThrow("GetVolumePathNamesForVolumeNameW");

    private static final FunctionDescriptor GetVolumePathNamesForVolumeNameWDescriptor =
            FunctionDescriptor.of(
                    JAVA_BOOLEAN,   // BOOL return
                    ADDRESS,        // LPCWSTR lpszVolumeName
                    ADDRESS,        // LPWCH lpszVolumePathNames
                    JAVA_INT,       // DWORD cchBufferLength
                    ADDRESS         // PDWORD lpcchReturnLength
            );

    private static final MethodHandle GetVolumePathNamesForVolumeNameW =
            linker.downcallHandle(GetVolumePathNamesForVolumeNameWAddress, GetVolumePathNamesForVolumeNameWDescriptor);

    private static boolean GetVolumePathNamesForVolumeNameW(
            MemorySegment lpszVolumeName,
            MemorySegment lpszVolumePathNames,
            int cchBufferLength,
            MemorySegment lpcchReturnLength
    ) {
        try {
            return (boolean) GetVolumePathNamesForVolumeNameW.invoke(
                    lpszVolumeName,
                    lpszVolumePathNames,
                    cchBufferLength,
                    lpcchReturnLength
            );
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

// --- High-level Java wrapper ---

    /**
     * Retrieves all mount points (drive letters and folder paths) for a volume.
     *
     * @param volumeGuidPath Volume GUID path in the form "\\?\Volume{xxxxxxxx-xxxx-...}\"
     * @return List of mount points (e.g., ["C:\\", "D:\\Mounted\\"]), or empty list on failure
     */
    public static List<String> getVolumeMountPoints(String volumeGuidPath) {
        if (volumeGuidPath == null || volumeGuidPath.isEmpty()) {
            return List.of();
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment volumeNameSeg = arena.allocateFrom(volumeGuidPath, StandardCharsets.UTF_16LE);

            // First call: get required buffer size (in wide chars)
            MemorySegment returnLengthSeg = arena.allocate(JAVA_INT);
            boolean success = GetVolumePathNamesForVolumeNameW(
                    volumeNameSeg,
                    MemorySegment.NULL,  // no output buffer yet
                    0,                   // buffer length = 0
                    returnLengthSeg
            );

            int neededChars = returnLengthSeg.get(JAVA_INT, 0);
            if (neededChars == 0) {
                // No mount points, or error (e.g., invalid volume path)
                return List.of();
            }

            // Allocate buffer for multi-string output (wide chars)
            MemorySegment pathNamesBuffer = arena.allocate(JAVA_SHORT, neededChars);

            // Second call: retrieve actual path names
            success = GetVolumePathNamesForVolumeNameW(
                    volumeNameSeg,
                    pathNamesBuffer,
                    neededChars,
                    returnLengthSeg
            );

            if (!success) {
                // Optionally log GetLastError() here
                return List.of();
            }

            // Parse multi-string: sequence of null-terminated strings, ending with double null
            List<String> mountPoints = new ArrayList<>();
            int offsetBytes = 0;
            final int totalBytes = neededChars * 2; // each wchar_t = 2 bytes

            while (offsetBytes < totalBytes) {
                short firstChar = pathNamesBuffer.get(JAVA_SHORT, offsetBytes);
                if (firstChar == 0) {
                    break; // end of list (double null terminator)
                }
                String path = pathNamesBuffer.getString(offsetBytes, StandardCharsets.UTF_16LE);
                mountPoints.add(path);
                offsetBytes += (path.length() + 1) * 2; // +1 for null terminator
            }

            return List.copyOf(mountPoints);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    // GetVolumeInformationW
    private static final MemorySegment GetVolumeInformationWAddress = kernel32.findOrThrow("GetVolumeInformationW");
    private static final FunctionDescriptor GetVolumeInformationWDescriptor = FunctionDescriptor.of(
            JAVA_BOOLEAN,           // BOOL return
            ADDRESS,                // LPCWSTR lpRootPathName
            ADDRESS,                // LPWSTR lpVolumeNameBuffer
            JAVA_INT,               // DWORD nVolumeNameSize
            ADDRESS,                // LPDWORD lpVolumeSerialNumber
            ADDRESS,                // LPDWORD lpMaximumComponentLength
            ADDRESS,                // LPDWORD lpFileSystemFlags
            ADDRESS,                // LPWSTR lpFileSystemNameBuffer
            JAVA_INT                // DWORD nFileSystemNameSize
    );
    private static final MethodHandle GetVolumeInformationW = linker.downcallHandle(GetVolumeInformationWAddress, GetVolumeInformationWDescriptor);


    record FileInformationByHandle(
        long lastModified,
        boolean isFile,
        boolean isDirectory,
        long size,
        long physicalSize,
        int volumeId,
        long fileIdLow,
        long fileIdHigh,
        int numberOfLinks
    ) {}

    public static FileInformationByHandle GetFileInformationByHandleEx(
        MemorySegment h
    ) {
        try (Arena a = Arena.ofConfined()) {
            var fileBasicInfo = a.allocate(FILE_BASIC_INFO_LAYOUT);
            var fileStandardInfo = a.allocate(FILE_STANDARD_INFO_LAYOUT);
            var fileIdInfo = a.allocate(FILE_ID_INFO_LAYOUT);

            boolean r1 = (boolean)GetFileInformationByHandleEx.invoke(h, 0, fileBasicInfo, (int)fileBasicInfo.byteSize());
            boolean r2 = (boolean)GetFileInformationByHandleEx.invoke(h, 1, fileStandardInfo, (int)fileStandardInfo.byteSize());
            boolean r3 = (boolean)GetFileInformationByHandleEx.invoke(h, 18, fileIdInfo, (int)fileIdInfo.byteSize());
            if (!r1 || !r2 || !r3) return null;
            long lastModified = fileTimeToUnixTime((long)ChangeTime.get(fileBasicInfo, 0L));
            int attributes = (int)FileAttributes.get(fileBasicInfo, 0L);
            long allocationSize = (long)AllocationSize.get(fileStandardInfo, 0L);
            long size = (long)EndOfFile.get(fileStandardInfo, 0L);
            long volumeId = (long)VolumeSerialNumber.get(fileIdInfo, 0L);
            long fileIdLow = (long)FileIdLow.get(fileIdInfo, 0L);
            long fileIdHigh = (long)FileIdHigh.get(fileIdInfo, 0L);
            int numberOfLinks = (int)NumberOfLinks.get(fileStandardInfo, 0L);

            boolean isNoFile = (attributes & FILE_ATTRIBUTE_NOFILE) != 0;
            boolean isDirectory = (attributes & FILE_ATTRIBUTE_DIRECTORY) != 0;
            return new FileInformationByHandle(
                    lastModified,
                    !isNoFile && !isDirectory,
                    !isNoFile && isDirectory,
                    size,
                    allocationSize,
                    (int)volumeId,
                    fileIdLow,
                    fileIdHigh,
                    numberOfLinks
            );
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }

    }

    public record VolumeInfo(
            String volumeName,          // e.g., "OS", "Data"
            int volumeSerialNumber,     // matches dwVolumeSerialNumber in BY_HANDLE_FILE_INFORMATION
            String fileSystemName,      // e.g., "NTFS", "FAT32", "exFAT"
            int maxComponentLength,     // max filename length (in chars)
            int fileSystemFlags         // e.g., FILE_READ_ONLY_VOLUME, etc.
    ) {}

    public static VolumeInfo getVolumeInformation(String volumeGuid) {
        try (Arena arena = Arena.ofConfined()) {

            MemorySegment volNameBuf = arena.allocate(JAVA_SHORT, 128);
            MemorySegment fsNameBuf  = arena.allocate(JAVA_SHORT, 128);
            MemorySegment serialNum  = arena.allocate(JAVA_INT);
            MemorySegment maxComp    = arena.allocate(JAVA_INT);
            MemorySegment fsFlags    = arena.allocate(JAVA_INT);

            boolean success = (boolean)GetVolumeInformationW.invoke(
                    arena.allocateFrom(volumeGuid, StandardCharsets.UTF_16LE),
                    volNameBuf,
                    128,
                    serialNum,
                    maxComp,
                    fsFlags,
                    fsNameBuf,
                    128
            );

            if (!success) {
                // Optionally log GetLastError() here
                return null;
            }

            String volumeName = volNameBuf.getString(0L, StandardCharsets.UTF_16LE);
            String fileSystemName = fsNameBuf.getString(0L, StandardCharsets.UTF_16LE);
            int serial = serialNum.get(JAVA_INT, 0);
            int maxLen = maxComp.get(JAVA_INT, 0);
            int flags  = fsFlags.get(JAVA_INT, 0);

            return new VolumeInfo(volumeName, serial, fileSystemName, maxLen, flags);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private static long FILETIME_DELTA;
    static {
        // Windows FILETIME epoch: January 1, 1601 UTC
        LocalDateTime filetimeEpoch = LocalDateTime.of(1601, 1, 1, 0, 0, 0);
        Instant filetimeInstant = filetimeEpoch.toInstant(ZoneOffset.UTC);

        // Unix epoch: January 1, 1970 UTC
        Instant unixEpoch = Instant.EPOCH; // 1970-01-01T00:00:00Z

        // Duration between the two epochs (in seconds)
        long secondsBetween = java.time.Duration.between(filetimeInstant, unixEpoch).getSeconds();

        // Convert to 100-nanosecond intervals (1 second = 10^7 × 100-ns)
        FILETIME_DELTA = secondsBetween * 10_000_000L;
    }
    private static long fileTimeToUnixTime(long fileTime) {
        return (fileTime - FILETIME_DELTA) / 10_000;
    }
}
