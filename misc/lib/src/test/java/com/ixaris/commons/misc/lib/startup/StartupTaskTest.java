package com.ixaris.commons.misc.lib.startup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class StartupTaskTest {
    
    static {
        StartupTask.loadAndRunTasks();
    }
    
    @Test
    public void testTaskRuns() {
        assertThat(TestStartupTask.count.get()).isEqualTo(1);
    }
    
}
