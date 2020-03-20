package com.ixaris.commons.multitenancy.lib.object;

/**
 * @author benjie.gatt
 */
public class DataHolder {
    
    private String data;
    
    public DataHolder(final String data) {
        this.data = data;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(final String data) {
        this.data = data;
    }
    
}
