package com.ixaris.commons.microservices.defaults.app;

import static io.jaegertracing.Configuration.ReporterConfiguration;
import static io.jaegertracing.Configuration.SamplerConfiguration;
import static io.jaegertracing.Configuration.SenderConfiguration;
import static io.jaegertracing.Configuration.fromEnv;
import static java.lang.String.format;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

/**
 * A {@link Configuration} class that is registering a {@link Tracer} {@link Bean}.
 *
 * @author <a href="mailto:jacob.falzon@ixaris.com">jacob.falzon</a>.
 */
@Configuration
public class TracingConfiguration {
    
    @Bean
    public Tracer jaegerTracer(@Value("${jaeger.host:jaeger}") final String jaegerHost,
                               @Value("${jaeger.sender.port:6831}") final int jaegerSenderPort,
                               @Value("${jaeger.sampler.port:5778}") final int jaegerSamplerPort,
                               @Value("${jaeger.reporter.flush.interval:10}") final int jaegerReporterFlushInterval,
                               @Value("${spring.application.name}") final String serviceName) {
        final SenderConfiguration senderConfiguration = new SenderConfiguration().withAgentHost(jaegerHost).withAgentPort(jaegerSenderPort);
        final ReporterConfiguration reporterConfiguration = new ReporterConfiguration().withSender(senderConfiguration).withFlushInterval(jaegerReporterFlushInterval);
        final SamplerConfiguration samplerConfiguration = new SamplerConfiguration()
            .withManagerHostPort(format("%s:%s", jaegerHost, jaegerSamplerPort))
            .withType(ConstSampler.TYPE)
            .withParam(1); // 1 => Constant Sampler will trace all calls, 0 => Constant Sampler will not trace any call
        
        final JaegerTracer jaegerTracer = fromEnv(serviceName).withReporter(reporterConfiguration).withSampler(samplerConfiguration).getTracer();
        GlobalTracer.registerIfAbsent(jaegerTracer);
        return jaegerTracer;
    }
}
