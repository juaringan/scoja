/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2008  Bankinter, S.A.
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

package org.scoja.server.target;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.scoja.cc.util.NumerableRange;
import org.scoja.rpc.rt.RPCUtil;
import org.scoja.rpc.util.nfs.NFSDevice;
import org.scoja.io.posix.FileAttributes;
import org.scoja.server.cache.SpaceController;

public class GenerateHierarchy {

    public static final String MOUNT_POINT = "/var/log/scoja";

    public static void main(final String[] args)
    throws Exception {
        if (args.length != 7) {
            System.err.println(
                "java " + GenerateHierarchy.class.getName()
                + " <host>:<dir> <from> <user>"
                + " <levels> <span> <filesize> <writesize>");
            System.exit(1);
        }
        int i = 0;
        final NFSDevice device = NFSDevice.parsed(args[i++]);
        final String from = args[i++];
        final String user = args[i++];
        final int levels = Integer.parseInt(args[i++]);
        final int span = Integer.parseInt(args[i++]);
        final int filesize = Integer.parseInt(args[i++]);
        final int writesize = Integer.parseInt(args[i++]);
        final int writetimes = (filesize + writesize - 1) / writesize;
        
        final NFSFileSystem fs = new NFSFileSystem(
            device, MOUNT_POINT,
            new NumerableRange.Int(512,1024), 
            RPCUtil.sysAuth(0x48aaff18, "localhost", 0, 0, new int[] {0}));
        
        fs.setSpaceControl(
            new SpaceController.HardLimited(256*1024*1024, 4*1024*1024));
        
        final Thread fsworker = new Thread(new Runnable() {
                public void run() {
                    System.err.println("Filesystem worker thread started");
                    try {
                        fs.run();
                    } catch (Throwable e) {
                        System.err.println("Filesystem worker thread error:");
                        e.printStackTrace(System.err);
                    } finally {
                        System.err.println("Filesystem worker thread ending");
                    }
                }
            });
        fsworker.start();
     
        final FileAttributes fileAttr
            = new FileAttributes(user, user, "rw-------");
        final FileAttributes dirAttr
            = new FileAttributes(user, user, "rwxr-xr-x");
        final FileBuilding building
            = new FileBuilding(fileAttr, dirAttr, true);
        final Flushing flushing = Flushing.getDefault();
            
        final List<Filler> fillers = openFiles(
            fs, MOUNT_POINT + "/" + from, levels, span, building, flushing);
        System.out.println(fillers.size() + " files opened");
        for (int j = 0; j < writetimes; j++) {
            for (final Filler f: fillers) f.write(writesize);
        }
        System.out.println(
            "Wrote " + writetimes + " blocks of size " + writesize
            + ": " + writetimes * writesize);
        for (final Filler f: fillers) {
            System.out.println(f.file);
            f.close();
        }
        
        while (fs.getCurrentlyOpen() > 0) {
            System.out.println(fs);
            Thread.sleep(1*1000);
        }
        fs.close();
    }
    
    protected static List<Filler> openFiles(
        final FileSystem fs, final String base,
        final int levels, final int span,
        final FileBuilding building, final Flushing flushing) {
        final List<Filler> fillers = new ArrayList<Filler>();
        openFiles(fillers, fs, base, levels, span, building, flushing);
        return fillers;
    }
    
    protected static void openFiles(
        final List<Filler> fillers, final FileSystem fs, final String base,
        final int levels, final int span,
        final FileBuilding building, final Flushing flushing) {
        if (levels == 0) {
            for (int i = 0; i < span; i++) {
                final String filename = base + "/f" + i;
                final Filler filler = new Filler(
                    filename, fs.openText(filename, building, flushing));
                fillers.add(filler);
            }
        } else {
            for (int i = 0; i < span; i++) {
                openFiles(fillers, fs, base+"/d"+i, levels-1, span,
                          building, flushing);
            }
        }
    }
        
    
    public static class Filler {
        protected final String name;
        protected final TextFile file;
        protected final String content;
        protected int offmod;
        
        public Filler(final String name, final TextFile file) {
            this.name = name;
            this.file = file;
            this.content = name + "\n";
            this.offmod = 0;
        }
        
        public void write(final int n) {
            final char[] data = new char[n];
            for (int i = 0; i < data.length; i++) {
                data[i] = content.charAt((offmod+i) % content.length());
            }
            offmod = (offmod + data.length) % content.length();
            try {
                file.getOut().write(data);
            } catch (IOException e) {
                System.err.println("Error while writing to " + name);
                e.printStackTrace(System.err);
            }
        }
        
        public void close() {
            file.close();
        }
    }
}
