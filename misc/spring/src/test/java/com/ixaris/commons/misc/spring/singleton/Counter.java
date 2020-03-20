package com.ixaris.commons.misc.spring.singleton;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class Counter {
    
    private int counter = -1;
    
    @EventListener
    public void initialise(final ContextRefreshedEvent e) {
        counter = TestTypeRegistry.getInstance().getRegisteredValues().size();
    }
    
    public int getCounter() {
        return counter;
    }
    
}
