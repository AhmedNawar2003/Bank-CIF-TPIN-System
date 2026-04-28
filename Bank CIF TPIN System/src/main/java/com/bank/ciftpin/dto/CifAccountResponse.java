package com.bank.ciftpin.dto;

import com.bank.ciftpin.enums.CifStatus;
import com.bank.ciftpin.model.CifAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "CIF account information response")
public class CifAccountResponse {

    @Schema(description = "CIF number", example = "123456789")
    private String cifNumber;

    @Schema(description = "Account holder full name", example = "Ahmed Nawar")
    private String fullName;

    @Schema(description = "Account holder email", example = "ahmed.nawar@bank.com")
    private String email;

    @Schema(description = "Current account status", example = "ACTIVE")
    private CifStatus status;

    @Schema(description = "Whether TPIN is configured")
    private boolean tpinConfigured;

    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;

    public static CifAccountResponse from(CifAccount account) {
        return CifAccountResponse.builder()
                .cifNumber(account.getCifNumber())
                .fullName(account.getFullName())
                .email(account.getEmail())
                .status(account.getStatus())
                .tpinConfigured(account.getTpinHash() != null)
                .createdAt(account.getCreatedAt())
                .build();
    }
}