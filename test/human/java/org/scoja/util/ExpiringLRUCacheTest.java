
package org.scoja.util;

public class ExpiringLRUCacheTest {

    public static void main(final String[] args) throws Exception {
        final ExpiringLRUCache cache = new ExpiringLRUCache(3, 2*1000);
        cache.setGraveyard(new Graveyard() {
                public void died(final Object key, final Object value) {
                    System.out.println("Expired[" + key + "] = " + value);
                }
            });
        put(cache, "a", "A");
        put(cache, "b", "B");
        put(cache, "c", "C");
        put(cache, "d", "D");
        put(cache, "e", "E");
        Thread.currentThread().sleep(3*1000);
        put(cache, "f", "F");
        put(cache, "g", "G");
    }
    
    private static void put(final ExpiringLRUCache cache,
                            final String key, final String value) {
        System.out.println("Putting " + key + "=" + value);
        LRUShell shell = null;
        try {
            shell = cache.get(key);
            if (shell.getValue() == null) {
                shell.put(value);
            }
        } finally {
            if (shell != null) shell.release();
        }
    }
}
