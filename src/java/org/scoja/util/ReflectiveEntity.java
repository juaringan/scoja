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

package org.scoja.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 */
public class ReflectiveEntity {

    public final String name;
    public final Class type;
    public Object entity;
    public final List errors;
    
    public ReflectiveEntity(final String name) {
        this(name, null);
    }
    
    public ReflectiveEntity(final String name, final Class type) {
        this.name = name;
        this.type = type;
        this.entity = null;
        this.errors = new ArrayList();
    }
    
    public void forget() {
        entity = null;
        errors.clear();
    }
    
    public Object getByAllMeans() 
    throws EntityNotFoundException {
        if (entity == null) {
            tryClassInstance();
            if (entity == null) tryStaticEntry();
            if (entity == null) throw new EntityNotFoundException(name);
        }
        return entity;
    }
    
    protected void tryClassInstance() {
        try {
            final Object candidate = Class.forName(name).newInstance();
            if (checkType(candidate)) entity = candidate;
        } catch (Throwable e) {}
    }
    
    protected void tryStaticEntry() {
        final String[] pieces = name.split("\\.");
        for (int i = pieces.length-1; (i >= 1) && (entity == null); i--) {
            tryStaticEntry(pieces, i);
        }
    }
    
    protected void tryStaticEntry(final String[] pieces, final int cl) {
        final String clazzName = buildClassName(pieces, cl);
        //System.err.println("Trying " + clazzName);
        try {
            final Class clazz = Class.forName(clazzName);
            tryStaticEntry(clazz, pieces, cl);
        } catch (ClassNotFoundException e) {
        } catch (Throwable e) {
            //System.err.println("Error " + e);
            //e.printStackTrace(System.err);
        }
    }
    
    protected String buildClassName(final String[] pieces, final int cl) {
        final StringBuffer sb = new StringBuffer(pieces[0]);
        for (int i = 1; i < cl; i++) sb.append('.').append(pieces[i]);
        return sb.toString();
    }
    
    protected void tryStaticEntry(final Class clazz, 
                                  final String[] pieces, final int cl) {
        tryEntry(clazz, null, pieces, cl);
    }
    
    protected void tryDynamicEntry(final Object candidate,
                                   final String[] pieces, final int cl) {
        if (pieces.length == cl) {
            //System.err.println("End of search path " + candidate);
            if (checkType(candidate)) entity = candidate;
        } else {
            tryEntry(candidate.getClass(), candidate, pieces, cl);
        }
    }
    
    protected void tryEntry(final Class clazz, final Object candidate,
                            final String[] pieces, final int cl) {
        final String piece = pieces[cl];
        //System.err.println("Searching " + piece + " at " + clazz.getName());
        try {
            final Field field = clazz.getDeclaredField(piece);
            if ((field.getModifiers() & Modifier.STATIC) != 0) {
                tryDynamicEntry(field.get(null), pieces, cl+1);
            } else if (candidate != null) {
                tryDynamicEntry(field.get(candidate), pieces, cl+1);
            }
        } catch (Throwable e) {}
        if (entity != null) return;
        try {
            final Method method = clazz.getDeclaredMethod(piece, new Class[0]);
            if ((method.getModifiers() & Modifier.STATIC) != 0) {
                tryDynamicEntry(
                    method.invoke(null,(Object[])null), pieces, cl+1);
            } else {
                tryDynamicEntry(
                    method.invoke(candidate,(Object[])null), pieces, cl+1);
            }
        } catch (Throwable e) {}
    }
    
    protected boolean checkType(final Object entity) {
        return type == null || entity == null || type.isInstance(entity);
    }
    
    //======================================================================
    public boolean equals(final Object other) {
        return (other instanceof ReflectiveEntity)
            && equals((ReflectiveEntity)other);
    }
    
    public boolean equals(final ReflectiveEntity other) {
        return other != null
            && this.name.equals(other.name)
            && this.type == other.type;
    }
    
    public int hashCode() {
        return name.hashCode();
    }
    
    public String toString() {
        return "The value " + name + ((type == null) ? "" : type.getName());
    }
}
