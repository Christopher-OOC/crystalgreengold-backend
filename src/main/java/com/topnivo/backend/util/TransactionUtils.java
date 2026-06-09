package com.topnivo.backend.util;

import java.security.SecureRandom;
import java.util.Random;

public class TransactionUtils {

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz";

    public static String generateReferenceId() {
        StringBuilder value = new StringBuilder("rf_");
        Random random = new SecureRandom();
        for (int i = 0; i < 16; i++) {
            value.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        return value.toString();
    }

    public static String generateTransactionId() {
        StringBuilder value = new StringBuilder("TX_");
        Random random = new SecureRandom();
        for (int i = 0; i < 16; i++) {
            value.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        return value.toString();
    }

}
