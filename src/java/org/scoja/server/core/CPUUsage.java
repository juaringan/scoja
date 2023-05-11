/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2008  Bankinter, SA
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
package org.scoja.server.core;

public class CPUUsage {
    
    protected long real, cpu, user;

    public static CPUUsage none() {
        return new CPUUsage();
    }
    
    public static CPUUsage forCurrentThread() {
        return new CPUUsage(System.currentTimeMillis(),
                            MeasureUtils.getCurrentThreadCpuTime(),
                            MeasureUtils.getCurrentThreadUserTime());
    }
        
    public CPUUsage() {
        this(0,0,0);
    }
    
    public CPUUsage(final long real, final long cpu, final long user) {
        this.real = real;
        this.cpu = cpu;
        this.user = user;
    }
    
    public long getRealTime() { return real; }
    public long getCPUTime() { return cpu; }
    public long getUserTime() { return user; }
    
    public CPUUsage add(final CPUUsage other) {
        return new CPUUsage(
            this.real + other.real,
            this.cpu + other.cpu,
            this.user + other.user);
    }
    
    public CPUUsage sub(final CPUUsage other) {
        return new CPUUsage(
            this.real - other.real,
            this.cpu - other.cpu,
            this.user - other.user);
    }
    
    public void inc(final CPUUsage other) {
        this.real += other.real;
        this.cpu += other.cpu;
        this.user += other.user;
    }
    
    public void dec(final CPUUsage other) {
        this.real -= other.real;
        this.cpu -= other.cpu;
        this.user -= other.user;
    }
    
    public String toString() {
        return "CPUUsage["
            + "real: " + real
            + ", cpu: " + cpu
            + ", user: " + user
            + "]";
    }
}
