package com.fooddelivery.persistence.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Location entity representing geographic coordinates.
 * Used for address geolocation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @NotNull(message = "Latitude is required")
    private Double lat;

    @NotNull(message = "Longitude is required")
    private Double lon;
}
