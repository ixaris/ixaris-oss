package com.ixaris.commons.protobuf.lib;

import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.ProtocolMessageEnum;

public final class MessageDescriptorHelper {
    
    public static <T extends ProtocolMessageEnum> EnumDescriptor getDescriptorForType(final Class<T> type) {
        try {
            return (EnumDescriptor) type.getMethod("getDescriptor").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private MessageDescriptorHelper() {}
    
}
