package com.mycompany.blackpassword.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.blackpassword.crypto.RSALogica;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Servicio de Paquete Maestro de Configuración v1.0.
 * Empaqueta y blinda la configuración de Firebase junto con el par de llaves RSA
 * en un único contenedor autónomo .kek / .wfile protegido con la contraseña maestra.
 */
public class ConfigBundleService {

    public static final String MAGIC_BUNDLE = "BLACKPASSWORD_CONFIG_BUNDLE_V2";
    private static final byte[] WFILE_MAGIC = new byte[]{0x57, 0x4B, 0x45, 0x4B}; // 'WKEK' v2

    public static class BundleResult {
        public final byte[] firebaseConfigBytes;
        public final PublicKey publicKey;
        public final PrivateKey privateKey;

        public BundleResult(byte[] firebaseConfigBytes, PublicKey publicKey, PrivateKey privateKey) {
            this.firebaseConfigBytes = firebaseConfigBytes;
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }
    }

    /**
     * Cifra un array de bytes usando PBKDF2 (600,000 iteraciones) + AES-256-GCM con salt e IV únicos.
     * Retorna una cadena Base64 con el formato [SALT(16B) + IV(12B) + CIPHERTEXT].
     */
    private static String encryptBytesAutonomous(byte[] plaintext, char[] password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        SecretKeySpec key = RSALogica.derivarLlavePBKDF2(password, salt, 600000);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] ciphertext = cipher.doFinal(plaintext);

        ByteBuffer bb = ByteBuffer.allocate(salt.length + iv.length + ciphertext.length);
        bb.put(salt);
        bb.put(iv);
        bb.put(ciphertext);

        Arrays.fill(ciphertext, (byte) 0);
        return Base64.getEncoder().encodeToString(bb.array());
    }

    /**
     * Descifra un payload cifrado por encryptBytesAutonomous.
     */
    private static byte[] decryptBytesAutonomous(String base64Enc, char[] password) throws Exception {
        byte[] raw = Base64.getDecoder().decode(base64Enc);
        if (raw.length < 28) {
            throw new IllegalArgumentException("Payload cifrado corrupto o incompleto.");
        }

        byte[] salt = Arrays.copyOfRange(raw, 0, 16);
        byte[] iv = Arrays.copyOfRange(raw, 16, 28);
        byte[] ciphertext = Arrays.copyOfRange(raw, 28, raw.length);

        SecretKeySpec key = RSALogica.derivarLlavePBKDF2(password, salt, 600000);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(ciphertext);
    }

    /**
     * Crea un archivo contenedor .kek maestro que empaqueta Firebase JSON, llave pública y llave privada.
     */
    public static void createBundle(byte[] firebaseConfig, PublicKey pubKey, PrivateKey privKey, char[] masterPassword, File targetFile) throws Exception {
        // 1. Cifrado individual de cada componente con salts independientes
        String encFirebase = encryptBytesAutonomous(firebaseConfig, masterPassword);
        String encPub = encryptBytesAutonomous(pubKey.getEncoded(), masterPassword);
        String encPriv = encryptBytesAutonomous(privKey.getEncoded(), masterPassword);

        // 2. Construcción de estructura JSON interna
        JsonObject json = new JsonObject();
        json.addProperty("magic", MAGIC_BUNDLE);
        json.addProperty("version", 2);
        json.addProperty("createdAt", System.currentTimeMillis());
        json.addProperty("firebaseConfigEnc", encFirebase);
        json.addProperty("publicKeyEnc", encPub);
        json.addProperty("privateKeyEnc", encPriv);

        byte[] jsonBytes = json.toString().getBytes(StandardCharsets.UTF_8);

        // 3. Blindaje externo con contenedor v2 WKEK (Streaming / autónomo)
        byte[] outerSalt = new byte[16];
        new SecureRandom().nextBytes(outerSalt);
        byte[] outerIv = new byte[12];
        new SecureRandom().nextBytes(outerIv);

        SecretKeySpec outerKey = RSALogica.derivarLlavePBKDF2(masterPassword, outerSalt, 600000);
        Cipher outerCipher = Cipher.getInstance("AES/GCM/NoPadding");
        outerCipher.init(Cipher.ENCRYPT_MODE, outerKey, new GCMParameterSpec(128, outerIv));
        byte[] encryptedPayload = outerCipher.doFinal(jsonBytes);

        try (OutputStream out = new FileOutputStream(targetFile)) {
            out.write(WFILE_MAGIC); // 4 bytes 'WKEK'
            out.write(outerSalt);   // 16 bytes salt
            out.write(outerIv);     // 12 bytes IV
            out.write(encryptedPayload);
        }

        Arrays.fill(jsonBytes, (byte) 0);
        Arrays.fill(encryptedPayload, (byte) 0);
    }

    /**
     * Comprueba si los bytes descifrados corresponden a un paquete de configuración maestro de Blackpassword.
     */
    public static boolean isBundle(byte[] data) {
        if (data == null || data.length < 30) return false;
        try {
            String str = new String(data, 0, Math.min(data.length, 256), StandardCharsets.UTF_8);
            return str.contains(MAGIC_BUNDLE);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Desempaqueta y descifra un paquete maestro en memoria RAM.
     */
    public static BundleResult unpackBundle(File bundleFile, char[] masterPassword) throws Exception {
        byte[] decryptedJsonBytes = ExportService.unblindFileToMemory(bundleFile, masterPassword);
        try {
            return unpackBundleFromMemory(decryptedJsonBytes, masterPassword);
        } finally {
            Arrays.fill(decryptedJsonBytes, (byte) 0);
        }
    }

    /**
     * Desempaqueta y descifra a partir de los bytes ya desblindados de memoria.
     */
    public static BundleResult unpackBundleFromMemory(byte[] decryptedJsonBytes, char[] masterPassword) throws Exception {
        String jsonStr = new String(decryptedJsonBytes, StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

        String magic = json.has("magic") ? json.get("magic").getAsString() : "";
        if (!MAGIC_BUNDLE.equals(magic)) {
            throw new IllegalArgumentException("El archivo no es un paquete maestro de configuración válido (Magic header desconocido).");
        }

        String encFirebase = json.get("firebaseConfigEnc").getAsString();
        String encPub = json.get("publicKeyEnc").getAsString();
        String encPriv = json.get("privateKeyEnc").getAsString();

        byte[] fbBytes = decryptBytesAutonomous(encFirebase, masterPassword);
        byte[] pubBytes = decryptBytesAutonomous(encPub, masterPassword);
        byte[] privBytes = decryptBytesAutonomous(encPriv, masterPassword);

        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
        PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));

        Arrays.fill(pubBytes, (byte) 0);
        Arrays.fill(privBytes, (byte) 0);

        return new BundleResult(fbBytes, pub, priv);
    }

    /**
     * Exporta tanto el paquete maestro .kek como los archivos individuales (.pub, .key, .black)
     * para que el usuario cuente con respaldos y flexibilidad.
     */
    public static void exportCompleteSuite(File dir, String baseName, byte[] firebaseConfig, PublicKey pubKey, PrivateKey privKey, char[] masterPassword) throws Exception {
        if (!dir.exists()) dir.mkdirs();

        // 1. Paquete Maestro Todo-en-Uno .kek
        File bundleFile = new File(dir, baseName + ".kek");
        createBundle(firebaseConfig, pubKey, privKey, masterPassword, bundleFile);

        // 2. Llave pública exportada en plano (.pub)
        File pubFile = new File(dir, baseName + ".pub");
        RSALogica.guardarLlave(pubFile, pubKey.getEncoded(), false, null, null);

        // 3. Llave privada cifrada con contraseña (.key)
        File privFile = new File(dir, baseName + ".key");
        byte[] keySalt = new byte[16];
        new SecureRandom().nextBytes(keySalt);
        RSALogica.guardarLlave(privFile, privKey.getEncoded(), true, masterPassword, keySalt);

        // 4. Configuración Firebase cifrada individualmente (.black)
        File fbFile = new File(dir, baseName + ".black");
        byte[] fbSalt = new byte[16];
        new SecureRandom().nextBytes(fbSalt);
        SecretKeySpec fbKey = RSALogica.derivarLlavePBKDF2(masterPassword, fbSalt, 600000);
        byte[] fbIv = new byte[12];
        new SecureRandom().nextBytes(fbIv);
        Cipher fbCipher = Cipher.getInstance("AES/GCM/NoPadding");
        fbCipher.init(Cipher.ENCRYPT_MODE, fbKey, new GCMParameterSpec(128, fbIv));
        byte[] fbEnc = fbCipher.doFinal(firebaseConfig);
        
        try (OutputStream fos = new FileOutputStream(fbFile)) {
            // Contenedor autónomo WKEK para máxima compatibilidad
            fos.write(WFILE_MAGIC);
            fos.write(fbSalt);
            fos.write(fbIv);
            fos.write(fbEnc);
        }
    }
}
