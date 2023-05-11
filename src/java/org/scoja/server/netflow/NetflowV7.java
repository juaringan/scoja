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

public class NetflowV7 {

    public static final int HEADER_SIZE = 24;
    public static final int FLOW_SIZE = 52;
    
    public static final Field
        FLAGS = new Field.UShort("FLAGS", false, 46),
        _ROUTERSC = new Field.UInt("ROUTERSC#", false, 48);
    
    protected static final Field[] basicFieldsNotInV1 = new Field[] {
        NetflowV5.PCKSEQ,
        
        NetflowV5.TCPFLAGS,
        NetflowV5.SRCAS,
        NetflowV5.DSTAS,
        NetflowV5.SRCMASK,
        NetflowV5.DSTMASK,
        FLAGS,
        _ROUTERSC,
    };
    
    public static Map<String,Field> withBasic(final Map<String,Field> fields){
        NetflowV1.withBasic(fields);
        for (final Field field: basicFieldsNotInV1) field.addTo(fields);
        return fields;
    }    
    
    public static Map<String,Field> withExtension(
        final Map<String,Field> fields) {
        final Field _routersc = fields.get("ROUTERSC#");
        final Field routersc = new Field.IP("ROUTERSC", _routersc);
        routersc.addTo(fields);
        return fields;
    }
    
    protected static final Map<String,Field> NAME2FIELD
        = withExtension(
            NetflowV5.withExtension(
                NetflowV1.withExtension(
                    withBasic(new HashMap<String,Field>()))));
    
    protected static final NetflowVSimple instance = new NetflowVSimple(
        7, HEADER_SIZE, FLOW_SIZE, 
        NetflowV1.FLOWS, NAME2FIELD.get("HEADEREPOCH"), NAME2FIELD);
        
    public static NetflowVSimple getInstance() { return instance; }
    
}
