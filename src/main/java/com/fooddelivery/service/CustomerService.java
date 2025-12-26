package com.fooddelivery.service;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.IncrementOptions;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.CustomerNotFoundException;
import com.fooddelivery.exception.DatabaseException;
import com.fooddelivery.exception.InternalServerException;
import com.fooddelivery.persistence.CustomerRepository;
import com.fooddelivery.persistence.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Customer service layer containing business logic for customer operations.
 * Calls repository and external service APIs in single line function calls.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CouchbaseTemplate couchbaseTemplate;
    private final PasswordEncoder passwordEncoder; // Injected from SecurityConfig bean
    
    private static final String CUSTOMER_COUNTER_KEY = "customer-counter";
    private static final long INITIAL_COUNTER_VALUE = 1L; // Start from 1
    private static final String DEFAULT_ROLE = "CUSTOMER"; // Default role for new customers

    /**
     * Generate a unique numeric customer ID using simple incrementing counter.
     * Format: Pure numeric ID (1, 2, 3, 4, ...)
     * 
     * Uses Couchbase atomic increment operation to ensure uniqueness across all instances.
     * Counter is persisted in Couchbase, so it survives service restarts.
     *
     * @return Generated numeric customer ID as String
     */
    private String generateCustomerId() {
        try {
            // Get Couchbase collection for atomic operations
            Collection collection = couchbaseTemplate.getCouchbaseClientFactory()
                    .getBucket()
                    .defaultCollection();
            
            // Atomic increment - thread-safe and distributed-safe
            // If counter doesn't exist, creates it with initial value
            IncrementOptions options = IncrementOptions.incrementOptions()
                    .initial(INITIAL_COUNTER_VALUE)
                    .delta(1);
            
            long nextId = collection.binary()
                    .increment(CUSTOMER_COUNTER_KEY, options)
                    .content();
            
            String customerId = String.valueOf(nextId);
            log.debug("Generated numeric customer ID: {}", customerId);
            return customerId;
        } catch (Exception e) {
            log.error("Error generating customer ID using Couchbase counter", e);
            throw new DatabaseException("Failed to generate customer ID", e);
        }
    }

    /**
     * Create a new customer.
     * Always generates a unique ID server-side (ignores any ID provided by client).
     * Sets timestamps and saves to database.
     *
     * @param customer Customer entity to create (ID will always be generated, client-provided ID is ignored)
     * @return Created customer with generated ID and timestamps set
     */
    public Customer createCustomer(Customer customer) {
        log.info("Creating customer with email: {}", customer.getEmail());
        
        // Always generate ID server-side - ignore any ID provided by client
        // This ensures uniqueness and prevents ID collision attacks
        if (customer.getId() != null && !customer.getId().isBlank()) {
            log.warn("Client provided ID '{}' will be ignored. Server will generate unique ID.", customer.getId());
        }
        
        // Always generate new unique ID (generateCustomerId has its own try-catch)
        customer.setId(generateCustomerId());
        log.debug("Generated customer ID: {}", customer.getId());
        
        // Hash password before storing (never store plaintext)
        if (customer.getPassword() != null && !customer.getPassword().isBlank()) {
            String hashedPassword = passwordEncoder.encode(customer.getPassword());
            customer.setPassword(hashedPassword);
            log.debug("Password hashed for customer: {}", customer.getId());
        } else {
            log.warn("No password provided for customer creation");
            throw new BadRequestException("Password is required for customer creation");
        }
        
        // Always set role to CUSTOMER for new customers (never accept role from client)
        // Role can only be changed later via admin-only updateRole API
        if (customer.getRole() != null && !customer.getRole().isBlank()) {
            log.warn("Client provided role '{}' will be ignored. All new customers default to '{}'.", 
                    customer.getRole(), DEFAULT_ROLE);
        }
        customer.setRole(DEFAULT_ROLE);
        log.debug("Role set to '{}' for new customer: {}", DEFAULT_ROLE, customer.getId());
        
        Instant now = Instant.now();
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        
        // Initialize empty lists for new customers if not provided
        if (customer.getAddresses() == null) {
            customer.setAddresses(new ArrayList<>());
        }
        if (customer.getFavoriteRestaurants() == null) {
            customer.setFavoriteRestaurants(new ArrayList<>());
        }
        
        // Save customer with try-catch for repository operation
        Customer savedCustomer;
        try {
            savedCustomer = customerRepository.save(customer);
            log.info("Customer created with ID: {}", savedCustomer.getId());
        } catch (Exception e) {
            log.error("Error saving customer with email: {}", customer.getEmail(), e);
            throw new DatabaseException("Failed to create customer: " + e.getMessage(), e);
        }
        
        return savedCustomer;
    }

    /**
     * Get customer by ID.
     *
     * @param id Customer ID
     * @return Optional containing customer if found
     */
    public Optional<Customer> getCustomerById(String id) {
        log.debug("Fetching customer with ID: {}", id);
        try {
            return customerRepository.findById(id);
        } catch (Exception e) {
            log.error("Error fetching customer with ID: {}", id, e);
            throw new DatabaseException("Failed to fetch customer: " + e.getMessage(), e);
        }
    }

    /**
     * Get customer by email.
     *
     * @param email Customer email
     * @return Optional containing customer if found
     */
    public Optional<Customer> getCustomerByEmail(String email) {
        log.debug("Fetching customer with email: {}", email);
        try {
            return customerRepository.findByEmail(email);
        } catch (Exception e) {
            log.error("Error fetching customer with email: {}", email, e);
            throw new DatabaseException("Failed to fetch customer by email: " + e.getMessage(), e);
        }
    }

    /**
     * Check if customer exists by email.
     *
     * @param email Customer email
     * @return true if customer exists, false otherwise
     */
    public boolean customerExistsByEmail(String email) {
        log.debug("Checking if customer exists with email: {}", email);
        try {
            return customerRepository.existsByEmail(email);
        } catch (Exception e) {
            log.error("Error checking if customer exists with email: {}", email, e);
            throw new DatabaseException("Failed to check customer existence: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing customer with partial update support.
     * Only updates fields that are provided (non-null), preserving existing values for others.
     * Updates timestamp and saves to database.
     * Note: ID in request body is ignored - ID comes from path parameter only.
     *
     * @param customerId Customer ID to update (from path parameter)
     * @param customerUpdate Customer entity with fields to update (only non-null fields will be updated)
     * @return Updated customer
     * @throws CustomerNotFoundException if customer not found
     * @throws DatabaseException if database operation fails
     */
    public Customer updateCustomer(String customerId, Customer customerUpdate) {
        log.info("Updating customer with ID: {}", customerId);
        
        // Ignore any ID provided in request body - use path parameter ID only
        if (customerUpdate.getId() != null && !customerUpdate.getId().equals(customerId)) {
            log.warn("ID '{}' in request body does not match path parameter '{}'. Path parameter ID will be used.", 
                    customerUpdate.getId(), customerId);
        }
        
        // Get existing customer with try-catch for internal API call
        Optional<Customer> existingCustomerOpt;
        try {
            existingCustomerOpt = customerRepository.findById(customerId);
        } catch (Exception e) {
            log.error("Error fetching customer with ID: {} during update operation", customerId, e);
            throw new DatabaseException("Failed to fetch customer for update: " + e.getMessage(), e);
        }
        
        if (existingCustomerOpt.isEmpty()) {
            log.error("Customer not found with ID: {}", customerId);
            throw new CustomerNotFoundException("Customer not found with ID: " + customerId);
        }
        
        Customer existingCustomer = existingCustomerOpt.get();
        
        // Partial update: only update fields that are provided (non-null)
        // Email
        if (customerUpdate.getEmail() != null && !customerUpdate.getEmail().isBlank()) {
            existingCustomer.setEmail(customerUpdate.getEmail());
            log.debug("Updating email for customer: {}", customerId);
        }
        
        // First name
        if (customerUpdate.getFirstName() != null && !customerUpdate.getFirstName().isBlank()) {
            existingCustomer.setFirstName(customerUpdate.getFirstName());
            log.debug("Updating firstName for customer: {}", customerId);
        }
        
        // Last name
        if (customerUpdate.getLastName() != null && !customerUpdate.getLastName().isBlank()) {
            existingCustomer.setLastName(customerUpdate.getLastName());
            log.debug("Updating lastName for customer: {}", customerId);
        }
        
        // Phone (can be null, so check if explicitly provided)
        if (customerUpdate.getPhone() != null) {
            existingCustomer.setPhone(customerUpdate.getPhone());
            log.debug("Updating phone for customer: {}", customerId);
        }
        
        // Password (only update if provided - hash it before storing)
        if (customerUpdate.getPassword() != null && !customerUpdate.getPassword().isBlank()) {
            String hashedPassword = passwordEncoder.encode(customerUpdate.getPassword());
            existingCustomer.setPassword(hashedPassword);
            log.debug("Password updated for customer: {}", customerId);
        }
        
        // Role update is NOT allowed in regular updateCustomer API
        // Role can only be updated via separate updateRole API (admin-only)
        if (customerUpdate.getRole() != null && !customerUpdate.getRole().equals(existingCustomer.getRole())) {
            log.warn("Role update attempted via updateCustomer API for customer: {}. Role updates are not allowed here. Use updateRole API instead.", customerId);
            // Do NOT update role - ignore the role field in updateCustomer
        }
        
        // Addresses (only update if explicitly provided - null means don't update, empty list means clear)
        if (customerUpdate.getAddresses() != null) {
            existingCustomer.setAddresses(customerUpdate.getAddresses());
            log.debug("Updating addresses for customer: {} (provided {} addresses)", 
                    customerId, customerUpdate.getAddresses().size());
        }
        
        // Favorite restaurants (only update if explicitly provided - null means don't update, empty list means clear)
        if (customerUpdate.getFavoriteRestaurants() != null) {
            existingCustomer.setFavoriteRestaurants(customerUpdate.getFavoriteRestaurants());
            log.debug("Updating favoriteRestaurants for customer: {} (provided {} favorites)", 
                    customerId, customerUpdate.getFavoriteRestaurants().size());
        }
        
        // Preserve createdAt timestamp, update updatedAt
        existingCustomer.setUpdatedAt(Instant.now());
        
        // Save updated customer with try-catch for repository operation
        Customer updatedCustomer;
        try {
            updatedCustomer = customerRepository.save(existingCustomer);
            log.info("Customer updated with ID: {}", updatedCustomer.getId());
        } catch (Exception e) {
            log.error("Error saving updated customer with ID: {}", customerId, e);
            throw new DatabaseException("Failed to save customer update: " + e.getMessage(), e);
        }
        
        return updatedCustomer;
    }

    /**
     * Delete customer by ID.
     *
     * @param id Customer ID
     * @return true if customer was deleted, false if not found
     */
    public boolean deleteCustomer(String id) {
        log.info("Deleting customer with ID: {}", id);
        
        // Check if customer exists with try-catch for internal API call
        boolean exists;
        try {
            exists = customerRepository.existsById(id);
        } catch (Exception e) {
            log.error("Error checking if customer exists with ID: {} during delete operation", id, e);
            throw new DatabaseException("Failed to check customer existence for delete: " + e.getMessage(), e);
        }
        
        if (!exists) {
            log.warn("Customer not found with ID: {}", id);
            return false;
        }
        
        try {
            customerRepository.deleteById(id);
            log.info("Customer deleted with ID: {}", id);
            return true;
        } catch (Exception e) {
            log.error("Error deleting customer with ID: {}", id, e);
            throw new DatabaseException("Failed to delete customer: " + e.getMessage(), e);
        }
    }

    /**
     * Get all customers.
     *
     * @return List of all customers
     */
    public List<Customer> getAllCustomers() {
        log.debug("Fetching all customers");
        try {
            return customerRepository.findAll();
        } catch (Exception e) {
            log.error("Error fetching all customers", e);
            throw new DatabaseException("Failed to fetch all customers: " + e.getMessage(), e);
        }
    }

    /**
     * Update customer role (Admin-only operation).
     * This is a separate method from updateCustomer to restrict role updates to admins only.
     * Regular customers cannot update their own role or anyone else's role via the regular updateCustomer API.
     *
     * @param customerId Customer ID whose role is being updated
     * @param newRole New role to assign (CUSTOMER or ADMIN)
     * @return Updated customer with new role
     * @throws CustomerNotFoundException if customer not found
     * @throws BadRequestException if role is invalid
     * @throws DatabaseException if database operation fails
     */
    public Customer updateCustomerRole(String customerId, String newRole) {
        log.info("Updating role for customer ID: {} to role: {}", customerId, newRole);
        
        // Validate role
        if (newRole == null || newRole.isBlank()) {
            throw new BadRequestException("Role cannot be null or empty");
        }
        
        // Validate role value (only CUSTOMER and ADMIN are allowed)
        List<String> validRoles = List.of("CUSTOMER", "ADMIN");
        String upperCaseRole = newRole.toUpperCase();
        if (!validRoles.contains(upperCaseRole)) {
            throw new BadRequestException("Invalid role: " + newRole + ". Valid roles: " + validRoles);
        }
        
        // Get existing customer
        Optional<Customer> existingCustomerOpt;
        try {
            existingCustomerOpt = customerRepository.findById(customerId);
        } catch (Exception e) {
            log.error("Error fetching customer with ID: {} during role update", customerId, e);
            throw new DatabaseException("Failed to fetch customer for role update: " + e.getMessage(), e);
        }
        
        if (existingCustomerOpt.isEmpty()) {
            log.error("Customer not found with ID: {}", customerId);
            throw new CustomerNotFoundException("Customer not found with ID: " + customerId);
        }
        
        Customer existingCustomer = existingCustomerOpt.get();
        
        // Update role
        existingCustomer.setRole(upperCaseRole);
        existingCustomer.setUpdatedAt(Instant.now());
        
        // Save updated customer
        Customer updatedCustomer;
        try {
            updatedCustomer = customerRepository.save(existingCustomer);
            log.info("Role updated for customer ID: {} to role: {}", updatedCustomer.getId(), updatedCustomer.getRole());
        } catch (Exception e) {
            log.error("Error saving role update for customer ID: {}", customerId, e);
            throw new DatabaseException("Failed to save role update: " + e.getMessage(), e);
        }
        
        return updatedCustomer;
    }
}
