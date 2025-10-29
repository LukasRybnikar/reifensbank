package com.task.reifensbank.util;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;

@UtilityClass
public final class ErrorCodeGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    public static final int ERROR_CODE_LENGTH = 8;

    public static String generateHexCode() {
        byte[] bytes = new byte[ERROR_CODE_LENGTH / 2];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(ERROR_CODE_LENGTH);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
