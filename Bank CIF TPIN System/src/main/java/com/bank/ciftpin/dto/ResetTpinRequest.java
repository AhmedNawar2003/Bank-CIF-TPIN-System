package com.bank.ciftpin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Request payload for resetting TPIN and unblocking a CIF")
public class ResetTpinRequest {

    @NotBlank(message = "CIF number is required")
    @Schema(description = "The blocked CIF number", example = "123456789")
    private String cifNumber;

    @NotBlank(message = "New TPIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "TPIN must be 4-6 digits")
    @Schema(description = "New 4-6 digit TPIN", example = "5678")
    private String newTpin;
}