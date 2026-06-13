package com.elian.wallet.security;

import com.elian.wallet.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class JwtService {
    private final String secret;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(Usuario usuario) {
        long expiration = Instant.now().plusSeconds(expirationMinutes * 60).getEpochSecond();
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url(String.format(
                "{\"sub\":\"%s\",\"uid\":\"%s\",\"role\":\"%s\",\"exp\":%d}",
                usuario.getEmail(),
                usuario.getId(),
                usuario.getRol(),
                expiration
        ));
        String unsigned = header + "." + payload;
        return unsigned + "." + sign(unsigned);
    }

    public String getEmail(String token) {
        return extractString(token, "sub");
    }

    public UUID getUserId(String token) {
        return UUID.fromString(extractString(token, "uid"));
    }

    public boolean isValid(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        String unsigned = parts[0] + "." + parts[1];
        if (!sign(unsigned).equals(parts[2])) {
            return false;
        }
        long exp = Long.parseLong(extractNumber(token, "exp"));
        return Instant.now().getEpochSecond() < exp;
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el token JWT", ex);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String payload(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
    }

    private String extractString(String token, String key) {
        String marker = "\"" + key + "\":\"";
        String data = payload(token);
        int start = data.indexOf(marker);
        if (start < 0) {
            throw new IllegalArgumentException("Token invalido");
        }
        start += marker.length();
        int end = data.indexOf("\"", start);
        return data.substring(start, end);
    }

    private String extractNumber(String token, String key) {
        String marker = "\"" + key + "\":";
        String data = payload(token);
        int start = data.indexOf(marker);
        if (start < 0) {
            throw new IllegalArgumentException("Token invalido");
        }
        start += marker.length();
        int end = data.indexOf(",", start);
        if (end < 0) {
            end = data.indexOf("}", start);
        }
        return data.substring(start, end);
    }
}
