package com.urlshortener.controller;

import com.urlshortener.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/qr")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "QR Code", description = "Generate QR codes for short URLs")
public class QrCodeController {

    private final QrCodeService qrCodeService;

    @Operation(summary = "Generate QR code", description = "Returns a QR code PNG image for the specified short code. The QR encodes the full short URL. Results are cached in Redis.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "QR code image generated",
                    content = @Content(mediaType = "image/png")),
            @ApiResponse(responseCode = "404", description = "Short code not found"),
            @ApiResponse(responseCode = "500", description = "QR code generation failed")
    })
    @GetMapping(value = "/{shortCode}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQrCode(
            @Parameter(description = "Short code to generate QR for", example = "abc1234")
            @PathVariable String shortCode
    ) {
        log.debug("QR code requested for: {}", shortCode);

        byte[] qrBytes = qrCodeService.generateQrCode(shortCode);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(qrBytes.length)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(qrBytes);
    }
}
