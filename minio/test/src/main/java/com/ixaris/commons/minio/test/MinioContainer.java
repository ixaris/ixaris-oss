package com.ixaris.commons.minio.test;

import com.ixaris.commons.misc.lib.net.Localhost;

public final class MinioContainer {//extends GenericContainer<MinioContainer> {
    
    public static final String ACCESS_KEY = "s3-access-key";
    public static final String SECRET_KEY = "s3-secret";
    
    private static final int PORT = Localhost.findAvailableTcpPort(8001);
    
    public String getHost() {
        return "localhost";
    }
    
    public int getFirstMappedPort() {
        return PORT;
    }
    
    //    public MinioContainer() {
    //        super("minio/minio:RELEASE.2020-01-25T02-50-51Z");
    //        setCommand("server", "/export");
    //        addEnv("MINIO_ACCESS_KEY", ACCESS_KEY);
    //        addEnv("MINIO_SECRET_KEY", SECRET_KEY);
    //        addExposedPort(9000);
    //    }
    
}
