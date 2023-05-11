# -*- coding: latin-1 -*-
# Scoja: Syslog COllector in JAva
# Copyright (C) 2003  Mario Martínez
# Copyright (C) 2012  LogTrust
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

# Defines scoja native configuration language.
# Most definitions are trivial constructions of Java objects.

import types
import java.lang
import java.io
import org

#======================================================================
# FOR THE SAKE OF OTHER DEFINITIONS

def tmp(filename):
    file = java.io.File(filename)
    if not file.isAbsolute():
        file = java.io.File(scoja.basedir, filename)
    file = file.canonicalFile
    return file
scoja.asSiblingFile = tmp

def tmp(e):
    return scoja.flatOn([], e)
scoja.flat = tmp

def tmp(list, e):
    t = type(e)
    if t == types.TupleType or t == types.ListType:
        for a in e:
            scoja.flatOn(list, a)
    else:
        list.append(e)
    return list
scoja.flatOn = tmp

def tmp(struct, keys):
    values = []
    for k in keys:
        values.append(struct[k])
    return values
scoja.getAll = tmp

def tmp(x):
    if x == None: return -1
    else: return x
scoja.None2Minus1 = tmp

def tmp(x):
    if x == None: return 0
    else: return x
scoja.None2Zero = tmp

#======================================================================
# TAKE IT EASY

no = false = 0
yes = true = 1

MILLISECONDS = MILLISECOND = 1
SECONDS = SECOND = 1000 * MILLISECONDS
MINUTES = MINUTE = 60 * SECONDS
HOURS = HOUR = 60 * MINUTES
DAYS = DAY = 24 * HOURS

Kb = 1024
Mb = Kb * Kb
Gb = Mb * Kb
KB = Kb
MB = Mb
GB = Gb

def relative(filename):
    return scoja.asSiblingFile(filename)

def include(filename, glob = false, asBase = false):
    if glob:
        file = org.scoja.cc.fs.RelativeFile(
            scoja.basedir, java.io.File(filename))
        files = org.scoja.cc.fs.FileGatherer.glob(file).current();
    else:
        files = [scoja.asSiblingFile(filename)]
    for f in files:
        scoja.soul.addDependency(f)
        if asBase:
            currentBasedir = scoja.basedir
            scoja.basedir = f.parentFile
        scoja.executor.execfile(f)
        if asBase: scoja.basedir = currentBasedir
    #execfile(file.path, globals())
    
def seq(a,b):
    return range(a,b+1)

def template(str):
    #if str == None or isinstance(str, org.scoja.server.template.Template):
    if str == None or type(str) != types.StringType:
        return str
    return org.scoja.server.template.Template.parse(str)

UNKNOWN = org.scoja.server.core.Environment.UNKNOWN

UNCHECKED_STR = org.scoja.server.core.QStr.NO_EXPLICIT_QUALITY
SECURE_STR = org.scoja.server.core.QStr.IS_FILENAME_SECURE
WITHOUT_EOLN_STR = org.scoja.server.core.QStr.HASNT_EOLN
PERFECT_STR = org.scoja.server.core.QStr.PERFECT


#======================================================================
# ENCODINGS

class Encoding:
    def __init__(self, encoder):
        self.encoder = encoder
    def __mul__(self, other):
        return Encoding(org.scoja.cc.text.escaping.Composed(
            self.encoder, other.encoder))

URL_ENCODING = Encoding(org.scoja.cc.text.escaping.URLLike.noControlSequence())
C_ENCODING = Encoding(org.scoja.cc.text.escaping.CLike.noControlSequence())
SECURE_ENCODING = Encoding(
    org.scoja.cc.text.escaping.Collapsing.secureForWhatever())
NOEOLN_ENCODING = Encoding(org.scoja.cc.text.escaping.SummingUp.withoutLines())


#======================================================================
# CONFIGURATION

def configureFileCache(maxFiles = None, maxInactivity = None):
    fileCache = scoja.soul.globalContext.fileCache
    if maxFiles != None: fileCache.size = maxFiles
    if maxInactivity != None: fileCache.inactivity = maxInactivity

def configureStats(period = None):
    stats = scoja.soul.globalContext.measurer
    if period != None: stats.period = period

def monitor(ip = None, registry = None, server = None,
            passwordFile = None, accessFile = None):
    m = org.scoja.server.core.JMXMonitoringService()
    if ip != None: m.ip = ip;
    if registry != None: m.registryPort = registry
    if server != None: m.serverPort = server
    elif registry != None: m.serverPort = registry+1
    if passwordFile != None:
        m.passwordFile = scoja.asSiblingFile(passwordFile).path
    if accessFile != None:
        m.accessFile = scoja.asSiblingFile(accessFile).path
    scoja.soul.addCluster(m)


#======================================================================
# TRANSPORT

def tcp(ip, port = 514, blocking = false,
        timeout = None, keepAlive = None, reuseAddress = None,
        rcvbuffer = None, sndbuffer = None,
        noDelay = None, linger = None):
    if blocking:
        trans = org.scoja.trans.tcp.TCPTransport(ip, port)
    else:
        trans = org.scoja.trans.nbtcp.NBTCPTransport(ip, port)
    conf = trans.configuration()
    if timeout != None: conf.setTimeout(timeout)
    if keepAlive != None: conf.setKeepAlive(keepAlive)
    if reuseAddress != None: conf.setReuseAddress(reuseAddress)
    if rcvbuffer != None: conf.setReceiveBufferSize(rcvbuffer)
    if sndbuffer != None: conf.setSendBufferSize(sndbuffer)
    if noDelay != None: conf.setNoDelay(noDelay)
    if linger != None: conf.setLinger(linger)
    return trans

SSL_AUTH_IGNORE = org.scoja.trans.ssl.SSLClientAuthenticationMode.IGNORE
SSL_AUTH_REQUEST = org.scoja.trans.ssl.SSLClientAuthenticationMode.REQUEST
SSL_AUTH_REQUIRE = org.scoja.trans.ssl.SSLClientAuthenticationMode.REQUIRE
def ssl(base, clientAuth = None,
        truststore = None, keystore = None, password = None):
    trans = org.scoja.trans.ssl.SSLTransport.tls(base)
    conf = trans.configuration()
    if clientAuth != None: conf.setClientAuth(clientAuth);
    if truststore != None:
        ks = org.scoja.trans.ssl.SSLUtils.loadKeyStoreForUnprivileged(
            scoja.asSiblingFile(truststore), None)
        conf.setTrustManagers(org.scoja.trans.ssl.SSLUtils.getTrusts(ks))
    if keystore != None:
        ks = org.scoja.trans.ssl.SSLUtils.loadKeyStoreForUnprivileged(
            scoja.asSiblingFile(keystore), password)
        conf.setKeyManagers(org.scoja.trans.ssl.SSLUtils.getKeys(ks, password))
    return trans

def connect(base, ip, port = 514, keepAlive = None, userAgent = None,
            auth = None, flavor = None,
            user = None, password = None,
            target = "", workstation = None, domain = None):
    if auth != None: auth = auth.lower()
    if auth == None:
        cauth = org.scoja.cc.minihttp.HttpNoClientAuth(true)
    elif "basic" == auth.lower():
        basic = org.scoja.cc.minihttp.HttpBasicAuth(true, user, password)
    elif "ntlm" == auth.lower():
        ntlm = org.scoja.cc.ntlm.NTLMClient(user, password)
        ntlm.target = target
        ntlm.resolveHost()
        if workstation != None: ntlm.workstation = workstation
        if domain != None: ntml.domain = domain
        if flavor != None:
            ntlm.auth = org.scoja.cc.ntlm.NTLMAuthType.valueOf(flavor.upper())
        cauth = org.scoja.cc.minihttp.NTLMOnHttpClient(ntlm, true)
    else:
        raise "Unknown authentication model `" + auth + "'"
    trans = org.scoja.trans.connect.ConnectTransport(base, ip, port, cauth);
    conf = trans.configuration()
    if keepAlive != None: conf.setKeepAlive(keepAlive)
    if userAgent != None: conf.setUserAgent(userAgent)
    return trans


#======================================================================
# SOURCES

def internal():
    return scoja.soul.internal

def measurer():
    return scoja.soul.measurerLink

LIGHT_PROTOCOL = org.scoja.server.parser.LightProtocolFactory.getInstance()
RAW_PROTOCOL = org.scoja.server.parser.RawProtocolFactory.getInstance()
NETFLOW_PROTOCOL = org.scoja.server.netflow.NetflowProtocolFactory.getInstance()

def lightProtocol():
    return LIGHT_PROTOCOL

def rawProtocol(ansmatch):
    return org.scoja.server.parser.RawProtocolFactory(ansmatch)
    
def netflowProtocol():
    return NETFLOW_PROTOCOL

def fromUDP(ip = "0.0.0.0", port = 514, reuseAddress = false, threads = 2,
            rcvbuffer = None, maxPacketSize = 2000,
            protocol = None,
            measured = false):
    source = org.scoja.server.source.UDPSource(
        ip = ip, port = port, reuseAddress = reuseAddress,
        maxPacketSize = maxPacketSize, threads = threads)
    if rcvbuffer != None: source.receiveBuffer = rcvbuffer
    if protocol != None: source.protocol = protocol
    scoja.soul.addCluster(source)
    link = source.linkable
    if measured:
        scoja.soul.measurerHub.addMeasurable(source)
        counter = org.scoja.server.core.CounterLink(source)
        scoja.soul.measurerHub.addMeasurable(counter)
        link.addTarget(counter)
        link = counter
    return link

def fromTCP(ip = "0.0.0.0", port = 514,
            keepAlive = yes, reuseAddress = false,
            threads = 2, maxConnections = 100,
            rcvbuffer = None, sndbuffer = None,
            inbuffer = None, outbuffer = None,
            protocol = None,
            measured = false):
    if threads > maxConnections:
        threads = maxConnections
    source = org.scoja.server.source.SelectingTCPSource(
        ip = ip, port = port,
        keepAlive = keepAlive, reuseAddress = reuseAddress,
        threads = threads, maxConnections = maxConnections)
    if rcvbuffer != None: source.receiveBuffer = rcvbuffer
    if sndbuffer != None: source.sendBuffer = sndbuffer
    if inbuffer != None: source.inBuffer = inbuffer
    if outbuffer != None: source.outBuffer = outbuffer
    if protocol != None: source.protocol = protocol
    scoja.soul.addCluster(source)
    link = source.linkable
    if measured:
        scoja.soul.measurerHub.addMeasurable(source)
        counter = org.scoja.server.core.CounterLink(source)
        scoja.soul.measurerHub.addMeasurable(counter)
        link.addTarget(counter)
        link = counter
    return link

def fromTransport(trans, threads = 2, maxConnections = 100,
                  inbuffer = None, outbuffer = None,
                  protocol = None,
                  measured = false):
    if threads > maxConnections:
        threads = maxConnections
    source = org.scoja.server.source.SelectingSource(
        transport = trans, threads = threads, maxConnections = maxConnections)
    if inbuffer != None: source.inBuffer = inbuffer
    if outbuffer != None: source.outBuffer = outbuffer
    if protocol != None: source.protocol = protocol
    scoja.soul.addCluster(source)
    link = source.linkable
    if measured:
        scoja.soul.measurerHub.addMeasurable(source)
        counter = org.scoja.server.core.CounterLink(source)
        scoja.soul.measurerHub.addMeasurable(counter)
        link.addTarget(counter)
        link = counter
    return link

def fromUnixDatagram(file = "/dev/log", attr = None,
                     maxPacketSize = 2000, threads = 1):
    if attr == None:
        attr = fileattr("root", "root", "rw-rw-rw-")
    pfile = org.scoja.io.posix.PosixFile(file, attr)
    source = org.scoja.server.source.UnixDatagramSource(
        file = pfile, maxPacketSize = maxPacketSize,
        threads = threads)
    scoja.soul.addCluster(source)
    return source.linkable

def fromUnixStream(file = "/dev/log", attr = None,
                   keepAlive = yes, reuseAddress = false,
                   threads = 1, maxConnections =100):
    if attr == None:
        attr = fileattr("root", "root", "rw-rw-rw-")
    pfile = org.scoja.io.posix.PosixFile(file, attr)
    if threads > maxConnections:
        threads = maxConnections
    source = org.scoja.server.source.SelectingUnixStreamSource(
        file = pfile,
        keepAlive = keepAlive, reuseAddress = reuseAddress,
        threads = threads, maxConnections = maxConnections)
    scoja.soul.addCluster(source)
    return source.linkable

def fromPipe(file):
    source = org.scoja.server.source.PipeSource(file)
    scoja.soul.addCluster(source)
    return source.linkable


#======================================================================
# TARGETS

def mount(fs, on = None):
    if on == None: on = fs.mountPoint
    scoja.soul.globalContext.fileSystem.mount(on, fs)

def systemAuthentication(stamp=0, client=None, uid=None, gid=None, gids=None,
                         user=None, group=None):
    if user != None:
        info = org.scoja.io.posix.UserInfo.forName(user) 
        uid = info.getID()
        if gid == None: gid = info.getGroupID()
    if group != None: gid = org.scoja.io.posix.UserInfo.forName(group).gegID()
    if uid == None: uid = org.scoja.io.posix.UserInfo.getEffective().getID()
    if gid == None: gid = org.scoja.io.posix.GroupInfo.getEffective().getID()
    if gids == None: gids = [gid]
    return org.scoja.rpc.rt.RPCUtil.sysAuth(stamp, client, uid, gid, gids)

def quota(total = None, perFile = None):
    if total == None and perFile == None:
        return org.scoja.server.cache.SpaceController.Unlimited()
    return org.scoja.server.cache.SpaceController.HardLimited(
        scoja.None2Zero(total), scoja.None2Zero(perFile))

def nfs(device, on, auth = None, cache = None):
    if auth == None: auth = systemAuthentication()
    nfs = org.scoja.server.target.NFSFileSystem(
        org.scoja.rpc.util.nfs.NFSDevice.parsed(device),
        on,
        org.scoja.cc.util.NumerableRange.Int(512,1024),
        auth)
    if cache != None: nfs.spaceControl = cache
    nfs.start()
    return nfs

def nao(ext = None,
        models = None, filter = None, hashers = None,
        chunkSize = None, indexCounts = None):
    nao = org.scoja.server.target.NaoFileSystem()
    if ext != None: nao.extension = ext
    if models != None: nao.models = models
    if filter != None: nao.filter = filter
    if hashers != None: nao.hashers = hashers
    if chunkSize != None: nao.chunkSize = chunkSize
    if indexCounts != None: nao.indexCounts = indexCounts
    return nao

BUFFER = org.scoja.server.target.Flushing.BUFFER
FLUSH  = org.scoja.server.target.Flushing.FLUSH
SYNC   = org.scoja.server.target.Flushing.SYNC
def flushing(method = FLUSH, after = 1, buffer = 16*1024):
    return org.scoja.server.target.Flushing(method, after, buffer)

def fileattr(owner = None, group = None, perm = None):
    return org.scoja.io.posix.FileAttributes(owner, group, perm)

def building(file = None, fileOwner = None, fileGroup = None, filePerm = None,
             dir = None, dirOwner = None, dirGroup = None, dirPerm = None,
             mkdirs = yes):
    if file == None:
        file = fileattr(fileOwner, fileGroup, scoja.None2Minus1(filePerm))
    if dir == None:
        dir = fileattr(dirOwner, dirGroup, scoja.None2Minus1(dirPerm))
    return org.scoja.server.target.FileBuilding(file, dir, mkdirs)

def tmp(flush):
    if flush == None:
        return None
    else:
        if type(flush) == types.IntType:
            return flushing(method = flush)
        else:
            return flush
scoja.buildFlushing = tmp

final = org.scoja.server.core.Final()

def tmp(target, format, flush):
    realFlush = scoja.buildFlushing(flush)
    link = org.scoja.server.target.PrintTarget(
        target, flushing = realFlush)
    if format != None:
        format = template(format)
        link.format = format
    return link
scoja.stdgen = tmp

def stdout(format = None, flush = FLUSH):
    return scoja.stdgen(target = java.lang.System.out,
                        format = format, flush = flush)

def stderr(format = None, flush = FLUSH):
    return scoja.stdgen(target = java.lang.System.err,
                        format = format, flush = flush)

def toFile(name = None, format = None, build = None, flush = None,
           measured = false):
    if name == None:
        error("'name' should be defined for a 'file'")
    fileTemplate = template(name)
    if fileTemplate.isConstant():
        target = org.scoja.server.target.FixedFileTarget(
            scoja.soul.globalContext.fileSystem,
            scoja.soul.globalContext.fileCache,
            name = fileTemplate.toFilename())
    else:
        target = org.scoja.server.target.TemplateFileTarget(
            scoja.soul.globalContext.fileSystem,
            scoja.soul.globalContext.fileCache,
            name = fileTemplate)
    if format != None:
        format = template(format)
        target.format = format 
    if build != None:
        target.building = build
    realFlush = scoja.buildFlushing(flush)
    if realFlush != None:
        target.flushing = realFlush
    if measured:
        target.stats = scoja.soul.globalContext.fileMeasures
    return target

ZERO = org.scoja.client.Syslogger.ZERO
CR = org.scoja.client.Syslogger.CR
LF = org.scoja.client.Syslogger.LF
CRLF = org.scoja.client.Syslogger.CRLF

def sendTo(way,
           packetLimit = None,
           host = org.scoja.server.target.SysloggerTarget.HOST,
           tag = None, message = None,
           eom = None, retries = 3, inactivityOnError = 2*SECOND):
    target = org.scoja.server.target.SysloggerTarget(way)
    if packetLimit == None:
        packetLimit = org.scoja.client.StandardFormatter.NO_PACKET_LIMIT;
    target.setPacketLimit(packetLimit)
    if host != None:
        host = template(host)
        target.host = host.asEventWriter()
    else:
        target.host = None
    if tag != None:
        tag = template(tag)
        target.tag = tag.asEventWriter()
    if message != None:
        message = template(message)
        target.message = message.asEventWriter()
    if eom != None:
        target.terminator = eom
    target.retries = retries
    target.inactivityOnError = inactivityOnError
    return target

def toUDP(ip, port = 514):
    return org.scoja.client.UDPSyslogger(ip, port)

def toTCP(ip, port = 514, reusing = yes,
          keepAlive = None, bufferSize = None, linger = None, noDelay = None):
    if reusing:
        to = org.scoja.client.ReusingTCPSyslogger(ip, port)
    else:
        to = org.scoja.client.TCPSyslogger(ip, port)
    if keepAlive != None: to.keepAlive = keepAlive
    if bufferSize != None: to.bufferSize = bufferSize
    if linger != None: to.linger = linger
    if noDelay != None: to.noDelay = noDelay
    return to

def datafall(maxSize, grow = 0.5, minUsage = 0.5):
    b = org.scoja.util.MemoryByteFall(maxSize);
    b.growRatio = grow
    b.minUsageRatio = min(minUsage, grow)
    return b

def toTransport(trans, reusing = yes, buffer = None):
    if not trans.isBlocking() and buffer == None:
        buffer = datafall(16*Kb)
    if reusing or not trans.isBlocking():
        to = org.scoja.client.ReusingTransportSyslogger(trans, buffer)
    else:
        to = org.scoja.client.TransportSyslogger(trans)
    return to

def toUnixDatagram(file):
    return org.scoja.client.UnixDatagramSyslogger(file)

def toUnixStream(file = "/dev/log", reusing = yes):
    if reusing:
        return org.scoja.client.ReusingUnixStreamSyslogger(file)
    else:
        return org.scoja.client.UnixStreamSyslogger(file)


#======================================================================
# EXPRESSIONS

data = org.scoja.server.expr.Data
program = org.scoja.server.expr.Program
message = org.scoja.server.expr.Message
rhost = org.scoja.server.expr.ResolvedIP
crhost = org.scoja.server.expr.CanonicalIP
host = org.scoja.server.expr.Host
chost = org.scoja.server.expr.CanonicalHost

def peer(key = None):
    if key == None:
        return org.scoja.server.expr.PeerPrincipal()
    else:
        return org.scoja.server.expr.PeerDN(key)

def get(var):
    return org.scoja.server.expr.EnvironmentGet(var)

FILENAME = 0
TEXT = 1
def instance(tpt, usage = FILENAME, quality = UNCHECKED_STR):
    if isinstance(tpt, org.scoja.server.expr.StringExpression): return tpt
    tpt = template(tpt)
    if usage == FILENAME:
        return org.scoja.server.expr.FilenameTemplateFunction(tpt, quality)
    else:
        return org.scoja.server.expr.TextTemplateFunction(tpt, quality)

def escaped(subexpr, withenc = URL_ENCODING):
    return org.scoja.server.expr.EscapeFunction(subexpr, withenc.encoder)

def mapping(expr, map = None, remap = None, default = None):
    if map != None:
        mapFunc = org.scoja.server.expr.StringMappingFunction(
            expr, map.keys(), scoja.getAll(map, map.keys()))
    elif remap != None:
        keys = remap.keys()
        values = []
        qualities = []
        for k in keys:
            qval = remap[k]
            if type(qval) == types.StringType:
                values.append(qval)
                qualities.append(UNCHECKED_STR)
            elif (type(qval) == types.TupleType and len(qval) == 2 and
                  type(qval[0]) == types.StringType and
                  type(qval[1]) == types.IntType):
                values.append(qval[0])
                qualities.append(qval[1])
            else:
                raise ("Image of a regular expression mapping should be" +
                       " an String or a pair (String,Int)")
        mapFunc = org.scoja.server.expr.RegexMappingFunction(
            expr, keys, values, qualities)
    else:
        raise ("'mapping' needs one of 'map' or 'remap' keyword arguments" +
               " be defined")
    if default != None:
        mapFunc.default = default
    return mapFunc


def confine(expr, default = "CONFINED",
            max = 10, history = 1*DAY, periods = 24):
    if history <= 0:
        confExpr = org.scoja.server.expr.EternalConfineFunction(
            expr, default, max)
    elif periods <= 1:
        confExpr = org.scoja.server.expr.SimpleConfineFunction(
            expr, default, max, history)
    else:
        confExpr = org.scoja.server.expr.ConfineFunction(
            expr, default, max, history, periods)
    return confExpr

def switch(expr, cases, default = None):
    keys = cases.keys()
    sw = org.scoja.server.core.SwitchLink(expr, keys, scoja.getAll(cases,keys))
    if default != None:
        sw.default = default
    return sw

def hash(expr, n):
    return org.scoja.server.expr.HashingFunction(expr, n)

def customizing(selector, period, fun):
    return org.scoja.server.expr.CustomizerFunction(selector, period, fun)

def upcased(expr):
    return org.scoja.server.expr.UpCased(expr)

def downcased(expr):
    return org.scoja.server.expr.DownCased(expr)
    
#======================================================================
# ACTIONS

local = org.scoja.server.action.LocalAction

def set(var, expr):
    if type(expr) == types.StringType:
        return org.scoja.server.action.EnvironmentSetString(var, expr)
    else:
        return org.scoja.server.action.EnvironmentSetExpression(var, expr)

def useSendTimestamp(use):
    return org.scoja.server.action.UseSendTimestamp(use)

def unknown(str):
    return org.scoja.server.action.EnvironmentSetUnknown(str)


#======================================================================
# FILTERS

def link(obj = None):
    if obj == None:
        return org.scoja.server.core.Link()
    else:
        return obj.linkable

# Levels
EMERG	= org.scoja.common.PriorityUtils.EMERG
ALERT	= org.scoja.common.PriorityUtils.ALERT
CRIT	= org.scoja.common.PriorityUtils.CRIT
ERR	= org.scoja.common.PriorityUtils.ERR
WARNING	= org.scoja.common.PriorityUtils.WARNING
NOTICE	= org.scoja.common.PriorityUtils.NOTICE
INFO	= org.scoja.common.PriorityUtils.INFO
DEBUG	= org.scoja.common.PriorityUtils.DEBUG

# Facilities
KERN	  = org.scoja.common.PriorityUtils.KERN
USER	  = org.scoja.common.PriorityUtils.USER
MAIL	  = org.scoja.common.PriorityUtils.MAIL
DAEMON	  = org.scoja.common.PriorityUtils.DAEMON
AUTH	  = org.scoja.common.PriorityUtils.AUTH
SYSLOG	  = org.scoja.common.PriorityUtils.SYSLOG
LPR	  = org.scoja.common.PriorityUtils.LPR
NEWS	  = org.scoja.common.PriorityUtils.NEWS
UUCP  	  = org.scoja.common.PriorityUtils.UUCP
CRON	  = org.scoja.common.PriorityUtils.CRON
AUTHPRIV  = org.scoja.common.PriorityUtils.AUTHPRIV
FTP       = org.scoja.common.PriorityUtils.FTP
LOCAL0	  = org.scoja.common.PriorityUtils.LOCAL0
LOCAL1	  = org.scoja.common.PriorityUtils.LOCAL1
LOCAL2	  = org.scoja.common.PriorityUtils.LOCAL2
LOCAL3	  = org.scoja.common.PriorityUtils.LOCAL3
LOCAL4	  = org.scoja.common.PriorityUtils.LOCAL4
LOCAL5	  = org.scoja.common.PriorityUtils.LOCAL5
LOCAL6	  = org.scoja.common.PriorityUtils.LOCAL6
LOCAL7	  = org.scoja.common.PriorityUtils.LOCAL7

def priority(*args):
    if (len(args) != 2):
        raise ("Two arguments required: facility, level ")
    return org.scoja.server.filter.Priority(args[0], args[1]);
    
def facility(*args):
    facs = scoja.flat(args)
    return org.scoja.server.filter.Facility(facs)

def level(*args):
    facs = scoja.flat(args)
    return org.scoja.server.filter.Level(facs)

def ip(*args):
    if len(args) == 0:
        return org.scoja.server.expr.IP()
    else:
        ips = scoja.flat(args)
        return org.scoja.server.filter.IP(ips)

def eq(expr1, expr2):
    return org.scoja.server.filter.EqStringFilter(expr1, expr2)

def startsWith(expr1, expr2):
    return org.scoja.server.filter.StartsWithStringFilter(expr1, expr2)

def contains(expr1, expr2):
    return org.scoja.server.filter.ContainsStringFilter(expr1, expr2)

MATCH_ALL = 0
MATCH_CONTAINS = 1
MATCH_BEGINNING = 2

def match(expr, pattern, 
          method = MATCH_CONTAINS, 
          define = None, defineWithTail = None, unknown = None):
    if method == MATCH_ALL:
        mf = org.scoja.server.filter.MatchAllFilter(expr, pattern)
    elif method == MATCH_CONTAINS:
        mf = org.scoja.server.filter.MatchContainsFilter(expr, pattern)
    elif method == MATCH_BEGINNING:
        mf = org.scoja.server.filter.MatchBeginningFilter(expr, pattern)
    else:
        error("Unknown method for a match: " +
              "should be MATCH_ALL, MATCH_CONTAINS or MATCH_BEGINNING")
    if unknown != None:
        mf.setUnknown(unknown)
    if define != None:
        if type(define) == types.ListType:
            groupsToDefine = seq(1,len(define))
            varsNameQ = define
        elif type(define) == types.DictionaryType:
            groupsToDefine = define.keys()
            varsNameQ = scoja.getAll(define, groupsToDefine)
        else:
            raise ("Value for match define parameter should be " +
                   "a list or a dictionary")
        varsToDefine = []
        qualities = []
        for varq in varsNameQ:
            if type(varq) == types.StringType:
                varsToDefine.append(varq)
                qualities.append(UNCHECKED_STR)
            elif (type(varq) == types.TupleType and len(varq) == 2
                  and type(varq[0]) == types.StringType
                  and type(varq[1]) == types.IntType):
                varsToDefine.append(varq[0])
                qualities.append(varq[1])
            else:
                raise ("Value " + str(varq) +
                       " isn't a correct variable specification for match" +
                       ": should be a String or a pair (String,Int)")
        mf.setVarsToDefine(groupsToDefine, varsToDefine, qualities)
    if defineWithTail != None:
        mf.setVarToDefineWithTail(defineWithTail)
    return mf

def limit(key, aging = None, forgetAfter = 1*HOUR,
          events = None, size = None, description = None):
    lf = org.scoja.server.filter.LimitFilter(
        instance(key, usage=TEXT), instance(aging, usage=TEXT), forgetAfter);
    if events != None: lf.maxEvents = events
    if size != None: lf.maxSize = size
    if description != None: lf.description = description
    return lf;


#======================================================================
# Counting

def counter(kind, subkind, parameter, clearAfterReport = no):
    kind = template(kind)
    subkind = template(subkind)
    parameter = template(parameter)
    if kind.isConstant() and subkind.isConstant() and parameter.isConstant():
        m = org.scoja.server.core.CounterLink(
            org.scoja.server.core.Measure.Key(
                kind.toFilename(), 
                subkind.toFilename(),
                parameter.toFilename()))
    else:
        m = org.scoja.server.core.TemplatedCounterLink(
            org.scoja.server.core.Measure.TemplatedKey(
                template(kind), template(subkind), template(parameter)))
    m.clearAfterReport = clearAfterReport
    scoja.soul.measurerHub.addMeasurable(m)
    return m
    
def pairedCounter(kind, subkind, parameters, selector,
                  first=None, last=None,
                  pairSeparator="/", clearAfterReport=no,
                  bound=None,
                  excessKind=None, excessSubkind=None, excessPair=None):
    m = org.scoja.server.core.PairedCounterLink(
        template(kind), template(subkind),
        template(first), template(parameters), template(last),
        selector, pairSeparator)
    m.clearAfterReport = clearAfterReport
    if bound != None: m.setBound(bound, excessKind, excessSubkind, excessPair)
    scoja.soul.measurerHub.addMeasurable(m)
    return m
    
    
#======================================================================
# OTHERS
def queue(name, threads = 2):
    q = org.scoja.server.core.EventQueue(name, threads = threads)
    scoja.soul.addCluster(q)
    return q


#======================================================================
tmp = None

