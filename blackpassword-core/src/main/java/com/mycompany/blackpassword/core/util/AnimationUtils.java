package com.mycompany.blackpassword.core.util;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import javax.swing.*;

/**
 * Utilidad de animaciones premium para JInternalFrames y componentes.
 * Migrada al módulo core para uso centralizado.
 */
public class AnimationUtils {

    /**
     * Realiza un efecto de desvanecimiento y deslizamiento suave para abrir ventanas internas.
     */
    public static void fadeInAndSlide(JInternalFrame frame, JDesktopPane desktop) {
        if (frame == null || desktop == null) return;

        if (frame.getParent() != desktop) {
            desktop.add(frame);
        }
        
        // Posición final centrada
        Dimension desktopSize = desktop.getSize();
        Dimension frameSize = frame.getSize();
        int finalX = (desktopSize.width - frameSize.width) / 2;
        int finalY = (desktopSize.height - frameSize.height) / 2;

        // Punto de inicio (ligeramente más abajo para efecto slide-up)
        frame.setLocation(finalX, finalY + 30);
        frame.setVisible(true);
        frame.toFront();

        Timer timer = new Timer(15, null);
        long startTime = System.currentTimeMillis();
        int duration = 300; // ms

        timer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1f, (float) elapsed / duration);
            
            // Efecto de deslizamiento (Easy-out)
            float easedProgress = 1f - (float) Math.pow(1f - progress, 3);
            int currentY = (finalY + 30) - (int)(30 * easedProgress);
            
            frame.setLocation(finalX, currentY);
            
            // El alpha decorativo se maneja vía FlatLaf si está soportado
            frame.putClientProperty(FlatClientProperties.STYLE, "opacity: " + (int)(100 * progress));

            if (progress >= 1f) {
                timer.stop();
                frame.setLocation(finalX, finalY);
                try {
                    frame.toFront();
                    frame.setSelected(true);
                } catch (Exception ex) {}
            }
        });
        timer.start();
    }

    public static void fadeIn(JComponent component) {
        Timer timer = new Timer(10, null);
        long startTime = System.currentTimeMillis();
        int duration = 250;

        timer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1f, (float) elapsed / duration);
            component.putClientProperty(FlatClientProperties.STYLE, "background: rgba(255,255,255," + progress + ")");
            if (progress >= 1f) timer.stop();
        });
        timer.start();
    }
}
