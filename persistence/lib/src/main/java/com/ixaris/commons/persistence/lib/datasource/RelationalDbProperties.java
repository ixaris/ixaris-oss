package com.ixaris.commons.persistence.lib.datasource;

public class RelationalDbProperties {
    
    private String url;
    
    private String user;
    
    private String password;
    
    public RelationalDbProperties() {
        // used by Spring Cloud Config
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(final String url) {
        this.url = url;
    }
    
    public String getUser() {
        return user;
    }
    
    public void setUser(final String user) {
        this.user = user;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(final String password) {
        this.password = password;
    }
    
}
