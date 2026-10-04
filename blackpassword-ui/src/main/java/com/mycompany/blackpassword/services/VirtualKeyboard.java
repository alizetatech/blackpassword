package com.mycompany.blackpassword.services;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;

/**
 * Teclado Virtual Seguro para evitar Keyloggers.
 * Las teclas se aleatorizan en cada apertura.
 */
public class VirtualKeyboard extends JDialog {

    private final JPasswordField targetField;
    private final JPanel keyPanel;

    public VirtualKeyboard(Window owner, JPasswordField target) {
        super(owner, "Teclado Seguro", ModalityType.APPLICATION_MODAL);
        this.targetField = target;
        this.keyPanel = new JPanel(new GridLayout(5, 13, 5, 5));
        
        initComponents();
        generateKeys();
        
        this.setResizable(false);
        this.pack();
        this.setLocationRelativeTo(owner);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF");

        JLabel lblInfo = new JLabel("Use el ratón para ingresar su contraseña de forma segura", SwingConstants.CENTER);
        lblInfo.putClientProperty(FlatClientProperties.STYLE, "font: $medium.font; foreground: #666666");
        mainPanel.add(lblInfo, BorderLayout.NORTH);

        keyPanel.setOpaque(false);
        mainPanel.add(keyPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        
        JButton btnClear = new JButton("Limpiar");
        btnClear.addActionListener(e -> targetField.setText(""));
        
        JButton btnDone = new JButton("Aceptar");
        btnDone.putClientProperty(FlatClientProperties.STYLE, "background: #0078D4; foreground: #FFFFFF; font: bold");
        btnDone.addActionListener(e -> dispose());

        bottomPanel.add(btnClear);
        bottomPanel.add(btnDone);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        this.setContentPane(mainPanel);
    }

    private void generateKeys() {
        keyPanel.removeAll();
        List<String> keys = new ArrayList<>();
        
        // Números y Letras
        for (char c = '0'; c <= '9'; c++) keys.add(String.valueOf(c));
        for (char c = 'a'; c <= 'z'; c++) keys.add(String.valueOf(c));
        
        // Caracteres especiales comunes
        String specials = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        for (char c : specials.toCharArray()) keys.add(String.valueOf(c));

        Collections.shuffle(keys);

        for (String key : keys) {
            JButton btn = createKeyButton(key);
            keyPanel.add(btn);
        }

        // Tecla de Retroceso
        JButton btnBack = createKeyButton("⌫");
        btnBack.putClientProperty(FlatClientProperties.STYLE, "background: #F3F3F3; foreground: #D83B01");
        btnBack.addActionListener(e -> {
            char[] current = targetField.getPassword();
            if (current.length > 0) {
                String text = new String(current);
                targetField.setText(text.substring(0, text.length() - 1));
            }
        });
        keyPanel.add(btnBack);

        keyPanel.revalidate();
        keyPanel.repaint();
    }

    private JButton createKeyButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(45, 45));
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 0,0,0,0; font: 14");
        btn.addActionListener(e -> {
            if (!text.equals("⌫")) {
                char[] current = targetField.getPassword();
                String newPass = new String(current) + text;
                targetField.setText(newPass);
            }
        });
        return btn;
    }
    
    public static void show(Window owner, JPasswordField target) {
        new VirtualKeyboard(owner, target).setVisible(true);
    }
}
