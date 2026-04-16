package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class TwoFactorVerifyRequestDTO {
    @NotBlank
    public String partialToken;

    @NotBlank
    public String method;

    @NotBlank
    public String code;
}
