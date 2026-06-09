package com.topnivo.backend.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class KeyUtils {

    public static SecretKey getSecretKey(String stringSecretKey) {
        final byte[] decoded = Base64.getDecoder().decode(stringSecretKey);

        return new SecretKeySpec(decoded, "HmacSHA256");
    }

}
