package id.ac.ui.cs.advprog.bidmart.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @Email
    @NotBlank
    public String email;

    @NotBlank
    @Size(min = 8, max = 72)
    public String password;
}