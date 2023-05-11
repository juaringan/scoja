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

import java.io.IOException;
import java.io.FileDescriptor;

/**
 * <p><b>Multithreading and C functions with static results</b>
 * Several C functions (like <code>getpwuid</code>) returns data
 * through a pointer to a static allocated memory that is shared for
 * all threads.
 * Thus, two concurrent executions of this function can mix theirs
 * results.
 * To prevent it, this class has a static lock for each C function
 * that return data through a shared static memory.
 * Before calling C funcion <code>fun</code>, code must synchronize on
 * <code>funLock</code>.
 * To avoid concurrent nightmares at C code, this class synchronize at
 * a Java method that calls the native method invoking the culprit C
 * function.
 * <p>
 * This technique <b>doesn't remove problems completely</b>.
 * If this class is loaded with 2 different class loaders, there will
 * be to sets of locks.
 * Threads working with one class can intermigle its results with
 * another thread working with the other class.
 * There is only one way to solve this: to synchronize at C level.
 * But this requires to know the threading model the JVM is using.
 */
public class NativePosixSystem
    implements PosixSystem {

    static {
        System.loadLibrary("NativePosixSystem");
    }

    protected static final Object getpwnamLock = new Object();
    protected static final Object getpwuidLock = new Object();
    protected static final Object getgrnamLock = new Object();
    protected static final Object getgrgidLock = new Object();
    
    //======================================================================
    public native int getCurrentUser();
    
    public native int getEffectiveUser();

    public native int getCurrentGroup();
    
    public native int getEffectiveGroup();
    
    public UserInfo getUserInfo(final String login) {
        synchronized (getpwnamLock) {
            return getUserInfo0(login.getBytes());
        }
    }
    
    public UserInfo getUserInfo(final int id) {
        synchronized (getpwuidLock) {
            return getUserInfo0(id);
        }
    }
    
    protected native UserInfo getUserInfo0(byte[] login);
    
    protected native UserInfo getUserInfo0(int id);
    
    
    public GroupInfo getGroupInfo(final String name) {
        synchronized (getgrnamLock) {
            return getGroupInfo0(name.getBytes());
        }
    }
    
    public GroupInfo getGroupInfo(final int id) {
        synchronized (getgrgidLock) {
            return getGroupInfo0(id);
        }
    }
    
    protected native GroupInfo getGroupInfo0(byte[] name);
    
    protected native GroupInfo getGroupInfo0(int id);
    
    
    //======================================================================
    public FileStat getFileStat(final String filename)
        throws IOException {
        return getFileStat(filename.getBytes());
    }
        
    protected native FileStat getFileStat(byte[] filename)
        throws IOException;
        
    public native FileStat getFileStat(FileDescriptor fileid)
        throws IOException;
    
    
    public void setFileMode(final String filename, final int mode)
        throws IOException {
        setFileMode(filename.getBytes(), mode);
    }
        
    protected native void setFileMode(byte[] filename, int mode)
        throws IOException;
        
    public native void setFileMode(FileDescriptor fileid, int mode)
        throws IOException;
        
        
    public void setFileOwner(final String filename,
                             final int userid, final int groupid)
        throws IOException {
        setFileOwner(filename.getBytes(), userid, groupid);
    }
    
    protected native void setFileOwner(byte[] filename,
                                       int userid, int groupid)
        throws IOException;
        
    public native void setFileOwner(FileDescriptor fileid,
                                    int userid, int groupid)
        throws IOException;
    
    
    //======================================================================
    public String toString() {
        return "Native implementation of usal Posix functions";
    }    
}
