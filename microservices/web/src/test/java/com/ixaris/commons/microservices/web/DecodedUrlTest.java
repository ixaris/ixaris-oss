package com.ixaris.commons.microservices.web;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.web.dynamic.DecodedUrl;

/**
 * @author <a href="mailto:bernice.zerafa@ixaris.com">bernice.zerafa</a>
 */
public class DecodedUrlTest {
    
    @Test
    public void urlWithUnderscore_validServiceNameAndPath() {
        final DecodedUrl decodedUrlParts = new DecodedUrl("/name/someResource/someSubResource/_", false);
        
        Assertions.assertThat(decodedUrlParts.serviceName).isEqualTo("name");
        Assertions.assertThat(decodedUrlParts.serviceKey).isNull();
        
        final ServicePathHolder path = decodedUrlParts.path;
        Assertions.assertThat(path.get(0)).isEqualTo("someResource");
        Assertions.assertThat(path.get(1)).isEqualTo("someSubResource");
        Assertions.assertThat(path.get(2)).isEqualTo("_");
    }
    
    @Test
    public void urlWithServiceKey_validServiceNameAndKeyAndPath() {
        final DecodedUrl decodedUrlParts = new DecodedUrl("/name/key/someResource/someSubResource/_", true);
        
        Assertions.assertThat(decodedUrlParts.serviceName).isEqualTo("name");
        Assertions.assertThat(decodedUrlParts.serviceKey).isEqualTo("key");
        
        final ServicePathHolder path = decodedUrlParts.path;
        Assertions.assertThat(path.get(0)).isEqualTo("someResource");
        Assertions.assertThat(path.get(1)).isEqualTo("someSubResource");
        Assertions.assertThat(path.get(2)).isEqualTo("_");
    }
    
    @Test
    public void urlWithNoServiceName_shouldThrowIllegalStateException() {
        Assertions.assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> new DecodedUrl("/", false));
    }
    
    @Test
    public void urlWithNoServiceKey_shouldThrowIllegalStateException() {
        Assertions
            .assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> new DecodedUrl("/serviceName", true));
    }
    
}
