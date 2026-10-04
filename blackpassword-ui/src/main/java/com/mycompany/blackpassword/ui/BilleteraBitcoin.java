package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mycompany.blackpassword.core.util.ToastNotification;
import com.mycompany.blackpassword.services.AuditService;
import com.mycompany.blackpassword.services.BitcoinWalletService;
import com.mycompany.blackpassword.services.QRCodeService;
import com.mycompany.blackpassword.services.SecuritySession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.bitcoinj.wallet.Wallet;

/**
 * Módulo de Billetera Bitcoin HD v1.0 - Zero-Trust SegWit Suite.
 * Genera, almacena y gestiona billeteras Bitcoin (BIP-39 / BIP-84) blindadas localmente con AES-256 GCM.
 */
public class BilleteraBitcoin extends javax.swing.JInternalFrame {

    private final String userEmail;
    private Wallet currentWallet;
    private boolean isTestnet = true;

    // Componentes visuales
    private JPanel contentContainer;
    private JLabel lblBalanceBtc;
    private JLabel lblBalanceSats;
    private JTextField txtAddress;
    private JLabel lblQr;
    private JLabel lblNetworkBadge;
    private JButton btnRefreshBalance;

    public BilleteraBitcoin(String email) {
        super("Billetera Bitcoin HD", true, true, true, true);
        this.userEmail = email;
        this.isTestnet = BitcoinWalletService.isTestnet(email);

        initComponentsCustom();
        applyPremiumStyle();
        cargarBilleteraInicial();
    }

    private void applyPremiumStyle() {
        putClientProperty(FlatClientProperties.STYLE, "background: #F8F9FA");
        setBorder(null);
    }

    private void initComponentsCustom() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(820, 680));

        // --- TOP BAR ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(15, 25, 15, 25)
        ));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);

        JLabel lblIcon = new JLabel("₿");
        lblIcon.putClientProperty(FlatClientProperties.STYLE, "font: bold 26; foreground: #F7931A");
        titlePanel.add(lblIcon);

        JPanel textTitles = new JPanel(new GridLayout(2, 1, 0, 2));
        textTitles.setOpaque(false);
        JLabel lblTitle = new JLabel("BÓVEDA BITCOIN HD (SEGWIT)");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 16; foreground: #202020");
        JLabel lblSub = new JLabel("Auto-custodia de criptoactivos blindada con criptografía AES-256 GCM y PBKDF2");
        lblSub.putClientProperty(FlatClientProperties.STYLE, "font: 11; foreground: #666666");
        textTitles.add(lblTitle);
        textTitles.add(lblSub);
        titlePanel.add(textTitles);

        topBar.add(titlePanel, BorderLayout.WEST);

        lblNetworkBadge = new JLabel(isTestnet ? "● RED TESTNET (PRUEBAS)" : "● RED MAINNET (REAL)");
        lblNetworkBadge.putClientProperty(FlatClientProperties.STYLE, isTestnet 
                ? "font: bold 11; foreground: #0078D4; background: #E1F0FE; border: 4,12,4,12, #E1F0FE, 1, 16"
                : "font: bold 11; foreground: #D83B01; background: #FDE7E9; border: 4,12,4,12, #FDE7E9, 1, 16");
        topBar.add(lblNetworkBadge, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // --- MAIN CONTAINER ---
        contentContainer = new JPanel(new BorderLayout());
        contentContainer.setOpaque(false);
        add(new JScrollPane(contentContainer), BorderLayout.CENTER);
    }

    private void cargarBilleteraInicial() {
        if (!BitcoinWalletService.existeBilletera(userEmail)) {
            renderizarPantallaBienvenida();
            return;
        }

        JPasswordField pfPin = new JPasswordField(4);
        int opc = JOptionPane.showConfirmDialog(this,
                new Object[]{"Ingrese su PIN de seguridad para descifrar su Billetera Bitcoin:", pfPin},
                "Desbloqueo de Billetera",
                JOptionPane.OK_CANCEL_OPTION);

        if (opc != JOptionPane.OK_OPTION) {
            renderizarPantallaBienvenida();
            return;
        }

        char[] pin = pfPin.getPassword();
        try {
            currentWallet = BitcoinWalletService.cargarBilleteraCifrada(userEmail, pin);
            isTestnet = BitcoinWalletService.isTestnet(userEmail);
            renderizarPantallaDashboard();
            ToastNotification.show(this, "Billetera desbloqueada con éxito.", ToastNotification.Type.SUCCESS);
            AuditService.log(userEmail, "BTC_WALLET_UNLOCK", "Billetera desbloqueada exitosamente.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto o error al descifrar la billetera.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
            renderizarPantallaBienvenida();
        } finally {
            Arrays.fill(pin, '0');
        }
    }

    private void renderizarPantallaBienvenida() {
        contentContainer.removeAll();

        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        int row = 0;

        JLabel lblHero = new JLabel("₿");
        lblHero.setHorizontalAlignment(SwingConstants.CENTER);
        lblHero.putClientProperty(FlatClientProperties.STYLE, "font: bold 72; foreground: #F7931A");
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = row++;
        p.add(lblHero, gbc);

        JLabel lblWelcome = new JLabel("No tienes una billetera Bitcoin vinculada en este perfil");
        lblWelcome.setHorizontalAlignment(SwingConstants.CENTER);
        lblWelcome.putClientProperty(FlatClientProperties.STYLE, "font: bold 18; foreground: #222222");
        gbc.insets = new Insets(0, 0, 8, 0); gbc.gridy = row++;
        p.add(lblWelcome, gbc);

        JLabel lblDesc = new JLabel("Puedes generar una nueva cartera jerárquica con 12 palabras semilla (BIP-39) o restaurar una existente.");
        lblDesc.setHorizontalAlignment(SwingConstants.CENTER);
        lblDesc.putClientProperty(FlatClientProperties.STYLE, "font: 13; foreground: #666666");
        gbc.insets = new Insets(0, 0, 30, 0); gbc.gridy = row++;
        p.add(lblDesc, gbc);

        // Opciones de acción
        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnGroup.setOpaque(false);

        JButton btnCrear = new JButton("✨ CREAR NUEVA BILLETERA");
        btnCrear.setPreferredSize(new Dimension(240, 48));
        btnCrear.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #0078D4; foreground: #FFFFFF; font: bold 13");
        btnCrear.addActionListener(e -> mostrarDialogoCreacion());
        btnGroup.add(btnCrear);

        JButton btnRestaurar = new JButton("📥 RESTAURAR DESDE 12 PALABRAS");
        btnRestaurar.setPreferredSize(new Dimension(260, 48));
        btnRestaurar.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #FFFFFF; foreground: #333333; font: bold 13; border: 1,1,1,1,#CCCCCC");
        btnRestaurar.addActionListener(e -> mostrarDialogoRestauracion());
        btnGroup.add(btnRestaurar);

        gbc.insets = new Insets(0, 0, 20, 0); gbc.gridy = row++;
        p.add(btnGroup, gbc);

        contentContainer.add(p, BorderLayout.CENTER);
        contentContainer.revalidate();
        contentContainer.repaint();
    }

    private void renderizarPantallaDashboard() {
        contentContainer.removeAll();

        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        // --- TARJETA DE BALANCE ---
        JPanel balanceCard = new JPanel(new BorderLayout(15, 10));
        balanceCard.setBackground(Color.WHITE);
        balanceCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));
        balanceCard.putClientProperty(FlatClientProperties.STYLE, "arc: 18");

        JPanel balanceLeft = new JPanel(new GridLayout(3, 1, 0, 4));
        balanceLeft.setOpaque(false);

        JLabel lblBalTitle = new JLabel("BALANCE DISPONIBLE");
        lblBalTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 11; foreground: #777777");
        balanceLeft.add(lblBalTitle);

        lblBalanceBtc = new JLabel("0.00000000 BTC");
        lblBalanceBtc.putClientProperty(FlatClientProperties.STYLE, "font: bold 26; foreground: #202020");
        balanceLeft.add(lblBalanceBtc);

        lblBalanceSats = new JLabel("0 Satoshis");
        lblBalanceSats.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #888888");
        balanceLeft.add(lblBalanceSats);

        balanceCard.add(balanceLeft, BorderLayout.CENTER);

        JPanel balanceRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        balanceRight.setOpaque(false);

        btnRefreshBalance = new JButton("🔄 ACTUALIZAR");
        btnRefreshBalance.setPreferredSize(new Dimension(140, 40));
        btnRefreshBalance.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #F3F3F3; font: bold 12");
        btnRefreshBalance.addActionListener(e -> refrescarBalance());
        balanceRight.add(btnRefreshBalance);

        balanceCard.add(balanceRight, BorderLayout.EAST);
        main.add(balanceCard, BorderLayout.NORTH);

        // --- TARJETA DE DIRECCIÓN Y CÓDIGO QR ---
        JPanel addressCard = new JPanel(new GridBagLayout());
        addressCard.setBackground(Color.WHITE);
        addressCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));
        addressCard.putClientProperty(FlatClientProperties.STYLE, "arc: 18");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        int dRow = 0;

        JLabel lblRecTitle = new JLabel("DIRECCIÓN DE RECEPCIÓN (SEGWIT NATIVO)");
        lblRecTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #0078D4");
        gbc.insets = new Insets(0, 0, 10, 0); gbc.gridy = dRow++;
        addressCard.add(lblRecTitle, gbc);

        String address = BitcoinWalletService.obtenerDireccion(currentWallet);

        JPanel addressRow = new JPanel(new BorderLayout(10, 0));
        addressRow.setOpaque(false);

        txtAddress = new JTextField(address);
        txtAddress.setEditable(false);
        txtAddress.putClientProperty(FlatClientProperties.STYLE, "font: bold 13; background: #F8F9FA; border: 8,12,8,12");
        addressRow.add(txtAddress, BorderLayout.CENTER);

        JButton btnCopy = new JButton("📋 COPIAR");
        btnCopy.setPreferredSize(new Dimension(100, 38));
        btnCopy.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #0078D4; foreground: #FFFFFF; font: bold 12");
        btnCopy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(address), null);
            ToastNotification.show(this, "Dirección copiada al portapapeles.", ToastNotification.Type.SUCCESS);
        });
        addressRow.add(btnCopy, BorderLayout.EAST);

        gbc.insets = new Insets(0, 0, 20, 0); gbc.gridy = dRow++;
        addressCard.add(addressRow, gbc);

        // QR Code
        lblQr = new JLabel();
        lblQr.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            BufferedImage qrImg = QRCodeService.generateQRCode(address, 200, 200);
            lblQr.setIcon(new ImageIcon(qrImg));
        } catch (Exception ex) {
            lblQr.setText("[Error generando QR]");
        }
        gbc.insets = new Insets(0, 0, 25, 0); gbc.gridy = dRow++;
        addressCard.add(lblQr, gbc);

        // Barra inferior de utilidades
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bottomBar.setOpaque(false);

        JButton btnVerSemilla = new JButton("🔑 VER 12 PALABRAS SEMILLA");
        btnVerSemilla.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FFF4CE; foreground: #795B00; font: bold 12");
        btnVerSemilla.addActionListener(e -> revelarSemillaConPin());
        bottomBar.add(btnVerSemilla);

        JButton btnEliminar = new JButton("🗑️ DESVINCULAR BILLETERA");
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FDE7E9; foreground: #A80000; font: bold 12");
        btnEliminar.addActionListener(e -> desvincularBilleteraConPin());
        bottomBar.add(btnEliminar);

        gbc.insets = new Insets(0, 0, 0, 0); gbc.gridy = dRow++;
        addressCard.add(bottomBar, gbc);

        main.add(addressCard, BorderLayout.CENTER);

        contentContainer.add(main, BorderLayout.CENTER);
        contentContainer.revalidate();
        contentContainer.repaint();

        // Consulta inicial de balance
        refrescarBalance();
    }

    private void refrescarBalance() {
        if (currentWallet == null) return;
        btnRefreshBalance.setEnabled(false);
        btnRefreshBalance.setText("Consultando...");
        String address = BitcoinWalletService.obtenerDireccion(currentWallet);

        SwingWorker<Long, Void> worker = new SwingWorker<>() {
            @Override
            protected Long doInBackground() {
                return BitcoinWalletService.consultarBalanceSats(address, isTestnet);
            }

            @Override
            protected void done() {
                try {
                    long sats = get();
                    if (sats >= 0) {
                        lblBalanceBtc.setText(BitcoinWalletService.satsToBtc(sats));
                        lblBalanceSats.setText(String.format("%,d Satoshis", sats));
                        ToastNotification.show(BilleteraBitcoin.this, "Balance actualizado.", ToastNotification.Type.SUCCESS);
                    } else {
                        ToastNotification.show(BilleteraBitcoin.this, "Nodo ocupado. Reintente en un momento.", ToastNotification.Type.WARNING);
                    }
                } catch (Exception ex) {
                    ToastNotification.show(BilleteraBitcoin.this, "Error al consultar balance.", ToastNotification.Type.ERROR);
                } finally {
                    btnRefreshBalance.setEnabled(true);
                    btnRefreshBalance.setText("🔄 ACTUALIZAR");
                }
            }
        };
        worker.execute();
    }

    private void mostrarDialogoCreacion() {
        JComboBox<String> cbRed = new JComboBox<>(new String[]{"Red Testnet (Recomendado para pruebas)", "Red Mainnet (Red Real)"});
        int opt = JOptionPane.showConfirmDialog(this, 
                new Object[]{"Seleccione la red Bitcoin de su nueva billetera:", cbRed},
                "Configuración de Red", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION) return;

        boolean testnet = cbRed.getSelectedIndex() == 0;
        Wallet nuevaWallet = BitcoinWalletService.crearBilletera(testnet);
        List<String> palabras = BitcoinWalletService.obtenerPalabras(nuevaWallet);

        mostrarVentanaPalabrasSemilla(palabras, "NUEVA BILLETERA CREADA CON ÉXITO", () -> {
            solicitarPinYGuardar(nuevaWallet, testnet);
        });
    }

    private void mostrarDialogoRestauracion() {
        JTextField txtPalabras = new JTextField();
        txtPalabras.putClientProperty(FlatClientProperties.STYLE, "font: 13; background: #F8F9FA");
        JComboBox<String> cbRed = new JComboBox<>(new String[]{"Red Testnet (Pruebas)", "Red Mainnet (Real)"});

        Object[] msg = {
            "Ingrese sus 12 o 24 palabras separadas por espacios:", txtPalabras,
            "Seleccione la red correspondiente:", cbRed
        };

        int opt = JOptionPane.showConfirmDialog(this, msg, "Restaurar Billetera Bitcoin", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION) return;

        String raw = txtPalabras.getText().trim();
        if (raw.isEmpty()) return;

        List<String> palabras = Arrays.asList(raw.toLowerCase().split("\\s+"));
        if (palabras.size() != 12 && palabras.size() != 24) {
            JOptionPane.showMessageDialog(this, "Debe ingresar exactamente 12 o 24 palabras.", "Formato Inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            boolean testnet = cbRed.getSelectedIndex() == 0;
            Wallet wallet = BitcoinWalletService.restaurarBilletera(palabras, testnet);
            solicitarPinYGuardar(wallet, testnet);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al restaurar billetera: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solicitarPinYGuardar(Wallet wallet, boolean testnet) {
        JPasswordField pfPin = new JPasswordField(4);
        int opc = JOptionPane.showConfirmDialog(this,
                new Object[]{"Ingrese su PIN para cifrar y blindar la billetera en su dispositivo:", pfPin},
                "Blindaje de Billetera",
                JOptionPane.OK_CANCEL_OPTION);

        if (opc != JOptionPane.OK_OPTION) return;

        char[] pin = pfPin.getPassword();
        if (!SecuritySession.verifyUserPin(pin)) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto. Operación cancelada.", "Error", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            return;
        }

        try {
            BitcoinWalletService.guardarBilleteraCifrada(wallet, userEmail, pin, testnet);
            this.currentWallet = wallet;
            this.isTestnet = testnet;
            this.lblNetworkBadge.setText(isTestnet ? "● RED TESTNET (PRUEBAS)" : "● RED MAINNET (REAL)");
            this.lblNetworkBadge.putClientProperty(FlatClientProperties.STYLE, isTestnet 
                    ? "font: bold 11; foreground: #0078D4; background: #E1F0FE; border: 4,12,4,12, #E1F0FE, 1, 16"
                    : "font: bold 11; foreground: #D83B01; background: #FDE7E9; border: 4,12,4,12, #FDE7E9, 1, 16");

            renderizarPantallaDashboard();
            ToastNotification.show(this, "Billetera guardada y blindada con éxito.", ToastNotification.Type.SUCCESS);
            AuditService.log(userEmail, "BTC_WALLET_SAVED", "Billetera guardada con éxito (Testnet: " + testnet + ")");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Fallo al guardar la billetera: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(pin, '0');
        }
    }

    private void mostrarVentanaPalabrasSemilla(List<String> palabras, String titulo, Runnable onConfirm) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), titulo, JDialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());
        dlg.setSize(620, 520);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new BorderLayout(0, 15));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        JLabel lblWarn = new JLabel("⚠️ ADVERTENCIA CRÍTICA: ANOTE ESTAS 12 PALABRAS EN PAPEL");
        lblWarn.putClientProperty(FlatClientProperties.STYLE, "font: bold 13; foreground: #D83B01");
        p.add(lblWarn, BorderLayout.NORTH);

        JPanel gridPalabras = new JPanel(new GridLayout(4, 3, 10, 10));
        gridPalabras.setOpaque(false);

        for (int i = 0; i < palabras.size(); i++) {
            JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            card.setBackground(new Color(248, 249, 250));
            card.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true));
            card.putClientProperty(FlatClientProperties.STYLE, "arc: 10");

            JLabel num = new JLabel(String.valueOf(i + 1) + ".");
            num.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #0078D4");
            JLabel word = new JLabel(palabras.get(i));
            word.putClientProperty(FlatClientProperties.STYLE, "font: bold 13; foreground: #222222");

            card.add(num); card.add(word);
            gridPalabras.add(card);
        }

        p.add(gridPalabras, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);

        JButton btnCopy = new JButton("📋 COPIAR");
        btnCopy.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; font: bold 12");
        btnCopy.addActionListener(e -> {
            String full = String.join(" ", palabras);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(full), null);
            ToastNotification.show(dlg, "Palabras copiadas al portapapeles.", ToastNotification.Type.SUCCESS);
        });
        bottom.add(btnCopy);

        JButton btnEntendido = new JButton("HE ANOTADO MI FRASE");
        btnEntendido.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #0078D4; foreground: #FFFFFF; font: bold 13");
        btnEntendido.addActionListener(e -> {
            dlg.dispose();
            if (onConfirm != null) onConfirm.run();
        });
        bottom.add(btnEntendido);

        p.add(bottom, BorderLayout.SOUTH);
        dlg.add(p);
        dlg.setVisible(true);
    }

    private void revelarSemillaConPin() {
        if (currentWallet == null) return;
        JPasswordField pfPin = new JPasswordField(4);
        int opc = JOptionPane.showConfirmDialog(this,
                new Object[]{"Ingrese su PIN para visualizar su Frase Semilla:", pfPin},
                "Verificación de Seguridad",
                JOptionPane.OK_CANCEL_OPTION);

        if (opc != JOptionPane.OK_OPTION) return;
        char[] pin = pfPin.getPassword();

        if (SecuritySession.verifyUserPin(pin)) {
            List<String> palabras = BitcoinWalletService.obtenerPalabras(currentWallet);
            if (palabras != null) {
                mostrarVentanaPalabrasSemilla(palabras, "FRASE SEMILLA DE RECUPERACIÓN (BIP-39)", null);
                AuditService.log(userEmail, "BTC_WALLET_SEED_REVEAL", "Frase semilla visualizada por el usuario.");
            } else {
                ToastNotification.show(this, "No se encontró semilla mnemónica.", ToastNotification.Type.WARNING);
            }
        } else {
            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        Arrays.fill(pin, '0');
    }

    private void desvincularBilleteraConPin() {
        int conf = JOptionPane.showConfirmDialog(this,
                "⚠️ ¿Está completamente seguro de desvincular esta billetera?\n" +
                "Si no anotó sus 12 palabras, PERDERÁ EL ACCESO A SUS FONDOS DE FORMA PERMANENTE.",
                "Confirmación de Eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (conf != JOptionPane.YES_OPTION) return;

        JPasswordField pfPin = new JPasswordField(4);
        int opc = JOptionPane.showConfirmDialog(this,
                new Object[]{"Ingrese su PIN para confirmar la eliminación:", pfPin},
                "Confirmación Crítica",
                JOptionPane.OK_CANCEL_OPTION);

        if (opc != JOptionPane.OK_OPTION) return;
        char[] pin = pfPin.getPassword();

        if (SecuritySession.verifyUserPin(pin)) {
            BitcoinWalletService.eliminarBilletera(userEmail);
            currentWallet = null;
            renderizarPantallaBienvenida();
            ToastNotification.show(this, "Billetera desvinculada exitosamente.", ToastNotification.Type.SUCCESS);
            AuditService.log(userEmail, "BTC_WALLET_DELETED", "Billetera eliminada por el usuario.");
        } else {
            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        Arrays.fill(pin, '0');
    }
}
