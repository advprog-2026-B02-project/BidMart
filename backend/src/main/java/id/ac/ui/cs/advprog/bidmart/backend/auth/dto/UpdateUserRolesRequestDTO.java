package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class UpdateUserRolesRequestDTO {
    @NotEmpty
    public List<String> roles;
}
