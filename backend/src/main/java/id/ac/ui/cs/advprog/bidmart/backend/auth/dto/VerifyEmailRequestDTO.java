package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyEmailRequestDTO {
    @NotBlank
    public String token;
}
