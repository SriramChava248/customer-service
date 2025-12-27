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
import java.util.ArrayList;
import java.util.List;

/**
 * Customer entity representing a customer in the system.
 * Stored in Couchbase as a document.
 */
@Document
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    private String id;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Field
    private String email;

    @Field
    private String password; // Hashed password (BCrypt)

    @Field
    @Builder.Default
    private String role = "CUSTOMER"; // CUSTOMER or ADMIN

    @Field
    private String firstName;

    @Field
    private String lastName;

    @Field
    private String phone;

    @Field
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    @Field
    @Builder.Default
    private List<String> favoriteRestaurants = new ArrayList<>();

    @Field
    private Instant createdAt;

    @Field
    private Instant updatedAt;
}
