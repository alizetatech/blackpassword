package com.mycompany.blackpassword.services;

import com.mycompany.blackpassword.services.LocalDatabaseService.BtcSeedEntry;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class BtcSeedVaultTest {

    @Test
    public void testGuardarDescifrarYEliminarSemilla() throws Exception {
        String testEmail = "vault_tester@blackpassword.com";
        String codigo = "WALLET-TEST-99";
        String red = "TESTNET";
        String direccion = "tb1qtestaddress1234567890";
        String fraseOriginal = "abandon ability able about above absent absorb abstract absurd abuse access accident";
        char[] pin = new char[]{'1', '2', '3', '4'};

        // 1. Guardar semilla en la base de datos
        boolean guardado = LocalDatabaseService.guardarSemillaWallet(testEmail, codigo, red, direccion, fraseOriginal, pin);
        assertTrue(guardado, "La semilla debería guardarse exitosamente en la BD");

        // 2. Obtener semillas
        List<BtcSeedEntry> semillas = LocalDatabaseService.obtenerSemillasWallet(testEmail);
        assertFalse(semillas.isEmpty(), "Debería haber al menos una semilla registrada");
        
        BtcSeedEntry found = null;
        for (BtcSeedEntry entry : semillas) {
            if (codigo.equalsIgnoreCase(entry.codigoWallet)) {
                found = entry;
                break;
            }
        }
        assertNotNull(found, "La semilla guardada debería ser encontrada por código");
        assertEquals("TESTNET", found.red);
        assertEquals(direccion, found.direccion);

        // 3. Descifrar con PIN correcto
        String fraseDescifrada = LocalDatabaseService.descifrarSemillaWallet(found, pin);
        assertEquals(fraseOriginal, fraseDescifrada, "La frase descifrada debe coincidir con la original");

        final BtcSeedEntry targetEntry = found;
        char[] wrongPin = new char[]{'9', '9', '9', '9'};
        assertThrows(Exception.class, () -> {
            LocalDatabaseService.descifrarSemillaWallet(targetEntry, wrongPin);
        }, "Descifrar con PIN incorrecto debe lanzar excepción");

        // 5. Eliminar de la base de datos
        boolean eliminado = LocalDatabaseService.eliminarSemillaWallet(found.id);
        assertTrue(eliminado, "La semilla debería eliminarse");

        Arrays.fill(pin, '0');
        Arrays.fill(wrongPin, '0');
    }
}
