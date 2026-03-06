package id.ac.ui.cs.advprog.bidmart.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshRequest {
    @NotBlank
    public String refreshToken;
}