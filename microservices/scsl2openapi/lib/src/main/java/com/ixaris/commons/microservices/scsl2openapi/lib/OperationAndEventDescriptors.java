package com.ixaris.commons.microservices.scsl2openapi.lib;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class OperationAndEventDescriptors {
    
    private final List<OpenAPIOperationDescriptor> operationDescriptors;
    private final List<OpenAPIEventDescriptor> eventDescriptors;
    
    public OperationAndEventDescriptors() {
        this.operationDescriptors = new ArrayList<>();
        this.eventDescriptors = new ArrayList<>();
    }
    
    public void addAll(final OperationAndEventDescriptors operationAndEventDescriptors) {
        operationDescriptors.addAll(operationAndEventDescriptors.getOperationDescriptors());
        eventDescriptors.addAll(operationAndEventDescriptors.getEventDescriptors());
    }
    
    public List<OpenAPIOperationDescriptor> getOperationDescriptors() {
        return operationDescriptors;
    }
    
    public List<OpenAPIEventDescriptor> getEventDescriptors() {
        return eventDescriptors;
    }
    
    public void addOperationDescriptor(final OpenAPIOperationDescriptor openAPIOperationDescriptor) {
        operationDescriptors.add(openAPIOperationDescriptor);
    }
    
    public void addEventDescriptor(final OpenAPIEventDescriptor openAPIEventDescriptor) {
        eventDescriptors.add(openAPIEventDescriptor);
    }
}
