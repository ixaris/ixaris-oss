package com.ixaris.commons.protobuf.codegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Set;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;

/**
 * Creates an executable {@code protoc} plugin (written in Java)
 */
public class ProtocPluginAssembler {
    
    private static final String DATA_MODEL_SYSPROP = "sun.arch.data.model";
    private static final String WIN_JVM_DATA_MODEL_32 = "32";
    private static final String WIN_JVM_DATA_MODEL_64 = "64";
    
    private final String javaHome;
    private final Set<String> classPath;
    private final File pluginDir;
    
    // Assuming we're running a HotSpot JVM, use the data model of the
    // current JVM as the default. This property is only relevant on
    // Windows where we need to pick the right version of the WinRun4J executable.
    private final String winJvmDataModel;
    
    public ProtocPluginAssembler(final Set<String> classPath, final File pluginDir) {
        javaHome = System.getProperty("java.home");
        
        this.classPath = classPath;
        this.pluginDir = pluginDir;
        
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            // try to guess the architecture by looking at the directories in the JDK/JRE javaHome points at.
            // If that fails, try to figure out from the currently running JVM.
            if (archDirectoryExists("amd64")) {
                winJvmDataModel = WIN_JVM_DATA_MODEL_64;
            } else if (archDirectoryExists("i386")) {
                winJvmDataModel = WIN_JVM_DATA_MODEL_32;
            } else if (System.getProperty(DATA_MODEL_SYSPROP) != null) {
                winJvmDataModel = System.getProperty(DATA_MODEL_SYSPROP);
            } else {
                winJvmDataModel = WIN_JVM_DATA_MODEL_32;
            }
        } else {
            winJvmDataModel = null;
        }
    }
    
    public File build(final ProtocPlugin plugin) {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return buildWindowsPlugin(plugin);
        } else {
            return buildUnixPlugin(plugin);
        }
    }
    
    private File buildWindowsPlugin(final ProtocPlugin plugin) {
        final File jvmLocation = findJvmLocation(new File(javaHome),
            "jre/bin/server/jvm.dll",
            "bin/server/jvm.dll",
            "jre/bin/client/jvm.dll",
            "bin/client/jvm.dll");
        
        final File winRun4JIniFile = new File(pluginDir, plugin.getName() + ".ini");
        
        try (final PrintWriter out = new PrintWriter(new FileWriter(winRun4JIniFile))) {
            if (jvmLocation != null) {
                out.println("vm.location=" + jvmLocation.getAbsolutePath());
            }
            int index = 1;
            for (final String cp : classPath) {
                out.println("classpath." + index + "=" + cp.substring(1).replaceAll("/$", "").replaceAll("/", "\\\\"));
                index++;
            }
            out.println("main.class=" + plugin.getClass().getName());
            
            out.println("vm.version.min=1.8");
            
            // keep from logging to stdout (the default)
            out.println("log.level=none");
            out.println("[ErrorMessages]");
            out.println("show.popup=false");
        } catch (final IOException e) {
            throw new IllegalStateException("Could not write WinRun4J ini file: " + winRun4JIniFile.getAbsolutePath() + " " + e.getMessage(), e);
        }
        
        final String executablePath = "winrun4j/WinRun4J" + winJvmDataModel + ".exe";
        final URL url = getClass().getClassLoader().getResource(executablePath);
        if (url == null) {
            throw new IllegalStateException("Could not locate WinRun4J executable at path: " + executablePath);
        }
        
        final File pluginExecutableFile = new File(pluginDir, plugin.getName() + ".exe");
        try {
            FileUtils.copyURLToFile(url, pluginExecutableFile);
        } catch (final IOException e) {
            throw new IllegalStateException("Could not copy WinRun4J executable to: " + pluginExecutableFile.getAbsolutePath() + " " + e.getMessage(), e);
        }
        return pluginExecutableFile;
    }
    
    private File findJvmLocation(final File javaHome, final String... paths) {
        for (final String path : paths) {
            final File jvmLocation = new File(javaHome, path);
            if (jvmLocation.isFile()) {
                return jvmLocation;
            }
        }
        return null;
    }
    
    private File buildUnixPlugin(final ProtocPlugin plugin) {
        final File pluginExecutableFile = new File(pluginDir, plugin.getName());
        
        try (final PrintWriter out = new PrintWriter(new FileWriter(pluginExecutableFile))) {
            out.println("#!/bin/sh");
            out.println();
            out.print("CP=");
            boolean first = true;
            for (final String cp : classPath) {
                if (!first) {
                    out.print(":");
                } else {
                    first = false;
                }
                out.print("\"" + cp + "\"");
            }
            out.println();
            out.println("\"" + new File(javaHome, "bin/java").getAbsolutePath() + "\" -cp $CP " + plugin.getClass().getName());
            out.println();
        } catch (final IOException e) {
            throw new IllegalStateException("Could not write plugin script file: " + pluginExecutableFile, e);
        }
        
        return pluginExecutableFile;
    }
    
    private boolean archDirectoryExists(final String arch) {
        return javaHome != null
            && (new File(javaHome, "jre/lib/" + arch).isDirectory()
                || new File(javaHome, "lib/" + arch).isDirectory());
    }
    
}
