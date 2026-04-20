package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AuthDtoTest {

    @Test
    void constructorDtos() {
        AuthResponse authResponse = new AuthResponse("a", "r");
        assertEquals("a", authResponse.accessToken);
        assertEquals("r", authResponse.refreshToken);

        UserResponseDTO user = new UserResponseDTO(UUID.randomUUID(), "a@b.com", "name", true, Instant.now(), List.of("BUYER"));
        LoginSuccessResponseDTO loginSuccess = new LoginSuccessResponseDTO("at", "rt", 60L, user);
        assertEquals("at", loginSuccess.accessToken);
        assertEquals("rt", loginSuccess.refreshToken);
        assertEquals(60L, loginSuccess.expiresIn);
        assertEquals("a@b.com", loginSuccess.user.email);

        RoleResponseDTO roleResponse = new RoleResponseDTO(UUID.randomUUID(), "ADMIN", List.of("users:write"));
        assertEquals("ADMIN", roleResponse.name);

        SessionResponseDTO session = new SessionResponseDTO(UUID.randomUUID(), "dev", "127.0.0.1", Instant.now(), true);
        assertTrue(session.current);

        TwoFactorSetupResponseDTO setup = new TwoFactorSetupResponseDTO("secret", null, List.of("ABC12345"));
        assertEquals("secret", setup.secret);

        PartialLoginResponseDTO partial = new PartialLoginResponseDTO("pt", true, List.of("TOTP"), 300L);
        assertEquals("pt", partial.partialToken);
        assertTrue(partial.requires2FA);
        assertEquals(300L, partial.expiresIn);
    }

    @Test
    void fieldOnlyDtos() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.email = "a@b.com";
        loginRequest.password = "pass";
        assertEquals("a@b.com", loginRequest.email);

        LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.email = "c@d.com";
        loginRequestDTO.password = "pass2";
        assertEquals("c@d.com", loginRequestDTO.email);

        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.refreshToken = "rt";
        assertEquals("rt", refreshRequest.refreshToken);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.email = "e@f.com";
        registerRequest.password = "12345678";
        registerRequest.displayName = "User";
        assertEquals("User", registerRequest.displayName);

        RegisterRequestDTO registerRequestDTO = new RegisterRequestDTO();
        registerRequestDTO.email = "g@h.com";
        registerRequestDTO.password = "abcdefgh";
        registerRequestDTO.displayName = "User2";
        assertEquals("g@h.com", registerRequestDTO.email);

        RoleRequestDTO roleRequestDTO = new RoleRequestDTO();
        roleRequestDTO.name = "admin";
        roleRequestDTO.permissions = List.of("users:write");
        assertEquals(1, roleRequestDTO.permissions.size());

        TwoFactorSetupRequestDTO setupReq = new TwoFactorSetupRequestDTO();
        setupReq.method = "TOTP";
        assertEquals("TOTP", setupReq.method);

        TwoFactorVerifyRequestDTO verifyReq = new TwoFactorVerifyRequestDTO();
        verifyReq.partialToken = "p";
        verifyReq.method = "TOTP";
        verifyReq.code = "123456";
        assertEquals("123456", verifyReq.code);

        TwoFactorConfirmRequestDTO confirmReq = new TwoFactorConfirmRequestDTO();
        confirmReq.code = "123456";
        assertEquals("123456", confirmReq.code);

        TwoFactorDisableRequestDTO disableReq = new TwoFactorDisableRequestDTO();
        disableReq.password = "abcdefgh";
        assertEquals("abcdefgh", disableReq.password);

        UpdateProfileRequest updateProfileRequest = new UpdateProfileRequest();
        updateProfileRequest.displayName = "new name";
        updateProfileRequest.avatarUrl = null;
        assertEquals("new name", updateProfileRequest.displayName);

        UpdateUserStatusRequestDTO updateStatus = new UpdateUserStatusRequestDTO();
        updateStatus.status = "ACTIVE";
        updateStatus.reason = "ok";
        assertEquals("ACTIVE", updateStatus.status);

        UpdateUserRolesRequestDTO updateRoles = new UpdateUserRolesRequestDTO();
        updateRoles.roles = List.of("ADMIN");
        assertEquals("ADMIN", updateRoles.roles.get(0));

        VerifyEmailRequestDTO verifyEmail = new VerifyEmailRequestDTO();
        verifyEmail.token = "token";
        assertEquals("token", verifyEmail.token);

        ChangePasswordRequestDTO changePassword = new ChangePasswordRequestDTO();
        changePassword.currentPassword = "oldpassword";
        changePassword.newPassword = "newpassword";
        assertEquals("oldpassword", changePassword.currentPassword);
        assertFalse(changePassword.newPassword.isBlank());
    }
}
