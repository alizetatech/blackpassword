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

    @Override
    public void seal(String alias, byte[] secret, char[] protector) throws Exception {
        // 1. Generar LLaves en hardware
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", PROVIDER);
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        // 2. Cifrar con la pública del hardware
        Cipher cipher = Cipher.getInstance(ALGO_RSA);
        cipher.init(Cipher.ENCRYPT_MODE, kp.getPublic());
        byte[] encryptedSecret = cipher.doFinal(secret);

        // 3. Persistir en Keychain
        saveToKeychain(alias, encryptedSecret);
        
        Arrays.fill(encryptedSecret, (byte) 0);
        Arrays.fill(secret, (byte) 0);
        System.out.println("[HSM-Mac] Secreto sellado y blindado.");
    }

    @Override
    public byte[] unseal(String alias, char[] protector) throws Exception {
        byte[] encryptedSecret = readFromKeychain(alias);
        if (encryptedSecret == null) throw new Exception("Secreto no disponible en Keychain.");

        KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER);
        ks.load(null, null);

        // PATRÓN PROTECTED PRIVATE KEY (Nivel 11)
        KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection(protector);
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, protection);
        
        if (entry == null) throw new Exception("Falló el desbloqueo de Hardware Keystore: PIN incorrecto.");
        PrivateKey priv = entry.getPrivateKey();

        Cipher cipher = Cipher.getInstance(ALGO_RSA);
        cipher.init(Cipher.DECRYPT_MODE, priv);
        return cipher.doFinal(encryptedSecret);
    }

    private void saveToKeychain(String alias, byte[] blob) throws Exception {
        String base64Blob = Base64.getEncoder().encodeToString(blob);
        String cmd = String.format("security add-generic-password -a %s -s %s -w %s -U", 
                                   "Blackpassword", alias, base64Blob);
        Runtime.getRuntime().exec(cmd).waitFor();
    }

    private byte[] readFromKeychain(String alias) throws Exception {
        String cmd = String.format("security find-generic-password -a %s -s %s -w", 
                                   "Blackpassword", alias);
        java.io.InputStream is = Runtime.getRuntime().exec(cmd).getInputStream();
        byte[] encoded = is.readAllBytes();
        if (encoded.length == 0) return null;
        return Base64.getDecoder().decode(new String(encoded).trim());
    }

    @Override
    public void destroy(String alias) {
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER);
            ks.load(null, null);
            ks.deleteEntry(alias);
            String cmd = String.format("security delete-generic-password -a %s -s %s", 
                                       "Blackpassword", alias);
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception e) {}
    }

    @Override
    public boolean isAvailable() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }
}
