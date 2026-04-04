package com.mycompany.blackpassword.services;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Entropía por Movimiento v1.0.
 * Recolecta coordenadas del ratón para generar semillas aleatorias de alta calidad.
 */
public class EntropyService {

    private final List<Byte> entropyPool = new ArrayList<>();
    private static final int REQUIRED_ENTROPY = 256; // Bytes de entropía necesarios

    public void addMouseMovement(int x, int y) {
        if (entropyPool.size() < REQUIRED_ENTROPY) {
            entropyPool.add((byte) (x & 0xFF));
            entropyPool.add((byte) (y & 0xFF));
            entropyPool.add((byte) (System.nanoTime() & 0xFF));
        }
    }

    public float getProgress() {
        return (float) entropyPool.size() / REQUIRED_ENTROPY;
    }

    public boolean isReady() {
        return entropyPool.size() >= REQUIRED_ENTROPY;
    }

    public byte[] getSeed() {
        byte[] seed = new byte[entropyPool.size()];
        for (int i = 0; i < entropyPool.size(); i++) {
            seed[i] = entropyPool.get(i);
        }
        return seed;
    }

    public String generateSecurePassword(int length, boolean includeSymbols) {
        if (!isReady()) return null;

        SecureRandom random = new SecureRandom(getSeed());
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String symbols = "!@#$%^&*()-_=+[]{}|;:,.<>?";
        
        String charPool = upper + lower + numbers;
        if (includeSymbols) charPool += symbols;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(charPool.charAt(random.nextInt(charPool.length())));
        }
        
        // Limpiar pool tras generar
        entropyPool.clear();
        return sb.toString();
    }
}
