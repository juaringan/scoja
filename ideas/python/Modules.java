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

import org.python.core.*;
import org.python.core.adapter.PyObjectAdapter; 
import org.python.core.adapter.ClassicPyObjectAdapter; 
import org.python.util.*;

public class Modules {

    public static void main(final String[] args) throws Throwable {
        final PyObjectAdapter adapter = new ClassicPyObjectAdapter();
        final InteractiveConsole interp = new InteractiveConsole();
        final PyModule mod = new PyModule("scoja", null);
        mod.__setattr__("a", adapter.adapt(new Integer(1)));
        interp.set("mod", mod);
        interp.interact();
    }
}
