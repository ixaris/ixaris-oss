package com.ixaris.commons.swaggergenerator.swagger;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class OperationAndEventDescriptors {
    
    private final List<SwaggerOperationDescriptor> operationDescriptors;
    private final List<SwaggerEventDescriptor> eventDescriptors;
    
    public OperationAndEventDescriptors() {
        this.operationDescriptors = new LinkedList<>();
        this.eventDescriptors = new LinkedList<>();
    }
    
    public void addAll(final OperationAndEventDescriptors operationAndEventDescriptors) {
        operationDescriptors.addAll(operationAndEventDescriptors.getOperationDescriptors());
        eventDescriptors.addAll(operationAndEventDescriptors.getEventDescriptors());
    }
    
    public List<SwaggerOperationDescriptor> getOperationDescriptors() {
        return operationDescriptors;
    }
    
    public List<SwaggerEventDescriptor> getEventDescriptors() {
        return eventDescriptors;
    }
    
    public void addOperationDescriptor(final SwaggerOperationDescriptor swaggerOperationDescriptor) {
        operationDescriptors.add(swaggerOperationDescriptor);
    }
    
    public void addEventDescriptor(final SwaggerEventDescriptor swaggerEventDescriptor) {
        eventDescriptors.add(swaggerEventDescriptor);
    }
}
