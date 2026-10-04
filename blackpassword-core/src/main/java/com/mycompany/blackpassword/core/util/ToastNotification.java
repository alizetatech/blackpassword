package com.mycompany.blackpassword.core.util;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import javax.swing.*;

/**
 * Utilidad de notificaciones Toast premium.
 * Migrada al módulo core para uso centralizado.
 */
public class ToastNotification extends JPanel {

    public enum Type {
        SUCCESS, ERROR, INFO, WARNING
    }

    private static final int DISMISS_DELAY = 3000;

    public ToastNotification(JFrame parent, String message, Type type) {
        setLayout(new BorderLayout(10, 0));
        
        // Estética Premium según el tipo
        Color bgColor;
        switch (type) {
            case SUCCESS:
                bgColor = new Color(34, 139, 34); // ForestGreen
                break;
            case ERROR:
                bgColor = new Color(178, 34, 34); // Firebrick
                break;
            case WARNING:
                bgColor = new Color(255, 152, 0); // Orange/Amber
                break;
            case INFO:
            default:
                bgColor = new Color(0, 120, 212); // Modern Blue
                break;
        }
        
        setBackground(bgColor);
        putClientProperty(FlatClientProperties.STYLE, "arc: 99");

        JLabel lblMsg = new JLabel(message);
        lblMsg.setForeground(Color.WHITE);
        lblMsg.setFont(new Font("Inter", Font.BOLD, 13));
        add(lblMsg, BorderLayout.CENTER);
        
        setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }

    /**
     * Método estático principal para mostrar notificaciones.
     */
    public static void show(Component parent, String message, Type type) {
        Window window = (parent != null) ? SwingUtilities.getWindowAncestor(parent) : null;
        
        // Si el parent es nulo o no tiene ventana, buscamos la ventana activa
        if (window == null) {
            for (Window w : Window.getWindows()) {
                if (w instanceof JFrame && w.isActive()) {
                    window = w;
                    break;
                }
            }
        }
        
        // Fallback al primer JFrame visible si no hay uno activo
        if (window == null) {
            for (Window w : Window.getWindows()) {
                if (w instanceof JFrame && w.isVisible()) {
                    window = w;
                    break;
                }
            }
        }

        if (window instanceof JFrame) {
            JFrame frame = (JFrame) window;
            ToastNotification toast = new ToastNotification(frame, message, type);
            
            JLayeredPane layeredPane = frame.getLayeredPane();
            layeredPane.add(toast, JLayeredPane.POPUP_LAYER);
            
            // Posicionamiento en la parte inferior central
            toast.setSize(toast.getPreferredSize());
            int x = (frame.getWidth() - toast.getWidth()) / 2;
            int y = frame.getHeight() - 100;
            toast.setLocation(x, y);
            
            toast.setVisible(true);
            
            Timer timer = new Timer(DISMISS_DELAY, e -> {
                toast.setVisible(false);
                layeredPane.remove(toast);
            });
            timer.setRepeats(false);
            timer.start();
        }
    }
}
