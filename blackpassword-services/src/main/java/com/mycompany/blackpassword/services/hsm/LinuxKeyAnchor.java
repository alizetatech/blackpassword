package com.mycompany.blackpassword.services.hsm;

import java.util.Arrays;
import java.util.Base64;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Anclaje HSM Linux (v11.0 - PROTECTED PASSPHRASE).
 * Utiliza libsecret para almacenar secretos de sesión protegidos.
 */
public class LinuxKeyAnchor implements KeyAnchorProvider {

    public interface LibSecret extends Library {
        LibSecret INSTANCE = Native.load("secret-1", LibSecret.class);
        
        Pointer secret_password_store_sync(Pointer schema, 
                                         String collection, 
                                         String label, 
                                         String password, 
                                         Pointer cancellable, 
                                         Pointer error, 
                                         String... attributes);
                                         
        String secret_password_lookup_sync(Pointer schema, 
                                         Pointer cancellable, 
                                         Pointer error, 
                                         String... attributes);
                                         
        boolean secret_password_clear_sync(Pointer schema, 
                                         Pointer cancellable, 
                                         Pointer error, 
                                         String... attributes);
    }

    private static final String APP_ID = "com.mycompany.blackpassword";

    @Override
    public void seal(String alias, byte[] secret, char[] protector) throws Exception {
        try {
            byte[] salt = new byte[16];
            new java.security.SecureRandom().nextBytes(salt);
            byte[] iv = new byte[12];
            new java.security.SecureRandom().nextBytes(iv);

            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(protector, salt, 210000, 256);
            byte[] keyBytes = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();

            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, new javax.crypto.spec.SecretKeySpec(keyBytes, "AES"), new javax.crypto.spec.GCMParameterSpec(128, iv));
            Arrays.fill(keyBytes, (byte) 0);
            byte[] cipherText = cipher.doFinal(secret);

            byte[] combined = new byte[salt.length + iv.length + cipherText.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(iv, 0, combined, salt.length, iv.length);
            System.arraycopy(cipherText, 0, combined, salt.length + iv.length, cipherText.length);

            String secretBase64 = Base64.getEncoder().encodeToString(combined);
            String protectorTag = deriveProtectorTag(alias, protector);

            LibSecret.INSTANCE.secret_password_store_sync(null, "session", 
                "Blackpassword Session Key", secretBase64, null, null, 
                "application", APP_ID, "alias", alias, "protector_tag", protectorTag);
            
            Arrays.fill(secret, (byte) 0);
            System.out.println("[HSM-Linux] Secreto cifrado con AES-256-GCM y sellado en Keyring.");
        } catch (UnsatisfiedLinkError e) {
            throw new Exception("libsecret no disponible.");
        }
    }

    @Override
    public byte[] unseal(String alias, char[] protector) throws Exception {
        String protectorTag = deriveProtectorTag(alias, protector);
        String secretBase64 = LibSecret.INSTANCE.secret_password_lookup_sync(null, null, null, 
                "application", APP_ID, "alias", alias, "protector_tag", protectorTag);
        
        if (secretBase64 == null) throw new Exception("Acceso denegado: Protector de hardware incorrecto.");
        byte[] combined = Base64.getDecoder().decode(secretBase64);
        if (combined.length < 28) throw new Exception("Datos sellados corruptos en Keyring.");

        byte[] salt = Arrays.copyOfRange(combined, 0, 16);
        byte[] iv = Arrays.copyOfRange(combined, 16, 28);
        byte[] cipherText = Arrays.copyOfRange(combined, 28, combined.length);

        javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(protector, salt, 210000, 256);
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        spec.clearPassword();

        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(keyBytes, "AES"), new javax.crypto.spec.GCMParameterSpec(128, iv));
        Arrays.fill(keyBytes, (byte) 0);
        return cipher.doFinal(cipherText);
    }

    private String deriveProtectorTag(String alias, char[] protector) throws Exception {
        java.nio.ByteBuffer bb = java.nio.charset.StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(protector));
        byte[] pBytes = Arrays.copyOfRange(bb.array(), bb.position(), bb.limit());
        Arrays.fill(bb.array(), (byte) 0);
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(alias.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(md.digest(pBytes));
        } finally {
            Arrays.fill(pBytes, (byte) 0);
        }
    }

    @Override
    public void destroy(String alias) {
        LibSecret.INSTANCE.secret_password_clear_sync(null, null, null, 
                "application", APP_ID, "alias", alias);
        System.out.println("[HSM-Linux] Purga de Keyring completa.");
    }

    @Override
    public boolean isAvailable() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }
}
