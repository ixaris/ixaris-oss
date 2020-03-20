package com.ixaris.commons.microservices.lib.common;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.Tuple2;

public final class ServicePathHolder implements Iterable<String> {
    
    public static ServicePathHolder parse(final String path) {
        return of(Arrays.asList(path.split("/", -1)));
    }
    
    public static ServicePathHolder of(final List<String> path) {
        if (path.isEmpty()) {
            return EMPTY;
        } else {
            final String[] p = new String[path.size()];
            for (int i = 0; i < p.length; i++) {
                p[i] = path.get(i);
            }
            return new ServicePathHolder(p);
        }
    }
    
    public static ServicePathHolder of(final List<String> path, final List<String> params) {
        if (path.isEmpty()) {
            return EMPTY;
        } else {
            final String[] p = new String[path.size()];
            int paramIndex = 0;
            for (int i = 0; i < p.length; i++) {
                final String pathPart = path.get(i);
                p[i] = "_".equals(pathPart) ? params.get(paramIndex++) : pathPart;
            }
            return new ServicePathHolder(p);
        }
    }
    
    private static final String[] EMPTY_PATH = new String[0];
    private static final ServicePathHolder EMPTY = new ServicePathHolder(EMPTY_PATH);
    
    public static ServicePathHolder empty() {
        return EMPTY;
    }
    
    private final String[] path;
    
    private ServicePathHolder(final String[] path) {
        this.path = path;
    }
    
    public boolean isEmpty() {
        return path.length == 0;
    }
    
    public int size() {
        return path.length;
    }
    
    public String get(final int index) {
        return path[index];
    }
    
    public String getFirst() {
        return path.length == 0 ? null : path[0];
    }
    
    public String getLast() {
        return path.length == 0 ? null : path[path.length - 1];
    }
    
    public ServicePathHolder push(final String part) {
        if (part == null) {
            throw new IllegalArgumentException("part is null");
        }
        final String[] newPath;
        if (path.length > 0) {
            newPath = new String[path.length + 1];
            System.arraycopy(path, 0, newPath, 0, path.length);
            newPath[path.length] = part;
        } else {
            newPath = new String[] { part };
        }
        return new ServicePathHolder(newPath);
    }
    
    public Tuple2<String, ServicePathHolder> pop() {
        if (path.length == 0) {
            throw new IllegalStateException("Cannot pop from empty path");
        }
        final String[] newPath = new String[path.length - 1];
        System.arraycopy(path, 0, newPath, 0, path.length - 1);
        return tuple(path[path.length - 1], new ServicePathHolder(newPath));
    }
    
    /**
     * Return a new path with the last segment replaced if it is _. Optimisation over pop() and push() to avoid creating
     * the nacking array twice
     */
    public ServicePathHolder replaceLastSegment(final String part) {
        if ((path.length < 1) || !path[path.length - 1].equals("_")) {
            throw new IllegalStateException("Path does not have last part _ for replacement");
        }
        final String[] newPath = new String[path.length];
        System.arraycopy(path, 0, newPath, 0, path.length - 1);
        newPath[path.length - 1] = part;
        return new ServicePathHolder(newPath);
    }
    
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            
            private int index = 0;
            
            @Override
            public boolean hasNext() {
                return index < path.length;
            }
            
            @Override
            public String next() {
                if (index < path.length) {
                    return path[index++];
                } else {
                    throw new NoSuchElementException();
                }
            }
            
        };
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> Arrays.equals(path, other.path));
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(path);
    }
    
    @Override
    public String toString() {
        return String.join("/", path);
    }
    
}
