package com.ixaris.commons.microservices.defaults.live.example;

import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">$user</a>
 */
@Component
public class ExampleComponent {
    
    public void exampleOperation(long sleepDuration) {
        try {
            Thread.sleep(sleepDuration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
