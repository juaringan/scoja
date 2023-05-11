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
import java.net.DatagramPacket;
import java.net.SocketException;

import org.scoja.io.GenericDatagramPacket;

/**
 * Posix services related to IO:
 * channel openning, data reading and writing, etc.
 * 
 * <p>Looking for setsockopt options; try /usr/include/asm/socket.h.
 *
 * @see PosixSystem
 * @see PosixFilesystem
 */
public interface PosixIO {

    public boolean hasIO();
    
    //======================================================================
    // GENERAL I/O OPERATIONS (with file descriptors)
    /*
    public void flush(int fd)
    throws IOException;
    */
      
    /**
     * Close socket <code>fd</code>.
     */
    public void close(int fd)
    throws IOException;
    
    public int select(int[] fds, int[] interests, int length,
                      int maxFD, long timeout)
    throws IOException;

    
    //======================================================================
    // BASIC I/O OPERATIONS (with file descriptors)
    
    public int read(int fd)
    throws IOException;
        
    public int read(int fd, byte[] b, int off, int len)
    throws IOException;
        
    public int write(int fd, int b)
    throws IOException;
        
    public int write(int fd, byte[] b, int off, int len)
    throws IOException;
        
            
    //======================================================================
    // PIPES
    
    public long newPipe()
    throws IOException;
    
    
    //======================================================================
    // SOCKET OPERATIONS
    
    /**
     * Creates an Internet domain datagram socket.
     * @return the socket descriptor.
     * @throws SocketException if the socket cannot be created.
     */
    public int newInetDatagram()
    throws SocketException;
    
    /**
     * Creates an Internet domain stream socket.
     * @return the socket descriptor.
     * @throws SocketException if the socket cannot be created.
     */
    public int newInetStream()
    throws SocketException;
    
    /**
     * Creates a Unix domain datagram socket.
     * @return the socket descriptor.
     * @throws SocketException if the socket cannot be created.
     */
    public int newUnixDatagram()
    throws SocketException;
    
    /**
     * Creates a Unix domain stream socket.
     * @return the socket descriptor.
     * @throws SocketException if the socket cannot be created.
     */
    public int newUnixStream()
    throws SocketException;

    
    /**
     * Binds socket <code>fd</code> to file <code>path</code>.
     * <tt>fd</tt> is supposed to be an Unix domain socket.
     */
    public void bind(int fd, String path)
    throws SocketException;
        
    /**
     * Binds socket <code>fd</code> to address <tt>ip</tt>:<tt>port</tt>.
     * <tt>fd</tt> is supposed to be an Inet domain socket.
     */
    public void bind(int fd, int ip, int port)
    throws SocketException;
        
    /**
     * Set limit for the pending incoming queue of socket
     * <code>fd</code>.
     * Only for {@link org.scoja.io.UnixServerSocket}.
     */
    public void listen(int fd, int incomingQueueLimit)
    throws SocketException;
        
    /**
     * Connect socket <code>fd</code> to file <code>path</code>.
     * <tt>fd</tt> is supposed to be an Unix domain socket.
     */
    public void connect(int fd, String path)
    throws SocketException;
    
    /**
     * Connect socket <code>fd</code> to address <tt>ip</tt>:<tt>port</tt>.
     * <tt>fd</tt> is supposed to be an Inet domain socket.
     */
    public void connect(int fd, int ip, int port)
    throws SocketException;
    
    /**
     * Accept a new connection to socket <code>fd</code>.
     * Only for {@link org.scoja.io.InetServerSocket}.
     */
    public InetSocketDescription acceptInet(int fd)
    throws SocketException;

    /**
     * Accept a new connection to socket <code>fd</code>.
     * Only for {@link org.scoja.io.UnixServerSocket}.
     */
    public UnixSocketDescription acceptUnix(int fd)
    throws SocketException;

    /**
     * Send the byte in <code>b</code> to socket <code>fd</code>.
     * @return real send bytes.
     */
    public int send(int fd, int b)
    throws IOException;
        
    /**
     * Send <code>len</code> bytes from <code>b</code> starting at
     * <code>off</code> to socket <code>fd</code>.
     * @return real send bytes.
     */
    public int send(int fd, byte[] b, int off, int len)
    throws IOException;
        
    /**
     * Just like {@link #send(int,byte[],int,int)}, but send to
     * socket named <code>path</code>.
     * @return real send bytes.
     */
    public int sendTo(int fd, byte[] b, int off, int len, String path)
    throws IOException;
            
    /**
     * Receive one byte from <code>fd</code>.
     * @return the received byte, or -1 if the socket has ended.
     */
    public int receive(int fd)
    throws IOException;
        
    /**
     * Receive upto <code>len</code> bytes from socket
     * <code>fd</code>. They are stored in <code>b</code> starting at
     * index <code>off</code>.
     * @return number of received bytes, or -1 if the socket has
     * ended.
     */
    public int receive(int fd, byte[] b, int off, int len)
    throws IOException;
        
    /**
     * Just like {@link #receive(int,byte[],int,int)}, but data is
     * returned throw <code>p</code>.
     */
    public void receiveFrom(int fd, GenericDatagramPacket p)
    throws IOException;
    
    /**
     * Just like {@link #receive(int,byte[],int,int)}, but data is
     * returned throw <code>p</code>.
     */
    public void receiveFrom(int fd, DatagramPacket p)
    throws IOException;
    
    
    public static final int READ_HALF = 1;
    public static final int WRITE_HALF = 2;
    public static final int BOTH_HALVES = READ_HALF | WRITE_HALF;
    
    /**
     * Closes the read-half, write-half or both-halves of a socket.
     */
    public void shutdown(int fd, int what)
    throws IOException;
    
    
    //======================================================================
    // SOCKET OPTIONS (lexicographically sorted)

    /**
     * Enable/disable SO_DEBUG for socket <tt>fd</tt>
     */
    public void setDebug(int fd, boolean on)
    throws SocketException;
    
    /**
     * Test if SO_DEBUG is enabled for socket <tt>fd</tt>
     */
    public boolean getDebug(int fd)
    throws SocketException;
    
    /**
     * Enable/disable SO_BROADCAST for socket <tt>fd</tt>.
     */
    public void setBroadcast(int fd, boolean on)
    throws SocketException;
    
    /**
     * Tests if SO_BROADCAST is enabled for socket <tt>fd</tt>.
     */
    public boolean getBroadcast(int fd)
    throws SocketException;
        
    /**
     * Enable/disable SO_KEEPALIVE for socket <tt>fd</tt>
     */
    public void setKeepAlive(int fd, boolean on)
    throws SocketException;
    
    /**
     * Test if SO_KEEPALIVE is enabled for socket <tt>fd</tt>
     */
    public boolean getKeepAlive(int fd)
    throws SocketException;
        
    /**
     * Enable/disable SO_OOBINLINE for socket <tt>fd</tt>.
     */
    public void setOOBInline(int fd, boolean on)
    throws SocketException;
    
    /**
     * Test if SO_OOBINLINE is enabled for socket <tt>fd</tt>.
     */
    public boolean getOOBInline(int fd)
    throws SocketException;
    
    /**
     * Change socket <code>fd</code> timeout to <code>timeout</code>.
     * <code>timeout</code> is in milliseconds.
     * Follows Unix convention: if timeout is less than or equal to 0,
     * disables timeout.
     * @throws SocketException if the timeout cannot be changed.
     */
    public void setReadTimeout(int fd, long timeout)
    throws SocketException;
    
    /**    
     * Get socket <code>fd</code> timeout in milliseconds
     * for socket <tt>fd</tt>.
     */
    public long getReadTimeout(int fd)
    throws SocketException;
    
    /**
     * Sets a default proposed value for the SO_RCVBUF option
     * of socket <tt>fd</tt>.
     */
    public void setReceiveBufferSize(int fd, int size)
    throws SocketException;
    
    /**
     * Get value of the SO_RCVBUF option for socket <tt>fd</tt>.
     */
    public int getReceiveBufferSize(int fd)
    throws SocketException;
    
    /**
     * Indicates whether address reuse should be enabled.
     * Enable/disable the SO_REUSEADDR socket option of <tt>fd</tt>
     */
    public void setReuseAddress(int fd, boolean reuse)
    throws SocketException;
    
    /**
     * Tests if SO_REUSEADDR is enabled for socket <tt>fd</tt>.
     */
    public boolean getReuseAddress(int fd)
    throws SocketException;
        
    /**
     * Sets a default proposed value for the SO_SNDBUF option
     * of socket <tt>fd</tt>.
     */
    public void setSendBufferSize(int fd, int size)
    throws SocketException;
    
    /**
     * Get value of the SO_SNDBUF option for socket <tt>fd</tt>.
     */
    public int getSendBufferSize(int fd)
    throws SocketException;
    
    /**
     * Enable/disable SO_LINGER with the specified linger time in seconds.
     */
    public void setSoLinger(int fd, boolean on, int linger)
    throws SocketException;
    
    /**
     * Returns setting for SO_LINGER of socket <tt>fd</tt>.
     * -1 returns implies that the option is disabled.
     */
    public int getSoLinger(int fd)
    throws SocketException;
    
    /**
     * Enable/disable TCP_NODELAY.
     */
    public void setTcpNoDelay(int fd, boolean on)
    throws SocketException;
    
    /**
     * Tests if TCP_NODELAY is enabled for socket <tt>fd</tt>.
     */
    public boolean getTcpNoDelay(int fd)
    throws SocketException;
}
