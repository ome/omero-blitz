/*
 *   $Id$
 *
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 *
 */

package omero.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container class for storing resources which should be cleaned up on close and
 * periodically checked.
 *
 * Note: this class uses java.util.logging (JUL) rather than commons-logging
 * since it may be used on the client-side. Any use server-side will have logs
 * forwarded to log4j via slf4j.
 */
public class Resources {

    /**
     * Interface to be implemented by any object which wants to be managed by
     * the {@link Resources} class.
     */
    public interface Entry {
        /**
         * Called during each cycle. If it returns false or throws an exception,
         * {@link #cleanup()} will be called on the instance, and it will be
         * removed.
         */
        boolean check();

        /**
         * Gives an {@link Entry} a chance to cleanup resources before it is
         * removed from checking.
         */
        void cleanup();
    }

    private static Logger log = LoggerFactory.getLogger(Resources.class.getName());

    private final int sleeptime;

    private final ScheduledFuture<?> future;

    private final ScheduledExecutorService service;

    private final List<Entry> stuff = new CopyOnWriteArrayList<Entry>();

    /**
     * As {@link Resources#Resources(int)} but specifies a 60 second sleep
     * period between task execution.
     */
    public Resources() {
        this(60);
    }

    /**
     * As {@link Resources#Resources(int, ScheduledExecutorService)} but uses a
     * {@link Executors#newSingleThreadExecutor()}.
     */
    public Resources(int sleeptimeSeconds) {
        this(sleeptimeSeconds, Executors.newSingleThreadScheduledExecutor());
    }

    public Resources(int sleeptimeSeconds, ScheduledExecutorService service) {
        this.sleeptime = sleeptimeSeconds;
        this.service = service;
        log.trace("Starting");
        this.future = this.service.scheduleAtFixedRate(task(), 1, sleeptime, TimeUnit.SECONDS);
    }

    private Runnable task() {
        return new Runnable() {
            public void run() {
                log.trace("Running checks...");
                for (Entry entry : stuff) {
                    log.trace("Checking " + entry);
                    boolean success = true;
                    try {
                        success = entry.check();
                    } catch (Exception e) {
                        log.warn("Exception thrown by entry: "
                                + e.getMessage());
                        success = false;
                    }
                    if (!success) {
                        remove(entry);
                    }
                }
                log.trace("Finished checks.");
            }

        };
    }

    public void add(Entry entry) {
        if (entry == null) {
            log.warn("Entry null");
            return;
        }
        log.trace("Adding object " + entry);
        stuff.add(entry);
    }

    public int size() {
        return stuff.size();
    }

    public void cleanup() {

        log.trace("Cleaning called");

        for (Entry entry : stuff) {
            remove(entry);
        }

        log.trace("Stopping");
        // Cancel thread; allows current task to finish
        future.cancel(false);
        log.trace("Stopped");
    }

    public void remove(Entry entry) {
        if (entry == null) {
            log.warn("Entry null");
            return;
        }
        log.trace("Cleaning " + entry);
        try {
            entry.cleanup();
        } catch (Exception e) {
            log.warn("Cleaning entry threw an exception" + e);
        }
        log.trace("Removing " + entry);
        stuff.remove(entry);
    }

}
