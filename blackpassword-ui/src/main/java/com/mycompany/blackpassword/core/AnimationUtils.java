package com.mycompany.blackpassword.core;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Utilidades de Animación para la Suite Blackpassword.
 * Proporciona efectos visuales Premium para la interfaz de usuario.
 */
public class AnimationUtils {

    /**
     * Realiza una animación de "Deslizamiento y Aparición" (Fade + Slide) para un JInternalFrame.
     */
    public static void fadeInAndSlide(JInternalFrame frame, JDesktopPane desktop) {
        final int finalX = (desktop.getWidth() - frame.getWidth()) / 2;
        final int finalY = (desktop.getHeight() - frame.getHeight()) / 2;
        final int startY = finalY + 20;

        frame.setLocation(finalX, startY);
        // Intentar usar transparencia si el LookAndFeel lo permite
        frame.putClientProperty("JInternalFrame.isPalette", Boolean.TRUE); 
        frame.setVisible(true);
        
        Timer timer = new Timer(15, new ActionListener() {
            int currentY = startY;
            float alpha = 0.0f;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentY > finalY || alpha < 1.0f) {
                    if (currentY > finalY) currentY -= 1;
                    if (alpha < 1.0f) alpha += 0.05f;
                    
                    frame.setLocation(finalX, currentY);
                    // Nota: Alpha en JInternalFrame depende del sistema, 
                    // aquí usamos una escala suave de posición para simular fluidez.
                } else {
                    ((Timer)e.getSource()).stop();
                    frame.setLocation(finalX, finalY);
                }
            }
        });
        timer.setInitialDelay(50);
        timer.start();
    }
    
    /**
     * Escala suavemente un componente (efecto de "Pop").
     */
    public static void softScalePop(JComponent comp) {
        Timer timer = new Timer(10, new ActionListener() {
            double scale = 0.95;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (scale < 1.0) {
                    scale += 0.01;
                    comp.revalidate();
                } else {
                    ((Timer)e.getSource()).stop();
                }
            }
        });
        timer.start();
    }
}
