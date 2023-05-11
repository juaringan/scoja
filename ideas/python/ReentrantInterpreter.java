/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2010  Mario Martínez
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

/**
 * It seems that PythonInterpreter is fully reentrant.
 * An script A evaluated in an interpreter I can ask I to execute another 
 * file calling execfile.
 * The global environment is shared.
 * Errors show the complete execution trace, with all the functions calls and
 * involved files.
 *
 * <p>Run this class with the following arguments:
 * r1.py r2.py r3.py r4.py
 */
public class ReentrantInterpreter {

    public static void main(final String[] args) {
        final PyObjectAdapter adapter = new ClassicPyObjectAdapter();
        final PythonInterpreter interp = new PythonInterpreter();
        interp.set("executor", adapter.adapt(interp));
        for (final String file: args) interp.execfile(file);
    }
}
