# Spring Health Linrary

Our microservices have the ability to register health checks via:
- Spring Boot Actuator health checks, or
- Dropwizard health checks

The first is handled automatically by Spring Boot. To add custom health checks, simply create `HealthIndicator` 
bean instances. Spring will automatically find them, and Spring Boot Actuator will expose them on a `/health` 
HTTP endpoint. For further details see: 
https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html
(and especially, https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html#production-ready-health)

[Dropwizard Metrics](http://metrics.dropwizard.io/3.2.3/) is a very popular framework for creating 
[metrics](http://metrics.dropwizard.io/3.2.3/manual/core.html) and 
[health checks](http://metrics.dropwizard.io/3.2.3/manual/healthchecks.html). Some 3rd party instances 
(eg. Hikari) make use of them to expose runtime metrics and health. Unfortunately, they will not be automatically 
detected by Spring as there is no integration as yet. As such, this module will 'bridge' the gap and allow the 
metrics and health checks to be exposed via the same actuator endpoint above. 

Without this module, the metrics/health checks created via the Dropwizard library will still be collected, but it 
will never actually be used as it will not have an endpoint for external monitoring services to query. Should 
Spring Boot Actuator not be on the classpath. or should there be no implemented Dropwizard metrics/health checks, 
a log message is output to that effect and this module will have no effect.


