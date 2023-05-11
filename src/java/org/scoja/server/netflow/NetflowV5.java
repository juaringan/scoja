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

public class NetflowV5 {

    public static final int HEADER_SIZE = 24;
    
    public static final Field
        PCKSEQ = new Field.UInt("PCKSEQ", true, 16),
        ENGINETYPE = new Field.UByte("ENGINETYPE", true, 20),
        ENGINEID = new Field.UByte("ENGINEID", true, 21);
    
    public static final int FLOW_SIZE = 48;

    public static final Field
        TCPFLAGS = new Field.UByte("TCPFLAGS", false, 37),
        SRCAS = new Field.UShort("SRCAS", false, 40),
        DSTAS = new Field.UShort("DSTAS", false, 42),
        SRCMASK = new Field.UByte("SRCMASK", false, 44),
        DSTMASK = new Field.UByte("DSTMASK", false, 45);
    
    protected static final Field[] basicFieldsNotInV1 = new Field[] {
        PCKSEQ,
        ENGINETYPE,
        ENGINEID,
        
        TCPFLAGS,
        SRCAS,
        DSTAS,
        SRCMASK,
        DSTMASK,
    };
    
    public static Map<String,Field> withBasic(final Map<String,Field> fields){
        NetflowV1.withBasic(fields);
        for (final Field field: basicFieldsNotInV1) field.addTo(fields);
        return fields;
    }    
    
    public static Map<String,Field> withExtension(
        final Map<String,Field> fields) {
        final Field
            flowno = fields.get("FLOWNO"),
            pckseq = fields.get("PCKSEQ");
        final Field flowseq = new Field.Add("FLOWSEQ", pckseq, flowno);
        flowseq.addTo(fields);
        return fields;
    }
    
    protected static final Map<String,Field> NAME2FIELD
        = withExtension(
            NetflowV1.withExtension(
                withBasic(new HashMap<String,Field>())));
        
    protected static final NetflowVSimple instance = new NetflowVSimple(
        5, HEADER_SIZE, FLOW_SIZE,
        NetflowV1.FLOWS, NAME2FIELD.get("HEADEREPOCH"), NAME2FIELD);
    
    public static NetflowVSimple getInstance() { return instance; }
    
}
