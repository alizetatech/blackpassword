package com.mycompany.blackpassword.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import org.junit.jupiter.api.Test;

public class WireguardServiceTest {

    @Test
    public void testGeneracionLlavesCurve25519() throws Exception {
        WireguardService.KeyPairWireguard keys = WireguardService.generarParLlaves();
        assertNotNull(keys);
        assertNotNull(keys.privateKey);
        assertNotNull(keys.publicKey);
        assertNotNull(keys.presharedKey);

        byte[] priv = Base64.getDecoder().decode(keys.privateKey);
        byte[] pub = Base64.getDecoder().decode(keys.publicKey);
        byte[] psk = Base64.getDecoder().decode(keys.presharedKey);

        assertEquals(32, priv.length, "La clave privada Curve25519 debe medir exactamente 32 bytes.");
        assertEquals(32, pub.length, "La clave pública Curve25519 debe medir exactamente 32 bytes.");
        assertEquals(32, psk.length, "La PresharedKey debe medir exactamente 32 bytes.");
    }

    @Test
    public void testParseoYReconstruccionConfig() {
        String template = "[Interface]\n"
                + "Address = 10.10.10.2/32,fd42::2/128\n"
                + "PrivateKey = aGVsbG93b3JsZHByaXZhdGVrZXkxMjM0NTY3ODkwMTI=\n"
                + "DNS = 1.1.1.1\n"
                + "MTU = 1450\n\n"
                + "[Peer]\n"
                + "PublicKey = c2VydmVycHVibGlja2V5MTIzNDU2Nzg5MDEyMzQ1Njc4OTA=\n"
                + "PresharedKey = cHJlc2hhcmVka2V5MTIzNDU2Nzg5MDEyMzQ1Njc4OTA=\n"
                + "AllowedIPs = 0.0.0.0/0,::/0\n"
                + "Endpoint = vpn.ejemplo.com:51820\n"
                + "PersistentKeepalive = 15\n";

        WireguardService.WireguardProfile profile = WireguardService.parseConfig(template, "Mi Servidor VPS");
        assertNotNull(profile);
        assertEquals("Mi Servidor VPS", profile.name);
        assertEquals("10.10.10.2/32,fd42::2/128", profile.address);
        assertEquals("1.1.1.1", profile.dns);
        assertEquals("1450", profile.mtu);
        assertEquals("vpn.ejemplo.com:51820", profile.endpoint);
        assertEquals("0.0.0.0/0,::/0", profile.allowedIps);
        assertEquals("15", profile.persistentKeepalive);

        String rebuilt = WireguardService.buildConfigString(profile);
        assertTrue(rebuilt.contains("[Interface]"));
        assertTrue(rebuilt.contains("10.10.10.2/32"));
        assertTrue(rebuilt.contains("vpn.ejemplo.com:51820"));
    }
}
