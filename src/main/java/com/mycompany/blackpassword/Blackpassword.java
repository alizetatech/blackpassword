package com.mycompany.blackpassword;

import PanelPrincipal.FramePrincipal;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada de Blackpassword v6.2-PLATINUM.
 * Motor visual con inyección de doble capa para estética garantizada.
 */
public class Blackpassword {

    public static void main(String[] args) {
        // 1. CAPA DE INYECCIÓN 1: Motor Global
        try {
            // Configuración de renderizado ultra-calidad
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            
            // Setup de FlatLaf como estándar de la suite
            FlatLightLaf.setup();
            
            // Personalización de componentes
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("TextComponent.arc", 12);
            UIManager.put("ProgressBar.arc", 12);
            
            System.out.println("[v6.2] Capa 1: Motor Visual Premium activo.");
        } catch (Exception e) {
            System.err.println("Fallo en Capa 1: " + e.getMessage());
        }

        // 2. CAJA NEGRA: Vigilancia de errores
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "🚨 ERROR CRÍTICO: " + e.toString(), "Caja Negra v6.2", JOptionPane.ERROR_MESSAGE);
        });

        System.out.println("\n--- INICIANDO BLACKPASSWORD v6.2-PLATINUM ---");
        
        // 3. LANZAMIENTO CON REFRESCO FORZADO
        java.awt.EventQueue.invokeLater(() -> {
            try {
                FramePrincipal frame = new FramePrincipal();
                // Forzar a toda la ventana a adoptar el tema antes de mostrarse
                SwingUtilities.updateComponentTreeUI(frame);
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
