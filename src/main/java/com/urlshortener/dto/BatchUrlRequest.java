package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for batch URL creation")
public class BatchUrlRequest {

    @NotEmpty(message = "URLs list cannot be empty")
    @Size(max = 100, message = "Maximum 100 URLs can be created at once")
    @Valid
    @Schema(description = "List of URLs to create (max 100)")
    private List<UrlMappingRequest> urls;
}
