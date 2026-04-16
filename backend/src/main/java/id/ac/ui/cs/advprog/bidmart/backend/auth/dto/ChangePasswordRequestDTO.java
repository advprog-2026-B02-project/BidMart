package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequestDTO {
    @NotBlank
    public String currentPassword;

    @NotBlank
    @Size(min = 8, max = 50)
    public String newPassword;
}
