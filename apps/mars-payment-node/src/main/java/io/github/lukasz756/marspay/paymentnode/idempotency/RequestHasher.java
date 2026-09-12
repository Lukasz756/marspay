package io.github.lukasz756.marspay.paymentnode.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RequestHasher {

    private RequestHasher() {
    }

    public static String sha256(String canonicalRequest) {
        if (canonicalRequest == null) {
            throw new IllegalArgumentException("Canonical request must not be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of()
                    .formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}