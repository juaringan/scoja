/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003  Mario Martínez
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.scoja.io.posix;

/**
 * This class is the way to add behaviour to a Posix file mode.
 * It supposes that the mode is an integer number organized as Posix file
 * modes.
 * It provides operations to convert to and from the usual string
 * representation.
 *
 * @todo
 * We are not using this class as much as it should be.
 */
public class FileMode {

    //======================================================================
    // Static stuff
    
    /** Bitmask for the file type bitfields */
    public static final int IFMT = 0170000;
    /** Socket. Value after masking with {@link #IFMT}. */
    public static final int IFSOCK = 0140000;   
    /** Symbolic link. */
    public static final int IFLNK = 0120000;
    /** Regular file. */
    public static final int IFREG = 0100000;
    /** Block device. */
    public static final int IFBLK = 0060000;
    /** Directory. */
    public static final int IFDIR = 0040000;
    /** Character device. */
    public static final int IFCHR = 0020000;
    /** Fifo. */
    public static final int IFIFO = 0010000;

    /** Mask to remove away file type info from a mode. */
    public static final int PURE_PERMISSIONS = 0007777;
        
    /** Set UID bit. */
    public static final int ISUID = 0004000;
    /** Set GID bit. */
    public static final int ISGID = 0002000;
    /** Sticky bit. */
    public static final int ISVTX = 0001000;
    
    /** Mask for file owner permissions. */
    public static final int IRWXU = 00700;
    /** Owner has read permission. */
    public static final int IRUSR = 00400;
    /** Owner has write permission. */
    public static final int IWUSR = 00200;
    /** Owner has execute permission. */
    public static final int IXUSR = 00100;
    /** Mask for group permissions. */
    public static final int IRWXG = 00070;
    /** Group has read permission. */
    public static final int IRGRP = 00040;
    /** Group has write permission. */
    public static final int IWGRP = 00020;
    /** Group has execute permission. */
    public static final int IXGRP = 00010;
    /** Mask for permissions for others (not in group). */
    public static final int IRWXO = 00007;
    /** Others have read permission. */
    public static final int IROTH = 00004;
    /** Others have write permisson. */
    public static final int IWOTH = 00002;
    /** Others have execute permission. */
    public static final int IXOTH = 00001;
    
    public static final int IRALL = IRUSR | IRGRP | IROTH;
    public static final int IWALL = IWUSR | IWGRP | IWOTH;
    public static final int IXALL = IXUSR | IXGRP | IXOTH;
    
    public static int parse(final String mode)
    throws IllegalArgumentException {
        if (mode.length() < 9 || 10 < mode.length()) {
            throw new IllegalArgumentException(
                "A legal mode should have 9 or 10 caracteres");
        }
        int base = 0;
        final int fileType;
        if (mode.length() == 10) {
            final char ftc = mode.charAt(0);
            fileType
                = (ftc == 's') ? IFSOCK
                : (ftc == 'l') ? IFLNK
                : (ftc == '-') ? IFREG
                : (ftc == 'b') ? IFBLK
                : (ftc == 'd') ? IFDIR
                : (ftc == 'c') ? IFCHR
                : (ftc == 'f') ? IFIFO
                : 0;
            base++;
        } else {
            fileType = 0;
        }
            
        int perm = 0;
        for (int i = 0; i < 9; i++) {
            final char c = mode.charAt(i+base);
            if (c == '-') continue;
            if (((i % 3) == 0 && c != 'r')
                || ((i % 3) == 1 && c != 'w')
                || ((i == 2 || i == 5) && c!='x' && c!='s' && c!='S')
                || (i == 8 && c != 'x' && c!='t' && c!='T')) {
                throw new IllegalArgumentException(
                    "Bad character '" + c + "' at position " + (base+i));
            }
            if ((i % 3) != 2 || c >= 'a') {
                perm |= 1 << (8-i);
            }
            if ((i % 3) == 2 && c != 'x') {
                if (i == 2) perm |= ISUID;
                else if (i == 5) perm |= ISGID;
                else if (i == 8) perm |= ISVTX;
            }
        }
            
        return fileType | perm;
    }

    public static String toString(final int mode) {
        final char[] ms = new char[1+3*3];
        
        final int fileType = mode & IFMT;
        ms[0] = (fileType == IFSOCK) ? 's'
            : (fileType == IFLNK) ? 'l'
            : (fileType == IFREG) ? '-'
            : (fileType == IFBLK) ? 'b'
            : (fileType == IFDIR) ? 'd'
            : (fileType == IFCHR) ? 'c'
            : (fileType == IFIFO) ? 'f'
            : '?';
        
        int mask = 0400;
        for (int i = 0; i < 9; i++, mask >>>= 1) {
            ms[i+1] = ((mode & mask) == 0) ? '-' : "rwx".charAt(i%3);
        }
        
        if ((mode & ISUID) != 0) {
            ms[3] = ((mode & IXUSR) == 0) ? 'S' : 's';
        }
        if ((mode & ISGID) != 0) {
            ms[6] = ((mode & IXGRP) == 0) ? 'S' : 's';
        }
        if ((mode & ISVTX) != 0) {
            ms[9] = ((mode & IXOTH) == 0) ? 'T' : 't';
        }
        
        return new String(ms);
    }
    
    
    //======================================================================
    // Dynamic stuff
    
    protected final int mode;
    
    public FileMode(final int mode) {
        this.mode = mode;
    }
    
    public FileMode(final String mode)
    throws IllegalArgumentException {
        int md;
        try {
            md = Integer.parseInt(mode, 8);
        } catch (NumberFormatException e) {
            md = parse(mode);
        }
        this.mode = md;
    }
    
    public int intValue() {
        return mode;
    }
    
    public int getFileType() {
        return mode & IFMT;
    }
    
    public boolean isFileType(final int fileType) {
        return getFileType() == fileType;
    }
    
    public boolean isSocket() {
        return isFileType(IFSOCK);
    }
    
    public boolean isLink() {
        return isFileType(IFLNK);
    }
    
    public boolean isFile() {
        return isFileType(IFREG);
    }
    
    public boolean isBlock() {
        return isFileType(IFBLK);
    }
    
    public boolean isDirectory() {
        return isFileType(IFDIR);
    }
    
    public boolean isCharacterDevice() {
        return isFileType(IFCHR);
    }
    
    public boolean isFifo() {
        return isFileType(IFIFO);
    }

    public int getPermissions() {
        return getPermissions(mode);
    }
    
    public static int getPermissions(final int mode) {
        return mode & PURE_PERMISSIONS;
    }
    
    public FileMode justPermissions() {
        return new FileMode(mode & PURE_PERMISSIONS);
    }
    
    //======================================================================
    public String toString() {
        return toString(mode);
    }
    
    public int hashCode() {
        return mode;
    }
    
    public boolean equals(final Object other) {
        return (other instanceof FileMode)
            && equals((FileMode)other);
    }
    
    public boolean equals(final FileMode other) {
        return this.mode == other.mode;
    }
}
