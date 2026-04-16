package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import java.util.List;

public class PartialLoginResponseDTO {
    public String partialToken;
    public boolean requires2FA;
    public List<String> methods;
    public long expiresIn;

    public PartialLoginResponseDTO(String partialToken, boolean requires2FA, List<String> methods, long expiresIn) {
        this.partialToken = partialToken;
        this.requires2FA = requires2FA;
        this.methods = methods;
        this.expiresIn = expiresIn;
    }
}
