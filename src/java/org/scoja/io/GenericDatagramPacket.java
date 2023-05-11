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

package org.scoja.io;

/**
 * This should be considered a superclass of
 * {@link java.net.DatagramPacket}, because it has the same
 * fundamental elements (an address and packet data) but assumes
 * nothing about the address.
 * It is funny that all the java.net, with all its fucking generality,
 * has fixed DatagramPacket for use with inet socket only.
 * <p>
 * This class has almost the same methods as
 * {@link java.net.DatagramPacket}.
 * But this class makes an explicit difference between
 * the space usable for writing in the data array ({@link #getLimit()})
 * and the space with real data ({@link #getLength()}).
 * <p>
 * When a GenericDatagramPacket is build, both sizes are supposed to
 * be equal.
 * This is sensible, because usually a packet is used only for reading
 * or for writting.
 * When a packet is used for reading, at most {@link #getLimit()}
 * bytes are read; length is modified with the total number of bytes
 * read.
 */
public class GenericDatagramPacket {

    protected byte[] data;
    protected int offset;
    protected int limit;
    protected int length;
    protected SocketAddress address;

    public GenericDatagramPacket(final int limit) {
        this(new byte[limit]);
    }
        
    public GenericDatagramPacket(final byte[] data) {
        this(data, data.length);
    }
    
    public GenericDatagramPacket(final byte[] data, final int limit) {
        this(data, 0, limit);
    }
    
    public GenericDatagramPacket(final byte[] data,
                                 final int offset, final int limit) {
        this(data, offset, limit, null);
    }

    public GenericDatagramPacket(final byte[] data,
                                 final SocketAddress address) {
        this(data, data.length, address);
    }
    
    public GenericDatagramPacket(final byte[] data, final int limit,
                                 final SocketAddress address) {
        this(data, 0, limit, address);
    }
    
    public GenericDatagramPacket(final byte[] data,
                                 final int offset, final int limit,
                                 final SocketAddress address) {
        setData(data, offset, limit);
        setSocketAddress(address);
    }
    
    public byte[] getData() {
        return data;
    }

    public String getDataAsString() {    
        try {
            return new String(data, offset, length, "ISO-8859-1");
        } catch (java.io.UnsupportedEncodingException e) {
            return new String(data, offset, length);
        }
    }
    
    public void setData(final byte[] data) {
        setData(data, 0, data.length);
    }
     
    public void setData(final byte[] data, final int offset, final int limit){
        this.data = data;
        setDataRegion(offset, limit);
    }
    
    public int getOffset() {
        return offset;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public int getLength() {
        return length;
    }

    public void setDataRegion(final int offset, final int limit) {
        if (offset < 0 || limit < 0 
            || (limit > 0 && data.length < offset+limit)) {
            throw new IllegalArgumentException
                ("Trying to access " + limit + " bytes starting at " + offset
                 + " from an array of length " + data.length);
        }
        this.offset = offset;
        this.length = this.limit = limit;
    }
    
    public void setLength(final int length) {
        this.length = Math.min(this.limit, length);
    }
    
    public SocketAddress getSocketAddress() {
        return address;
    }
    
    public void setSocketAddress(final SocketAddress address) {
        this.address = address;
    }
    
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("GenericDatagramPacket[");
        sb.append("data: ").append(getDataAsString());
        sb.append(", data length: ").append(data.length);
        sb.append(", offset: ").append(offset);
        sb.append(", length: ").append(length);
        sb.append(", limit: ").append(limit);
        sb.append(']');
        return sb.toString();
    }
}
