package com.ixaris.commons.files.lib;

/**
 * Represents a file object persisted by the backing data handler.
 */
public final class FileObject {
    
    private final String name;
    private final boolean isFolder;
    
    public FileObject(final String name, final boolean isFolder) {
        this.name = name;
        this.isFolder = isFolder;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isFile() {
        return !isFolder;
    }
    
    public boolean isFolder() {
        return isFolder;
    }
    
}
