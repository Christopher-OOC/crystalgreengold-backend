package com.topnivo.backend.util;

import java.util.Random;
import java.security.SecureRandom;

public class OrderUtils {

    private static final String CHARACTERS = "0123456789";

    public static String generateOrder(int length) {
        StringBuilder value = new StringBuilder("order_");
        Random random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            value.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        return value.toString();
    }
}
