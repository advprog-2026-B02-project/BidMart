package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email tidak valid")
    public String email;

    @NotBlank(message = "Password tidak boleh kosong")
    @Size(min = 8, max = 50, message = "Password minimal 8 karakter dan maximal 50 karakter")
    public String password;

    @NotBlank(message = "Nama tampilan tidak boleh kosong")
    @Size(max = 100, message = "Nama tampilan maksimal 100 karakter")
    public String displayName;
}