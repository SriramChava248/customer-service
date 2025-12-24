package com.fooddelivery.persistence;

import com.fooddelivery.persistence.model.Customer;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Customer repository interface for Couchbase database operations.
 * Extends CouchbaseRepository to get standard CRUD operations.
 * Spring Data Couchbase automatically implements this interface.
 */
@Repository
public interface CustomerRepository extends CouchbaseRepository<Customer, String> {

    /**
     * Find customer by email address.
     * Spring Data automatically implements this method based on method name.
     * 
     * @param email Customer email address
     * @return Optional containing Customer if found, empty otherwise
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Check if customer exists with given email.
     * 
     * @param email Customer email address
     * @return true if customer exists, false otherwise
     */
    boolean existsByEmail(String email);
}

