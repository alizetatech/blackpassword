package com.mycompany.blackpassword.crypto;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Utilidades de seguridad para manejo seguro de contraseñas.
 * Resuelve CWE-316, CWE-590: Cleartext Storage of Sensitive Information
 * 
 * Principio: NUNCA usar String para almacenar contraseñas.
 * Los char[] deben ser limpiados inmediatamente después de usar.
 */
public final class SecureCharArray {

    private SecureCharArray() {}

    /**
     * Compara dos char[] de forma timing-safe para evitar ataques de timing.
     * IMPORTANTE: Siempre usa este método para comparar contraseñas hasheadas.
     */
    public static boolean constantTimeEquals(char[] a, char[] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * Compara dos byte[] de forma timing-safe.
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * Convierte char[] a byte[] sin crear Strings intermedios.
     */
    public static byte[] toBytes(char[] chars) {
        CharBuffer cb = CharBuffer.wrap(chars);
        ByteBuffer bb = StandardCharsets.UTF_8.encode(cb);
        byte[] result = Arrays.copyOfRange(bb.array(), bb.position(), bb.limit());
        Arrays.fill(bb.array(), (byte) 0);
        return result;
    }

    /**
     * Convierte byte[] a char[] sin crear String.
     */
    public static char[] fromBytes(byte[] bytes) {
        CharBuffer cb = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
        char[] result = Arrays.copyOfRange(cb.array(), cb.position(), cb.limit());
        Arrays.fill(cb.array(), '\u0000');
        return result;
    }

    /**
     * Limpia un char[] sobrescribiendo con ceros.
     */
    public static void clear(char[] array) {
        if (array != null) {
            Arrays.fill(array, (char) 0);
        }
    }

    /**
     * Limpia un byte[] sobrescribiendo con ceros.
     */
    public static void clear(byte[] array) {
        if (array != null) {
            Arrays.fill(array, (byte) 0);
        }
    }

    /**
     * Hash SHA-256 de un char[] que retorna char[] (no String).
     * El llamador es responsable de limpiar tanto el input como el output.
     */
    public static char[] hashSHA256(char[] input, byte[] salt) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        if (salt != null) md.update(salt);
        byte[] inputBytes = toBytes(input);
        try {
            byte[] hash = md.digest(inputBytes);
            char[] result = new char[hash.length];
            for (int i = 0; i < hash.length; i++) {
                result[i] = (char) (hash[i] & 0xFF);
            }
            return result;
        } finally {
            clear(inputBytes);
        }
    }

    /**
     * Crea una copia defensiva de un char[].
     */
    public static char[] copy(char[] original) {
        if (original == null) return null;
        return Arrays.copyOf(original, original.length);
    }

    /**
     * Crea una copia defensiva de un byte[].
     */
    public static byte[] copy(byte[] original) {
        if (original == null) return null;
        return Arrays.copyOf(original, original.length);
    }

    /**
     * Enmascara un char[] para logging (muestra solo primeros caracteres).
     */
    public static String toMaskedString(char[] password, int visibleChars) {
        if (password == null) return "null";
        int show = Math.min(visibleChars, password.length);
        StringBuilder sb = new StringBuilder(show + 3);
        for (int i = 0; i < show; i++) sb.append(password[i]);
        if (password.length > visibleChars) sb.append("...");
        return sb.toString();
    }

    /**
     * Try-with-resources style para passwords.
     * Uso: try (PasswordGuard guard = new PasswordGuard(password)) { ... }
     */
    public static class PasswordGuard implements AutoCloseable {
        private final char[] password;

        public PasswordGuard(char[] password) {
            this.password = password;
        }

        public char[] getPassword() {
            return copy(password);
        }

        public void clear() {
            SecureCharArray.clear(password);
        }

        @Override
        public void close() {
            clear();
        }
    }
}
