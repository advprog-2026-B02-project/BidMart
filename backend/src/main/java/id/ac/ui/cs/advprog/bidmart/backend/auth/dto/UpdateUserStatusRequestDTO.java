package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateUserStatusRequestDTO {
    @NotBlank
    public String status;

    public String reason;
}
