package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotBlank(message = "Nama tampilan tidak boleh kosong")
    @Size(max = 100, message = "Nama tampilan maksimal 100 karakter")
    public String displayName;

    public String avatarUrl;
}