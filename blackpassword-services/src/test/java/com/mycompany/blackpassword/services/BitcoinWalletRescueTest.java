package com.mycompany.blackpassword.services;

import org.bitcoinj.wallet.Wallet;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class BitcoinWalletRescueTest {

    @Test
    public void testCifradoYDescifradoConRescate() throws Exception {
        String testEmail = "test_rescue_user@blackpassword.com";
        char[] realPin = new char[]{'7', '8', '9', '0'};
        char[] zeroPin = new char[]{0, 0, 0, 0};

        // 1. Simular billetera guardada con PIN en ceros (el bug de verifyUserPin)
        Wallet originalWallet = BitcoinWalletService.crearBilletera(true);
        BitcoinWalletService.guardarBilleteraCifrada(originalWallet, testEmail, zeroPin, true);

        // 2. Cargar con el PIN real del usuario -> el mecanismo de auto-rescate debe recuperar la billetera
        Wallet loadedWallet = BitcoinWalletService.cargarBilleteraCifrada(testEmail, realPin, true);
        assertNotNull(loadedWallet, "La billetera debe ser rescatada exitosamente");
        assertEquals(BitcoinWalletService.obtenerDireccion(originalWallet), BitcoinWalletService.obtenerDireccion(loadedWallet));

        // 3. Probar que ahora está re-cifrada con el PIN real
        Wallet reloadedWallet = BitcoinWalletService.cargarBilleteraCifrada(testEmail, realPin, true);
        assertNotNull(reloadedWallet, "La billetera re-cifrada debe cargar directamente con el PIN real");
        assertEquals(BitcoinWalletService.obtenerDireccion(originalWallet), BitcoinWalletService.obtenerDireccion(reloadedWallet));

        // Limpieza
        BitcoinWalletService.eliminarBilletera(testEmail, true);
        Arrays.fill(realPin, '0');
        Arrays.fill(zeroPin, '0');
    }
}
