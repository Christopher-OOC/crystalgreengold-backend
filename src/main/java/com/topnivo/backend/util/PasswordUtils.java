package com.topnivo.backend.util;

import java.util.Random;
import java.security.SecureRandom;
import java.util.function.Predicate;

public class PasswordUtils {

    private static final String CHARACTERS = "0123456789";

    public static String generatePassword(int length) {
        StringBuilder value = new StringBuilder();
        Random random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            value.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        return value.toString();
    }
}
