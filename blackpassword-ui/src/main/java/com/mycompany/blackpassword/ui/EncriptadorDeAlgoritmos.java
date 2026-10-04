package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mycompany.blackpassword.core.util.ToastNotification;
import org.apache.commons.codec.binary.Hex;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Encriptador de Algoritmos Criptográficos - LUXURY COMMAND EDITION.
 * Soporta funciones hash MD5, SHA-1, SHA-256 y SHA-512 con enmascaramiento y
 * visibilidad protegida bajo demanda tanto para el texto de entrada como para el hash resultante.
 */
public class EncriptadorDeAlgoritmos extends JInternalFrame {

    private JComboBox<String> cbalgoritmo;
    private JPasswordField txtexto;
    private JPasswordField txencripcion;
    private JCheckBox chkOcultarTexto;
    private JButton btnEncriptar;
    private JButton btnLimpiar;
    private JButton btnCerrar;
    private JButton btnCopiar;
    private JButton btnToggleVerHash;

    public EncriptadorDeAlgoritmos() {
        super("ENCRIPTADOR DE ALGORITMOS", true, true, true, true);
        initComponentsCustom();
        applyPremiumStyle();
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        this.setBorder(null);

        String fieldStyle = "arc: 12; margin: 6,12,6,12; background: #FFFFFF; borderWidth: 1; focusColor: #0078D4";
        txtexto.putClientProperty(FlatClientProperties.STYLE, fieldStyle + "; showRevealButton: true");
        txencripcion.putClientProperty(FlatClientProperties.STYLE, fieldStyle + "; showRevealButton: true; background: #F8F9FA");

        cbalgoritmo.putClientProperty(FlatClientProperties.STYLE, "arc: 12; margin: 4,8,4,8; background: #FFFFFF; borderWidth: 1; focusColor: #0078D4");

        btnEncriptar.putClientProperty(FlatClientProperties.STYLE,
                "arc: 16; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold 13");

        String secondaryBtn = "arc: 12; background: #F3F3F3; foreground: #333333; hoverBackground: #E5E5E5; borderWidth: 0; font: bold 12";
        btnLimpiar.putClientProperty(FlatClientProperties.STYLE, secondaryBtn);
        btnCerrar.putClientProperty(FlatClientProperties.STYLE, secondaryBtn);
        btnCopiar.putClientProperty(FlatClientProperties.STYLE, secondaryBtn);
        btnToggleVerHash.putClientProperty(FlatClientProperties.STYLE, secondaryBtn);

        try {
            btnEncriptar.setIcon(new FlatSVGIcon("img/shield.svg", 18, 18));
            btnLimpiar.setIcon(new FlatSVGIcon("img/panic.svg", 16, 16));
            btnCerrar.setIcon(new FlatSVGIcon("img/vault.svg", 16, 16));
            btnCopiar.setIcon(new FlatSVGIcon("img/files.svg", 16, 16));
            btnToggleVerHash.setIcon(new FlatSVGIcon("img/unlock.svg", 16, 16));
        } catch (Exception ignored) {}
    }

    private void initComponentsCustom() {
        setLayout(new BorderLayout());

        JPanel cardPanel = new JPanel(new GridBagLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        int row = 0;

        // Header Title
        JLabel lblTitulo = new JLabel("🔐 ENCRIPTADOR DE ALGORITMOS HASH");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font: bold 20; foreground: #0078D4");
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.gridy = row++;
        cardPanel.add(lblTitulo, gbc);

        JLabel lblSub = new JLabel("Genera funciones hash seguras con protección y ocultamiento de datos.");
        lblSub.putClientProperty(FlatClientProperties.STYLE, "foreground: #777777; font: 12");
        gbc.insets = new Insets(0, 0, 20, 0);
        gbc.gridy = row++;
        cardPanel.add(lblSub, gbc);

        // Algoritmo Selector
        JLabel lblAlgo = new JLabel("Algoritmo Criptográfico:");
        lblAlgo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.gridy = row++;
        cardPanel.add(lblAlgo, gbc);

        cbalgoritmo = new JComboBox<>(new String[]{"SHA-256", "SHA-512", "SHA-1", "MD5"});
        cbalgoritmo.setSelectedItem("SHA-256");
        cbalgoritmo.setPreferredSize(new Dimension(100, 38));
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.gridy = row++;
        cardPanel.add(cbalgoritmo, gbc);

        // Texto Input con Checkbox de ocultamiento
        JPanel textHeaderPanel = new JPanel(new BorderLayout());
        textHeaderPanel.setOpaque(false);
        JLabel lblTexto = new JLabel("Texto Plano a Encriptar:");
        lblTexto.setFont(new Font("Segoe UI", Font.BOLD, 12));
        textHeaderPanel.add(lblTexto, BorderLayout.WEST);

        chkOcultarTexto = new JCheckBox("Ocultar texto", true);
        chkOcultarTexto.setOpaque(false);
        chkOcultarTexto.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkOcultarTexto.addActionListener(e -> {
            txtexto.setEchoChar(chkOcultarTexto.isSelected() ? '•' : (char) 0);
        });
        textHeaderPanel.add(chkOcultarTexto, BorderLayout.EAST);

        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.gridy = row++;
        cardPanel.add(textHeaderPanel, gbc);

        txtexto = new JPasswordField();
        txtexto.setPreferredSize(new Dimension(100, 40));
        txtexto.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Introduce el texto que deseas encriptar...");
        txtexto.addPropertyChangeListener("echoChar", evt -> {
            chkOcultarTexto.setSelected(txtexto.echoCharIsSet());
        });
        // Permitir enter para encriptar
        txtexto.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ejecutarEncriptacion();
                }
            }
        });
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.gridy = row++;
        cardPanel.add(txtexto, gbc);

        // Resultado Encriptado (Hash)
        JLabel lblHash = new JLabel("Resultado Encriptado (Hash Protegido):");
        lblHash.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.gridy = row++;
        cardPanel.add(lblHash, gbc);

        JPanel hashRowPanel = new JPanel(new BorderLayout(8, 0));
        hashRowPanel.setOpaque(false);

        txencripcion = new JPasswordField();
        txencripcion.setEditable(false);
        txencripcion.setPreferredSize(new Dimension(100, 40));
        txencripcion.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "El hash encriptado aparecerá aquí protegido...");

        // Shortcut Ctrl+C en campo de hash protegido
        txencripcion.getInputMap().put(KeyStroke.getKeyStroke("ctrl C"), "copyHash");
        txencripcion.getActionMap().put("copyHash", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copiarAlPortapapeles();
            }
        });

        hashRowPanel.add(txencripcion, BorderLayout.CENTER);

        JPanel hashButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        hashButtonsPanel.setOpaque(false);

        btnToggleVerHash = new JButton("Ver / Ocultar");
        btnToggleVerHash.setToolTipText("Alternar visibilidad del hash");
        btnToggleVerHash.setPreferredSize(new Dimension(135, 40));
        btnToggleVerHash.addActionListener(e -> {
            boolean isMasked = txencripcion.echoCharIsSet();
            txencripcion.setEchoChar(isMasked ? (char) 0 : '•');
            btnToggleVerHash.setText(isMasked ? "Ocultar" : "Ver Hash");
        });
        hashButtonsPanel.add(btnToggleVerHash);

        btnCopiar = new JButton("Copiar");
        btnCopiar.setToolTipText("Copiar hash al portapapeles");
        btnCopiar.setPreferredSize(new Dimension(100, 40));
        btnCopiar.addActionListener(e -> copiarAlPortapapeles());
        hashButtonsPanel.add(btnCopiar);

        hashRowPanel.add(hashButtonsPanel, BorderLayout.EAST);

        gbc.insets = new Insets(0, 0, 20, 0);
        gbc.gridy = row++;
        cardPanel.add(hashRowPanel, gbc);

        // Botón Encriptar Principal
        btnEncriptar = new JButton("ENCRIPTAR TEXTO");
        btnEncriptar.setPreferredSize(new Dimension(100, 44));
        btnEncriptar.addActionListener(e -> ejecutarEncriptacion());
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.gridy = row++;
        cardPanel.add(btnEncriptar, gbc);

        // Barra inferior con Limpiar y Cerrar
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomPanel.setOpaque(false);

        btnLimpiar = new JButton("Limpiar Campos");
        btnLimpiar.setPreferredSize(new Dimension(140, 36));
        btnLimpiar.addActionListener(e -> {
            txtexto.setText("");
            txencripcion.setText("");
            ToastNotification.show(this, "Campos limpiados", ToastNotification.Type.INFO);
        });
        bottomPanel.add(btnLimpiar);

        btnCerrar = new JButton("Cerrar");
        btnCerrar.setPreferredSize(new Dimension(100, 36));
        btnCerrar.addActionListener(e -> dispose());
        bottomPanel.add(btnCerrar);

        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridy = row++;
        cardPanel.add(bottomPanel, gbc);

        add(cardPanel, BorderLayout.CENTER);
        pack();
        setSize(Math.max(getWidth(), 640), Math.max(getHeight(), 480));
    }

    private void ejecutarEncriptacion() {
        char[] pass = txtexto.getPassword();
        if (pass == null || pass.length == 0) {
            ToastNotification.show(this, "Introduce texto para encriptar", ToastNotification.Type.WARNING);
            return;
        }
        String texto = new String(pass);
        encriptacion(texto);
    }

    public void encriptacion(String texto) {
        if (cbalgoritmo.getSelectedItem() == null) {
            cbalgoritmo.setSelectedItem("SHA-256");
        }
        String algo = cbalgoritmo.getSelectedItem().toString();
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            byte[] digest = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            char[] hexChars = Hex.encodeHex(digest);
            String hash = new String(hexChars);

            txencripcion.setText(hash);
            txencripcion.setEchoChar('•'); // Se asegura que aparezca como contraseña protegida
            btnToggleVerHash.setText("Ver Hash");

            ToastNotification.show(this, "Encriptado con éxito (" + algo + ")", ToastNotification.Type.SUCCESS);
        } catch (NoSuchAlgorithmException ex) {
            JOptionPane.showMessageDialog(this, "Algoritmo no soportado: " + algo, "Error Criptográfico", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void copiarAlPortapapeles() {
        char[] hashChars = txencripcion.getPassword();
        if (hashChars == null || hashChars.length == 0) {
            ToastNotification.show(this, "No hay ningún hash generado para copiar", ToastNotification.Type.WARNING);
            return;
        }
        String hashStr = new String(hashChars);
        StringSelection sel = new StringSelection(hashStr);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
        ToastNotification.show(this, "Hash copiado al portapapeles", ToastNotification.Type.SUCCESS);
    }
}
