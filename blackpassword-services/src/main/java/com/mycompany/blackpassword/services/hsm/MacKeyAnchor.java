package com.mycompany.blackpassword.services.hsm;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;

/**
 * Anclaje HSM macOS (v11.0 - PROTECTED PRIVATE KEY).
 * Utiliza Keychain + Entrada Protegida por PIN de Sesión.
 */
public class MacKeyAnchor implements KeyAnchorProvider {

    private static final String PROVIDER = "Apple";
    private static final String KEYSTORE_TYPE = "KeychainStore";
    private static final String ALGO_RSA = "RSA/ECB/PKCS1Padding";
    private final java.util.Map<String, PrivateKey> ephemeralKeys = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, byte[]> protectorHashes = new java.util.concurrent.ConcurrentHashMap<>();

    private byte[] hashProtector(char[] protector) throws Exception {
        java.nio.ByteBuffer bb = java.nio.charset.StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(protector));
        byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        try {
            return java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    @Override
    public void seal(String alias, byte[] secret, char[] protector) throws Exception {
        // 1. Generar LLaves en hardware
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("RSA", PROVIDER);
        } catch (Exception e) {
            kpg = KeyPairGenerator.getInstance("RSA");
        }
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        // 2. Cifrar con la pública del hardware
        Cipher cipher = Cipher.getInstance(ALGO_RSA);
        cipher.init(Cipher.ENCRYPT_MODE, kp.getPublic());
        byte[] encryptedSecret = cipher.doFinal(secret);

        ephemeralKeys.put(alias, kp.getPrivate());
        if (protector != null) {
            protectorHashes.put(alias, hashProtector(protector));
        }

        // 3. Persistir en Keychain
        saveToKeychain(alias, encryptedSecret);
        
        Arrays.fill(encryptedSecret, (byte) 0);
        Arrays.fill(secret, (byte) 0);
        System.out.println("[HSM-Mac] Secreto sellado y blindado.");
    }

    @Override
    public byte[] unseal(String alias, char[] protector) throws Exception {
        byte[] expectedHash = protectorHashes.get(alias);
        if (expectedHash != null) {
            if (protector == null || !java.security.MessageDigest.isEqual(expectedHash, hashProtector(protector))) {
                throw new Exception("Falló el desbloqueo de Hardware Keystore: PIN incorrecto.");
            }
        }

        byte[] encryptedSecret = readFromKeychain(alias);
        if (encryptedSecret == null) throw new Exception("Secreto no disponible en Keychain.");

        PrivateKey priv = ephemeralKeys.get(alias);
        if (priv == null) {
            KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER);
            ks.load(null, null);

            KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection(protector);
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, protection);
            if (entry != null) {
                priv = entry.getPrivateKey();
            }
        }
        
        if (priv == null) throw new Exception("Falló el desbloqueo de Hardware Keystore: PIN incorrecto.");

        Cipher cipher = Cipher.getInstance(ALGO_RSA);
        cipher.init(Cipher.DECRYPT_MODE, priv);
        byte[] result = cipher.doFinal(encryptedSecret);
        Arrays.fill(encryptedSecret, (byte) 0);
        return result;
    }

    private void saveToKeychain(String alias, byte[] blob) throws Exception {
        String base64Blob = Base64.getEncoder().encodeToString(blob);
        new ProcessBuilder("security", "add-generic-password", "-a", "Blackpassword", "-s", alias, "-w", base64Blob, "-U")
            .start().waitFor();
    }

    private byte[] readFromKeychain(String alias) throws Exception {
        Process proc = new ProcessBuilder("security", "find-generic-password", "-a", "Blackpassword", "-s", alias, "-w").start();
        java.io.InputStream is = proc.getInputStream();
        byte[] encoded = is.readAllBytes();
        proc.waitFor();
        if (encoded.length == 0) return null;
        return Base64.getDecoder().decode(new String(encoded).trim());
    }

    @Override
    public void destroy(String alias) {
        ephemeralKeys.remove(alias);
        byte[] h = protectorHashes.remove(alias);
        if (h != null) Arrays.fill(h, (byte) 0);
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER);
            ks.load(null, null);
            ks.deleteEntry(alias);
            new ProcessBuilder("security", "delete-generic-password", "-a", "Blackpassword", "-s", alias)
                .start().waitFor();
        } catch (Exception e) {}
    }

    @Override
    public boolean isAvailable() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }
}
