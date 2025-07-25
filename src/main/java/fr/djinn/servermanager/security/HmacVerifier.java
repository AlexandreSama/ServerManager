package fr.djinn.servermanager.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HmacVerifier {

    private static final long MAX_TIME_DRIFT_MS = 30_000; // 30 sec

    public static boolean isValidSignature(String method, String uri, Map<String, String> headers, String body, String secretKey) {
        String username = headers.get("x-username");
        String timestampStr = headers.get("x-timestamp");
        String clientSig = headers.get("x-signature");

        if (username == null || timestampStr == null || clientSig == null) return false;

        try {
            long timestamp = Long.parseLong(timestampStr);
            long now = System.currentTimeMillis();
            if (Math.abs(now - timestamp) > MAX_TIME_DRIFT_MS) {
                return false; // trop vieux ou trop tôt
            }

            String toSign = username + "|" + timestampStr + "|" + method.toUpperCase() + "|" + uri + "|" + body;
            String computedSig = computeHmacSHA256(toSign, secretKey);

            return clientSig.equalsIgnoreCase(computedSig);
        } catch (Exception e) {
            return false;
        }
    }

    public static String computeHmacSHA256(String message, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hashBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}