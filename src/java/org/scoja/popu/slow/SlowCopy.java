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

package org.scoja.popu.slow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.scoja.cc.lang.Rate;
import org.scoja.cc.lang.Unit;
import org.scoja.cc.io.ControlledInputStream;
import org.scoja.cc.io.RateControl;
import org.scoja.cc.io.StreamControl;
import org.scoja.cc.getopt.Option;
import org.scoja.cc.getopt.OptionGetter;
import org.scoja.cc.getopt.OptionException;
import org.scoja.cc.getopt.OptionUsage;
import org.scoja.cc.text.PrettyPrinter;

import org.scoja.io.posix.FileMode;
import org.scoja.io.posix.FileStat;
import org.scoja.io.posix.Posix;
import org.scoja.io.posix.PosixLike;

public class SlowCopy {

    public static void main(final String[] args) {
        final SlowCopy copier = new SlowCopy();
        copier.process(args);
        if (copier.shouldHelp()) copier.help();
    }
    
    protected final PrettyPrinter pp = new PrettyPrinter(System.err);
    
    protected PosixLike posix = null;
    
    protected boolean helpRequested = false;
    protected Throwable error = null;
    
    protected boolean verbose = false;
    protected boolean recursive = false;
    protected boolean preserveMode = false;
    protected boolean preserveOwnership = false;
    protected boolean preserveTimestamp = false;
    protected Rate inputRate = null, inputRateX = null;
    protected Rate outputRate = null, outputRateX = null;
    protected StreamControl inputControl = null;
    protected StreamControl outputControl = null;

    public boolean shouldHelp() {
        return helpRequested || error != null;
    }
    
    protected Option[] getOptions() {
        return new Option[] {
            new Option("h", "help")
            .withHelp("This help message"),
            new Option("I", "input-rate").withArgument("rate")
            .withHelp("First: set main input rate;"
                      + " second: set extra input rate"),
            new Option("O", "output-rate").withArgument("rate")
            .withHelp("Set output rate"),
            new Option("p")
            .withHelp("Same as --preserve"),
            new Option("preserve").optionalArgument("attrlist")
            .withHelp("Preserve attributes of the original files/directories."
                      + " Default: mode,ownership,timestamps"),
            new Option("R", "r", "recursive")
            .withHelp("Copy directories recursively"),
            new Option("v", "verbose")
            .withHelp("Explain what is being done"),
        };
    }
    
    public void help() {
        if (error != null) {
            pp.print("Error: ").indent().println(error.getMessage()).outdent();
            if (verbose) pp.print("TRACE:").indent().println().outdent();
        }
        if (error == null || error instanceof OptionException) {
            pp.print("Usage: java " + getClass().getName()
                     + " <option...> <source...> <dest>"
                     + "\nOptions:")
                .indent().println()
                .print(new OptionGetter(getOptions()))
                .outdent().println();
        }
        pp.flush();
    }
    
    public void process(final String[] args) {
        try {
            process0(args);
        } catch (Throwable e) {
            this.error = e;
        }
    }
    
    protected void process0(final String[] args)
    throws Exception {
        if (args.length == 0) { helpRequested = true; return; }
        final OptionGetter getopt
            = new OptionGetter(getOptions()).reset(args);
        for (;;) {
            final OptionUsage ou = getopt.next();
            if (ou == null) break;
            final Option opt = ou.getOption();
            if (opt.equals("help")) {
                helpRequested = true;
                break;
            } else if (opt.equals("input-rate")) {
                final Rate rate = parseRate("input-rate", ou.getValue());
                if (inputRate == null) inputRate = rate;
                else if (inputRateX == null) inputRateX = rate;
                else throw new OptionException(
                    "Option `" +ou.getUsedName()+ "' can only be used twice");
            } else if (opt.equals("ouput-rate")) {
                final Rate rate = parseRate("output-rate", ou.getValue());
                if (outputRate == null) outputRate = rate;
                else if (outputRateX == null) outputRateX = rate;
                else throw new OptionException(
                    "Option `" +ou.getUsedName()+ "' can only be used twice");
            } else if (opt.equals("recursive")) {
                recursive = true;
            } else if (opt.equals("verbose")) {
                verbose = true;
            } else if (opt.equals("p") || opt.equals("preserve")) {
                if (ou.getArity() == 0) {
                    preserveMode = preserveOwnership = preserveTimestamp
                        = true;
                } else {
                    final String[] attrs = ou.getValue().split(" *, *");
                    for (int i = 0; i < attrs.length; i++) {
                        if ("mode".equalsIgnoreCase(attrs[i])) {
                            preserveMode = true;
                        } else if ("ownership".equalsIgnoreCase(attrs[i])) {
                            preserveOwnership = true;
                        } else if ("timestamps".equalsIgnoreCase(attrs[i])) {
                            preserveTimestamp = true;
                        } else {
                            warning("Unknown attribute `" + attrs[i]
                                    + "' to option `" +ou.getUsedName()+ "'");
                        }
                    }
                }
            } else {
                throw new OptionException(
                    "Unknown option `" + ou.getUsedName() + "'");
            }
        }
        posix = Posix.getPosix();
        final List<String> files = getopt.getRemaining();
        if (files.isEmpty()) {
            throw new OptionException("No source given");
        }
        if (verbose && inputRate != null) estimateCopyTime(files);
        if (files.size() == 1) {
            copy(files.get(0), ".");
        } else {
            for (int i = 0, l = files.size()-1; i < l; i++) {
                copy(files.get(i), files.get(l));
            }
        }
    }
    
    protected Rate parseRate(final String option, final String strval)
    throws OptionException {
        try {
            return Rate.byteTransfer(strval);
        } catch (Throwable e) {
            throw new OptionException(
                "Option `" + option + "' was given an illegal value `"
                + strval + "': " + e.getMessage(), e);
        }
    }
    
    
    //======================================================================
    protected void estimateCopyTime(final List<String> files)
    throws IOException {
        final long size = copySize(files);
        final double time = inputRate.expectedTime(size);
        System.out.println(
            "Expected time to copy " + size
            + " bytes: " + Unit.humanPeriod(time));
    }
    
    protected long copySize(final List<String> files)
    throws IOException {
        long totalSize = 0;
        final int end = Math.max(1, files.size()-1);
        for (int i = 0; i < end; i++) {
            totalSize += copySize(new File(files.get(i)));
        }
        return totalSize;
    }
    
    protected long copySize(final File file)
    throws IOException {
        if (!file.exists()
            || !file.isFile() && !file.isDirectory()
            || file.isDirectory() && !recursive) return 0;
        if (file.isFile()) return file.length();
        long totalSize = 0;
        final File[] entries = file.listFiles();
        for (int i = 0; i < entries.length; i++) {
            totalSize += copySize(entries[i]);
        }
        return totalSize;
    }
    
    
    //======================================================================
    protected void copy(final String source, final String dest) {
        try {
            copyInit(source, dest);
        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }
    
    protected void copyInit(final String src, final String dst)
    throws IOException {
        info("Copying given " + src + " to " + dst);
        final File fsrc = new File(src);
        final File fdst = new File(dst);
        if (!fsrc.exists()) {
            warning("Entry `" + src + "' doesn't exists");
        } else if (fsrc.isFile()) {
            copyFileInit(fsrc, fdst);
        } else if (fsrc.isDirectory()) {
            if (recursive) copyDirInit(fsrc, fdst);
            else warning("Entry `" + src + "' is a directory");
        } else {
            warning("Entry `" + src + "' is neither a directory nor a file");
        }
    }
    
    protected void copyFileInit(final File src, final File dst)
    throws IOException {
        final File dst0
            = dst.isDirectory() ? new File(dst, src.getName()) : dst;
        copyFile(src, dst0);
    }
    
    protected void copyFile(final File src, final File dst)
    throws IOException {
        if (dst.exists() && dst.isDirectory()) {
            warning("Cannot overwrite directory `" + dst
                    + "' with non-directory `" + src + "'");
            return;
        }
        copyFile0(src, dst);
    }
    
    protected void copyFile0(final File src, final File dst)
    throws IOException {
        if (src.getCanonicalPath().equals(dst.getCanonicalPath())) {
            warning("`" + src + "' and `" + dst + "' are the same file");
            return;
        }
        info("Copying file " + src + " to " + dst);
        InputStream in = null;
        OutputStream out = null;
        try {
            in = openInput(src);
            out = openOutput(dst);
            final byte[] buffer = new byte[16*1024];
            for (;;) {
                final int read = in.read(buffer);
                if (read < 0) break;
                out.write(buffer, 0, read);
            }
            preserveAttributes(src, dst);
        } finally {
            if (in != null) try {in.close();} catch (Throwable e) {}
            if (out != null) try {out.close();} catch (Throwable e) {}
        }
    }
    
    protected InputStream openInput(final File src)
    throws IOException {
        InputStream in = new FileInputStream(src);
        if (inputRate != null) {
            if (inputControl == null) {
                inputControl = RateControl.along(inputRate, inputRateX);
            }
            in = new ControlledInputStream(inputControl, in);
        }
        return in;
    }
    
    protected OutputStream openOutput(final File dst)
    throws IOException {
        OutputStream out = new FileOutputStream(dst);
        if (outputRate != null) {
            if (outputControl == null) {
                outputControl = RateControl.along(outputRate, outputRateX);
            }
            //out = new ControlledOutputStream(outputControl, out);
        }
        return out;
    }
    
    protected void copyDirInit(final File src, final File dst)
    throws IOException {
        final File dst0
            = dst.isDirectory() ? new File(dst, src.getName()) : dst;
        copyDir(src, dst0);
    }
    
    protected void copyDir(final File src, final File dst)
    throws IOException {
        if (!dst.exists()) {
            dst.mkdir();
        } else if (!dst.isDirectory()) {
            warning("Cannot overwrite non-directory `" + dst
                    + "' with directory `" + src + "'");
            return;
        }
        copyDir0(src, dst);
    }
    
    protected void copyDir0(final File src, final File dst)
    throws IOException {
        if (dst.getCanonicalPath().startsWith(src.getCanonicalPath())) {
            warning("Cannot copy a directory, `" + src
                    + "', into itself, `" + dst + "'");
            return;
        }
        info("Copying directory " + src + " to " + dst);
        final File[] entries = src.listFiles();
        for (final File e: entries) {
            if (e.isDirectory()) {
                copyDir(e, new File(dst, e.getName()));
            } else if (e.isFile()) {
                copyFile(e, new File(dst, e.getName()));
            } else {
                warning(
                    "Cannot copy `" + e + "': neither a file nor a directory");
            }
        }
        preserveAttributes(src, dst);
    }
    
    protected void preserveAttributes(final File src, final File dst) {
        info("Coping attributes of " + src + " to " + dst);
        if (preserveTimestamp) {
            dst.setLastModified(src.lastModified());
        }
        if ((preserveMode || preserveOwnership)
            && posix != null && posix.hasFilesystem()) {
            final FileStat stat;
            try {
                stat = posix.getFileStat(src.toString());
            } catch (Throwable e) {
                warning("Cannot stat `" + src + "' to copy attributes");
                return;
            }
            final String dstpath = dst.toString();
            if (preserveMode) {
                try {
                    posix.setFileMode(
                        dstpath, FileMode.getPermissions(stat.getMode()));
                } catch (Throwable e) {
                    warning("Cannot set file permissions of `" + dst + "'"
                            + " to " + stat.getFileMode().justPermissions());
                }
            }
            if (preserveOwnership) {
                try {
                    posix.setFileOwner(
                        dstpath, stat.getUserID(), stat.getGroupID());
                } catch (Throwable e) {
                    warning("Cannot change user/group of `" + dst + "' to "
                            + stat.getUserID() + "/" + stat.getGroupID());
                }
            }
        }
    }
    
    
    //======================================================================
    protected void info(final String message) {
        if (verbose) {
            System.err.println(message);
        }
    }
        
    protected void warning(final String message) {
        System.err.println("Warning: " + message);
    }
}
