package com.bank.ciftpin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request payload for registering a new CIF")
public class RegisterRequest {

    @NotBlank(message = "CIF number is required")
    @Pattern(regexp = "^[0-9]{6,12}$", message = "CIF number must be 6-12 digits")
    @Schema(description = "Unique CIF number (6-12 digits)", example = "123456789")
    private String cifNumber;

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    @Schema(description = "Account holder full name", example = "Ahmed Nawar")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Account holder email address", example = "ahmed.nawar@bank.com")
    private String email;
}