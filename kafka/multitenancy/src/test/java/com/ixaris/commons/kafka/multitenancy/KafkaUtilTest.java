package com.ixaris.commons.kafka.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class KafkaUtilTest {
    
    @Test
    public void testEventTopicName_RootPath() {
        assertThat(KafkaUtil.resolveTopicName("test", Collections.emptyList())).isEqualTo("TEST");
    }
    
    @Test
    public void testEventTopicName_SingleWord() {
        assertThat(KafkaUtil.resolveTopicName("test", Arrays.asList("test", "event"))).isEqualTo("TEST.TEST.EVENT");
    }
    
    @Test
    public void testEventTopicName_MultipleWordTopic() {
        assertThat(KafkaUtil.resolveTopicName("test", Collections.singletonList("anotherEvent"))).isEqualTo("TEST.ANOTHER_EVENT");
    }
    
    @Test
    public void testEventTopicName_MultipleWordServiceTopc() {
        assertThat(KafkaUtil.resolveTopicName("testUtils", Arrays.asList("someTest", "anotherEvent"))).isEqualTo("TEST_UTILS.SOME_TEST.ANOTHER_EVENT");
    }
    
}
