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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.scoja.cc.io.Path;
import org.scoja.cc.lang.Void;
import org.scoja.cc.util.Link;

import org.scoja.rpc.proto.rpc2.Auth;
import org.scoja.rpc.proto.nfs3.CommitResult;
import org.scoja.rpc.proto.nfs3.CreateHow;
import org.scoja.rpc.proto.nfs3.CreationMode;
import org.scoja.rpc.proto.nfs3.CreateOK;
import org.scoja.rpc.proto.nfs3.CreateResult;
import org.scoja.rpc.proto.nfs3.FileAttr;
import org.scoja.rpc.proto.nfs3.FileType;
import org.scoja.rpc.proto.nfs3.FSInfoOK;
import org.scoja.rpc.proto.nfs3.FSInfoResult;
import org.scoja.rpc.proto.nfs3.GetAttrResult;
import org.scoja.rpc.proto.nfs3.LookUpOK;
import org.scoja.rpc.proto.nfs3.LookUpResult;
import org.scoja.rpc.proto.nfs3.MkDirOK;
import org.scoja.rpc.proto.nfs3.MkDirResult;
import org.scoja.rpc.proto.nfs3.Mount3;
import org.scoja.rpc.proto.nfs3.MountResult;
import org.scoja.rpc.proto.nfs3.MountStat;
import org.scoja.rpc.proto.nfs3.NFS3;
import org.scoja.rpc.proto.nfs3.NFSStat;
import org.scoja.rpc.proto.nfs3.SetAttr;
import org.scoja.rpc.proto.nfs3.SetTime;
import org.scoja.rpc.proto.nfs3.StableHow;
import org.scoja.rpc.proto.nfs3.TimeHow;
import org.scoja.rpc.proto.nfs3.WriteResult;
import org.scoja.rpc.rt.PortFinder;
import org.scoja.rpc.rt.CallHandler;
import org.scoja.rpc.rt.TCPRPCCaller;
import org.scoja.rpc.rt.RPCCallBack;
import org.scoja.rpc.rt.RPCException;
import org.scoja.rpc.util.nfs.NFSDevice;

import org.scoja.io.posix.FileAttributes;
import org.scoja.server.cache.SpaceController;
import org.scoja.server.cache.BufferResizer;

public class NFSFileSystem 
    extends BinaryFileSystem {
    
    public static final int PRIORITY_DATA = 0;
    public static final int PRIORITY_META = 1;
    public static final int PRIORITY_FS = 2;
    
    protected final NFSDevice device;
    protected final Path mountPoint;
    protected final Set<Integer> clientPorts;
    protected final Auth auth;

    protected SpaceController spacer;
    protected BufferResizer resizer;
    protected int onErrorDelay;
    protected float pendingProcessingRatio;
    
    protected final Object lock;
    protected boolean closed;
    protected Thread worker;
    protected int mountPort;
    protected TCPRPCCaller mountCaller;
    protected Mount3.Client mount;
    protected byte[] rootfh;
    protected int nfsPort;
    protected TCPRPCCaller nfsCaller;
    protected NFS3.AsynchronousClient nfs;
    protected int preferredWrite;

    protected final Set<NFSOutputStream> opened;    
    protected final Map<NFSOutputStream,Link.Valued<NFSOutputStream>> waiting;
    protected final Link.Valued<NFSOutputStream> waitingHead;
    protected final Map<NFSOutputStream,Link.Valued<NFSOutputStream>>erroneous;
    protected final Link.Valued<NFSOutputStream> erroneousHead;
    
    public NFSFileSystem(final NFSDevice device, final String mountPoint,
                         final Set<Integer> clientPorts, final Auth auth)
    throws IOException, RPCException {
        this.device = device;
        this.mountPoint = Path.parse(mountPoint);
        if (!this.mountPoint.isAbsolute()) {
            throw new IllegalArgumentException(
                "Mount point must be an absolute path"
                + "; `" + mountPoint + "' is relative");
        }
        this.clientPorts = clientPorts;
        this.auth = auth;
        
        this.spacer = new SpaceController.Unlimited();
        this.resizer = new BufferResizer.Multiplicative();
        this.resizer.setSpaceControl(spacer);
        this.onErrorDelay = 10*1000;
        this.pendingProcessingRatio = 10;

        this.lock = new Object();
        this.closed = false;
        this.mountPort = 0;
        this.mountCaller = null;
        this.mount = null;
        this.rootfh = null;
        this.nfsPort = 0;
        this.nfsCaller = null;
        this.nfs = null;
        this.preferredWrite = 4*1024;

        this.opened = new HashSet<NFSOutputStream>();
        this.waiting
            = new HashMap<NFSOutputStream,Link.Valued<NFSOutputStream>>();
        this.waitingHead = new Link.Valued<NFSOutputStream>();
        this.waitingHead.selfLink();
        this.erroneous
            = new HashMap<NFSOutputStream,Link.Valued<NFSOutputStream>>();
        this.erroneousHead = new Link.Valued<NFSOutputStream>();
        this.erroneousHead.selfLink();
    }
    
    public String getMountPoint() {
        return mountPoint.toString();
    }
    
    public void setSpaceControl(final SpaceController spacer) {
        this.spacer = spacer;
        this.resizer.setSpaceControl(spacer);
    }
    
    public void setBufferResizer(final BufferResizer resizer) {
        this.resizer = resizer;
        this.resizer.setSpaceControl(spacer);
    }
    
    public void setOnErrorDelay(final int delay) {
        this.onErrorDelay = delay;
    }
    
    public void setPendingProcessingRatio(final float ratio) {
        this.pendingProcessingRatio = ratio;
    }

    public BinaryFile openBinary(final String filename,
                                 final FileBuilding building) {
        System.err.println("Request to open `" + filename + "'");
        final NFSOutputStream file
            =  new NFSOutputStream(this, filename, building);
        synchronized (opened) { opened.add(file); }
        return file;
    }
    
    public int getCurrentlyOpen() {
        synchronized (opened) { return opened.size(); }
    }
    
    protected void close(final NFSOutputStream file) {
        System.err.println("Closing " + file.filename);
        synchronized (opened) { opened.remove(file); }
    }
    
    
    //----------------------------------------------------------------------
    public void close() {
        synchronized (lock) {
            if (closed) return;
            closed = true;
            if (nfsCaller != null) nfsCaller.close();
            if (worker == null) effectiveClose();
            else lock.notifyAll();
        }
    }
    
    public void effectiveClose() {
        if (nfsCaller != null) nfsCaller.close();
        nfsCaller = null;
        nfs = null;
        if (mountCaller != null) mountCaller.close();
        mountCaller = null;
        mount = null;
        rootfh = null;
    }
    
    public void start() {
        new Thread(new Runnable() {
                public void run() {
                    System.err.println(
                        "Starting thread for " + NFSFileSystem.this);
                    try {
                        NFSFileSystem.this.run();
                    } catch (InterruptedException e) {
                        e.printStackTrace(System.err);
                    }
                    System.err.println(
                        "Ending thread for " + NFSFileSystem.this);
                }
            }).start();
    }
    
    public void run()
    throws InterruptedException {
        synchronized (lock) {
            for (;;) {
                if (closed) return;
                if (worker == null) break;
                lock.wait();
            }
            worker = Thread.currentThread();
        }
        try {
            for (;;) {
                try {
                    if (nfsCaller == null) setup();
                    synchronized (lock) { if (closed) break; }
                    nfsCaller.run();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    synchronized (lock) { effectiveClose(); }
                    Thread.sleep(onErrorDelay/10);
                }
            }
        } finally {
            synchronized (lock) {
                worker = null;
                if (closed) effectiveClose();
                else lock.notify();
            }
        }
    }
    
        
    //----------------------------------------------------------------------
    private void setup()
    throws IOException, RPCException {
        connect();
        mount();
        fsInfo();
        System.err.println(device + " successfully mount on " + mountPoint);
        doPending();
    }
    
    private void connect()
    throws IOException, RPCException {
        boolean error = false;
        final String server = device.getServer();
        final PortFinder portFinder = PortFinder.byTCP(server);
        try {
            mountPort = portFinder.getPort(
                Mount3.PROGRAM, Mount3.VERSION, PortFinder.TCP);
            nfsPort = portFinder.getPort(
                NFS3.PROGRAM, NFS3.VERSION, PortFinder.TCP);
            mountCaller = new TCPRPCCaller(server,mountPort,clientPorts,auth);
            nfsCaller = new TCPRPCCaller(server,nfsPort,clientPorts,auth);
            mount = new Mount3.StdClient(mountCaller);
            nfs = new NFS3.StdAsynchronousClient(nfsCaller);
        } catch (IOException e) {
            error = true;
            throw e;
        } catch (RPCException e) {
            error = true;
            throw e;
        } finally {
            portFinder.close();
        }
    }

    private void mount()
    throws IOException, RPCException {    
        final MountResult maymount = mount.mount(device.getDirectory());
        if (maymount.status != MountStat.OK) {
            throw new RPCException(
                "Cannot mount `" + device.getDirectory()
                + "': " + maymount.status);
        }
        rootfh = maymount.getMountInfo().fhandle;
    }
    
    private void fsInfo() {
        nfs.fsInfo(rootfh, PRIORITY_FS, new RPCCallBack<FSInfoResult>() {
            public void ok(CallHandler ch, FSInfoResult result) {
                fsInfoOK(result); }
            public void fail(CallHandler ch, Exception e) {
                fsInfoFail(e); }
        });
    }
    
    private void fsInfoOK(final FSInfoResult result) {
        replyBegin();
        if (result.status == NFSStat.OK) {
            final FSInfoOK fsinfo = result.getOk();
            System.err.println("Preferred write size: " + fsinfo.wtpref);
            synchronized (lock) {
                preferredWrite = fsinfo.wtpref;
            }
        } else {
            System.err.println(
                "Getting filesystem info failed: " + result.status);
        }
        replyEnd();
    }
    
    private void fsInfoFail(final Exception e) {
        replyBegin();
        System.err.println(
            "Getting filesystem info failed: " + e.getMessage());
        e.printStackTrace(System.err);
        replyEnd();
    }

    
    //----------------------------------------------------------------------
    protected int preferredWrite() {
        synchronized (lock) {
            return preferredWrite;
        }
    }

    protected BufferResizer getResizer() {
        return resizer;
    }
    
    protected SpaceController getSpaceControl() {
        return spacer;
    }
    
    protected void asSoonAsPossible(final NFSOutputStream action) {
        //System.err.println("asSoonAsPossible: " + action);
        synchronized (lock) {
            if (!waiting.containsKey(action)
                && !erroneous.containsKey(action)) {
                final Link.Valued<NFSOutputStream> node
                    = new Link.Valued<NFSOutputStream>(action);
                waiting.put(action, node);
                node.linkBefore(waitingHead);
            }
        }
    }
    
    protected void whenReconsidered(final NFSOutputStream action) {
        //System.err.println("whenReconsidered: " + action);
        synchronized (lock) {
            if (!erroneous.containsKey(action)) {
                Link.Valued<NFSOutputStream> node = waiting.remove(action);
                if (node == null) {
                    node = new Link.Valued<NFSOutputStream>(action);
                } else {
                    node.unlink();
                }
                erroneous.put(action, node);
                node.linkBefore(erroneousHead);
                action.setReconsiderAt(
                    System.currentTimeMillis() + onErrorDelay);
            }
        }
    }
    
    protected boolean shouldWait() {
        synchronized (lock) {
            return rootfh == null
                || (nfsCaller.pendingProcessingRatio() 
                    > this.pendingProcessingRatio);
        }
    }

    protected void doPending() {    
        Link.Valued<NFSOutputStream> ptr, next;
        final long now = System.currentTimeMillis();
        synchronized (lock) {
            ptr = erroneousHead.next();
        }
        while (ptr != erroneousHead && !shouldWait()) {
            final NFSOutputStream file = ptr.getValue();
            if (file.getReconsiderAt() > now) break;
            synchronized (lock) {
                next = ptr.next();
                ptr.unlink();
                erroneous.remove(file);
            }
            file.doPending();
            ptr = next;
        }
            
        synchronized (lock) {
            ptr = waitingHead.next();
        }
        while (ptr != waitingHead && !shouldWait()) {
            final NFSOutputStream file = ptr.getValue();
            synchronized (lock) {
                next = ptr.next();
                ptr.unlink();
                waiting.remove(file);
            }
            file.doPending();
            ptr = next;
        }
    }
    
    protected void replyBegin() {
        doPending();
    }
    
    protected void replyEnd() {
        doPending();
    }
    
    protected abstract class SimpleCB<A,B> implements RPCCallBack<A> {
        protected final NFSCallBack<B> callback;
        
        public SimpleCB(final NFSCallBack<B> callback) {
            this.callback = callback;
        }
        
        public void ok(final CallHandler ch, final A result) {
            replyBegin(); try { ok(result); } finally { replyEnd(); }
        }
        
        public void fail(final CallHandler ch, final Exception error) {
            replyBegin(); try { callback.fail(error); } finally { replyEnd(); }
        }
        
        public abstract void ok(A result);
    }
    

    //----------------------------------------------------------------------
    protected void write(final NFSFile file, final long fileOffset,
                         final byte[] buffer, final int off, final int len,
                         final NFSCallBack<Integer> callback) {
        //System.err.println(
        //    "Writing to " + file + " at " + off + "+" + len);
        final byte[] tmp = new byte[len];
        System.arraycopy(buffer,off, tmp,0,len);
        nfs.write(file.getHandle(), fileOffset, len, StableHow.UNSTABLE, tmp,
                  PRIORITY_DATA, new SimpleCB<WriteResult,Integer>(callback) {
            public void ok(final WriteResult result) {
                writeOK(result, callback); }});
    }
    
    private void writeOK(final WriteResult result,
                         final NFSCallBack<Integer> callback) {
        if (result.status == NFSStat.OK) {
            callback.ok(result.getOk().count);
        } else {
            callback.fail(new Exception(result.status.toString()));
        }
    }

        
    //----------------------------------------------------------------------
    protected void sync(final NFSFile file, final NFSCallBack<Void> callback) {
        nfs.commit(file.getHandle(), 0, 0, PRIORITY_DATA,
                   new SimpleCB<CommitResult,Void>(callback) {
            public void ok(final CommitResult result) {
                syncOK(result, callback); }});
    }
    
    private void syncOK(final CommitResult result, 
                        final NFSCallBack<Void> callback) {
        if (result.status == NFSStat.OK) {
            callback.ok(Void.ME);
        } else {
            callback.fail(new Exception(result.status.toString()));
        }
    }
    
        
    //----------------------------------------------------------------------
    protected void open(final String filename, final FileBuilding fb,
                        final NFSCallBack<NFSFile> callback) {
        final Path filePath = Path.parse(filename);
        //System.err.println("Opening " + filePath);
        final Path relFilePath = filePath.relativeFrom(mountPoint);
        if (relFilePath == null) {
            callback.fail(
                new IllegalArgumentException(
                    "File " + filename + "[" + filePath
                    + "] is outside of mount-point `"
                    + mountPoint + "' of this filesystem"));
            return;
        }
        //System.err.println("Opening relative " + relFilePath);
        final Path dirpath = relFilePath.getParent();
        final String entryname = relFilePath.getName();
        locateDirPath(dirpath, fb, new NFSCallBack<byte[]>() {
            public void ok(byte[] dir) { open(dir, entryname, fb, callback); }
            public void fail(Exception e) { callback.fail(e); }
        });
    }

    private void locateDirPath(final Path dirpath, final FileBuilding fb,
                               final NFSCallBack<byte[]> callback) {
        if (dirpath == null || dirpath.isRoot()) {
            callback.ok(rootfh); 
            return;
        }
        final String dirname = dirpath.getName();
        locateDirPath(dirpath.getParent(), fb, new NFSCallBack<byte[]>(){
            public void ok(byte[] dirfh) {
                locateDir(dirfh, dirname, fb, callback); }
            public void fail(Exception e) { callback.fail(e); }
        });
    }
    
    private void locateDir(final byte[] parentfh, final String dirname,
                           final FileBuilding fb,
                           final NFSCallBack<byte[]> callback) {
        //System.err.println("Locating " + dirname);
        nfs.lookUp(parentfh, dirname, PRIORITY_META,
                   new SimpleCB<LookUpResult,byte[]>(callback) {
            public void ok(LookUpResult result) {
                locateDirOK(result, parentfh, dirname, fb, callback); } });
    }
    
    private void locateDirOK(final LookUpResult maylook,
                             final byte[] parentfh, final String dirname,
                             final FileBuilding fb,
                             final NFSCallBack<byte[]> callback) {
        if (maylook.status == NFSStat.OK) {
            final LookUpOK look = maylook.getOk();
            if (look.fileAttributes != null
                && look.fileAttributes.type != FileType.DIR) {
                callback.fail(
                    new Exception(
                        "Entry `" + dirname + "' should be a directory"
                        + " but it is a " + look.fileAttributes.type));
            } else {
                callback.ok(look.fh);
            }
            return;
        }
        if (maylook.status != NFSStat.ERR_NOENT
            || fb == null || !fb.shouldMakeDirectories()) {
            callback.fail(new Exception(maylook.status.toString()));
            return;
        }
        final FileAttributes posixAttr = fb.getDirectoryAttributes();
        final SetAttr nfsAttr = new SetAttr();
        nfsAttr.mode = posixAttr.getMode();
        nfsAttr.uid = posixAttr.getUserID();
        nfsAttr.gid = posixAttr.getGroupID();
        nfsAttr.atime = nfsAttr.mtime
            = SetTime.empty(TimeHow.SET_TO_SERVER_TIME);
        System.err.println("Making directory with ids: "
                           + nfsAttr.uid + " " + nfsAttr.gid);
        nfs.mkDir(parentfh, dirname, nfsAttr, PRIORITY_META,
                   new SimpleCB<MkDirResult,byte[]>(callback) {
            public void ok(MkDirResult result) {
                createDirOK(result, parentfh, dirname, fb, callback); } });
    }
    
    private void createDirOK(final MkDirResult maymkdir,
                             final byte[] parentfh, final String dirname,
                             final FileBuilding fb,
                             final NFSCallBack<byte[]> callback) {
        if (maymkdir.status == NFSStat.OK) {
            final MkDirOK mkdir = maymkdir.getOk();
            if (mkdir.newDir != null) callback.ok(mkdir.newDir);
            else locateDir(parentfh, dirname, null, callback);
        } else if (maymkdir.status == NFSStat.ERR_EXIST) {
            locateDir(parentfh, dirname, null, callback);
        } else {
            callback.fail(new Exception(maymkdir.status.toString()));
        }
    }
    
    private void open(final byte[] dirfh, final String entryname, 
                      final FileBuilding fb,
                      final NFSCallBack<NFSFile> callback) {
        nfs.lookUp(dirfh, entryname, PRIORITY_META,
                   new SimpleCB<LookUpResult,NFSFile>(callback) {
            public void ok(LookUpResult result) {
                openOK(result, dirfh, entryname, fb, callback); } });
    }
    
    private void openOK(final LookUpResult maylook,
                        final byte[] dirfh, final String entryname, 
                        final FileBuilding fb,
                        final NFSCallBack<NFSFile> callback) {
        //System.err.println("Open " + entryname + ": " + maylook.status);
        if (maylook.status == NFSStat.OK) {
            final LookUpOK look = maylook.getOk();
            if (look.fileAttributes != null) {
                openFull(look.fh, look.fileAttributes, callback);
            } else {
                getAttr(look.fh, callback);
            }
            return;
        }
        if (maylook.status != NFSStat.ERR_NOENT || fb == null) {
            callback.fail(new Exception(maylook.status.toString()));
            return;
        }
        final FileAttributes posixAttr = fb.getFileAttributes();
        final SetAttr nfsAttr = new SetAttr();
        nfsAttr.mode = posixAttr.getMode();
        nfsAttr.uid = posixAttr.getUserID();
        nfsAttr.gid = posixAttr.getGroupID();
        nfsAttr.atime = nfsAttr.mtime
            = SetTime.empty(TimeHow.SET_TO_SERVER_TIME);
        System.err.println("Creating file with ids: "
                           + nfsAttr.uid + " " + nfsAttr.gid);
        nfs.create(dirfh, entryname, 
                   CreateHow.withAttributes(CreationMode.GUARDED,nfsAttr),
                   PRIORITY_META, new SimpleCB<CreateResult,NFSFile>(callback){
            public void ok(CreateResult result) {
                createOK(result, dirfh, entryname, fb, callback); } });
    }
    
    private void createOK(final CreateResult maycreate,
                          final byte[] dirfh, final String entryname,
                          final FileBuilding fb,
                          final NFSCallBack<NFSFile> callback) {
        //System.err.println("Create " + entryname + ": " + maycreate.status);
        if (maycreate.status == NFSStat.OK) {
            final CreateOK create = maycreate.getOk();
            if (create.fh != null) {
                if (create.fileAttributes != null) {
                    openFull(create.fh, create.fileAttributes, callback);
                } else {
                    getAttr(create.fh, callback);
                }
            } else {
                open(dirfh, entryname, null, callback);
            }
        } else {
            callback.fail(new Exception(maycreate.status.toString()));
        }
    }
    
    private void getAttr(final byte[] fh,
                         final NFSCallBack<NFSFile> callback) {
        nfs.getAttr(fh, PRIORITY_META, 
                    new SimpleCB<GetAttrResult,NFSFile>(callback) {
            public void ok(GetAttrResult result) {
                getAttrOK(result, fh, callback); } });
    }
    
    private void getAttrOK(final GetAttrResult mayattr, final byte[] fh,
                           final NFSCallBack<NFSFile> callback) {
        if (mayattr.status == NFSStat.OK) {
            openFull(fh, mayattr.getAttributes(), callback);
        } else {
            callback.fail(new Exception(mayattr.status.toString()));
        }
    }
    
    private void openFull(final byte[] fh, final FileAttr attributes,
                          final NFSCallBack<NFSFile> callback) {
        callback.ok(new NFSFile(fh, attributes.size));
    }
    
    
    //----------------------------------------------------------------------
    public String toString() {
        synchronized (lock) {
            return "NFSFileSystem["
                + "NFS caller: " + nfsCaller
                + "]";
        }
    }
}
