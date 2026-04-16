package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

public class LoginSuccessResponseDTO {
    public String accessToken;
    public String refreshToken;
    public long expiresIn;
    public UserResponseDTO user;

    public LoginSuccessResponseDTO(String accessToken, String refreshToken, long expiresIn, UserResponseDTO user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }
}
