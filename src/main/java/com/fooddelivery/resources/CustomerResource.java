package com.fooddelivery.resources;

import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.CustomerNotFoundException;
import com.fooddelivery.persistence.model.Customer;
import com.fooddelivery.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/customers")
@Slf4j
@RequiredArgsConstructor
public class CustomerResource {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody Customer customer) {
        log.info("Resource: Creating customer with email: {}", customer.getEmail());
        Customer createdCustomer = customerService.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCustomer);
    }

    /**
     * Get customer by ID.
     * Allowed to: ADMIN (any customer) or CUSTOMER (own data only).
     * RBAC: Method-level security checks if customer is accessing their own data.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<Customer> getCustomerById(@PathVariable String id) {
        log.info("Resource: Fetching customer with ID: {}", id);
        
        // Check if customer is accessing their own data (unless admin)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin && !currentUserId.equals(id)) {
            log.warn("Customer {} attempted to access another customer's data: {}", currentUserId, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Exceptions will be handled by GlobalExceptionHandler
        Optional<Customer> customer = customerService.getCustomerById(id);
        if (customer.isEmpty()) {
            throw new CustomerNotFoundException("Customer not found with ID: " + id);
        }
        return ResponseEntity.ok(customer.get());
    }

    /**
     * Authentication endpoint for API Gateway.
     * Validates customer credentials (email + password) and returns user details.
     * This endpoint is unauthenticated (permitAll) - used by API Gateway during login flow.
     * 
     * Request body: { "email": "user@example.com", "password": "plaintextPassword" }
     * Response: { "id": "customer-id", "email": "user@example.com", "role": "CUSTOMER" }
     * 
     * Security: Generic error message to prevent email enumeration.
     */
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, String>> authenticate(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");
        
        log.info("Resource: Authentication attempt for email: {}", email);
        
        // Validate input
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("Login attempt with missing email or password");
            throw new BadRequestException("Email and password are required");
        }
        
        // Authenticate customer - exceptions will be handled by GlobalExceptionHandler
        Map<String, String> userDetails = customerService.authenticate(email, password);
        
        log.info("Resource: Authentication successful for email: {}", email);
        return ResponseEntity.ok(userDetails);
    }

    /**
     * Update customer details.
     * Allowed to: ADMIN (any customer) or CUSTOMER (own data only).
     * RBAC: Method-level security checks if customer is updating their own data.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<Customer> updateCustomer(@PathVariable String id, @RequestBody Customer customerUpdate) {
        log.info("Resource: Updating customer with ID: {}", id);
        
        // Check if customer is updating their own data (unless admin)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin && !currentUserId.equals(id)) {
            log.warn("Customer {} attempted to update another customer's data: {}", currentUserId, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Exceptions will be handled by GlobalExceptionHandler
        Customer updatedCustomer = customerService.updateCustomer(id, customerUpdate);
        return ResponseEntity.ok(updatedCustomer);
        }

    /**
     * Delete customer (Admin-only).
     * RBAC: Only ADMIN can delete customers.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        log.info("Resource: Deleting customer with ID: {}", id);
        // Exceptions will be handled by GlobalExceptionHandler
        boolean deleted = customerService.deleteCustomer(id);
        if (!deleted) {
            throw new CustomerNotFoundException("Customer not found with ID: " + id);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all customers with pagination (Admin-only endpoint).
     * RBAC: Only ADMIN role can view all customers.
     * 
     * Query parameters:
     * - page: Page number (0-indexed, default: 0)
     * - size: Number of customers per page (default: 20, max: 100)
     * 
     * Examples:
     * - GET /customers (uses defaults: page=0, size=20)
     * - GET /customers?page=0&size=20
     * - GET /customers?page=1&size=50
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Customer>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // Validate size to prevent excessive requests
        if (size > 100) {
            size = 100; // Cap at 100 per page
            log.warn("Requested page size exceeds maximum (100), capping to 100");
        }
        if (size < 1) {
            size = 20; // Minimum 1, default to 20
            log.warn("Invalid page size, defaulting to 20");
        }
        if (page < 0) {
            page = 0; // Minimum page is 0
            log.warn("Invalid page number, defaulting to 0");
        }
        
        log.info("Resource: Fetching customers - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Customer> customers = customerService.getAllCustomers(pageable);
        return ResponseEntity.ok(customers);
    }

    /**
     * Update customer role (Admin-only endpoint).
     * RBAC: Only ADMIN role can update customer roles.
     * Request body: { "role": "CUSTOMER" } or { "role": "ADMIN" }
     *
     * @param id Customer ID whose role is being updated
     * @param requestBody Request body containing new role (e.g., {"role": "ADMIN"})
     * @return Updated customer
     */
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Customer> updateCustomerRole(
            @PathVariable String id,
            @RequestBody Map<String, String> requestBody) {
        
        String newRole = requestBody.get("role");
        if (newRole == null || newRole.isBlank()) {
            log.warn("Role update request missing 'role' field for customer ID: {}", id);
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Resource: Updating role for customer ID: {} to role: {}", id, newRole);
        // Exceptions will be handled by GlobalExceptionHandler
        Customer updatedCustomer = customerService.updateCustomerRole(id, newRole);
        return ResponseEntity.ok(updatedCustomer);
    }
}
