package spacemonger1.fs;

import java.util.Set;

public class Common {
    private static final Set<String> KNOWN_PHYSICAL_FS = Set.of(
        "afpfs",
        "apfs",
        "bcachefs",
        "btrfs",
        "cd9660",
        "cifs",
        "exfat",
        "ext2",
        "ext3",
        "ext4",
        "f2fs",
        "fat",
        "fuseblk",
        "hfs",
        "hfsplus",
        "iso9660",
        "jfs",
        "msdos",
        "msdosfs",
        "nfs",
        "ntfs",
        "ntfs3",
        "reiserfs",
        "smbfs",
        "udf",
        "ufs",
        "vfat",
        "webdav",
        "xfs",
        "zfs"
    );

    public static boolean isVirtualFs(String fs) {
        return !KNOWN_PHYSICAL_FS.contains(fs);
    }

    public static boolean hasVirtualFs(String mountLine) {
        for (String fs: KNOWN_PHYSICAL_FS) {
            if (mountLine.contains(" " + fs + " ")) return false;
        }
        return true;
    }
}
