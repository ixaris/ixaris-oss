package com.ixaris.commons.microservices.web.swagger;

import static com.ixaris.commons.async.lib.Async.result;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEvent;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEventAck;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEventAck.Status;

@Component
public class TestConditionalNonProcessableSwaggerEventFilter implements TestSwaggerEventFilter {
    
    private static final Logger LOG = LoggerFactory.getLogger(TestConditionalNonProcessableSwaggerEventFilter.class);
    
    private final Set<Long> nonProcessableEvents = new HashSet<>();
    private final List<Long> seenEvents = new LinkedList<>();
    
    public void addNonProcessableEventByIntentId(final long intentId) {
        nonProcessableEvents.add(intentId);
    }
    
    public List<Long> getSeenEvents() {
        return seenEvents;
    }
    
    @Override
    public Async<SwaggerEventAck> doFilter(final SwaggerEvent in, final AsyncFilterNext<SwaggerEvent, SwaggerEventAck> next) {
        seenEvents.add(in.getHeader().getIntentId());
        if (nonProcessableEvents.contains(in.getHeader().getIntentId())) {
            LOG.info("Skipping non processable event {}", in);
            return result(new SwaggerEventAck(in, new Status(false, false), null));
        }
        return next.next(in);
    }
    
    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
    
}
