
package org.scoja.util;

import java.util.Random;

public class ExpiringLRUCacheStressTest {

    public static void main(final String[] args) throws Exception {
        int argc = 0;
        if (args.length != 5) {
            System.err.print(
                "java " + ExpiringLRUCacheStressTest.class.getName()
                +" <threads> <key-range> <expire-millis> <round> <exec-millis>"
                +"\n");
            System.exit(1);
        }
        final int userCount = Integer.parseInt(args[argc++]);
        final int range = Integer.parseInt(args[argc++]);
        final long duration = Long.parseLong(args[argc++]);
        final long round = Long.parseLong(args[argc++]);
        final long total = Long.parseLong(args[argc++]);
        
        final int cacheSize = userCount*range;
        
        final ExpiringLRUCache cache
            = new ExpiringLRUCache(cacheSize, duration);
        cache.setGraveyard(new Graveyard() {
                public void died(final Object key, final Object value) {
                    ((User.KillMe)value).expired();
                }
            });
            
        final User users[] = new User[userCount];
        for (int i = 0; i < users.length; i++) {
            users[i] = new User(cache, range, round);
            users[i].start();
        }
            
        Thread.currentThread().sleep(total);
        for (int i = 0; i < users.length; i++) {
            users[i].shouldStop();
        }
        Thread.currentThread().sleep(2*1000);
        cache.killAll();
        int alive = 0, totalAccess = 0, notFoundAccess = 0, periods = 0;
        int killed = 0, created = 0;
        for (int i = 0; i < users.length; i++) {
            System.out.print("Thread " + i
                             + ", access " + users[i].getTotalAccess()
                             + ", not found " + users[i].getNotFoundAccess()
                             + ", periods " + users[i].periodCount()
                             + ", created " + users[i].createdCount()
                             + ", killed " + users[i].killedCount()
                             + ", alive " + users[i].isAlive()
                             + "\n");
            if (users[i].isAlive()) alive++;
            totalAccess += users[i].getTotalAccess();
            notFoundAccess += users[i].getNotFoundAccess();
            periods += users[i].periodCount();
            killed += users[i].killedCount();
            created += users[i].createdCount();
        }
        System.out.print("Threads alive: " + alive
                         + "\nTotal access: " + totalAccess
                         + "\nNot found: " + notFoundAccess
                         + "\nTotal periods: " + periods
                         + "\nTotal killed: " + killed
                         + "\nTotal created: " + created
                         + "\n");
    }

    public static class User extends Thread {
        final ExpiringLRUCache cache;
        final int range;
        final long round;
        final Random random;
        int totalAccess;
        int notFoundAccess;
        int base;
        long periodEnd;
        int periods;
        int created;
        int killed;
        boolean stopRequested;
        
        
        public User(final ExpiringLRUCache cache,
                    final int range,
                    final long round) {
            this.cache = cache;
            this.range = range;
            this.round = round;
            this.random = new Random();
            this.totalAccess = 0;
            this.notFoundAccess = 0;
            this.base = 0;
            this.periodEnd = 0;
            this.periods = 0;
            this.created = 0;
            this.killed = 0;
            this.stopRequested = false;
        }
        
        public void shouldStop() {
            stopRequested = true;
        }
        
        public int getTotalAccess() {
            return totalAccess;
        }
        
        public int getNotFoundAccess() {
            return notFoundAccess;
        }
        
        public int periodCount() {
            return periods;
        }
        
        public int createdCount() {
            return created;
        }
        
        public synchronized void addKilled() {
            killed++;
        }
        
        public int killedCount() {
            return killed;
        }
        
        public void run() {
            while (!stopRequested) {
                final long now = System.currentTimeMillis();
                if (periodEnd < now) {
                    base = random.nextInt(Integer.MAX_VALUE/2);
                    periodEnd = now + (3*round)/4
                        + (random.nextLong() % (round/2));
                    periods++;
                }
                access();
                /*
                try {
                    Thread.currentThread().sleep(1);
                } catch (InterruptedException e) {}
                */
            }
        }

        private void access() {
            final Integer key = new Integer(base + random.nextInt(range));
            LRUShell shell = null;
            try {
                shell = cache.get(key);
                if (shell.getValue() == null) {
                    shell.put(new KillMe(key));
                    notFoundAccess++;
                    created++;
                }
            } finally {
                if (shell != null) shell.release();
            }
            totalAccess++;
        }
        
        public class KillMe {
            protected final Object value;
            
            public KillMe(final Object value) {
                this.value = value;
            }
            
            public void expired() {
                User.this.addKilled();
            }
            
            public String toString() { return String.valueOf(value); }
        }
        
    }
}
