package com.mycompany.blackpassword.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * GuardedString v1.0 - Blindaje de Memoria RAM.
 * Mantiene los datos cifrados con una llave efímera aleatoria.
 */
public final class GuardedString implements AutoCloseable {

    private static final SecretKey SESSION_MEMORY_KEY;
    static {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256);
            SESSION_MEMORY_KEY = kg.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Fallo al inicializar motor de memoria segura.", e);
        }
    }

    private byte[] encryptedValue;
    private byte[] iv;
    private final int originalLength;

    public GuardedString(char[] value) {
        if (value == null) {
            this.encryptedValue = null;
            this.iv = null;
            this.originalLength = 0;
            return;
        }
        this.originalLength = value.length;
        byte[] raw = new byte[value.length * 2];
        for (int i = 0; i < value.length; i++) {
            raw[i * 2] = (byte) (value[i] >> 8);
            raw[i * 2 + 1] = (byte) value[i];
        }

        try {
            this.iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, SESSION_MEMORY_KEY, new GCMParameterSpec(128, iv));
            this.encryptedValue = c.doFinal(raw);
        } catch (Exception e) {
            throw new RuntimeException("Error al blindar cadena en memoria.", e);
        } finally {
            Arrays.fill(raw, (byte) 0);
        }
    }

    /**
     * Acceso seguro al valor mediante un callback.
     * El valor descifrado solo existe durante la ejecución del accessor.
     */
    public void access(Accessor accessor) {
        if (encryptedValue == null) {
            accessor.access(null);
            return;
        }

        char[] clearValue = new char[originalLength];
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, SESSION_MEMORY_KEY, new GCMParameterSpec(128, iv));
            byte[] raw = c.doFinal(encryptedValue);
            
            for (int i = 0; i < originalLength; i++) {
                clearValue[i] = (char) (((raw[i * 2] & 0xFF) << 8) | (raw[i * 2 + 1] & 0xFF));
            }
            Arrays.fill(raw, (byte) 0);
            
            accessor.access(clearValue);
        } catch (Exception e) {
            throw new RuntimeException("Fallo al descifrar GuardedString.", e);
        } finally {
            Arrays.fill(clearValue, (char) 0);
        }
    }

    @Override
    public void close() {
        if (encryptedValue != null) Arrays.fill(encryptedValue, (byte) 0);
        if (iv != null) Arrays.fill(iv, (byte) 0);
        encryptedValue = null;
        iv = null;
    }

    public interface Accessor {
        void access(char[] clearValue);
    }
}
