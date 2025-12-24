package com.fooddelivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;

/**
 * Couchbase configuration class.
 * Configures connection to Couchbase cluster and enables repository support.
 */
@Configuration
@EnableCouchbaseRepositories(basePackages = "com.fooddelivery.persistence")
public class CouchbaseConfig extends AbstractCouchbaseConfiguration {

    @Value("${spring.couchbase.connection-string:couchbase://localhost}")
    private String connectionString;

    @Value("${spring.couchbase.username:Administrator}")
    private String username;

    @Value("${spring.couchbase.password:password}")
    private String password;

    @Value("${spring.data.couchbase.bucket-name:customer-data}")
    private String bucketName;

    @Override
    public String getConnectionString() {
        return connectionString;
    }

    @Override
    public String getUserName() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }
}

