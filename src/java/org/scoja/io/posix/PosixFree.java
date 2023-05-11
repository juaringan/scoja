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
 * This is a do-nothing-or-fail implementation of {@link PosixLike}.
 * It is implemented in Java and it is instaled as the default Posix
 * server for the system at {@link Posix}.
 * Some methods, like {@link #setFileMode}, do nothing at all,
 * others, like {@link #getCurrentUser}, make up an id,
 * others, like {@link #newUnixDatagram}, fail with an exception.
 * Scoja server inherits this behaviour: some Posix operations
 * (i.e., establishing owners and permissions) are silently ignored,
 * but others (i.e., opening a Unix server socket) fail.
 */
public class PosixFree
    implements PosixLike {

    public boolean hasSystem() { return false; }
    public boolean hasFilesystem() { return false; }
    public boolean hasIO() { return false; }
        
    private static final String UNKNOWN = "unknown";
    private static final int DEFAULT_USER_ID = 0;
    private static final int DEFAULT_GROUP_ID = 0;
    
    //======================================================================
    // From PosixSystem
    
    public int getCurrentUser() {
        return DEFAULT_USER_ID;
    }
    
    public int getEffectiveUser() {
        return DEFAULT_USER_ID;
    }
    
    public int getCurrentGroup() {
        return DEFAULT_GROUP_ID;
    }
    
    public int getEffectiveGroup() {
        return DEFAULT_GROUP_ID;
    }

    public UserInfo getUserInfo(final String login) {
        return new UserInfo(login, DEFAULT_USER_ID, DEFAULT_GROUP_ID,
                            UNKNOWN, UNKNOWN, UNKNOWN);
    }
    
    public UserInfo getUserInfo(final int id) {
        return new UserInfo(UNKNOWN, id, DEFAULT_GROUP_ID,
                            UNKNOWN, UNKNOWN, UNKNOWN);
    }
    
    public GroupInfo getGroupInfo(final String name) {
        return new GroupInfo(name, DEFAULT_GROUP_ID, new String[0]);
    }
    
    public GroupInfo getGroupInfo(final int id) {
        return new GroupInfo(UNKNOWN, id, new String[0]);
    }

    
    //======================================================================
    // From PosixFilesytem
    
    private static final FileStat DEFAULT_FILESTAT = new FileStat(
        0, 0, 0, 1, DEFAULT_USER_ID, DEFAULT_GROUP_ID, 0,
        0, 0, 0, 0, 0, 0);
    
    public FileStat getFileStat(final String filename) {
        return DEFAULT_FILESTAT;
    }
    
    public FileStat getFileStat(final FileDescriptor fileid) {
        return DEFAULT_FILESTAT;
    }
    
    
    public void setFileMode(final String filename, final int mode) {
    }
        
    public void setFileMode(final FileDescriptor fileid, final int mode) {
    }
        
    public void setFileOwner(final String filename,
                             final int userid, final int groupid) {
    }
        
    public void setFileOwner(final FileDescriptor fileid,
                             final int userid, final int groupid) {
    }
    
    
    //======================================================================
    // From PosixIO
    
    //----------------------------------------------------------------------
    public void close(final int fd)
    throws IOException {
        throw new UnsupportedIOException();
    }

    public int select(final int[] fds, final int[] interests, final int len,
                      final int maxFD, final long timeout)
    throws IOException {
        throw new UnsupportedIOException();
    }
    

    //----------------------------------------------------------------------
    public int read(int fd)
    throws IOException {
        throw new UnsupportedIOException();
    }
        
    public int read(final int fd, final byte[] b, final int off, final int len)
    throws IOException {
        throw new UnsupportedIOException();
    }
        
    public int write(final int fd, final int b)
    throws IOException {
        throw new UnsupportedIOException();
    }
        
    public int write(final int fd,
                     final byte[] b, final int off, final int len)
    throws IOException {
        throw new UnsupportedIOException();
    }
        
            
    //----------------------------------------------------------------------
    public long newPipe()
    throws IOException {
        throw new UnsupportedIOException();
    }
    
    
    //----------------------------------------------------------------------
    public int newInetDatagram()
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public int newInetStream()
    throws SocketException {
        throw new UnsupportedIOException();
    }
        
    public int newUnixDatagram()
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public int newUnixStream()
    throws SocketException {
        throw new UnsupportedIOException();
    }
        
    public void bind(final int fd, final String path)
    throws SocketException {
        throw new UnsupportedIOException();
    }
        
    public void bind(final int fd, final int ip, final int port)
    throws SocketException {
        throw new UnsupportedIOException();
    }
        
    public void listen(final int fd, final int incomingQueueLimit)
    throws SocketException {
        throw new UnsupportedIOException();
    }
        
    public void connect(final int fd, final String path)
    throws SocketException {
        throw new UnsupportedIOException();
    }
        
    public void connect(final int fd, final int ip, final int port)
    throws SocketException {
        throw new UnsupportedIOException();
    }
        
    public InetSocketDescription acceptInet(final int fd)
    throws SocketException {
        throw new UnsupportedIOException();
    }

    public UnixSocketDescription acceptUnix(final int fd)
    throws SocketException {
        throw new UnsupportedIOException();
    }

    public int send(final int fd, final int b)
    throws IOException {
        throw new UnsupportedIOException();
    }
        
    public int send(final int fd, final byte[] b, final int off, final int len)
    throws IOException {
        throw new UnsupportedIOException();
    }
        
    public int sendTo(final int fd,
                      final byte[] b, final int off, final int len,
                      final String path)
    throws IOException {
        throw new UnsupportedIOException();
    }
            
    public int receive(final int fd)
    throws IOException {
        throw new UnsupportedIOException();
    }
        
    public int receive(final int fd,
                       final byte[] b, final int off, final int len)
    throws IOException {
        throw new UnsupportedIOException();
    }
        
    public void receiveFrom(final int fd, final GenericDatagramPacket p)
    throws IOException {
        throw new UnsupportedIOException();
    }
    
    public void receiveFrom(final int fd, final DatagramPacket p)
    throws IOException {
        throw new UnsupportedIOException();
    }
    
    public void shutdown(final int fd, final int what)
    throws IOException {
        throw new UnsupportedIOException();
    }
    
    
    //----------------------------------------------------------------------
    // SOCKET OPTIONS
    
    public void setDebug(final int fd, final boolean on)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public boolean getDebug(final int fd)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public void setBroadcast(final int fd, final boolean on)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public boolean getBroadcast(final int fd)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public void setKeepAlive(final int fd, final boolean on)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public boolean getKeepAlive(final int fd)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public void setOOBInline(final int fd, final boolean on)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public boolean getOOBInline(final int fd)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public void setReadTimeout(final int fd, final long timeout)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public long getReadTimeout(final int fd)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public void setReceiveBufferSize(final int fd, final int size)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public int getReceiveBufferSize(final int fd)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public void setReuseAddress(final int fd, final boolean reuse)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public boolean getReuseAddress(final int fd)
    throws SocketException {
        throw new UnsupportedIOException();
    }        
    
    public void setSendBufferSize(final int fd, final int size)
    throws SocketException{
        throw new UnsupportedIOException();
    }
    
    public int getSendBufferSize(final int fd)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public void setSoLinger(final int fd, final boolean on, final int linger)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public int getSoLinger(final int fd)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public void setTcpNoDelay(final int fd, final boolean on)
    throws SocketException {
        throw new UnsupportedIOException();
    }
    
    public boolean getTcpNoDelay(final int fd)
    throws SocketException {
        throw new UnsupportedIOException();
    }
}
