package id.ac.ui.cs.advprog.bidmart.backend.auth.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

@Service
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public String generateBase32Secret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        String normalizedCode = code.trim();
        long currentStep = Instant.now().getEpochSecond() / 30;
        for (long i = -1; i <= 1; i++) {
            String expected = generateCodeForStep(secret, currentStep + i);
            if (expected.equals(normalizedCode)) {
                return true;
            }
        }
        return false;
    }

    public String generateCodeForStep(String secret, long step) {
        try {
            byte[] key = decodeBase32(secret);
            byte[] data = new byte[8];
            long value = step;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (value & 0xff);
                value >>= 8;
            }

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);

            int otp = binary % 1_000_000;
            return String.format(Locale.ROOT, "%06d", otp);
        } catch (Exception e) {
            return "";
        }
    }

    private String encodeBase32(byte[] input) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : input) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1f;
                bitsLeft -= 5;
                out.append(BASE32_ALPHABET.charAt(index));
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1f;
            out.append(BASE32_ALPHABET.charAt(index));
        }
        return out.toString();
    }

    private byte[] decodeBase32(String input) {
        String normalized = input.trim().replace("=", "").toUpperCase(Locale.ROOT);
        int buffer = 0;
        int bitsLeft = 0;
        byte[] out = new byte[(normalized.length() * 5) / 8];
        int outPos = 0;

        for (char c : normalized.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) {
                continue;
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out[outPos++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }

        if (outPos == out.length) {
            return out;
        }
        byte[] resized = new byte[outPos];
        System.arraycopy(out, 0, resized, 0, outPos);
        return resized;
    }
}
