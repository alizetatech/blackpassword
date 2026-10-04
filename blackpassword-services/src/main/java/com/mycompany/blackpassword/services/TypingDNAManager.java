package com.mycompany.blackpassword.services;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor de Biometría Conductual v1.0 (Typing DNA).
 * Analiza el ritmo y la cadencia del tecleo para validar la identidad del usuario.
 */
public class TypingDNAManager {

    private final List<Long> pressTimestamps = new ArrayList<>();
    private final List<Long> releaseTimestamps = new ArrayList<>();

    /**
     * Registra un evento de pulsación.
     */
    public void recordKeyPress() {
        pressTimestamps.add(System.currentTimeMillis());
    }

    /**
     * Registra un evento de liberación de tecla.
     */
    public void recordKeyRelease() {
        releaseTimestamps.add(System.currentTimeMillis());
    }

    /**
     * Genera un perfil de ritmo basado en los intervalos capturados.
     * Formato: "dwell1,flight1,dwell2,flight2..."
     */
    public String getTypingProfile() {
        if (pressTimestamps.size() < 2 || pressTimestamps.size() != releaseTimestamps.size()) {
            return "INSUFFICIENT_DATA";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pressTimestamps.size(); i++) {
            // Dwell Time: Tiempo que la tecla permanece presionada
            long dwell = releaseTimestamps.get(i) - pressTimestamps.get(i);
            sb.append(dwell);
            
            if (i < pressTimestamps.size() - 1) {
                // Flight Time: Tiempo entre soltar una tecla y presionar la siguiente
                long flight = pressTimestamps.get(i + 1) - releaseTimestamps.get(i);
                sb.append(",").append(flight).append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Compara dos perfiles de ritmo y devuelve un porcentaje de similitud (0-100).
     * Utiliza el algoritmo de Distancia de Manhattan normalizada.
     */
    public static double compareProfiles(String profile1, String profile2) {
        if (profile1 == null || profile2 == null || profile1.equals("INSUFFICIENT_DATA") || profile2.equals("INSUFFICIENT_DATA")) {
            return 0;
        }

        String[] p1 = profile1.split(",");
        String[] p2 = profile2.split(",");

        if (p1.length != p2.length) return 0;

        double totalDiff = 0;
        double totalMagnitude = 0;

        for (int i = 0; i < p1.length; i++) {
            long v1 = Long.parseLong(p1[i]);
            long v2 = Long.parseLong(p2[i]);
            totalDiff += Math.abs(v1 - v2);
            totalMagnitude += Math.max(v1, v2);
        }

        if (totalMagnitude == 0) return 0;
        
        // Calculamos la similitud inversa a la diferencia
        double similarity = (1.0 - (totalDiff / totalMagnitude)) * 100;
        return Math.max(0, similarity);
    }

    public void reset() {
        pressTimestamps.clear();
        releaseTimestamps.clear();
    }
}
