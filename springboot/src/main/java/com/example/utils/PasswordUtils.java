package com.example.utils;

import cn.hutool.crypto.digest.BCrypt;

import java.util.regex.Pattern;

public class PasswordUtils {

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]?\\$\\d{2}\\$.{53}$");

    private PasswordUtils() {
    }

    public static String encrypt(String rawPassword) {
        return BCrypt.hashpw(rawPassword);
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isEncrypted(storedPassword)) {
            return BCrypt.checkpw(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }

    public static boolean isEncrypted(String password) {
        return password != null && BCRYPT_PATTERN.matcher(password).matches();
    }
}
