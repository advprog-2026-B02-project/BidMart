package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import java.util.List;

public class TwoFactorSetupResponseDTO {
    public String secret;
    public String qrCodeUri;
    public List<String> backupCodes;

    public TwoFactorSetupResponseDTO(String secret, String qrCodeUri, List<String> backupCodes) {
        this.secret = secret;
        this.qrCodeUri = qrCodeUri;
        this.backupCodes = backupCodes;
    }
}
