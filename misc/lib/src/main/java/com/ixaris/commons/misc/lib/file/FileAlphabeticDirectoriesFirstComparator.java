package com.ixaris.commons.misc.lib.file;

import java.io.File;
import java.util.Comparator;

public final class FileAlphabeticDirectoriesFirstComparator implements Comparator<File> {
    
    private static final FileAlphabeticDirectoriesFirstComparator INSTANCE = new FileAlphabeticDirectoriesFirstComparator();
    
    public static FileAlphabeticDirectoriesFirstComparator getInstance() {
        return INSTANCE;
    }
    
    private FileAlphabeticDirectoriesFirstComparator() {}
    
    // Comparator interface requires defining compare method.
    public int compare(final File filea, final File fileb) {
        
        // ... Sort directories before files,
        // otherwise alphabetical (case sensitive)
        if (filea.isDirectory() && !fileb.isDirectory()) {
            return -1;
            
        } else if (!filea.isDirectory() && fileb.isDirectory()) {
            return 1;
            
        } else {
            return filea.getName().compareTo(fileb.getName());
        }
    }
}
