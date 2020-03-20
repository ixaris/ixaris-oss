package com.ixaris.commons.microservices.defaults.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launches or stops Docker containers necessary for black-box testing a microservice. This class takes two separate docker-compose files, one
 * for the actual microservice under test and one for the infrastructure to use for the test. Note that the class has sensible defaults for
 * docker-compose files; if you follow the Ixaris file structure convention, you can use this class's default constructor. Otherwise, use the
 * other constructors to change which docker-compose files get loaded.
 *
 * @author <a href="mailto:kurt.micallef@ixaris.com">Kurt Micallef</a>
 * @author <a href="mailto:kyle.pullicino@ixaris.com">Kyle Pullicino</a>
 */
public final class DockerStack {
    
    private static final Logger LOG = LoggerFactory.getLogger(DockerStack.class);
    private static final long TIMEOUT = 180L;
    
    private static final String DEFAULT_INFRASTRUCTURE_DOCKER_COMPOSE = "../../../docker/docker-compose-infra.yml";
    
    private final String microserviceComposeFile;
    private final String microserviceName;
    private final String infrastructureComposeFile;
    
    /**
     * Initialises a Docker Container which starts the given file which and the "../../../docker/docker-compose-infra.yml" file. The given file
     * should be a docker-compose file containing the microservice under test.
     *
     * @param microserviceComposeFile Docker-compose file for the microservice. Cannot be {@literal null}.
     * @param microserviceName The service to run, mapping to the name given in the {@code microserviceComposeFile}. Cannot be {@literal null}.
     */
    public DockerStack(final String microserviceComposeFile, final String microserviceName) {
        this(Objects.requireNonNull(microserviceComposeFile), Objects.requireNonNull(microserviceName), DEFAULT_INFRASTRUCTURE_DOCKER_COMPOSE);
    }
    
    /**
     * Initialises a Docker Container which starts the given files. The given files should be docker-compose files containing the microservice
     * under test and a docker-compose file for the infrastructure stack to use while testing.
     *
     * @param microserviceComposeFile Docker-compose file for the microservice. Cannot be {@literal null}.
     * @param microserviceName The service to run, mapping to the name given in the {@code microserviceComposeFile}. Cannot be {@literal null}.
     * @param infrastructureComposeFile Docker-compose file for the infrastructure. Cannot be {@literal null}.
     */
    public DockerStack(final String microserviceComposeFile, final String microserviceName, final String infrastructureComposeFile) {
        Objects.requireNonNull(microserviceName);
        Objects.requireNonNull(infrastructureComposeFile);
        this.microserviceComposeFile = microserviceComposeFile;
        this.microserviceName = microserviceName;
        this.infrastructureComposeFile = infrastructureComposeFile;
    }
    
    /**
     * Starts the microservice under test and the infrastructure stack configured for this docker container. Use the {@link #stop()} method to
     * shutdown all services once you no longer need them. This method will block until all the necessary processes have been launched and are
     * ready to be used.
     */
    public void start() {
        LOG.info("Starting the Docker container...");
        final String springBootApplicationStartedMessage = "[microservices.app.Application : ] (main:[]) Started Application";
        executeAsyncAndWaitOutput(new String[] { "docker-compose", "-f", infrastructureComposeFile, "up" }, springBootApplicationStartedMessage);
        executeAsyncAndWaitOutput(new String[] { "docker-compose", "-f", microserviceComposeFile, "up", microserviceName },
            springBootApplicationStartedMessage);
        LOG.info("The Docker container has started.");
    }
    
    /**
     * Stops the microservice under test and the infrastructure stack configured for this docker container.
     */
    public void stop() {
        executeAsyncAndWaitOutput(new String[] { "docker-compose", "-f", microserviceComposeFile, "kill", microserviceName }, "... done");
        executeAsyncAndWaitOutput(new String[] { "docker-compose", "-f", infrastructureComposeFile, "kill" }, "... done");
    }
    
    private static void executeAsyncAndWaitOutput(final String[] command, final CharSequence outputToWaitFor) {
        try {
            Executors.newFixedThreadPool(1).submit(() -> executeAndWaitOutput(command, outputToWaitFor)).get(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IllegalStateException(String.format("An error occurred while executing the following command: %s", Arrays.toString(command)), e);
        }
    }
    
    private static Process executeAndWaitOutput(final String[] command, final CharSequence outputToWaitFor) throws IOException {
        final Process subprocess = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (final BufferedReader dockerConsole = new BufferedReader(new InputStreamReader(subprocess.getInputStream()))) {
            waitForOutput(outputToWaitFor, dockerConsole);
            requireSuccessfulTermination(subprocess);
        }
        return subprocess;
    }
    
    private static void requireSuccessfulTermination(final Process process) {
        if (!process.isAlive() && process.exitValue() != 0) {
            throw new IllegalStateException(String.format("The container failed to start and exited with error code %d", process.exitValue()));
        }
    }
    
    private static void waitForOutput(final CharSequence textToWaitFor, final BufferedReader outputSource) throws IOException {
        String line;
        do {
            line = outputSource.readLine();
            LOG.info(line);
        } while (line != null && !line.contains(textToWaitFor));
    }
}
