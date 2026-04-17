package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class RoleRequestDTO {
    @NotBlank
    public String name;

    @NotEmpty
    public List<String> permissions;
}
