package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.core.util.ToastNotification;
import com.mycompany.blackpassword.services.AuditService;
import com.mycompany.blackpassword.services.BitcoinWalletService;
import com.mycompany.blackpassword.services.BitcoinWalletService.BtcAddressEntry;
import com.mycompany.blackpassword.services.BitcoinWalletService.BtcTxInfo;
import com.mycompany.blackpassword.services.LocalDatabaseService;
import com.mycompany.blackpassword.services.LocalDatabaseService.BtcSeedEntry;
import com.mycompany.blackpassword.services.QRCodeService;
import com.mycompany.blackpassword.services.SecuritySession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import com.mycompany.blackpassword.services.BitcoinWalletService.SendResult;
import java.awt.Desktop;
import java.awt.datatransfer.DataFlavor;
import java.net.URI;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.bitcoinj.core.Address;
import org.bitcoinj.wallet.Wallet;

/**
 * Billetera Bitcoin Inteligente v2.0 - Zero-Trust SegWit HD Suite.
 * Soporta cambio dinámico Mainnet/Testnet, libreta de direcciones con títulos,
 * historial de transacciones en vivo con estado de confirmación y cifrado AES-256 GCM.
 */
public class BilleteraBitcoin extends javax.swing.JInternalFrame {

    private final String userEmail;
    private Wallet currentWallet;
    private boolean isTestnet = true;

    // Componentes principales
    private JPanel contentContainer;
    private JComboBox<String> cbNetworkSelector;
    private JLabel lblBalanceBtc;
    private JLabel lblBalanceSats;
    private JTextField txtMainAddress;
    private JLabel lblMainQr;
    private JButton btnRefreshAll;

    // Tablas
    private DefaultTableModel txTableModel;
    private JTable txTable;
    private DefaultTableModel addressTableModel;
    private JTable addressTable;

    // Componentes de Envío y Transferencia
    private JTabbedPane mainTabbedPane;
    private long currentBalanceSats = 0;
    private JLabel lblBalanceDispEnviar;
    private JTextField txtDestinoEnviarBtc;
    private JTextField txtMontoEnviarBtc;
    private JComboBox<String> cbFeeBtc;
    private JLabel lblSatsCalculados;
    private JLabel lblResumenDestinoBtc;
    private JLabel lblResumenMontoBtc;
    private JLabel lblResumenFeeBtc;
    private JLabel lblResumenTotalBtc;
    private JButton btnEjecutarEnvioBtc;

    public BilleteraBitcoin(String email) {
        super("Billetera Bitcoin Inteligente HD", true, true, true, true);
        this.userEmail = email;
        this.isTestnet = BitcoinWalletService.isTestnet(email);

        initComponentsCustom();
        applyPremiumStyle();
        cargarBilleteraParaRedActual();
    }

    private void applyPremiumStyle() {
        putClientProperty(FlatClientProperties.STYLE, "background: #F8F9FA");
        setBorder(null);
    }

    private void initComponentsCustom() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(880, 700));

        // --- TOP BAR CON SELECTOR DE RED ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(12, 22, 12, 22)
        ));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);

        JLabel lblIcon = new JLabel("₿");
        lblIcon.putClientProperty(FlatClientProperties.STYLE, "font: bold 26; foreground: #F7931A");
        titlePanel.add(lblIcon);

        JPanel textTitles = new JPanel(new GridLayout(2, 1, 0, 2));
        textTitles.setOpaque(false);
        JLabel lblTitle = new JLabel("BILLETERA BITCOIN INTELIGENTE (BIP-84)");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 15; foreground: #202020");
        JLabel lblSub = new JLabel("SegWit Nativo • Libreta de Direcciones • Historial de Transacciones");
        lblSub.putClientProperty(FlatClientProperties.STYLE, "font: 11; foreground: #666666");
        textTitles.add(lblTitle);
        textTitles.add(lblSub);
        titlePanel.add(textTitles);

        topBar.add(titlePanel, BorderLayout.WEST);

        // Selector de Red (Mainnet / Testnet)
        JPanel netPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        netPanel.setOpaque(false);

        JLabel lblRed = new JLabel("RED:");
        lblRed.putClientProperty(FlatClientProperties.STYLE, "font: bold 11; foreground: #666666");
        netPanel.add(lblRed);

        cbNetworkSelector = new JComboBox<>(new String[]{"🟢 TESTNET (PRUEBAS)", "🟠 MAINNET (REAL)"});
        cbNetworkSelector.setSelectedIndex(isTestnet ? 0 : 1);
        cbNetworkSelector.setPreferredSize(new Dimension(195, 36));
        cbNetworkSelector.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; arc: 10");
        cbNetworkSelector.addActionListener(e -> {
            boolean nuevaTestnet = cbNetworkSelector.getSelectedIndex() == 0;
            if (nuevaTestnet != isTestnet) {
                isTestnet = nuevaTestnet;
                cargarBilleteraParaRedActual();
            }
        });
        netPanel.add(cbNetworkSelector);

        topBar.add(netPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Contenedor principal dinámico
        contentContainer = new JPanel(new BorderLayout());
        contentContainer.setOpaque(false);
        add(contentContainer, BorderLayout.CENTER);
    }

    private void cargarBilleteraParaRedActual() {
        if (!BitcoinWalletService.existeBilletera(userEmail, isTestnet)) {
            renderizarPantallaBienvenida();
            return;
        }

        JPasswordField pfPin = new JPasswordField(4);
        int opc = JOptionPane.showConfirmDialog(this,
                new Object[]{"Ingrese su PIN para desbloquear la billetera (" + (isTestnet ? "Testnet" : "Mainnet") + "):", pfPin},
                "Desbloqueo de Billetera",
                JOptionPane.OK_CANCEL_OPTION);

        if (opc != JOptionPane.OK_OPTION) {
            renderizarPantallaBienvenida();
            return;
        }

        char[] pin = pfPin.getPassword();
        char[] pinCheck = Arrays.copyOf(pin, pin.length);
        if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pinCheck)) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            renderizarPantallaBienvenida();
            return;
        }

        try {
            currentWallet = BitcoinWalletService.cargarBilleteraCifrada(userEmail, pin, isTestnet);
            renderizarPantallaDashboard();
            ToastNotification.show(this, "Billetera (" + (isTestnet ? "Testnet" : "Mainnet") + ") desbloqueada.", ToastNotification.Type.SUCCESS);
            AuditService.log(userEmail, "BTC_WALLET_UNLOCK", "Billetera desbloqueada (Testnet: " + isTestnet + ").");
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
        lblHero.putClientProperty(FlatClientProperties.STYLE, "font: bold 64; foreground: " + (isTestnet ? "#0078D4" : "#F7931A"));
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = row++;
        p.add(lblHero, gbc);

        JLabel lblWelcome = new JLabel("No hay billetera configurada en " + (isTestnet ? "RED TESTNET" : "RED MAINNET"));
        lblWelcome.setHorizontalAlignment(SwingConstants.CENTER);
        lblWelcome.putClientProperty(FlatClientProperties.STYLE, "font: bold 18; foreground: #222222");
        gbc.insets = new Insets(0, 0, 8, 0); gbc.gridy = row++;
        p.add(lblWelcome, gbc);

        JLabel lblDesc = new JLabel("Cree una nueva billetera jerárquica con 12 palabras o restaure una existente para esta red.");
        lblDesc.setHorizontalAlignment(SwingConstants.CENTER);
        lblDesc.putClientProperty(FlatClientProperties.STYLE, "font: 13; foreground: #666666");
        gbc.insets = new Insets(0, 0, 30, 0); gbc.gridy = row++;
        p.add(lblDesc, gbc);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnGroup.setOpaque(false);

        JButton btnCrear = new JButton("✨ CREAR NUEVA BILLETERA");
        btnCrear.setPreferredSize(new Dimension(230, 46));
        btnCrear.putClientProperty(FlatClientProperties.STYLE, "arc: 14; background: #0078D4; foreground: #FFFFFF; font: bold 13; margin: 8,16,8,16");
        btnCrear.addActionListener(e -> crearNuevaBilleteraFlujo());
        btnGroup.add(btnCrear);

        JButton btnRestaurar = new JButton("📥 RESTAURAR BILLETERA");
        btnRestaurar.setPreferredSize(new Dimension(230, 46));
        btnRestaurar.putClientProperty(FlatClientProperties.STYLE, "arc: 14; background: #FFFFFF; foreground: #333333; font: bold 13; border: 1,1,1,1,#CCCCCC; margin: 8,16,8,16");
        btnRestaurar.addActionListener(e -> restaurarBilleteraFlujo());
        btnGroup.add(btnRestaurar);

        JButton btnBoveda = new JButton("🔐 BÓVEDA DE SEMILLAS");
        btnBoveda.setPreferredSize(new Dimension(230, 46));
        btnBoveda.putClientProperty(FlatClientProperties.STYLE, "arc: 14; background: #6B21A8; foreground: #FFFFFF; font: bold 13; margin: 8,16,8,16");
        btnBoveda.addActionListener(e -> mostrarDialogoBovedaSemillas());
        btnGroup.add(btnBoveda);

        gbc.insets = new Insets(0, 0, 20, 0); gbc.gridy = row++;
        p.add(btnGroup, gbc);

        contentContainer.add(p, BorderLayout.CENTER);
        contentContainer.revalidate();
        contentContainer.repaint();
    }

    private void renderizarPantallaDashboard() {
        contentContainer.removeAll();

        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.putClientProperty(FlatClientProperties.STYLE, "tabAreaInsets: 10,15,0,15; font: bold 12");

        mainTabbedPane.addTab("📊 RESUMEN & BALANCE", crearPanelResumen());
        mainTabbedPane.addTab("📤 ENVIAR BITCOIN", crearPanelEnviar());
        mainTabbedPane.addTab("🏷️ DIRECCIONES & ETIQUETAS", crearPanelDirecciones());
        mainTabbedPane.addTab("📜 HISTORIAL DE TRANSACCIONES", crearPanelTransacciones());
        mainTabbedPane.addTab("🔐 BÓVEDA DE SEMILLAS (NUBE)", crearPanelBovedaSemillas());
        mainTabbedPane.addTab("⚙️ SEGURIDAD", crearPanelSeguridad());

        contentContainer.add(mainTabbedPane, BorderLayout.CENTER);
        contentContainer.revalidate();
        contentContainer.repaint();

        // Actualizar datos en segundo plano
        refrescarDatosEnSegundoPlano();
    }

    // --- PANEL 1: RESUMEN Y BALANCE ---

    private JPanel crearPanelResumen() {
        JPanel main = new JPanel(new BorderLayout(0, 18));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Tarjeta de Balance
        JPanel balanceCard = new JPanel(new BorderLayout(15, 10));
        balanceCard.setBackground(Color.WHITE);
        balanceCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));
        balanceCard.putClientProperty(FlatClientProperties.STYLE, "arc: 16");

        JPanel balanceLeft = new JPanel(new GridLayout(3, 1, 0, 3));
        balanceLeft.setOpaque(false);

        JLabel lblBalTitle = new JLabel("BALANCE TOTAL DISPONIBLE (" + (isTestnet ? "TESTNET" : "MAINNET") + ")");
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

        JButton btnIrEnviar = new JButton("📤 ENVIAR BITCOIN");
        btnIrEnviar.setPreferredSize(new Dimension(170, 40));
        btnIrEnviar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #0078D4; foreground: #FFFFFF; font: bold 12; margin: 8,14,8,14");
        btnIrEnviar.addActionListener(e -> {
            if (mainTabbedPane != null) {
                mainTabbedPane.setSelectedIndex(1);
            }
        });
        balanceRight.add(btnIrEnviar);

        btnRefreshAll = new JButton("🔄 ACTUALIZAR");
        btnRefreshAll.setPreferredSize(new Dimension(140, 40));
        btnRefreshAll.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #F3F3F3; font: bold 12");
        btnRefreshAll.addActionListener(e -> refrescarDatosEnSegundoPlano());
        balanceRight.add(btnRefreshAll);

        balanceCard.add(balanceRight, BorderLayout.EAST);
        main.add(balanceCard, BorderLayout.NORTH);

        // Tarjeta de Dirección Principal y QR
        JPanel addressCard = new JPanel(new GridBagLayout());
        addressCard.setBackground(Color.WHITE);
        addressCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));
        addressCard.putClientProperty(FlatClientProperties.STYLE, "arc: 16");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        int dRow = 0;

        JLabel lblRecTitle = new JLabel("DIRECCIÓN DE RECEPCIÓN PRINCIPAL (SEGWIT BECH32)");
        lblRecTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #0078D4");
        gbc.insets = new Insets(0, 0, 10, 0); gbc.gridy = dRow++;
        addressCard.add(lblRecTitle, gbc);

        String mainAddress = BitcoinWalletService.obtenerDireccion(currentWallet);

        JPanel addressRow = new JPanel(new BorderLayout(10, 0));
        addressRow.setOpaque(false);

        txtMainAddress = new JTextField(mainAddress);
        txtMainAddress.setEditable(false);
        txtMainAddress.putClientProperty(FlatClientProperties.STYLE, "font: bold 13; background: #F8F9FA; border: 8,12,8,12");
        addressRow.add(txtMainAddress, BorderLayout.CENTER);

        JButton btnCopy = new JButton("📋 COPIAR");
        btnCopy.setPreferredSize(new Dimension(100, 38));
        btnCopy.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #0078D4; foreground: #FFFFFF; font: bold 12");
        btnCopy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(mainAddress), null);
            ToastNotification.show(this, "Dirección copiada al portapapeles.", ToastNotification.Type.SUCCESS);
        });
        addressRow.add(btnCopy, BorderLayout.EAST);

        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = dRow++;
        addressCard.add(addressRow, gbc);

        // QR Code
        lblMainQr = new JLabel();
        lblMainQr.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            BufferedImage qrImg = QRCodeService.generateQRCode(mainAddress, 180, 180);
            lblMainQr.setIcon(new ImageIcon(qrImg));
        } catch (Exception ex) {
            lblMainQr.setText("[Error generando QR]");
        }
        gbc.insets = new Insets(0, 0, 10, 0); gbc.gridy = dRow++;
        addressCard.add(lblMainQr, gbc);

        main.add(addressCard, BorderLayout.CENTER);
        return main;
    }

    // --- PANEL DE ENVÍO Y TRANSFERENCIA BITCOIN ---

    private JScrollPane crearPanelEnviar() {
        ResponsiveScrollablePanel main = new ResponsiveScrollablePanel(new BorderLayout(0, 15));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Tarjeta Superior: Estado del saldo disponible y dirección origen
        JPanel originCard = new JPanel(new BorderLayout(15, 8));
        originCard.setBackground(Color.WHITE);
        originCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        originCard.putClientProperty(FlatClientProperties.STYLE, "arc: 14");

        JPanel originLeft = new JPanel(new GridLayout(2, 1, 0, 4));
        originLeft.setOpaque(false);

        String fromAddr = (currentWallet != null) ? BitcoinWalletService.obtenerDireccion(currentWallet) : "No inicializada";
        JLabel lblFromTitle = new JLabel("DIRECCIÓN ORIGEN (SU BILLETERA): " + fromAddr);
        lblFromTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 11; foreground: #0078D4");
        originLeft.add(lblFromTitle);

        lblBalanceDispEnviar = new JLabel(String.format("Saldo disponible: %s (%,d Sats)", BitcoinWalletService.satsToBtc(currentBalanceSats), currentBalanceSats));
        lblBalanceDispEnviar.putClientProperty(FlatClientProperties.STYLE, "font: bold 15; foreground: #222222");
        originLeft.add(lblBalanceDispEnviar);

        originCard.add(originLeft, BorderLayout.CENTER);
        main.add(originCard, BorderLayout.NORTH);

        // Formulario Central de Envío
        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));
        formCard.putClientProperty(FlatClientProperties.STYLE, "arc: 16");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        int row = 0;

        // 1. Destinatario
        JLabel lblDestTitle = new JLabel("DIRECCIÓN BITCOIN DE DESTINO (SegWit bech32 o Legacy):");
        lblDestTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #333333");
        gbc.insets = new Insets(0, 0, 6, 0); gbc.gridy = row++;
        formCard.add(lblDestTitle, gbc);

        JPanel destRow = new JPanel(new BorderLayout(8, 0));
        destRow.setOpaque(false);

        txtDestinoEnviarBtc = new JTextField();
        txtDestinoEnviarBtc.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, isTestnet ? "tb1q... o 2... o m/n..." : "bc1q... o 1... o 3...");
        txtDestinoEnviarBtc.putClientProperty(FlatClientProperties.STYLE, "font: 13; background: #F8F9FA; border: 8,10,8,10");
        destRow.add(txtDestinoEnviarBtc, BorderLayout.CENTER);

        JPanel destBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        destBtns.setOpaque(false);

        JButton btnPegar = new JButton("📋 PEGAR");
        btnPegar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11; margin: 6,12,6,12");
        btnPegar.addActionListener(e -> {
            try {
                String clip = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                if (clip != null && !clip.trim().isEmpty()) {
                    txtDestinoEnviarBtc.setText(clip.trim());
                    actualizarResumenEnvioBtc();
                }
            } catch (Exception ignored) {}
        });
        destBtns.add(btnPegar);

        JButton btnLibreta = new JButton("📖 LIBRETA");
        btnLibreta.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11; background: #F0F4F8; margin: 6,12,6,12");
        btnLibreta.addActionListener(e -> seleccionarDireccionDeLibretaBtc());
        destBtns.add(btnLibreta);

        destRow.add(destBtns, BorderLayout.EAST);

        gbc.insets = new Insets(0, 0, 16, 0); gbc.gridy = row++;
        formCard.add(destRow, gbc);

        // 2. Monto
        JLabel lblMontoTitle = new JLabel("MONTO A ENVIAR (EN BTC):");
        lblMontoTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #333333");
        gbc.insets = new Insets(0, 0, 6, 0); gbc.gridy = row++;
        formCard.add(lblMontoTitle, gbc);

        JPanel montoRow = new JPanel(new BorderLayout(8, 0));
        montoRow.setOpaque(false);

        txtMontoEnviarBtc = new JTextField("0.00000000");
        txtMontoEnviarBtc.putClientProperty(FlatClientProperties.STYLE, "font: bold 14; background: #F8F9FA; border: 8,10,8,10");
        montoRow.add(txtMontoEnviarBtc, BorderLayout.CENTER);

        JButton btnMax = new JButton("⚡ MÁXIMO");
        btnMax.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11; background: #FFF4CE; foreground: #8A6D00; margin: 6,14,6,14");
        btnMax.addActionListener(e -> calcularMaximoEnvioBtc());
        montoRow.add(btnMax, BorderLayout.EAST);

        gbc.insets = new Insets(0, 0, 4, 0); gbc.gridy = row++;
        formCard.add(montoRow, gbc);

        lblSatsCalculados = new JLabel("≈ 0 Satoshis (1 BTC = 100,000,000 Sats)");
        lblSatsCalculados.putClientProperty(FlatClientProperties.STYLE, "font: 11; foreground: #666666");
        gbc.insets = new Insets(0, 0, 16, 0); gbc.gridy = row++;
        formCard.add(lblSatsCalculados, gbc);

        // 3. Comisión de Red (Fee)
        JLabel lblFeeTitle = new JLabel("COMISIÓN DE RED (MINER FEE):");
        lblFeeTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #333333");
        gbc.insets = new Insets(0, 0, 6, 0); gbc.gridy = row++;
        formCard.add(lblFeeTitle, gbc);

        cbFeeBtc = new JComboBox<>(new String[]{
                "🐢 Económica (~1,000 Sats / aprox. 0.00001000 BTC)",
                "⚡ Normal / Estándar (~2,500 Sats / aprox. 0.00002500 BTC)",
                "🚀 Rápida / Prioritaria (~5,000 Sats / aprox. 0.00005000 BTC)"
        });
        cbFeeBtc.setSelectedIndex(1);
        cbFeeBtc.putClientProperty(FlatClientProperties.STYLE, "font: 12; arc: 10");
        cbFeeBtc.addActionListener(e -> actualizarResumenEnvioBtc());
        gbc.insets = new Insets(0, 0, 20, 0); gbc.gridy = row++;
        formCard.add(cbFeeBtc, gbc);

        // 4. Tarjeta Resumen
        JPanel cardResumen = new JPanel(new GridLayout(4, 1, 0, 5));
        cardResumen.setBackground(new Color(248, 250, 252));
        cardResumen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        lblResumenDestinoBtc = new JLabel("Destinatario: (Sin especificar)");
        lblResumenDestinoBtc.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #475569");
        cardResumen.add(lblResumenDestinoBtc);

        lblResumenMontoBtc = new JLabel("Monto a enviar: 0.00000000 BTC (0 Sats)");
        lblResumenMontoBtc.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #475569");
        cardResumen.add(lblResumenMontoBtc);

        lblResumenFeeBtc = new JLabel("Comisión de red: 2,500 Sats");
        lblResumenFeeBtc.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #475569");
        cardResumen.add(lblResumenFeeBtc);

        lblResumenTotalBtc = new JLabel("Total a debitar: 0.00002500 BTC (2,500 Sats)");
        lblResumenTotalBtc.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #0F172A");
        cardResumen.add(lblResumenTotalBtc);

        gbc.insets = new Insets(0, 0, 20, 0); gbc.gridy = row++;
        formCard.add(cardResumen, gbc);

        // 5. Botón de Envío
        btnEjecutarEnvioBtc = new JButton("🚀 AUTORIZAR Y ENVIAR BITCOIN AHORA");
        btnEjecutarEnvioBtc.setPreferredSize(new Dimension(300, 48));
        btnEjecutarEnvioBtc.putClientProperty(FlatClientProperties.STYLE, "arc: 14; background: #0078D4; foreground: #FFFFFF; font: bold 13; margin: 10,20,10,20");
        btnEjecutarEnvioBtc.addActionListener(e -> ejecutarEnvioBitcoin());
        gbc.insets = new Insets(0, 0, 0, 0); gbc.gridy = row++;
        formCard.add(btnEjecutarEnvioBtc, gbc);

        // Document Listener para actualización en tiempo real
        DocumentListener docL = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { actualizarResumenEnvioBtc(); }
            @Override public void removeUpdate(DocumentEvent e) { actualizarResumenEnvioBtc(); }
            @Override public void changedUpdate(DocumentEvent e) { actualizarResumenEnvioBtc(); }
        };
        txtMontoEnviarBtc.getDocument().addDocumentListener(docL);
        txtDestinoEnviarBtc.getDocument().addDocumentListener(docL);

        main.add(formCard, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(main);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        return scroll;
    }

    private long obtenerFeeSatsSeleccionadoBtc() {
        if (cbFeeBtc == null) return 2500;
        switch (cbFeeBtc.getSelectedIndex()) {
            case 0: return 1000;
            case 2: return 5000;
            default: return 2500;
        }
    }

    private void calcularMaximoEnvioBtc() {
        long fee = obtenerFeeSatsSeleccionadoBtc();
        if (currentBalanceSats <= fee + 546) {
            ToastNotification.show(this, "Saldo insuficiente para cubrir el monto mínimo y la comisión de red.", ToastNotification.Type.WARNING);
            return;
        }
        long maxSats = currentBalanceSats - fee;
        double btc = (double) maxSats / 100_000_000.0;
        txtMontoEnviarBtc.setText(String.format(java.util.Locale.US, "%.8f", btc));
        actualizarResumenEnvioBtc();
    }

    private void actualizarResumenEnvioBtc() {
        if (txtDestinoEnviarBtc == null || txtMontoEnviarBtc == null) return;
        String dest = txtDestinoEnviarBtc.getText().trim();
        lblResumenDestinoBtc.setText("Destinatario: " + (dest.isEmpty() ? "(Sin especificar)" : dest));

        long fee = obtenerFeeSatsSeleccionadoBtc();
        long amountSats = 0;
        try {
            double btc = Double.parseDouble(txtMontoEnviarBtc.getText().trim().replace(",", "."));
            amountSats = Math.max(0, Math.round(btc * 100_000_000.0));
        } catch (Exception ignored) {}

        lblSatsCalculados.setText(String.format("≈ %,d Satoshis (1 BTC = 100,000,000 Sats)", amountSats));
        lblResumenMontoBtc.setText(String.format(java.util.Locale.US, "Monto a enviar: %.8f BTC (%,d Sats)", (double) amountSats / 100_000_000.0, amountSats));
        lblResumenFeeBtc.setText(String.format("Comisión de red: %,d Sats", fee));

        long total = amountSats + fee;
        lblResumenTotalBtc.setText(String.format(java.util.Locale.US, "Total a debitar: %.8f BTC (%,d Sats)", (double) total / 100_000_000.0, total));
    }

    private void seleccionarDireccionDeLibretaBtc() {
        List<BtcAddressEntry> list = BitcoinWalletService.cargarDireccionesEtiquetadas(userEmail, isTestnet);
        if (list.isEmpty()) {
            ToastNotification.show(this, "No hay direcciones guardadas en la libreta.", ToastNotification.Type.INFO);
            return;
        }

        String[] opciones = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            BtcAddressEntry e = list.get(i);
            opciones[i] = (e.label.isEmpty() ? "(Sin título)" : e.label) + " - " + e.address;
        }

        String sel = (String) JOptionPane.showInputDialog(
                this,
                "Seleccione una dirección guardada en su libreta:",
                "Libreta de Direcciones Bitcoin",
                JOptionPane.PLAIN_MESSAGE,
                null,
                opciones,
                opciones[0]
        );

        if (sel != null && !sel.isEmpty()) {
            for (BtcAddressEntry e : list) {
                if (sel.contains(e.address)) {
                    txtDestinoEnviarBtc.setText(e.address);
                    actualizarResumenEnvioBtc();
                    break;
                }
            }
        }
    }

    private void ejecutarEnvioBitcoin() {
        if (currentWallet == null) {
            ToastNotification.show(this, "Billetera no inicializada.", ToastNotification.Type.ERROR);
            return;
        }

        String toAddr = txtDestinoEnviarBtc.getText().trim();
        if (toAddr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese la dirección Bitcoin de destino.", "Dato Requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Address.fromString(BitcoinWalletService.getNetwork(isTestnet), toAddr);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "La dirección de destino no es válida para la red " + (isTestnet ? "TESTNET" : "MAINNET") + ":\n" + e.getMessage(),
                    "Dirección Inválida", JOptionPane.ERROR_MESSAGE);
            return;
        }

        long amountSats;
        try {
            double btc = Double.parseDouble(txtMontoEnviarBtc.getText().trim().replace(",", "."));
            amountSats = Math.round(btc * 100_000_000.0);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese un monto numérico válido en BTC.", "Monto Inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (amountSats < 546) {
            JOptionPane.showMessageDialog(this, "El monto mínimo permitido es de 546 Satoshis (límite de polvo/dust).", "Monto Muy Bajo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        long feeSats = obtenerFeeSatsSeleccionadoBtc();
        long totalReq = amountSats + feeSats;

        if (currentBalanceSats < totalReq) {
            JOptionPane.showMessageDialog(this,
                    String.format(java.util.Locale.US,
                            "Saldo insuficiente en la billetera.\n\nSaldo actual: %s (%,d Sats)\nRequerido (monto + comisión): %.8f BTC (%,d Sats)\n\n" +
                            "Si acaba de recibir fondos, espere a que la transacción obtenga confirmaciones en la blockchain.",
                            BitcoinWalletService.satsToBtc(currentBalanceSats), currentBalanceSats,
                            (double) totalReq / 100_000_000.0, totalReq),
                    "Saldo Insuficiente", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPasswordField pfPin = new JPasswordField(4);
        int opc = JOptionPane.showConfirmDialog(this,
                new Object[]{
                        "🔒 AUTORIZACIÓN DE TRANSFERENCIA ZERO-TRUST\n\n" +
                        "Está a punto de transmitir fondos en " + (isTestnet ? "RED TESTNET" : "RED BITCOIN MAINNET") + ":\n" +
                        "• Destino: " + toAddr + "\n" +
                        "• Monto: " + String.format(java.util.Locale.US, "%.8f BTC (%,d Sats)", (double) amountSats / 100_000_000.0, amountSats) + "\n" +
                        "• Comisión de red: " + feeSats + " Sats\n\n" +
                        "Ingrese su PIN de seguridad de 4 dígitos para firmar criptográficamente:",
                        pfPin
                },
                "Confirmar Transmisión Bitcoin",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (opc != JOptionPane.OK_OPTION) return;

        char[] pin = pfPin.getPassword();
        char[] pinCheck = Arrays.copyOf(pin, pin.length);
        if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pinCheck)) {
            JOptionPane.showMessageDialog(this, "PIN de autorización incorrecto. Operación abortada.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            return;
        }
        Arrays.fill(pin, '0');

        String fromAddr = BitcoinWalletService.obtenerDireccion(currentWallet);
        btnEjecutarEnvioBtc.setEnabled(false);
        btnEjecutarEnvioBtc.setText("⏳ Firmando y transmitiendo transacción a la red...");

        SwingWorker<SendResult, Void> worker = new SwingWorker<>() {
            @Override
            protected SendResult doInBackground() {
                return BitcoinWalletService.enviarBitcoin(currentWallet, fromAddr, toAddr, amountSats, feeSats, isTestnet);
            }

            @Override
            protected void done() {
                btnEjecutarEnvioBtc.setEnabled(true);
                btnEjecutarEnvioBtc.setText("🚀 AUTORIZAR Y ENVIAR BITCOIN AHORA");

                try {
                    SendResult res = get();
                    if (res != null && res.success) {
                        mostrarDialogoExitoBtc(res.txid, toAddr, amountSats, feeSats);
                        txtDestinoEnviarBtc.setText("");
                        txtMontoEnviarBtc.setText("0.00000000");
                        actualizarResumenEnvioBtc();
                        refrescarDatosEnSegundoPlano();
                        AuditService.log(userEmail, "BTC_SEND_SUCCESS", "Enviados " + amountSats + " sats a " + toAddr + " (TXID: " + res.txid + ")");
                    } else {
                        String errMsg = (res != null && res.errorMessage != null) ? res.errorMessage : "Error desconocido al procesar transacción.";
                        JOptionPane.showMessageDialog(BilleteraBitcoin.this,
                                "No se pudo transmitir la transacción:\n\n" + errMsg,
                                "Error de Transmisión Bitcoin", JOptionPane.ERROR_MESSAGE);
                        AuditService.log(userEmail, "BTC_SEND_FAILED", "Fallo al enviar a " + toAddr + ": " + errMsg);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(BilleteraBitcoin.this,
                            "Excepción durante el proceso de envío: " + ex.getMessage(),
                            "Error de Red", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void mostrarDialogoExitoBtc(String txid, String destino, long sats, long feeSats) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "¡Transacción Bitcoin Transmitida!", JDialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());
        dlg.setSize(520, 380);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(25, 25, 20, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        int r = 0;

        JLabel lblOk = new JLabel("✅ TRANSMISIÓN EXITOSA A LA RED BITCOIN", SwingConstants.CENTER);
        lblOk.putClientProperty(FlatClientProperties.STYLE, "font: bold 15; foreground: #107C10");
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = r++;
        p.add(lblOk, gbc);

        JLabel lblDet = new JLabel(String.format(java.util.Locale.US,
                "<html>Se han transmitido <b>%.8f BTC</b> (%,d Sats) hacia:<br/><code>%s</code><br/>Comisión minera incluida: %,d Sats</html>",
                (double) sats / 100_000_000.0, sats, destino, feeSats));
        lblDet.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #333333");
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = r++;
        p.add(lblDet, gbc);

        JLabel lblTxidTitle = new JLabel("IDENTIFICADOR DE TRANSACCIÓN (TXID):");
        lblTxidTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 11; foreground: #0078D4");
        gbc.insets = new Insets(0, 0, 4, 0); gbc.gridy = r++;
        p.add(lblTxidTitle, gbc);

        JTextField tfTxid = new JTextField(txid);
        tfTxid.setEditable(false);
        tfTxid.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; background: #F8F9FA; border: 6,8,6,8");
        gbc.insets = new Insets(0, 0, 20, 0); gbc.gridy = r++;
        p.add(tfTxid, gbc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);

        JButton btnCopy = new JButton("📋 COPIAR TXID");
        btnCopy.putClientProperty(FlatClientProperties.STYLE, "arc: 12; font: bold 12; margin: 8,16,8,16");
        btnCopy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(txid), null);
            ToastNotification.show(dlg, "TXID copiado al portapapeles.", ToastNotification.Type.SUCCESS);
        });
        btnRow.add(btnCopy);

        JButton btnExplorer = new JButton("🌐 VER EN EXPLORADOR");
        btnExplorer.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #0078D4; foreground: #FFFFFF; font: bold 12; margin: 8,16,8,16");
        btnExplorer.addActionListener(e -> {
            try {
                String url = (isTestnet ? "https://mempool.space/testnet/tx/" : "https://mempool.space/tx/") + txid;
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    ToastNotification.show(dlg, "URL: " + url, ToastNotification.Type.INFO);
                }
            } catch (Exception ex) {
                ToastNotification.show(dlg, "No se pudo abrir el navegador.", ToastNotification.Type.WARNING);
            }
        });
        btnRow.add(btnExplorer);

        gbc.insets = new Insets(0, 0, 0, 0); gbc.gridy = r++;
        p.add(btnRow, gbc);

        dlg.add(p, BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    // --- PANEL 2: LIBRETA DE DIRECCIONES CON TÍTULOS ---

    private JPanel crearPanelDirecciones() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Barra superior de acciones
        JPanel topActions = new JPanel(new BorderLayout());
        topActions.setOpaque(false);

        JLabel lblInfo = new JLabel("Cree códigos de direcciones únicos con etiquetas para identificar quién le envía fondos.");
        lblInfo.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #666666");
        topActions.add(lblInfo, BorderLayout.WEST);

        JButton btnNuevaDir = new JButton("➕ GENERAR DIRECCIÓN CON TÍTULO");
        btnNuevaDir.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #0078D4; foreground: #FFFFFF; font: bold 12");
        btnNuevaDir.addActionListener(e -> mostrarDialogoNuevaDireccion());
        topActions.add(btnNuevaDir, BorderLayout.EAST);

        panel.add(topActions, BorderLayout.NORTH);

        // Tabla de Direcciones
        addressTableModel = new DefaultTableModel(new Object[]{"TÍTULO / ETIQUETA", "DIRECCIÓN BITCOIN", "FECHA CREACIÓN"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        addressTable = new JTable(addressTableModel);
        addressTable.getTableHeader().setReorderingAllowed(false);
        addressTable.putClientProperty(FlatClientProperties.STYLE, "rowHeight: 34; showHorizontalLines: true");
        addressTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        addressTable.getColumnModel().getColumn(1).setPreferredWidth(340);
        addressTable.getColumnModel().getColumn(2).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(addressTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        panel.add(scroll, BorderLayout.CENTER);

        // Barra inferior de botones
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);

        JButton btnCopiarDir = new JButton("📋 COPIAR SELECCIONADA");
        btnCopiarDir.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11");
        btnCopiarDir.addActionListener(e -> {
            int row = addressTable.getSelectedRow();
            if (row != -1) {
                String addr = (String) addressTableModel.getValueAt(row, 1);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(addr), null);
                ToastNotification.show(this, "Dirección copiada: " + addr, ToastNotification.Type.SUCCESS);
            }
        });
        bottomBar.add(btnCopiarDir);

        JButton btnVerQr = new JButton("📱 VER QR");
        btnVerQr.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11");
        btnVerQr.addActionListener(e -> {
            int row = addressTable.getSelectedRow();
            if (row != -1) {
                String addr = (String) addressTableModel.getValueAt(row, 1);
                String tag = (String) addressTableModel.getValueAt(row, 0);
                mostrarDialogoQrDireccion(addr, tag);
            }
        });
        bottomBar.add(btnVerQr);

        JButton btnEliminarDir = new JButton("🗑️ ELIMINAR DE LIBRETA");
        btnEliminarDir.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #FDE7E9; foreground: #A80000; font: bold 11");
        btnEliminarDir.addActionListener(e -> {
            int row = addressTable.getSelectedRow();
            if (row != -1) {
                String addr = (String) addressTableModel.getValueAt(row, 1);
                BitcoinWalletService.eliminarDireccionEtiquetada(userEmail, isTestnet, addr);
                cargarTablaDirecciones();
                ToastNotification.show(this, "Dirección removida de la libreta.", ToastNotification.Type.INFO);
            }
        });
        bottomBar.add(btnEliminarDir);

        panel.add(bottomBar, BorderLayout.SOUTH);
        cargarTablaDirecciones();
        return panel;
    }

    private void cargarTablaDirecciones() {
        if (addressTableModel == null) return;
        addressTableModel.setRowCount(0);
        List<BtcAddressEntry> list = BitcoinWalletService.cargarDireccionesEtiquetadas(userEmail, isTestnet);
        
        // Si no hay direcciones etiquetadas, agregamos la principal
        if (list.isEmpty() && currentWallet != null) {
            String mainAddr = BitcoinWalletService.obtenerDireccion(currentWallet);
            BitcoinWalletService.guardarDireccionEtiquetada(userEmail, isTestnet, mainAddr, "Dirección Principal");
            list = BitcoinWalletService.cargarDireccionesEtiquetadas(userEmail, isTestnet);
        }

        for (BtcAddressEntry entry : list) {
            String label = entry.label.isEmpty() ? "(Sin etiqueta)" : entry.label;
            addressTableModel.addRow(new Object[]{label, entry.address, entry.date});
        }
    }

    private void mostrarDialogoNuevaDireccion() {
        if (currentWallet == null) return;

        JTextField txtTag = new JTextField();
        txtTag.putClientProperty(FlatClientProperties.STYLE, "font: 13; background: #F8F9FA");

        Object[] msg = {
            "Ingrese un TÍTULO o ETIQUETA para identificar quién le envía fondos:\n(O déjelo vacío si no desea asignarle un nombre)",
            txtTag
        };

        int opc = JOptionPane.showConfirmDialog(this, msg, "Generar Código de Dirección", JOptionPane.OK_CANCEL_OPTION);
        if (opc != JOptionPane.OK_OPTION) return;

        String tag = txtTag.getText().trim();
        String nuevaAddr = BitcoinWalletService.generarNuevaDireccion(currentWallet);

        // Guardar en libreta
        BitcoinWalletService.guardarDireccionEtiquetada(userEmail, isTestnet, nuevaAddr, tag);
        cargarTablaDirecciones();

        // Mostrar ventana con la dirección creada y su QR
        mostrarDialogoQrDireccion(nuevaAddr, tag.isEmpty() ? "Nueva Dirección Generada" : tag);
        AuditService.log(userEmail, "BTC_ADDRESS_CREATED", "Generada dirección: " + nuevaAddr + " [Tag: " + tag + "]");
    }

    private void mostrarDialogoQrDireccion(String address, String title) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Dirección: " + title, JDialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());
        dlg.setSize(420, 480);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        int r = 0;

        JLabel lblT = new JLabel(title.toUpperCase(), SwingConstants.CENTER);
        lblT.putClientProperty(FlatClientProperties.STYLE, "font: bold 14; foreground: #0078D4");
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = r++;
        p.add(lblT, gbc);

        JLabel lblQ = new JLabel();
        lblQ.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            BufferedImage img = QRCodeService.generateQRCode(address, 220, 220);
            lblQ.setIcon(new ImageIcon(img));
        } catch (Exception ex) {}
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = r++;
        p.add(lblQ, gbc);

        JTextField tf = new JTextField(address);
        tf.setEditable(false);
        tf.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; background: #F8F9FA; border: 6,8,6,8");
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = r++;
        p.add(tf, gbc);

        JButton btnC = new JButton("📋 COPIAR DIRECCIÓN");
        btnC.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #0078D4; foreground: #FFFFFF; font: bold 12");
        btnC.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(address), null);
            ToastNotification.show(dlg, "Copiada al portapapeles.", ToastNotification.Type.SUCCESS);
        });
        gbc.insets = new Insets(0, 0, 0, 0); gbc.gridy = r++;
        p.add(btnC, gbc);

        dlg.add(p, BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    // --- PANEL 3: HISTORIAL DE TRANSACCIONES CON CONFIRMACIÓN ---

    private JPanel crearPanelTransacciones() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("Historial de Transacciones Entrantes y Salientes de su Billetera");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #666666");
        header.add(lblTitle, BorderLayout.WEST);

        JButton btnRefrescarTxs = new JButton("🔄 REFRESCAR TRANSACCIONES");
        btnRefrescarTxs.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11; background: #E1F0FE; foreground: #0078D4");
        btnRefrescarTxs.addActionListener(e -> refrescarTransaccionesEnSegundoPlano());
        header.add(btnRefrescarTxs, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);

        txTableModel = new DefaultTableModel(new Object[]{"TIPO", "MONTO BTC", "ESTADO / CONFIRMACIÓN", "FECHA / HORA", "TXID (HASH)"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        txTable = new JTable(txTableModel);
        txTable.getTableHeader().setReorderingAllowed(false);
        txTable.putClientProperty(FlatClientProperties.STYLE, "rowHeight: 34; showHorizontalLines: true");
        txTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        txTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        txTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        txTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        txTable.getColumnModel().getColumn(4).setPreferredWidth(200);

        // Renderizador de color para Tipo y Estado
        txTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String val = String.valueOf(value);
                if (val.contains("ENTRANTE")) {
                    c.setForeground(new Color(16, 124, 16));
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(new Color(216, 59, 1));
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
                return c;
            }
        });

        txTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String val = String.valueOf(value);
                if (val.startsWith("✅")) {
                    c.setForeground(new Color(16, 124, 16));
                } else {
                    c.setForeground(new Color(180, 100, 0));
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(txTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);

        JButton btnCopiarTxid = new JButton("📋 COPIAR TXID COMPLETO");
        btnCopiarTxid.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11");
        btnCopiarTxid.addActionListener(e -> {
            int row = txTable.getSelectedRow();
            if (row != -1) {
                String txid = (String) txTableModel.getValueAt(row, 4);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(txid), null);
                ToastNotification.show(this, "TXID copiado: " + txid, ToastNotification.Type.SUCCESS);
            }
        });
        bottom.add(btnCopiarTxid);

        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void refrescarTransaccionesEnSegundoPlano() {
        if (currentWallet == null || txTableModel == null) return;
        String address = BitcoinWalletService.obtenerDireccion(currentWallet);

        SwingWorker<List<BtcTxInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<BtcTxInfo> doInBackground() {
                return BitcoinWalletService.consultarTransacciones(address, isTestnet);
            }

            @Override
            protected void done() {
                try {
                    List<BtcTxInfo> txs = get();
                    txTableModel.setRowCount(0);
                    if (txs.isEmpty()) {
                        ToastNotification.show(BilleteraBitcoin.this, "No hay transacciones registradas aún.", ToastNotification.Type.INFO);
                    } else {
                        for (BtcTxInfo tx : txs) {
                            String tipo = tx.isIncoming ? "📥 ENTRANTE" : "📤 SALIENTE";
                            txTableModel.addRow(new Object[]{
                                tipo,
                                tx.getBtcAmount(),
                                tx.getStatusDescription(),
                                tx.getFormattedDate(),
                                tx.txid
                            });
                        }
                        ToastNotification.show(BilleteraBitcoin.this, "Transacciones actualizadas (" + txs.size() + ").", ToastNotification.Type.SUCCESS);
                    }
                } catch (Exception ex) {
                    ToastNotification.show(BilleteraBitcoin.this, "Error al cargar historial de transacciones.", ToastNotification.Type.ERROR);
                }
            }
        };
        worker.execute();
    }

    // --- PANEL 4: SEGURIDAD ---

    private JPanel crearPanelSeguridad() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        int r = 0;

        JLabel lblSecTitle = new JLabel("GESTIÓN DE SEGURIDAD ZERO-TRUST");
        lblSecTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 16; foreground: #202020");
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = r++;
        panel.add(lblSecTitle, gbc);

        JLabel lblDesc = new JLabel("<html>Su clave privada se deriva de una semilla BIP-39 y se almacena protegida con <b>AES-256 GCM</b> derivado de su PIN.<br/>Nunca comparta su frase de recuperación con nadie.</html>");
        lblDesc.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #555555");
        gbc.insets = new Insets(0, 0, 25, 0); gbc.gridy = r++;
        panel.add(lblDesc, gbc);

        JButton btnGuardarBd = new JButton("💾 GUARDAR SEMILLA EN BD (CON CÓDIGO)");
        btnGuardarBd.setPreferredSize(new Dimension(420, 46));
        btnGuardarBd.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #E0E7FF; foreground: #3730A3; font: bold 13; margin: 8,18,8,18");
        btnGuardarBd.addActionListener(e -> guardarSemillaActualEnBdFlujo());
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = r++;
        panel.add(btnGuardarBd, gbc);

        JButton btnSemilla = new JButton("🔑 REVELAR FRASE SEMILLA (12 PALABRAS)");
        btnSemilla.setPreferredSize(new Dimension(420, 46));
        btnSemilla.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FFF4CE; foreground: #795B00; font: bold 13; margin: 8,18,8,18");
        btnSemilla.addActionListener(e -> revelarSemillaConPin());
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = r++;
        panel.add(btnSemilla, gbc);

        JButton btnEliminar = new JButton("🗑️ DESTRUIR BILLETERA LOCAL DE FORMA SEGURA");
        btnEliminar.setPreferredSize(new Dimension(420, 46));
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FDE7E9; foreground: #A80000; font: bold 13; margin: 8,18,8,18");
        btnEliminar.addActionListener(e -> eliminarBilleteraSeguraConPin());
        gbc.insets = new Insets(0, 0, 0, 0); gbc.gridy = r++;
        panel.add(btnEliminar, gbc);

        return panel;
    }

    // --- MÉTODOS DE SOPORTE Y SEGUNDO PLANO ---

    private void refrescarDatosEnSegundoPlano() {
        if (currentWallet == null) return;
        if (btnRefreshAll != null) {
            btnRefreshAll.setEnabled(false);
            btnRefreshAll.setText("Consultando...");
        }

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
                        currentBalanceSats = sats;
                        if (lblBalanceBtc != null && lblBalanceSats != null) {
                            lblBalanceBtc.setText(BitcoinWalletService.satsToBtc(sats));
                            lblBalanceSats.setText(String.format("%,d Satoshis", sats));
                        }
                        if (lblBalanceDispEnviar != null) {
                            lblBalanceDispEnviar.setText(String.format("Saldo disponible: %s (%,d Sats)", BitcoinWalletService.satsToBtc(sats), sats));
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    if (btnRefreshAll != null) {
                        btnRefreshAll.setEnabled(true);
                        btnRefreshAll.setText("🔄 ACTUALIZAR");
                    }
                }
            }
        };
        worker.execute();

        // Cargar transacciones
        refrescarTransaccionesEnSegundoPlano();
    }

    private void crearNuevaBilleteraFlujo() {
        Wallet nuevaWallet = BitcoinWalletService.crearBilletera(isTestnet);
        List<String> palabras = BitcoinWalletService.obtenerPalabras(nuevaWallet);

        mostrarVentanaPalabrasSemilla(palabras, "NUEVA BILLETERA (" + (isTestnet ? "TESTNET" : "MAINNET") + ")", () -> {
            solicitarPinYGuardar(nuevaWallet);
        });
    }

    private void restaurarBilleteraFlujo() {
        JTextField txtPalabras = new JTextField();
        txtPalabras.putClientProperty(FlatClientProperties.STYLE, "font: 13; background: #F8F9FA");

        Object[] msg = {
            "Ingrese sus 12 o 24 palabras separadas por espacios para la red " + (isTestnet ? "TESTNET:" : "MAINNET:"),
            txtPalabras
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
            Wallet wallet = BitcoinWalletService.restaurarBilletera(palabras, isTestnet);
            solicitarPinYGuardar(wallet);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al restaurar billetera: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solicitarPinYGuardar(Wallet wallet) {
        JPasswordField pfPin = new JPasswordField(4);
        int opc = JOptionPane.showConfirmDialog(this,
                new Object[]{"Ingrese su PIN para cifrar y blindar la billetera en este equipo:", pfPin},
                "Blindaje de Billetera",
                JOptionPane.OK_CANCEL_OPTION);

        if (opc != JOptionPane.OK_OPTION) return;

        char[] pin = pfPin.getPassword();
        char[] pinCheck = Arrays.copyOf(pin, pin.length);
        if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pinCheck)) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto. Operación cancelada.", "Error", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            return;
        }

        try {
            BitcoinWalletService.guardarBilleteraCifrada(wallet, userEmail, pin, isTestnet);
            this.currentWallet = wallet;
            renderizarPantallaDashboard();
            ToastNotification.show(this, "Billetera guardada y blindada con éxito.", ToastNotification.Type.SUCCESS);
            AuditService.log(userEmail, "BTC_WALLET_SAVED", "Billetera guardada con éxito (Testnet: " + isTestnet + ")");
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

        JButton btnGuardarBdModal = new JButton("💾 GUARDAR EN BD");
        btnGuardarBdModal.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #E0E7FF; foreground: #3730A3; font: bold 12");
        btnGuardarBdModal.addActionListener(e -> {
            JTextField txtCod = new JTextField("WALLET-" + (isTestnet ? "TESTNET" : "MAINNET") + "-01");
            JPasswordField pfP = new JPasswordField(4);
            int opt = JOptionPane.showConfirmDialog(dlg,
                new Object[]{"Código / Nombre para identificar en la BD:", txtCod, "PIN de autorización:", pfP},
                "Guardar Semilla en Base de Datos", JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.OK_OPTION) {
                char[] pChars = pfP.getPassword();
                char[] pCheck = Arrays.copyOf(pChars, pChars.length);
                if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pCheck)) {
                    JOptionPane.showMessageDialog(dlg, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
                    Arrays.fill(pChars, '0');
                    return;
                }
                String c = txtCod.getText().trim();
                if (!c.isEmpty()) {
                    String frase = String.join(" ", palabras);
                    boolean ok = LocalDatabaseService.guardarSemillaWallet(userEmail, c, isTestnet ? "TESTNET" : "MAINNET", "", frase, pChars);
                    if (ok) {
                        ToastNotification.show(dlg, "Semilla guardada en BD con código: " + c.toUpperCase(), ToastNotification.Type.SUCCESS);
                    }
                }
                Arrays.fill(pChars, '0');
            }
        });
        bottom.add(btnGuardarBdModal);

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
                new Object[]{"Ingrese su PIN para visualizar su Frase Semilla (" + (isTestnet ? "Testnet" : "Mainnet") + "):", pfPin},
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

    private void eliminarBilleteraSeguraConPin() {
        int conf = JOptionPane.showConfirmDialog(this,
                "⚠️ DESTRUCCIÓN SEGURA DE BILLETERA LOCAL (" + (isTestnet ? "Testnet" : "Mainnet") + ")\n\n" +
                "Esta operación sobrescribirá criptográficamente las claves privadas almacenadas en este equipo\n" +
                "y las eliminará de forma irreversible para prevenir recuperación forense.\n\n" +
                "¿Se aseguró de respaldar su frase semilla o guardarla en la Bóveda de la Base de Datos antes de continuar?",
                "Confirmación de Destrucción Segura",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (conf != JOptionPane.YES_OPTION) return;

        JPasswordField pfPin = new JPasswordField(4);
        int opc = JOptionPane.showConfirmDialog(this,
                new Object[]{"Ingrese su PIN para autorizar la destrucción segura:", pfPin},
                "Autorización Crítica",
                JOptionPane.OK_CANCEL_OPTION);

        if (opc != JOptionPane.OK_OPTION) return;
        char[] pin = pfPin.getPassword();
        char[] pinCheck = Arrays.copyOf(pin, pin.length);

        if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pinCheck)) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            return;
        }

        try {
            BitcoinWalletService.eliminarBilletera(userEmail, isTestnet);
            currentWallet = null;
            System.gc();
            renderizarPantallaBienvenida();
            ToastNotification.show(this, "Billetera (" + (isTestnet ? "Testnet" : "Mainnet") + ") destruida de forma segura.", ToastNotification.Type.SUCCESS);
            AuditService.log(userEmail, "BTC_WALLET_SECURE_DESTROY", "Billetera local destruida con sobrescritura criptográfica.");
        } finally {
            Arrays.fill(pin, '0');
        }
    }

    // --- PANEL 5: BÓVEDA DE SEMILLAS EN BASE DE DATOS ---

    private DefaultTableModel seedTableModel;
    private JTable tblSeeds;
    private List<BtcSeedEntry> currentSeedList;

    private JPanel crearPanelBovedaSemillas() {
        JPanel p = new JPanel(new BorderLayout(0, 15));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Cabecera informativa y barra de herramientas
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 4));
        info.setOpaque(false);
        JLabel title = new JLabel("BÓVEDA DE SEMILLAS CRIPTOGRÁFICAS (NUBE FIREBASE & BD LOCAL)");
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold 15; foreground: #1E293B");
        JLabel desc = new JLabel("Frases semilla sincronizadas en la Nube Firebase y respaldadas con cifrado AES-256 GCM.");
        desc.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #64748B");
        info.add(title);
        info.add(desc);
        top.add(info, BorderLayout.CENTER);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tools.setOpaque(false);

        JButton btnGuardarActual = new JButton("💾 GUARDAR EN LA NUBE");
        btnGuardarActual.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #4F46E5; foreground: #FFFFFF; font: bold 12");
        btnGuardarActual.addActionListener(e -> guardarSemillaActualEnBdFlujo());
        tools.add(btnGuardarActual);

        JButton btnGuardarManual = new JButton("➕ GUARDAR OTRA SEMILLA");
        btnGuardarManual.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #FFFFFF; foreground: #334155; font: bold 12; border: 1,1,1,1,#CBD5E1");
        btnGuardarManual.addActionListener(e -> guardarSemillaManualEnBdFlujo());
        tools.add(btnGuardarManual);

        JButton btnRefrescar = new JButton("🔄 ACTUALIZAR");
        btnRefrescar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F1F5F9; font: bold 12");
        btnRefrescar.addActionListener(e -> cargarTablaSemillas());
        tools.add(btnRefrescar);

        top.add(tools, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        // Tabla
        String[] cols = {"CÓDIGO WALLET", "RED", "DIRECCIÓN BITCOIN", "ESTADO CIFRADO", "FECHA REGISTRO"};
        seedTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tblSeeds = new JTable(seedTableModel);
        tblSeeds.getTableHeader().setReorderingAllowed(false);
        tblSeeds.setRowHeight(34);
        tblSeeds.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines: true; selectionBackground: #E0E7FF; selectionForeground: #1E1B4B");
        tblSeeds.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font: bold 12; background: #F8FAFC");
        tblSeeds.getColumnModel().getColumn(0).setPreferredWidth(160);
        tblSeeds.getColumnModel().getColumn(1).setPreferredWidth(90);
        tblSeeds.getColumnModel().getColumn(2).setPreferredWidth(320);
        tblSeeds.getColumnModel().getColumn(3).setPreferredWidth(160);
        tblSeeds.getColumnModel().getColumn(4).setPreferredWidth(140);

        JScrollPane scroll = new JScrollPane(tblSeeds);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        p.add(scroll, BorderLayout.CENTER);

        // Barra inferior de acciones
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        bottom.setOpaque(false);

        JButton btnVerSemilla = new JButton("👁️ REVELAR SEMILLA (12 PALABRAS)");
        btnVerSemilla.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #FEF3C7; foreground: #92400E; font: bold 12");
        btnVerSemilla.addActionListener(e -> revelarSemillaSeleccionada());
        bottom.add(btnVerSemilla);

        JButton btnRestaurarSeleccion = new JButton("🚀 RESTAURAR COMO ACTIVA");
        btnRestaurarSeleccion.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #D1FAE5; foreground: #065F46; font: bold 12");
        btnRestaurarSeleccion.addActionListener(e -> restaurarSemillaSeleccionada());
        bottom.add(btnRestaurarSeleccion);

        JButton btnBorrarSemilla = new JButton("🗑️ ELIMINAR DE LA NUBE");
        btnBorrarSemilla.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #FEE2E2; foreground: #991B1B; font: bold 12");
        btnBorrarSemilla.addActionListener(e -> eliminarSemillaSeleccionada());
        bottom.add(btnBorrarSemilla);

        p.add(bottom, BorderLayout.SOUTH);

        cargarTablaSemillas();
        return p;
    }

    private void cargarTablaSemillas() {
        if (seedTableModel == null) return;
        seedTableModel.setRowCount(0);
        currentSeedList = LocalDatabaseService.obtenerSemillasWallet(userEmail);
        for (BtcSeedEntry se : currentSeedList) {
            seedTableModel.addRow(new Object[]{
                se.codigoWallet,
                se.red,
                (se.direccion != null && !se.direccion.isEmpty()) ? se.direccion : "(No registrada)",
                "🔒 AES-256 GCM (Nube Firebase)",
                se.getFormattedDate()
            });
        }
    }

    private void guardarSemillaActualEnBdFlujo() {
        if (currentWallet == null) {
            ToastNotification.show(this, "No hay una billetera activa actualmente.", ToastNotification.Type.WARNING);
            return;
        }

        List<String> palabras = BitcoinWalletService.obtenerPalabras(currentWallet);
        if (palabras == null || palabras.isEmpty()) {
            ToastNotification.show(this, "No se pudo extraer la frase semilla mnemónica.", ToastNotification.Type.ERROR);
            return;
        }

        String dir = BitcoinWalletService.obtenerDireccion(currentWallet);

        JTextField txtCodigo = new JTextField("WALLET-" + (isTestnet ? "TESTNET" : "MAINNET") + "-01");
        txtCodigo.putClientProperty(FlatClientProperties.STYLE, "font: 13; background: #F8F9FA");
        JPasswordField pfPin = new JPasswordField(4);

        Object[] msg = {
            "Asigne un Código / Nombre para identificar esta billetera en la nube (Firebase):",
            txtCodigo,
            "Ingrese su PIN para autorizar y cifrar la semilla en Firebase Firestore:",
            pfPin
        };

        int opt = JOptionPane.showConfirmDialog(this, msg, "Guardar Semilla en la Nube (Firebase)", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION) return;

        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty()) {
            ToastNotification.show(this, "Debe ingresar un código para la billetera.", ToastNotification.Type.WARNING);
            return;
        }

        char[] pin = pfPin.getPassword();
        char[] pinCheck = Arrays.copyOf(pin, pin.length);
        if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pinCheck)) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto. Operación cancelada.", "Error", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            return;
        }

        try {
            String frase = String.join(" ", palabras);
            boolean guardado = LocalDatabaseService.guardarSemillaWallet(
                userEmail, codigo, isTestnet ? "TESTNET" : "MAINNET", dir, frase, pin
            );
            if (guardado) {
                ToastNotification.show(this, "Semilla guardada y sincronizada en Firebase con código: " + codigo.toUpperCase(), ToastNotification.Type.SUCCESS);
                AuditService.log(userEmail, "BTC_SEED_SAVED_CLOUD", "Semilla de billetera (" + codigo + ") guardada en Firebase Cloud.");
                cargarTablaSemillas();
            } else {
                ToastNotification.show(this, "No se pudo guardar la semilla en la nube.", ToastNotification.Type.ERROR);
            }
        } finally {
            Arrays.fill(pin, '0');
        }
    }

    private void guardarSemillaManualEnBdFlujo() {
        JTextField txtCodigo = new JTextField();
        txtCodigo.putClientProperty(FlatClientProperties.STYLE, "font: 13; background: #F8F9FA");
        JComboBox<String> cmbRed = new JComboBox<>(new String[]{"TESTNET", "MAINNET"});
        JTextField txtPalabras = new JTextField();
        txtPalabras.putClientProperty(FlatClientProperties.STYLE, "font: 13; background: #F8F9FA");
        JPasswordField pfPin = new JPasswordField(4);

        Object[] msg = {
            "Código / Identificador de la Billetera (ej: WALLET-AHORROS):",
            txtCodigo,
            "Red de la Billetera:",
            cmbRed,
            "12 o 24 Palabras Semilla separadas por espacio:",
            txtPalabras,
            "PIN de autorización:",
            pfPin
        };

        int opt = JOptionPane.showConfirmDialog(this, msg, "Guardar Semilla Externa en Firebase Cloud", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION) return;

        String codigo = txtCodigo.getText().trim();
        String palabrasRaw = txtPalabras.getText().trim();
        if (codigo.isEmpty() || palabrasRaw.isEmpty()) {
            ToastNotification.show(this, "Debe completar todos los campos.", ToastNotification.Type.WARNING);
            return;
        }

        List<String> words = Arrays.asList(palabrasRaw.split("\\s+"));
        if (words.size() != 12 && words.size() != 24) {
            ToastNotification.show(this, "La frase debe contener exactamente 12 o 24 palabras.", ToastNotification.Type.WARNING);
            return;
        }

        char[] pin = pfPin.getPassword();
        char[] pinCheck = Arrays.copyOf(pin, pin.length);
        if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pinCheck)) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            return;
        }

        String redSelec = (String) cmbRed.getSelectedItem();
        boolean isRedTestnet = "TESTNET".equalsIgnoreCase(redSelec);
        String dir = "";
        try {
            Wallet tempWallet = BitcoinWalletService.restaurarBilletera(words, isRedTestnet);
            dir = BitcoinWalletService.obtenerDireccion(tempWallet);
        } catch (Exception ignored) {}

        try {
            boolean guardado = LocalDatabaseService.guardarSemillaWallet(
                userEmail, codigo, redSelec, dir, palabrasRaw, pin
            );
            if (guardado) {
                ToastNotification.show(this, "Semilla guardada y sincronizada en Firebase con código: " + codigo.toUpperCase(), ToastNotification.Type.SUCCESS);
                AuditService.log(userEmail, "BTC_SEED_SAVED_CLOUD", "Semilla externa guardada en Firebase (" + codigo + ")");
                cargarTablaSemillas();
            } else {
                ToastNotification.show(this, "Error al guardar en Firebase / Base de datos.", ToastNotification.Type.ERROR);
            }
        } finally {
            Arrays.fill(pin, '0');
        }
    }

    private void revelarSemillaSeleccionada() {
        if (tblSeeds == null || tblSeeds.getSelectedRow() < 0) {
            ToastNotification.show(this, "Seleccione una semilla de la tabla primero.", ToastNotification.Type.WARNING);
            return;
        }
        int idx = tblSeeds.getSelectedRow();
        if (currentSeedList == null || idx >= currentSeedList.size()) return;
        BtcSeedEntry entry = currentSeedList.get(idx);

        JPasswordField pfPin = new JPasswordField(4);
        int opc = JOptionPane.showConfirmDialog(this,
                new Object[]{"Ingrese su PIN para descifrar y revelar la semilla [" + entry.codigoWallet + "]:", pfPin},
                "Verificación de Seguridad",
                JOptionPane.OK_CANCEL_OPTION);
        if (opc != JOptionPane.OK_OPTION) return;

        char[] pin = pfPin.getPassword();
        char[] pinCheck = Arrays.copyOf(pin, pin.length);
        if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pinCheck)) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            return;
        }

        try {
            String frase = LocalDatabaseService.descifrarSemillaWallet(entry, pin);
            List<String> palabras = Arrays.asList(frase.trim().split("\\s+"));
            mostrarVentanaPalabrasSemilla(palabras, "SEMILLA DESCIFRADA: " + entry.codigoWallet + " (" + entry.red + ")", null);
            AuditService.log(userEmail, "BTC_SEED_REVEAL_DB", "Semilla (" + entry.codigoWallet + ") revelada desde BD.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al descifrar semilla: PIN erróneo o datos corruptos.", "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(pin, '0');
        }
    }

    private void restaurarSemillaSeleccionada() {
        if (tblSeeds == null || tblSeeds.getSelectedRow() < 0) {
            ToastNotification.show(this, "Seleccione una semilla de la tabla primero.", ToastNotification.Type.WARNING);
            return;
        }
        int idx = tblSeeds.getSelectedRow();
        if (currentSeedList == null || idx >= currentSeedList.size()) return;
        BtcSeedEntry entry = currentSeedList.get(idx);

        boolean entryIsTestnet = "TESTNET".equalsIgnoreCase(entry.red);

        int conf = JOptionPane.showConfirmDialog(this,
                "¿Desea restaurar y establecer la billetera [" + entry.codigoWallet + "] (" + entry.red + ") como la billetera activa?\n" +
                (entryIsTestnet != isTestnet ? "Nota: Se cambiará la vista a la red " + entry.red + ".\n" : ""),
                "Restaurar Billetera desde Bóveda",
                JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        JPasswordField pfPin = new JPasswordField(4);
        int opc = JOptionPane.showConfirmDialog(this,
                new Object[]{"Ingrese su PIN para autorizar la restauración:", pfPin},
                "Autorización",
                JOptionPane.OK_CANCEL_OPTION);
        if (opc != JOptionPane.OK_OPTION) return;

        char[] pin = pfPin.getPassword();
        char[] pinCheck = Arrays.copyOf(pin, pin.length);
        if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pinCheck)) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            return;
        }

        try {
            String frase = LocalDatabaseService.descifrarSemillaWallet(entry, pin);
            List<String> palabras = Arrays.asList(frase.trim().split("\\s+"));
            Wallet restored = BitcoinWalletService.restaurarBilletera(palabras, entryIsTestnet);

            this.isTestnet = entryIsTestnet;
            if (cbNetworkSelector != null) {
                cbNetworkSelector.setSelectedIndex(entryIsTestnet ? 0 : 1);
            }
            BitcoinWalletService.guardarBilleteraCifrada(restored, userEmail, pin, entryIsTestnet);
            this.currentWallet = restored;
            renderizarPantallaDashboard();
            ToastNotification.show(this, "Billetera [" + entry.codigoWallet + "] restaurada como activa con éxito.", ToastNotification.Type.SUCCESS);
            AuditService.log(userEmail, "BTC_WALLET_RESTORED_DB", "Billetera restaurada desde semilla en BD (" + entry.codigoWallet + ").");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al restaurar billetera: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(pin, '0');
        }
    }

    private void eliminarSemillaSeleccionada() {
        if (tblSeeds == null || tblSeeds.getSelectedRow() < 0) {
            ToastNotification.show(this, "Seleccione una semilla de la tabla primero.", ToastNotification.Type.WARNING);
            return;
        }
        int idx = tblSeeds.getSelectedRow();
        if (currentSeedList == null || idx >= currentSeedList.size()) return;
        BtcSeedEntry entry = currentSeedList.get(idx);

        int conf = JOptionPane.showConfirmDialog(this,
                "⚠️ ¿Está seguro de eliminar de la nube (Firebase) y la base de datos la semilla [" + entry.codigoWallet + "]?\n" +
                "Esta acción es permanente e irreversible.",
                "Eliminar Semilla de la Bóveda en la Nube",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;

        JPasswordField pfPin = new JPasswordField(4);
        int opc = JOptionPane.showConfirmDialog(this,
                new Object[]{"Ingrese su PIN para confirmar el borrado:", pfPin},
                "Confirmación",
                JOptionPane.OK_CANCEL_OPTION);
        if (opc != JOptionPane.OK_OPTION) return;

        char[] pin = pfPin.getPassword();
        char[] pinCheck = Arrays.copyOf(pin, pin.length);
        if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pinCheck)) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            return;
        }

        try {
            boolean ok = LocalDatabaseService.eliminarSemillaWallet(entry.id);
            if (ok) {
                ToastNotification.show(this, "Semilla eliminada de la nube y la base de datos.", ToastNotification.Type.SUCCESS);
                AuditService.log(userEmail, "BTC_SEED_DELETED_CLOUD", "Semilla (" + entry.codigoWallet + ") eliminada de Firebase y BD.");
                cargarTablaSemillas();
            } else {
                ToastNotification.show(this, "No se pudo eliminar la semilla.", ToastNotification.Type.ERROR);
            }
        } finally {
            Arrays.fill(pin, '0');
        }
    }

    private void mostrarDialogoBovedaSemillas() {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Bóveda de Semillas Criptográficas (Nube Firebase & BD)", JDialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());
        dlg.setSize(880, 520);
        dlg.setLocationRelativeTo(this);
        dlg.add(crearPanelBovedaSemillas(), BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    /**
     * Panel adaptable para JScrollPane que responde dinámicamente al ancho de la ventana
     * y habilita scroll vertical fluido cuando la altura excede el área visible.
     */
    private static class ResponsiveScrollablePanel extends JPanel implements Scrollable {
        public ResponsiveScrollablePanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            Container parent = getParent();
            if (parent instanceof JViewport) {
                return parent.getHeight() > getPreferredSize().height;
            }
            return false;
        }
    }
}
