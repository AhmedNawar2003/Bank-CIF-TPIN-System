package com.bank.ciftpin.controller;

import com.bank.ciftpin.dto.*;
import com.bank.ciftpin.security.JwtService;
import com.bank.ciftpin.service.CifTpinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/cif")
@RequiredArgsConstructor
@Tag(name = "CIF & TPIN Management", description = "APIs for managing Bank CIF accounts and TPIN")
public class CifTpinController {

    private final CifTpinService cifTpinService;
    private final JwtService jwtService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new CIF",
            description = "Creates a new CIF account in PENDING_TPIN status. TPIN must be set separately."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "CIF registered successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "CIF or email already exists")
    })
    public ResponseEntity<ApiResponse<CifAccountResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("[API] POST /register - CIF: {}", request.getCifNumber());
        CifAccountResponse response = cifTpinService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CIF registered successfully. Please set your TPIN.", response));
    }

    @PostMapping("/set-tpin")
    @Operation(
            summary = "Set TPIN for a new CIF",
            description = "Sets the TPIN for a CIF in PENDING_TPIN status. Activates the account."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "TPIN set successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "TPIN already set or invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "CIF is blocked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "CIF not found")
    })
    public ResponseEntity<ApiResponse<CifAccountResponse>> setTpin(
            @Valid @RequestBody SetTpinRequest request) {
        log.info("[API] POST /set-tpin - CIF: {}", request.getCifNumber());
        CifAccountResponse response = cifTpinService.setTpin(request);
        return ResponseEntity.ok(ApiResponse.success("TPIN set successfully. Account is now active.", response));
    }

    @PostMapping("/authenticate")
    @Operation(
            summary = "Authenticate CIF with TPIN",
            description = "Authenticates a CIF using its TPIN. Returns a JWT Bearer token on success. Three consecutive failures will block the CIF."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Authentication successful — JWT token returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "TPIN not set"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Invalid TPIN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "CIF is blocked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "CIF not found")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(
            @Valid @RequestBody AuthRequest request) {
        log.info("[API] POST /authenticate - CIF: {}", request.getCifNumber());
        CifAccountResponse account = cifTpinService.authenticate(request);
        String token = jwtService.generateToken(account.getCifNumber());
        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400)
                .account(account)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Authentication successful.", authResponse));
    }

    @PostMapping("/reset-tpin")
    @Operation(
            summary = "Reset TPIN",
            description = "Unblocks a CIF (if blocked) and sets a new TPIN. Also works for non-blocked CIFs."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "TPIN reset successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Invalid request or no TPIN was previously set"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "CIF not found")
    })
    public ResponseEntity<ApiResponse<CifAccountResponse>> resetTpin(
            @Valid @RequestBody ResetTpinRequest request) {
        log.info("[API] POST /reset-tpin - CIF: {}", request.getCifNumber());
        CifAccountResponse response = cifTpinService.resetTpin(request);
        return ResponseEntity.ok(ApiResponse.success("TPIN reset successfully. Account is now active.", response));
    }
}