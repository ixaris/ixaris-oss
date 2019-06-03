package com.ixaris.commons.misc.lib.startup;

import java.util.ServiceLoader;

/**
 * Startup tasks should avoid importing many classes since some startup tasks need to run before other classes are
 * loaded (e.g. setting system properties)
 */
@FunctionalInterface
public interface StartupTask {
    
    static void loadAndRunTasks() {
        for (final StartupTask task : ServiceLoader.load(StartupTask.class)) {
            task.run();
        }
    }
    
    void run();
    
}
