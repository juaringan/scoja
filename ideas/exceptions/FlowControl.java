/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
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

import java.util.Random;

/**
 * To check if exceptions are a good way to do flow control.
 * This test compares an if driven flow control with 2 exception flow controls:
 * with exception reuse and with creating new exceptions.
 * Conclusions:
 *   1. Raising exception is expensive.
 *   2. Catching exception cost is similar to an if execution.
 * So, control flow with a reused exception is similar to control flow with
 * an special value.
 * Speed being similar, decision must be taken on other aspects.
 */
public class FlowControl {

    public static void main(final String[] args) {
        if (args.length != 3) {
            System.err.println("java WithException <kind> <times> <fail%>");
            System.exit(0);
        }
        int argc = 0;
        final String kind = args[argc++];
        final int times = Integer.parseInt(args[argc++]);
        final float fails = Float.parseFloat(args[argc++]) / 100;
        Worker worker = null;
        Function function = null;
        if ("null".equals(kind)) {
            worker = new WorkerNull();
            function = new FunctionNull();
        } else if ("exception".equals(kind)) {
            worker = new WorkerException();
            function = new FunctionReuseException();
        } else if ("newexception".equals(kind)) {
            worker = new WorkerException();
            function = new FunctionNewException();
        } else {
            System.err.println("Bad kind " + kind);
            System.exit(1);
        }
        final int failures = work(worker, function, times, fails);
        System.out.println("Failures: " + failures);
    }
    
    protected static int work(final Worker worker, final Function function,
                              final int times, final float fails) {
        final int t0 = (int)Math.sqrt(times);
        int failures = 0;
        for (int remain = times; remain > 0; remain -= t0) {
            failures += worker.work(function, t0, fails);
        }
        return failures;
    }
    
    public static interface Worker {
        public int work(Function f, int times, float fails);
    }
    
    public static class WorkerNull implements Worker {
        public int work(final Function f, final int times, final float fails) {
            int failures = 0;
            for (int i = 0; i < times; i++) {
                if (f.eval(fails) == DISCARD_VALUE) failures++;
            }
            return failures;
        }
    }
    
    public static class WorkerException implements Worker {
        public int work(final Function f, final int times, final float fails) {
            int failures = 0;
            for (int i = 0; i < times; i++) {
                try {
                    f.eval(fails);
                } catch (RuntimeException e) {
                    failures++;
                }
            }
            return failures;
        }
    }
    
    public static final Object DISCARD_VALUE
        = new Object();
    public static final RuntimeException DISCARD_EXCEPTION
        = new RuntimeException();
    
    public static abstract class Function {
        protected final Random r = new Random();
        public abstract Object eval(float fails);
    }

    public static class FunctionNull extends Function {
        public Object eval(final float fails) {
            return (r.nextFloat() < fails) ? DISCARD_VALUE : null;
        }
    }
    
    public static class FunctionReuseException extends Function {
        public Object eval(final float fails) {
            if (r.nextFloat() < fails) throw DISCARD_EXCEPTION;
            return null;
        }
    }
    
    public static class FunctionNewException extends Function {
        public Object eval(final float fails) {
            if (r.nextFloat() < fails) throw new RuntimeException();
            return null;
        }
    }
}
