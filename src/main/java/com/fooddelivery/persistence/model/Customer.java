package com.fooddelivery.persistence.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * Customer entity representing a customer in the system.
 * Stored in Couchbase with document ID: customer::{customerId}
 */
@Document
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    private String id; // ID is optional for create (will be generated), required for update

    @Field
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Field
    @NotBlank(message = "First name is required")
    private String firstName;

    @Field
    @NotBlank(message = "Last name is required")
    private String lastName;

    @Field
    private String phone;

    @Field
    private String password; // Hashed password (BCrypt) - never store plaintext
    // Note: Password validation is handled in service layer (required for create, optional for update)

    @Field
    private String role; // CUSTOMER, ADMIN, RESTAURANT_OWNER, DRIVER
    // Note: Role validation is handled in service layer (defaults to CUSTOMER for new customers)

    @Field
    private List<Address> addresses; // Null by default - only set when explicitly provided

    @Field
    private List<String> favoriteRestaurants; // Null by default - only set when explicitly provided

    @Field
    private Instant createdAt;

    @Field
    private Instant updatedAt;
}
