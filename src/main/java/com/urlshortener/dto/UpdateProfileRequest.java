package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update profile request payload")
public class UpdateProfileRequest {

    @Schema(description = "Updated first name", example = "John")
    private String firstName;

    @Schema(description = "Updated last name", example = "Doe")
    private String lastName;
}
