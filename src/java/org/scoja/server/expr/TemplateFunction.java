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
package org.scoja.server.expr;

import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;
import org.scoja.server.template.Template;

/**
 * This is the intermediate abstract class for the two Template function
 * alternatives:
 * template to use in a filename ({@link FilenameTemplateFunction)
 * and template to use in text ({@link TextTemplateFunction}).
 */
public abstract class TemplateFunction extends StringExpressionAtPython {
    
    protected final Template tpt;
    protected final int quality;
    
    public TemplateFunction(final Template tpt, final int quality) {
        this.tpt = tpt;
        this.quality = quality;
    }
    
    public QStr eval(final EventContext env) {
        final String instance = strEval(env);
        return new QStr(instance, quality);
    }
    
    protected abstract String strEval(EventContext env);
}
