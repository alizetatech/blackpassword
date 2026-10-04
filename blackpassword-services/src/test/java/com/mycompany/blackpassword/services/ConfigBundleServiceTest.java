package com.mycompany.blackpassword.services;

import com.mycompany.blackpassword.crypto.RSALogica;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigBundleServiceTest {

    @Test
    public void testCreateAndUnpackBundle(@TempDir Path tempDir) throws Exception {
        String dummyFirebase = "{\"type\": \"service_account\", \"project_id\": \"blackpassword-test\"}";
        char[] masterPass = "SuperSecretMasterKey2026!".toCharArray();

        KeyPair kp = RSALogica.generarLlaves();
        assertNotNull(kp);

        File targetBundle = tempDir.resolve("test_config.kek").toFile();

        // 1. Crear el paquete maestro KEK
        ConfigBundleService.createBundle(dummyFirebase.getBytes(StandardCharsets.UTF_8), kp.getPublic(), kp.getPrivate(), masterPass, targetBundle);
        assertTrue(targetBundle.exists());
        assertTrue(targetBundle.length() > 0);

        // 2. Desempaquetar con la contraseña correcta en RAM
        ConfigBundleService.BundleResult result = ConfigBundleService.unpackBundle(targetBundle, masterPass);
        assertNotNull(result);
        assertNotNull(result.firebaseConfigBytes);
        assertNotNull(result.publicKey);
        assertNotNull(result.privateKey);

        String unpackedFirebase = new String(result.firebaseConfigBytes, StandardCharsets.UTF_8);
        assertEquals(dummyFirebase, unpackedFirebase);

        // Verificar que las llaves reconstruidas coinciden con las originales
        assertArrayEquals(kp.getPublic().getEncoded(), result.publicKey.getEncoded());
        assertArrayEquals(kp.getPrivate().getEncoded(), result.privateKey.getEncoded());

        // Verificar que el ciclo de cifrado/descifrado híbrido funciona con las llaves reconstruidas
        byte[] testData = "Confidential message inside vault".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = RSALogica.cifrarBytesHibrido(testData, result.publicKey);
        byte[] decrypted = RSALogica.descifrarBytesHibrido(encrypted, result.privateKey);
        assertArrayEquals(testData, decrypted);

        // 3. Desempaquetar con contraseña incorrecta debe lanzar excepción de seguridad
        char[] wrongPass = "WrongPassword999!".toCharArray();
        assertThrows(Exception.class, () -> {
            ConfigBundleService.unpackBundle(targetBundle, wrongPass);
        });
    }

    @Test
    public void testExportCompleteSuite(@TempDir Path tempDir) throws Exception {
        String dummyFirebase = "{\"project_id\": \"suite-test\"}";
        char[] masterPass = "MasterKeySuite123".toCharArray();
        KeyPair kp = RSALogica.generarLlaves();

        File exportDir = tempDir.resolve("export_suite").toFile();
        ConfigBundleService.exportCompleteSuite(exportDir, "mi_config", dummyFirebase.getBytes(StandardCharsets.UTF_8), kp.getPublic(), kp.getPrivate(), masterPass);

        File bundleFile = new File(exportDir, "mi_config.kek");
        File pubFile = new File(exportDir, "mi_config.pub");
        File privFile = new File(exportDir, "mi_config.key");
        File fbFile = new File(exportDir, "mi_config.black");

        assertTrue(bundleFile.exists(), "El bundle .kek debe existir");
        assertTrue(pubFile.exists(), "La llave .pub debe existir");
        assertTrue(privFile.exists(), "La llave .key debe existir");
        assertTrue(fbFile.exists(), "El archivo .black debe existir");

        // Verificar que el bundle generado en la suite es plenamente legible
        ConfigBundleService.BundleResult bundle = ConfigBundleService.unpackBundle(bundleFile, masterPass);
        assertEquals(dummyFirebase, new String(bundle.firebaseConfigBytes, StandardCharsets.UTF_8));
    }
}
