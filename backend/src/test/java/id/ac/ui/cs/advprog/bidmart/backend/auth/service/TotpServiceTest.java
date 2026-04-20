package id.ac.ui.cs.advprog.bidmart.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TotpServiceTest {

    private final TotpService service = new TotpService();

    @Test
    void generateBase32Secret() {
        String secret = service.generateBase32Secret();

        assertNotNull(secret);
        assertTrue(secret.length() >= 32);
        assertTrue(secret.matches("[A-Z2-7]+"));
    }

    @Test
    void generateCodeForStep_Deterministic() {
        String secret = "JBSWY3DPEHPK3PXP";

        String code1 = service.generateCodeForStep(secret, 10L);
        String code2 = service.generateCodeForStep(secret, 10L);

        assertEquals(code1, code2);
        assertEquals(6, code1.length());
        assertTrue(code1.matches("\\d{6}"));
    }

    @Test
    void generateCodeForStep_InvalidSecret() {
        String code = service.generateCodeForStep("", 10L);
        assertEquals("", code);
    }

    @Test
    void verifyCode_ValidAndInvalidInputs() {
        String secret = "JBSWY3DPEHPK3PXP";
        String code = service.generateCodeForStep(secret, java.time.Instant.now().getEpochSecond() / 30);

        assertTrue(service.verifyCode(secret, code));
        assertFalse(service.verifyCode(secret, "000000"));
        assertFalse(service.verifyCode(null, code));
        assertFalse(service.verifyCode(secret, null));
        assertFalse(service.verifyCode(secret, "   "));
        assertFalse(service.verifyCode(" ", " "));
    }

    @Test
    void generateCodeForStep_Base32Normalization() {
        String canonical = service.generateCodeForStep("JBSWY3DPEHPK3PXP", 100L);
        String mixed = service.generateCodeForStep("jbsw y3dp=ehpk3pxp", 100L);
        assertEquals(canonical, mixed);
    }

    @Test
    void generateCodeForStep_IgnoreInvalidBase32Chars() {
        String code = service.generateCodeForStep("JBSWY3DP!@#EHPK3PXP", 200L);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void verifyCode_WithPaddingNormalized() {
        String secret = "JBSWY3DPEHPK3PXP==";
        String code = service.generateCodeForStep("JBSWY3DPEHPK3PXP", java.time.Instant.now().getEpochSecond() / 30);
        assertTrue(service.verifyCode(secret, code));
    }

    @Test
    void encodeBase32_LeftoverBitsBranch() {
        String encoded = (String) ReflectionTestUtils.invokeMethod(service, "encodeBase32", new byte[]{0x01});
        assertNotNull(encoded);
        assertFalse(encoded.isBlank());
    }
}
