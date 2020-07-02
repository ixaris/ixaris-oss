package com.ixaris.commons.persistence.lib.datasource;

public final class MultiTenancyDataSourceDefaultsConfig {
    
    private RelationalDbProperties mysql;
    
    public RelationalDbProperties getMysql() {
        return mysql;
    }
    
    public void setMysql(RelationalDbProperties mysql) {
        this.mysql = mysql;
    }
    
    @Deprecated
    public void setDbUrl(final String url) {
        if (mysql == null) {
            mysql = new RelationalDbProperties();
        }
        mysql.setUrl(url);
    }
    
    @Deprecated
    public void setDbUser(final String user) {
        if (mysql == null) {
            mysql = new RelationalDbProperties();
        }
        mysql.setUser(user);
    }
    
    @Deprecated
    public void setDbPassword(final String password) {
        if (mysql == null) {
            mysql = new RelationalDbProperties();
        }
        mysql.setPassword(password);
    }
    
}
