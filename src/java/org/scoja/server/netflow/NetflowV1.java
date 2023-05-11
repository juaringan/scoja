/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2012  LogTrust
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

package org.scoja.server.netflow;

import java.util.Map;
import java.util.HashMap;

public class NetflowV1 {

    public static final int HEADER_SIZE = 16;
    
    public static final Field
        VERSION = new Field.UShort("VERSION", true, 0),
        FLOWS = new Field.UShort("FLOWS", true, 2),
        UPTIME =  new Field.UInt("UPTIME", true, 4),
        HEADEREPOCHSEC = new Field.UInt("HEADEREPOCHSEC", true, 8),
        HEADEREPOCHNANOS = new Field.UInt("HEADEREPOCHNANOS", true, 12),
        HEADEREPOCH = new Field.SecNanos("HEADEREPOCH", true, 8);
    
    public static final int FLOW_SIZE = 48;
    
    public static final Field
        FLOWNO = new Field.RelativeFlowSeq("FLOWNO");
    
    public static final Field
        _SRCIP = new Field.UInt("SRCIP#", false, 0),
        _DSTIP = new Field.UInt("DSTIP#", false, 4),
        _NEXTHOP = new Field.UInt("NEXTHOP#", false, 8),
        III = new Field.UShort("III", false, 12),
        IOI = new Field.UShort("IOI", false, 14),
        PACKETS = new Field.UInt("PACKETS", false, 16),
        BYTES = new Field.UInt("BYTES", false, 20),
        FIRSTUP = new Field.UInt("FIRSTUP", false, 24),
        LASTUP = new Field.UInt("LASTUP", false, 28),
        SRCPORT = new Field.UShort("SRCPORT", false, 32),
        DSTPORT = new Field.UShort("DSTPORT", false, 34),
        IPPROTO = new Field.UByte("IPPROTO", false, 38),
        TOS = new Field.UByte("TOS", false, 39),
        TCPFLAGS = new Field.UByte("TCPFLAGS", false, 40);
        
    protected static final Field[] basicFields = new Field[] {
        VERSION,
        FLOWS,
        UPTIME,
        HEADEREPOCHSEC,
        HEADEREPOCHNANOS,
        HEADEREPOCH,
        
        FLOWNO,
        _SRCIP,
        _DSTIP,
        _NEXTHOP,
        III,
        IOI,
        PACKETS,
        BYTES,
        FIRSTUP,
        LASTUP,
        SRCPORT,
        DSTPORT,
        IPPROTO,
        TOS,
        TCPFLAGS,
    };
    
    public static Map<String,Field> withBasic(final Map<String,Field> fields){
        for (final Field field: basicFields) field.addTo(fields);
        return fields;
    }    
    
    public static Map<String,Field> withExtension(
        final Map<String,Field> fields) {
        final Field 
            headerepoch = fields.get("HEADEREPOCH"),
            //headerepochsecs = fields.get("HEADEREPOCHSECS"),
            //headerepochnanos = fields.get("HEADEREPOCHNANOS"),
            uptime = fields.get("UPTIME"),
            firstup = fields.get("FIRSTUP"),
            lastup = fields.get("LASTUP");
        final Field
            //epoch = new Field.Add("EPOCH", 
            //        new Field.Mul(headerepochsecs, new Field.Const(1000)),
            //       new Field.Div(headerepochnanos, new Field.Const(1000000))),
            upepoch = new Field.Sub("UPEPOCH", headerepoch, uptime),
            firstepoch = new Field.Add("FIRSTEPOCH", upepoch, firstup),
            lastepoch = new Field.Add("LASTEPOCH", upepoch, lastup),
            headerdate = new Field.Date("HEADERDATE", headerepoch),
            first = new Field.Date("FIRST", firstepoch),
            last = new Field.Date("LAST", lastepoch);
        //epoch.addTo(fields);
        upepoch.addTo(fields);
        firstepoch.addTo(fields);
        lastepoch.addTo(fields);
        headerdate.addTo(fields);
        first.addTo(fields);
        last.addTo(fields);
        
        final Field
            _srcip = fields.get("SRCIP#"),
            _dstip = fields.get("DSTIP#"),
            _nexthop = fields.get("NEXTHOP#");
        final Field
            srcip = new Field.IP("SRCIP", _srcip),
            dstip = new Field.IP("DSTIP", _dstip),
            nexthop = new Field.IP("NEXTHOP", _nexthop);
        srcip.addTo(fields);
        dstip.addTo(fields);
        nexthop.addTo(fields);
        
        return fields;
    }
    
    protected static final Map<String,Field> NAME2FIELD
        = withExtension(withBasic(new HashMap<String,Field>()));
        
    protected static final NetflowVSimple instance = new NetflowVSimple(
        1, HEADER_SIZE, FLOW_SIZE,
        FLOWS, NAME2FIELD.get("HEADEREPOCH"), NAME2FIELD);
    
    public static NetflowVSimple getInstance() { return instance; }
}
