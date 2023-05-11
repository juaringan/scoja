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

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;

import org.scoja.io.GenericDatagramPacket;
import org.scoja.io.UnsupportedIOException;

/**
 * A full implementation of {@link PosixLike}, using native calls when
 * necessary.
 *
 * <p><b>Multithreading and C functions with static results</b>
 * Several Unix functions (like <code>getpwuid</code>) returns data
 * through a pointer to a static allocated memory that is shared for
 * all threads.
 * Thus, two concurrent executions of this function can mix theirs
 * results.
 * To prevent it, this class uses Posix variants that need memory buffers
 * allocated at the call point (like <code>getpwuid_r</code>).
 */
public class PosixNative
    implements PosixLike {
    
    static {
        System.loadLibrary("PosixNative");
    }
    
    public boolean hasSystem() { return true; }
    public boolean hasFilesystem() { return true; }
    public boolean hasIO() { return true; }
    
    //======================================================================
    // From PosixSystem
    
    public native int getCurrentUser();
    
    public native int getEffectiveUser();
    
    public native int getCurrentGroup();
    
    public native int getEffectiveGroup();

    public UserInfo getUserInfo(final String login) {
        return getUserInfo(login.getBytes());
    }
    
    public native UserInfo getUserInfo(byte[] login);
    
    public native UserInfo getUserInfo(int id);
    
    public GroupInfo getGroupInfo(final String name) {
        return getGroupInfo(name.getBytes());
    }
    
    public native GroupInfo getGroupInfo(byte[] name);
    
    public native GroupInfo getGroupInfo(int id);
    
    
    //======================================================================
    // From PosixFilesytem
    
    public FileStat getFileStat(final String filename)
    throws IOException {
        return getFileStat(filename.getBytes());
    }
    
    public native FileStat getFileStat(byte[] filename)
    throws IOException;
        
    public native FileStat getFileStat(FileDescriptor fileid)
    throws IOException;

    public void setFileMode(final String filename, final int mode)
    throws IOException {
        setFileMode(filename.getBytes(), mode);
    }
        
    public native void setFileMode(byte[] filename, int mode)
    throws IOException;
    
    public native void setFileMode(FileDescriptor fileid, int mode)
    throws IOException;
        
    public void setFileOwner(final String filename,
                             final int userid, final int groupid)
    throws IOException {
        setFileOwner(filename.getBytes(), userid, groupid);
    }
    
    public native void setFileOwner(byte[] filename, int userid, int groupid)
    throws IOException;
    
    public native void setFileOwner(FileDescriptor fileid,
                                    int userid, int groupid)
    throws IOException;
    
    
    //======================================================================
    // From PosixIO
    
    //----------------------------------------------------------------------
    public native void close(int fd)
    throws IOException;

    public native int select(int[] fds, int[] interests, int length,
                             int maxFD, long timeout)
    throws IOException;

    //----------------------------------------------------------------------
    public native int read(int fd)
    throws IOException;
        
    public native int read(int fd, byte[] b, int off, int len)
    throws IOException;
        
    public native int write(int fd, int b)
    throws IOException;
        
    public native int write(int fd, byte[] b, int off, int len)
    throws IOException;
        
    //----------------------------------------------------------------------
    public native long newPipe()
    throws IOException;
    
    //----------------------------------------------------------------------
    public native int newInetDatagram()
    throws SocketException;
    
    public native int newInetStream()
    throws SocketException;
        
    public native int newUnixDatagram()
    throws SocketException;
    
    public native int newUnixStream()
    throws SocketException;
        
    public void bind(final int fd, final String path)
    throws SocketException {
        bind(fd, path.getBytes());
    }
        
    public native void bind(int fd, byte[] path)
    throws SocketException;
    
    public native void bind(int fd, int ip, int port)
    throws SocketException;
    
    public native void listen(int fd, int incomingQueueLimit)
    throws SocketException;
        
    public void connect(final int fd, final String path)
    throws SocketException {
        connect(fd, path.getBytes());
    }
        
    public native void connect(int fd, byte[] path)
    throws SocketException;
    
    public native void connect(int fd, int ip, int port)
    throws SocketException;
    
    public native InetSocketDescription acceptInet(int fd)
    throws SocketException;

    public native UnixSocketDescription acceptUnix(int fd)
    throws SocketException;

    public native int send(int fd, int b)
    throws IOException;
        
    public native int send(int fd, byte[] b, int off, int len)
    throws IOException;
        
    public int sendTo(final int fd,
                      final byte[] b, final int off, final int len,
                      final String path)
    throws IOException {
        return sendTo(fd, b, off, len, path.getBytes());
    }
    
    public native int sendTo(int fd, byte[] b, int off, int len, byte[] path)
    throws IOException;
            
    public native int receive(int fd)
    throws IOException;
        
    public native int receive(int fd, byte[] b, int off, int len)
    throws IOException;
        
    public native void receiveFrom(int fd, GenericDatagramPacket p)
    throws IOException;
    
    public native void receiveFrom(int fd, DatagramPacket p)
    throws IOException;
    
    public native void shutdown(int fd, int what)
    throws IOException;
    
    //----------------------------------------------------------------------
    /*new*/
    public native void setDebug(int fd, boolean on)
    throws SocketException;
    
    public native boolean getDebug(int fd)
    throws SocketException;
    
    public native void setBroadcast(int fd, boolean on)
    throws SocketException;
    
    public native boolean getBroadcast(int fd)
    throws SocketException;
    
    /*new*/
    public native void setKeepAlive(int fd, boolean on)
    throws SocketException;
    
    public native boolean getKeepAlive(int fd)
    throws SocketException;

    /*new*/    
    public native void setOOBInline(int fd, boolean on)
    throws SocketException;
    
    public native boolean getOOBInline(int fd)
    throws SocketException;
    
    public native void setReadTimeout(int fd, long timeout)
    throws SocketException;
    
    public native long getReadTimeout(int fd)
    throws SocketException;
    
    public native void setReceiveBufferSize(int fd, int size)
    throws SocketException;
    
    public native int getReceiveBufferSize(int fd)
    throws SocketException;
    
    public native void setReuseAddress(int fd, boolean reuse)
    throws SocketException;
    
    public native boolean getReuseAddress(int fd)
    throws SocketException;
    
    public native void setSendBufferSize(int fd, int size)
    throws SocketException;
    
    public native int getSendBufferSize(int fd)
    throws SocketException;
    
    /*new*/
    public native void setSoLinger(int fd, boolean on, int linger)
    throws SocketException;
    
    public native int getSoLinger(int fd)
    throws SocketException;

    /*new*/    
    public native void setTcpNoDelay(int fd, boolean on)
    throws SocketException;
    
    public native boolean getTcpNoDelay(int fd)
    throws SocketException;
}
