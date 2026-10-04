package com.mycompany.blackpassword.services;

import org.bitcoinj.wallet.Wallet;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class BitcoinWalletRescueTest {

    @Test
    public void testCifradoYDescifradoConRescateYBaseDeDatos() throws Exception {
        String testEmail = "test_rescue_user@blackpassword.com";
        char[] realPin = new char[]{'7', '8', '9', '0'};
        char[] zeroPin = new char[]{0, 0, 0, 0};

        // 1. Simular billetera guardada con PIN en ceros (el bug de verifyUserPin)
        Wallet originalWallet = BitcoinWalletService.crearBilletera(true);
        BitcoinWalletService.guardarBilleteraCifrada(originalWallet, testEmail, zeroPin, true);

        // Verificar que se guardó en la Base de Datos SQLite
        assertTrue(LocalDatabaseService.existeBtcWalletActiva(testEmail, true), "Debe existir en la base de datos local");

        // 2. Cargar con el PIN real del usuario -> el mecanismo de auto-rescate debe recuperar la billetera
        Wallet loadedWallet = BitcoinWalletService.cargarBilleteraCifrada(testEmail, realPin, true);
        assertNotNull(loadedWallet, "La billetera debe ser rescatada exitosamente");
        assertEquals(BitcoinWalletService.obtenerDireccion(originalWallet), BitcoinWalletService.obtenerDireccion(loadedWallet));

        // 3. Probar que ahora está re-cifrada con el PIN real en la BD
        Wallet reloadedWallet = BitcoinWalletService.cargarBilleteraCifrada(testEmail, realPin, true);
        assertNotNull(reloadedWallet, "La billetera re-cifrada debe cargar directamente con el PIN real");
        assertEquals(BitcoinWalletService.obtenerDireccion(originalWallet), BitcoinWalletService.obtenerDireccion(reloadedWallet));

        // 4. Probar libreta de direcciones en base de datos
        String testAddr = BitcoinWalletService.obtenerDireccion(originalWallet);
        BitcoinWalletService.guardarDireccionEtiquetada(testEmail, true, testAddr, "Mi Dirección Principal");
        List<BitcoinWalletService.BtcAddressEntry> entries = BitcoinWalletService.cargarDireccionesEtiquetadas(testEmail, true);
        assertFalse(entries.isEmpty(), "La libreta en BD debe contener la etiqueta");
        assertEquals("Mi Dirección Principal", entries.get(0).label);

        // 5. Destrucción segura en la BD
        BitcoinWalletService.eliminarBilletera(testEmail, true);
        assertFalse(LocalDatabaseService.existeBtcWalletActiva(testEmail, true), "Debe haber sido eliminada de la base de datos");

        Arrays.fill(realPin, '0');
        Arrays.fill(zeroPin, '0');
    }
}
