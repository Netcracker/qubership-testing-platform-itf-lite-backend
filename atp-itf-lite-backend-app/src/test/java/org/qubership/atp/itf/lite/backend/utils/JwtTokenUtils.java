package org.qubership.atp.itf.lite.backend.utils;

import java.util.Base64;
import java.util.UUID;

import org.apache.commons.codec.digest.HmacUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class JwtTokenUtils {

    private final static String SECRET_KEY = "cAtwa1kkEy";
    private final static String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    /**
     * Generates jwt token with session id.
     * @param sessionId session id
     * @return bearer token
     */
    public static String generateJwtTokenWithSessionId(UUID sessionId, String sessionStateFieldName) {
        String payloadInfo = "{\"" + sessionStateFieldName + "\": \"" + sessionId + "\"}";
        return generateJwtToken(payloadInfo);
    }

    /**
     * Generates jwt token with user id.
     * @param userId user id
     * @return bearer token
     */
    public static String generateJwtTokenWithUserId(UUID userId, String subFieldName) {
        String payloadInfo = "{\"" + subFieldName + "\": \"" + userId + "\"}";
        return generateJwtToken(payloadInfo);
    }

    private static String generateJwtToken(String payloadInfo) {
        String header = "{ \"alg\": \"HS256\", \"typ\": \"JWT\"}";
        String unsignedToken = Base64.getUrlEncoder().encodeToString(header.getBytes())
                + "." + Base64.getUrlEncoder().encodeToString(payloadInfo.getBytes());
        String signature = new HmacUtils(HMAC_SHA256_ALGORITHM, SECRET_KEY).hmacHex(unsignedToken);
        return "Bearer " + unsignedToken + "." + Base64.getUrlEncoder().encodeToString(signature.getBytes());
    }

}
