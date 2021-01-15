package com.ixaris.commons.kafka.multitenancy;

import com.google.common.base.CaseFormat;

public class KafkaUtil {
    
    /**
     * Resolves the topic name from the service name and event name. This util uses the UPPER_UNDERSCORE convention for topic names.
     *
     * <p><em>E.g. the event [testEvent] from service [Instruments] resolves to [INSTRUMENTS_TEST_EVENT]</em>
     *
     * @param serviceName
     * @param path
     * @return The topic name to be used for the specified event
     */
    public static String resolveTopicName(final String serviceName, final Iterable<String> path) {
        final String upperCaseServiceName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, serviceName);
        final String upperCaseEventName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, String.join(".", path));
        
        return upperCaseEventName.isEmpty() ? upperCaseServiceName : (upperCaseServiceName + '.' + upperCaseEventName);
    }
    
}
