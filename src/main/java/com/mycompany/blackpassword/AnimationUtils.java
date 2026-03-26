package com.mycompany.blackpassword;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Utilidades de Animación para la Suite Blackpassword.
 * Proporciona efectos visuales Premium para la interfaz de usuario.
 */
public class AnimationUtils {

    /**
     * Realiza una animación de "Deslizamiento y Aparición" para un JInternalFrame.
     * @param frame El frame a animar.
     * @param desktop El desktop pane donde se encuentra.
     */
    public static void fadeInAndSlide(JInternalFrame frame, JDesktopPane desktop) {
        final int finalX = (desktop.getWidth() - frame.getWidth()) / 2;
        final int finalY = (desktop.getHeight() - frame.getHeight()) / 2;
        final int startY = finalY + 30; // Empieza 30 píxeles abajo

        frame.setLocation(finalX, startY);
        frame.setVisible(true);
        
        // Animación de posición
        Timer timer = new Timer(10, new ActionListener() {
            int currentY = startY;
            float opacity = 0.0f;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentY > finalY) {
                    currentY -= 2; // Sube 2 píxeles por frame
                    frame.setLocation(finalX, currentY);
                } else {
                    ((Timer)e.getSource()).stop();
                    frame.setLocation(finalX, finalY); // Asegurar posición final
                }
            }
        });
        timer.start();
    }
    
    /**
     * Animación suave para cerrar una ventana.
     */
    public static void fadeOutAndClose(JInternalFrame frame) {
        Timer timer = new Timer(10, new ActionListener() {
            int steps = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (steps < 10) {
                    frame.setLocation(frame.getX(), frame.getY() + 2);
                    steps++;
                } else {
                    ((Timer)e.getSource()).stop();
                    frame.dispose();
                }
            }
        });
        timer.start();
    }
}
