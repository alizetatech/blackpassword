package com.mycompany.blackpassword;

import com.formdev.flatlaf.FlatClientProperties;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Sistema de Notificaciones "Toast" Premium.
 * Proporciona avisos visuales no intrusivos y animados.
 */
public class ToastNotification extends JWindow {

    public enum Type { SUCCESS, INFO, WARNING, ERROR }

    public ToastNotification(JFrame parent, String message, Type type) {
        super(parent);
        
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc: 20");
        
        Color bgColor;
        String iconEmoji;
        
        switch (type) {
            case SUCCESS -> { bgColor = new Color(16, 124, 16); iconEmoji = "✅"; }
            case ERROR -> { bgColor = new Color(216, 59, 1); iconEmoji = "❌"; }
            case WARNING -> { bgColor = new Color(255, 185, 0); iconEmoji = "⚠️"; }
            default -> { bgColor = new Color(0, 120, 212); iconEmoji = "ℹ️"; }
        }
        
        panel.setBackground(bgColor);
        
        JLabel lblIcon = new JLabel(iconEmoji);
        lblIcon.setForeground(Color.WHITE);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        lblIcon.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 0));
        
        JLabel lblMsg = new JLabel(message);
        lblMsg.setForeground(Color.WHITE);
        lblMsg.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblMsg.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 20));
        
        panel.add(lblIcon, BorderLayout.WEST);
        panel.add(lblMsg, BorderLayout.CENTER);
        
        setContentPane(panel);
        pack();
        
        // Posicionamiento (Esquina inferior derecha del padre)
        int x = parent.getX() + parent.getWidth() - getWidth() - 20;
        int y = parent.getY() + parent.getHeight() - getHeight() - 20;
        setLocation(x, y);
        
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
        setAlwaysOnTop(true);
    }

    public void showToast() {
        setVisible(true);
        Timer timer = new Timer(3000, e -> {
            fadeOut();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void fadeOut() {
        Timer timer = new Timer(20, null);
        timer.addActionListener(e -> {
            dispose();
            timer.stop();
        });
        timer.start();
    }

    /**
     * Método estático de conveniencia.
     */
    public static void show(Component parent, String message, Type type) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        if (window instanceof JFrame frame) {
            new ToastNotification(frame, message, type).showToast();
        }
    }
}
