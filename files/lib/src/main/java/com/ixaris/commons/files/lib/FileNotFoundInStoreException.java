package com.ixaris.commons.files.lib;

public final class FileNotFoundInStoreException extends RuntimeException {
    
    public FileNotFoundInStoreException(final String fileName, final Throwable cause) {
        super(String.format("File %s not found", fileName), cause);
    }
    
}
