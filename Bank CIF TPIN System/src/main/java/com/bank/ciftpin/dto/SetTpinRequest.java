package com.bank.ciftpin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Request payload for setting a TPIN")
public class SetTpinRequest {

    @NotBlank(message = "CIF number is required")
    @Schema(description = "The CIF number to set TPIN for", example = "123456789")
    private String cifNumber;

    @NotBlank(message = "TPIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "TPIN must be 4-6 digits")
    @Schema(description = "4-6 digit numeric TPIN", example = "1234")
    private String tpin;
}