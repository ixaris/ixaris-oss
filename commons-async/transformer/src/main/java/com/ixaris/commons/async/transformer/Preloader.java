package com.ixaris.commons.async.transformer;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Preloader {
    
    public static void preloadClassesInPackage(final ClassLoader classLoader, final String pkgName) {
        try {
            String pkgPath = pkgName.replace('.', '/');
            final Enumeration<URL> resources = classLoader.getResources(pkgPath);
            
            for (URL url; resources.hasMoreElements() && ((url = resources.nextElement()) != null);) {
                final URLConnection connection = url.openConnection();
                
                if (connection instanceof JarURLConnection) {
                    checkJarFile((JarURLConnection) connection, pkgPath);
                }
            }
        } catch (final ClassNotFoundException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private static void checkJarFile(final JarURLConnection connection, final String pkgPath) throws ClassNotFoundException, IOException {
        final JarFile jarFile = connection.getJarFile();
        final Enumeration<JarEntry> entries = jarFile.entries();
        for (JarEntry jarEntry; entries.hasMoreElements() && ((jarEntry = entries.nextElement()) != null);) {
            String name = jarEntry.getName();
            if (name.contains(".class") && name.startsWith(pkgPath)) {
                name = name.substring(0, name.length() - 6).replace('/', '.');
                Class.forName(name);
            }
        }
    }
    
}
