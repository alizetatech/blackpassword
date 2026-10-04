package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.services.EntropyService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Generador de Contraseñas Pro v1.0.
 * Utiliza entropía de movimiento del ratón para generar claves de grado militar.
 */
public class PasswordGeneratorPro extends JDialog {

    private final EntropyService entropyService = new EntropyService();
    private JProgressBar progressBar;
    private JLabel lblStatus;
    private JTextField txtResult;
    private JCheckBox checkSymbols;
    private JButton btnCopy;
    private String generatedPassword = null;

    public PasswordGeneratorPro(java.awt.Window parent) {
        super(parent, "GENERADOR DE ENTROPÍA PRO", ModalityType.APPLICATION_MODAL);
        initComponents();
        setupEntropyArea();
    }

    private void initComponents() {
        setLayout(new BorderLayout(20, 20));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        getContentPane().setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("GENERADOR DE GRADO MILITAR", SwingConstants.CENTER);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #0078D4");
        add(lblTitle, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);

        lblStatus = new JLabel("Mueva el ratón sobre el área gris para generar entropía...", SwingConstants.CENTER);
        centerPanel.add(lblStatus, BorderLayout.NORTH);

        JPanel entropyArea = new JPanel();
        entropyArea.setPreferredSize(new Dimension(400, 200));
        entropyArea.setBackground(new Color(245, 245, 245));
        entropyArea.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 2, true));
        centerPanel.add(entropyArea, BorderLayout.CENTER);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(400, 15));
        progressBar.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        centerPanel.add(progressBar, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);

        txtResult = new JTextField();
        txtResult.setEditable(false);
        txtResult.setHorizontalAlignment(SwingConstants.CENTER);
        txtResult.setFont(new Font("Monospaced", Font.BOLD, 18));
        txtResult.setPreferredSize(new Dimension(400, 45));
        txtResult.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F8F9FA");
        bottomPanel.add(txtResult, BorderLayout.NORTH);

        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        
        checkSymbols = new JCheckBox("Incluir Símbolos Especiales", true);
        actions.add(checkSymbols, BorderLayout.WEST);

        btnCopy = new JButton("USAR ESTA CONTRASEÑA");
        btnCopy.setEnabled(false);
        btnCopy.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #107C10; foreground: #FFFFFF; font: bold");
        btnCopy.addActionListener(e -> {
            generatedPassword = txtResult.getText();
            dispose();
        });
        actions.add(btnCopy, BorderLayout.CENTER);

        bottomPanel.add(actions, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(getOwner());
    }

    private void setupEntropyArea() {
        JPanel area = (JPanel) ((JPanel)getContentPane().getComponent(1)).getComponent(1);
        area.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!entropyService.isReady()) {
                    entropyService.addMouseMovement(e.getX(), e.getY());
                    int progress = (int) (entropyService.getProgress() * 100);
                    progressBar.setValue(progress);
                    
                    if (entropyService.isReady()) {
                        finalizeEntropy();
                    }
                }
            }
        });
    }

    private void finalizeEntropy() {
        lblStatus.setText("¡Entropía recolectada con éxito!");
        lblStatus.setForeground(new Color(16, 124, 16));
        String pass = entropyService.generateSecurePassword(16, checkSymbols.isSelected());
        txtResult.setText(pass);
        btnCopy.setEnabled(true);
    }

    public String getGeneratedPassword() {
        return generatedPassword;
    }
}
