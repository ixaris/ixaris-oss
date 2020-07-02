# Microservice App

Spring boot app common to all microservices for bootstrapping

## Logging

This module provides configuration file `log4j2-gelf.xml` which uses the GELF 
appender. It should be activated only when the Java System property 
`GRAYLOG_HOST_ADDRESS` is set, otherwise log events will be lost.

A Spring Boot application can start with the default configuration and then
switch to the GELF-enabled one when configuration is loaded from the Spring
Cloud Config server:

1. Switch Spring LoggingSystem to `com.ixaris.core.microservices.app.Log4J2System`
(`-Dorg.springframework.boot.logging.LoggingSystem=com.ixaris.core.microservices.app.Log4J2System`).
2. Set environment property `logging.config`  to `classpath:log4j2-gelf.xml`.
3. Set environment property `GRAYLOG_HOST_ADDRESS`.
