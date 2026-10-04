package com.mycompany.blackpassword.services;

import com.mycompany.blackpassword.services.DogecoinWalletService.DogeWalletHolder;
import com.mycompany.blackpassword.services.LocalDatabaseService.DogeSeedEntry;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class DogecoinWalletTest {

    @Test
    public void testCrearYCifrarBilleteraDoge() throws Exception {
        String testEmail = "doge_tester@blackpassword.com";
        char[] pin = new char[]{'4', '3', '2', '1'};

        // 1. Crear billetera
        DogeWalletHolder wallet = DogecoinWalletService.crearBilletera(false);
        assertNotNull(wallet);
        assertNotNull(wallet.mnemonic);
        assertEquals(12, wallet.mnemonic.size());
        assertTrue(wallet.currentAddress.startsWith("D"), "Dirección Dogecoin Mainnet debe iniciar con 'D': " + wallet.currentAddress);

        // 2. Guardar cifrada
        DogecoinWalletService.guardarBilleteraCifrada(wallet, testEmail, pin, false);
        assertTrue(DogecoinWalletService.existeBilletera(testEmail, false));

        // 3. Cargar y descifrar
        DogeWalletHolder loaded = DogecoinWalletService.cargarBilleteraCifrada(testEmail, pin, false);
        assertNotNull(loaded);
        assertEquals(wallet.currentAddress, loaded.currentAddress);
        assertEquals(wallet.mnemonic, loaded.mnemonic);

        // 4. Intentar cargar con PIN incorrecto
        char[] wrongPin = new char[]{'9', '9', '9', '9'};
        assertThrows(Exception.class, () -> {
            DogecoinWalletService.cargarBilleteraCifrada(testEmail, wrongPin, false);
        });

        // 5. Eliminar
        DogecoinWalletService.eliminarBilletera(testEmail, false);
        assertFalse(DogecoinWalletService.existeBilletera(testEmail, false));

        Arrays.fill(pin, '0');
        Arrays.fill(wrongPin, '0');
    }

    @Test
    public void testBovedaSemillasDoge() throws Exception {
        String testEmail = "doge_vault@blackpassword.com";
        String codigo = "DOGE-COLD-STORAGE";
        String red = "MAINNET";
        String direccion = "D8vR5k9q2mXy...test";
        String fraseOriginal = "abandon ability able about above absent absorb abstract absurd abuse access accident";
        char[] pin = new char[]{'7', '8', '9', '0'};

        // 1. Guardar en bóveda
        boolean ok = LocalDatabaseService.guardarSemillaDogeWallet(testEmail, codigo, red, direccion, fraseOriginal, pin);
        assertTrue(ok, "Debe guardar la semilla Doge");

        // 2. Listar
        List<DogeSeedEntry> list = LocalDatabaseService.obtenerSemillasDogeWallet(testEmail);
        assertFalse(list.isEmpty());
        DogeSeedEntry found = null;
        for (DogeSeedEntry entry : list) {
            if (codigo.equalsIgnoreCase(entry.codigoWallet)) {
                found = entry;
                break;
            }
        }
        assertNotNull(found);
        assertEquals(red, found.red);

        // 3. Descifrar semilla
        String descifrada = LocalDatabaseService.descifrarSemillaDogeWallet(found, pin);
        assertEquals(fraseOriginal, descifrada);

        // 4. Eliminar de bóveda
        boolean del = LocalDatabaseService.eliminarSemillaDogeWallet(found.id);
        assertTrue(del);

        Arrays.fill(pin, '0');
    }
}
