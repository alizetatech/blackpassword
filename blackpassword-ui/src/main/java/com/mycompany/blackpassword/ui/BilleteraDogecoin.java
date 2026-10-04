package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.core.util.ToastNotification;
import com.mycompany.blackpassword.services.AuditService;
import com.mycompany.blackpassword.services.DogecoinWalletService;
import com.mycompany.blackpassword.services.DogecoinWalletService.DogeAddressEntry;
import com.mycompany.blackpassword.services.DogecoinWalletService.DogeTxInfo;
import com.mycompany.blackpassword.services.DogecoinWalletService.DogeWalletHolder;
import com.mycompany.blackpassword.services.LocalDatabaseService;
import com.mycompany.blackpassword.services.LocalDatabaseService.DogeSeedEntry;
import com.mycompany.blackpassword.services.QRCodeService;
import com.mycompany.blackpassword.services.SecuritySession;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * Módulo de Billetera Dogecoin Inteligente HD (BIP-39 / BIP-44) para Blackpassword.
 * Proporciona soporte multired (Mainnet y Testnet), libreta de direcciones etiquetadas,
 * monitor de saldo e historial de transacciones en tiempo real, bóveda de semillas en la nube (Firebase)
 * y destrucción segura con estilo refinado temático Dogecoin (Gold / Amarillo cálido).
 */
public class BilleteraDogecoin extends JInternalFrame {

    private final String userEmail;
    private boolean isTestnet = false;
    private DogeWalletHolder currentWallet;

    // Componentes UI principales
    private JPanel contentContainer;
    private JComboBox<String> cbNetworkSelector;
    private JLabel lblBalanceDoge;
    private JLabel lblBalanceKoinus;
    private JTextField txtMainAddress;
    private JLabel lblMainQr;
    private JButton btnRefreshAll;

    // Tablas
    private DefaultTableModel txTableModel;
    private JTable txTable;
    private DefaultTableModel addressTableModel;
    private JTable addressTable;
    private DefaultTableModel seedTableModel;
    private JTable tblSeeds;
    private List<DogeSeedEntry> currentSeedList;

    public BilleteraDogecoin(String email) {
        super("Billetera Dogecoin Inteligente HD (Ð)", true, true, true, true);
        this.userEmail = email;

        initComponentsCustom();
        applyPremiumStyle();
        cargarBilleteraParaRedActual();
    }

    private void applyPremiumStyle() {
        putClientProperty(FlatClientProperties.STYLE, "background: #FDFCF7");
        setBorder(null);
    }

    private void initComponentsCustom() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(880, 700));

        // --- BARRA SUPERIOR CON SELECTOR DE RED ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 225, 210)),
                BorderFactory.createEmptyBorder(12, 22, 12, 22)
        ));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);

        JLabel lblIcon = new JLabel("Ð");
        lblIcon.putClientProperty(FlatClientProperties.STYLE, "font: bold 28; foreground: #C2A633");
        titlePanel.add(lblIcon);

        JPanel textTitles = new JPanel(new GridLayout(2, 1, 0, 2));
        textTitles.setOpaque(false);
        JLabel lblTitle = new JLabel("BILLETERA DOGECOIN INTELIGENTE (BIP-44)");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 15; foreground: #2D250E");
        JLabel lblSub = new JLabel("Direcciones 'D' • Libreta de Etiquetas • Monitor en Vivo • Bóveda Nube");
        lblSub.putClientProperty(FlatClientProperties.STYLE, "font: 11; foreground: #786D55");
        textTitles.add(lblTitle);
        textTitles.add(lblSub);
        titlePanel.add(textTitles);

        topBar.add(titlePanel, BorderLayout.WEST);

        // Selector de Red (Mainnet / Testnet)
        JPanel netPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        netPanel.setOpaque(false);

        JLabel lblRed = new JLabel("RED:");
        lblRed.putClientProperty(FlatClientProperties.STYLE, "font: bold 11; foreground: #786D55");
        netPanel.add(lblRed);

        cbNetworkSelector = new JComboBox<>(new String[]{"🟡 MAINNET (REAL - 'D')", "🟢 TESTNET (PRUEBAS - 'n')"});
        cbNetworkSelector.setSelectedIndex(isTestnet ? 1 : 0);
        cbNetworkSelector.setPreferredSize(new Dimension(205, 36));
        cbNetworkSelector.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FFFFFF; font: bold 12");
        cbNetworkSelector.addActionListener(e -> cambiarRed(cbNetworkSelector.getSelectedIndex() == 1));
        netPanel.add(cbNetworkSelector);

        btnRefreshAll = new JButton("🔄");
        btnRefreshAll.setToolTipText("Actualizar balance y transacciones");
        btnRefreshAll.setPreferredSize(new Dimension(36, 36));
        btnRefreshAll.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FEF3C7; foreground: #B45309; font: bold 13");
        btnRefreshAll.addActionListener(e -> refrescarDatosEnSegundoPlano());
        netPanel.add(btnRefreshAll);

        topBar.add(netPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        contentContainer = new JPanel(new BorderLayout());
        contentContainer.setOpaque(false);
        add(contentContainer, BorderLayout.CENTER);
    }

    private void cambiarRed(boolean nuevaEsTestnet) {
        if (this.isTestnet == nuevaEsTestnet) return;
        this.isTestnet = nuevaEsTestnet;
        cargarBilleteraParaRedActual();
    }

    private void cargarBilleteraParaRedActual() {
        boolean existe = DogecoinWalletService.existeBilletera(userEmail, isTestnet);
        if (!existe) {
            this.currentWallet = null;
            renderizarPantallaBienvenida();
            return;
        }

        // Solicitar PIN para descifrar la billetera de esta red
        JPasswordField pfPin = new JPasswordField(4);
        pfPin.putClientProperty(FlatClientProperties.STYLE, "font: 14; showRevealButton: true");
        Object[] msg = {
                "Ingrese su PIN para desbloquear la billetera Dogecoin (" + (isTestnet ? "TESTNET" : "MAINNET") + "):",
                pfPin
        };
        int opt = JOptionPane.showConfirmDialog(this, msg, "Desbloqueo de Billetera Dogecoin", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) {
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
            currentWallet = DogecoinWalletService.cargarBilleteraCifrada(userEmail, pin, isTestnet);
            renderizarPantallaDashboard();
            ToastNotification.show(this, "Billetera Dogecoin (" + (isTestnet ? "Testnet" : "Mainnet") + ") desbloqueada.", ToastNotification.Type.SUCCESS);
            AuditService.log(userEmail, "DOGE_WALLET_UNLOCK", "Billetera Dogecoin desbloqueada (Testnet: " + isTestnet + ").");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto o error al descifrar la billetera Dogecoin.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
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

        JLabel lblHero = new JLabel("Ð");
        lblHero.setHorizontalAlignment(SwingConstants.CENTER);
        lblHero.putClientProperty(FlatClientProperties.STYLE, "font: bold 64; foreground: #C2A633");
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = row++;
        p.add(lblHero, gbc);

        JLabel lblWelcome = new JLabel("No hay billetera Dogecoin configurada en " + (isTestnet ? "RED TESTNET" : "RED MAINNET"));
        lblWelcome.setHorizontalAlignment(SwingConstants.CENTER);
        lblWelcome.putClientProperty(FlatClientProperties.STYLE, "font: bold 18; foreground: #2D250E");
        gbc.insets = new Insets(0, 0, 8, 0); gbc.gridy = row++;
        p.add(lblWelcome, gbc);

        JLabel lblDesc = new JLabel("Cree una nueva billetera jerárquica con 12 palabras o restaure una existente con formato oficial 'D'.");
        lblDesc.setHorizontalAlignment(SwingConstants.CENTER);
        lblDesc.putClientProperty(FlatClientProperties.STYLE, "font: 13; foreground: #786D55");
        gbc.insets = new Insets(0, 0, 30, 0); gbc.gridy = row++;
        p.add(lblDesc, gbc);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnGroup.setOpaque(false);

        JButton btnCrear = new JButton("✨ CREAR NUEVA BILLETERA DOGE");
        btnCrear.setPreferredSize(new Dimension(245, 46));
        btnCrear.putClientProperty(FlatClientProperties.STYLE, "arc: 14; background: #D97706; foreground: #FFFFFF; font: bold 13");
        btnCrear.addActionListener(e -> crearNuevaBilleteraFlujo());
        btnGroup.add(btnCrear);

        JButton btnRestaurar = new JButton("📥 RESTAURAR DESDE 12 PALABRAS");
        btnRestaurar.setPreferredSize(new Dimension(245, 46));
        btnRestaurar.putClientProperty(FlatClientProperties.STYLE, "arc: 14; background: #FFFFFF; foreground: #333333; font: bold 13; border: 1,1,1,1,#D5CCA8");
        btnRestaurar.addActionListener(e -> restaurarBilleteraFlujo());
        btnGroup.add(btnRestaurar);

        JButton btnBoveda = new JButton("🔐 BÓVEDA DE SEMILLAS (NUBE)");
        btnBoveda.setPreferredSize(new Dimension(240, 46));
        btnBoveda.putClientProperty(FlatClientProperties.STYLE, "arc: 14; background: #78350F; foreground: #FFFFFF; font: bold 13");
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

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.putClientProperty(FlatClientProperties.STYLE, "tabAreaInsets: 10,15,0,15; font: bold 12");

        tabbedPane.addTab("📊 RESUMEN & BALANCE", crearPanelResumen());
        tabbedPane.addTab("🏷️ DIRECCIONES & ETIQUETAS", crearPanelDirecciones());
        tabbedPane.addTab("📜 HISTORIAL DE TRANSACCIONES", crearPanelTransacciones());
        tabbedPane.addTab("🔐 BÓVEDA DE SEMILLAS (NUBE)", crearPanelBovedaSemillas());
        tabbedPane.addTab("⚙️ SEGURIDAD", crearPanelSeguridad());

        contentContainer.add(tabbedPane, BorderLayout.CENTER);
        contentContainer.revalidate();
        contentContainer.repaint();

        refrescarDatosEnSegundoPlano();
    }

    // --- PANEL 1: RESUMEN Y BALANCE ---

    private JPanel crearPanelResumen() {
        JPanel main = new JPanel(new BorderLayout(0, 18));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Tarjeta de Balance
        JPanel cardBalance = new JPanel(new GridBagLayout());
        cardBalance.setBackground(new Color(254, 243, 199)); // Doge Gold light
        cardBalance.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(245, 158, 11), 1, true),
                BorderFactory.createEmptyBorder(22, 28, 22, 28)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 1.0;
        int row = 0;

        JLabel lblTag = new JLabel("BALANCE TOTAL (" + (isTestnet ? "TESTNET" : "DOGECOIN MAINNET") + ")");
        lblTag.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #92400E");
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 8, 0);
        cardBalance.add(lblTag, gbc);

        lblBalanceDoge = new JLabel("0.0000 DOGE");
        lblBalanceDoge.putClientProperty(FlatClientProperties.STYLE, "font: bold 32; foreground: #78350F");
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 6, 0);
        cardBalance.add(lblBalanceDoge, gbc);

        lblBalanceKoinus = new JLabel("≈ 0 Koinus (1 DOGE = 100,000,000 Koinu)");
        lblBalanceKoinus.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #B45309");
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 0, 0);
        cardBalance.add(lblBalanceKoinus, gbc);

        main.add(cardBalance, BorderLayout.NORTH);

        // Panel de Dirección Principal y QR
        JPanel cardAddress = new JPanel(new BorderLayout(25, 0));
        cardAddress.setBackground(Color.WHITE);
        cardAddress.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 225, 210), 1, true),
                BorderFactory.createEmptyBorder(22, 22, 22, 22)
        ));

        lblMainQr = new JLabel();
        lblMainQr.setPreferredSize(new Dimension(160, 160));
        lblMainQr.setHorizontalAlignment(SwingConstants.CENTER);
        lblMainQr.setBorder(BorderFactory.createLineBorder(new Color(240, 235, 220), 1, true));
        cardAddress.add(lblMainQr, BorderLayout.WEST);

        JPanel details = new JPanel(new GridBagLayout());
        details.setOpaque(false);
        GridBagConstraints gbcD = new GridBagConstraints();
        gbcD.gridx = 0; gbcD.fill = GridBagConstraints.HORIZONTAL; gbcD.weightx = 1.0;
        int r = 0;

        JLabel lblAddrTitle = new JLabel("DIRECCIÓN PÚBLICA DE RECEPCIÓN DOGECOIN (LEGACY / P2PKH)");
        lblAddrTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #78350F");
        gbcD.gridy = r++; gbcD.insets = new Insets(0, 0, 8, 0);
        details.add(lblAddrTitle, gbcD);

        txtMainAddress = new JTextField();
        txtMainAddress.setEditable(false);
        txtMainAddress.putClientProperty(FlatClientProperties.STYLE, "font: bold 13; background: #FFFBEB; border: 8,10,8,10");
        gbcD.gridy = r++; gbcD.insets = new Insets(0, 0, 15, 0);
        details.add(txtMainAddress, gbcD);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);

        JButton btnCopy = new JButton("📋 COPIAR DIRECCIÓN");
        btnCopy.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #D97706; foreground: #FFFFFF; font: bold 12");
        btnCopy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(txtMainAddress.getText()), null);
            ToastNotification.show(this, "Dirección Dogecoin copiada.", ToastNotification.Type.SUCCESS);
        });
        btnRow.add(btnCopy);

        JButton btnNewAddr = new JButton("➕ GENERAR NUEVA DIRECCIÓN");
        btnNewAddr.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FEF3C7; foreground: #92400E; font: bold 12");
        btnNewAddr.addActionListener(e -> generarNuevaDireccionFlujo());
        btnRow.add(btnNewAddr);

        gbcD.gridy = r++; gbcD.insets = new Insets(0, 0, 0, 0);
        details.add(btnRow, gbcD);

        cardAddress.add(details, BorderLayout.CENTER);
        main.add(cardAddress, BorderLayout.CENTER);

        return main;
    }

    // --- PANEL 2: LIBRETA DE DIRECCIONES Y ETIQUETAS ---

    private JPanel crearPanelDirecciones() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("Libreta de Direcciones Dogecoin con Título / Identificador");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #786D55");
        header.add(lblTitle, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton btnAddCustom = new JButton("🏷️ AGREGAR DIRECCIÓN CON TÍTULO");
        btnAddCustom.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 12; background: #D97706; foreground: #FFFFFF");
        btnAddCustom.addActionListener(e -> agregarDireccionEtiquetadaFlujo());
        actions.add(btnAddCustom);

        JButton btnCopySel = new JButton("📋 COPIAR");
        btnCopySel.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 12; background: #FEF3C7; foreground: #92400E");
        btnCopySel.addActionListener(e -> copiarDireccionSeleccionada());
        actions.add(btnCopySel);

        JButton btnDeleteSel = new JButton("🗑️ ELIMINAR");
        btnDeleteSel.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 12; background: #FEE2E2; foreground: #991B1B");
        btnDeleteSel.addActionListener(e -> eliminarDireccionSeleccionada());
        actions.add(btnDeleteSel);

        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        String[] cols = {"TÍTULO / ETIQUETA", "DIRECCIÓN DOGECOIN", "FECHA CREACIÓN"};
        addressTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        addressTable = new JTable(addressTableModel);
        addressTable.setRowHeight(36);
        addressTable.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines: true; selectionBackground: #FEF3C7; selectionForeground: #78350F");
        addressTable.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font: bold 12; background: #FDFBF0");
        addressTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        addressTable.getColumnModel().getColumn(1).setPreferredWidth(380);
        addressTable.getColumnModel().getColumn(2).setPreferredWidth(160);

        JScrollPane scroll = new JScrollPane(addressTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 225, 210)));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // --- PANEL 3: HISTORIAL DE TRANSACCIONES ---

    private JPanel crearPanelTransacciones() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("Historial de Transacciones de la Billetera Dogecoin (En Mempool & Confirmadas)");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #786D55");
        header.add(lblTitle, BorderLayout.WEST);

        JButton btnRefrescarTxs = new JButton("🔄 REFRESCAR TRANSACCIONES");
        btnRefrescarTxs.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11; background: #FEF3C7; foreground: #92400E");
        btnRefrescarTxs.addActionListener(e -> refrescarTransaccionesEnSegundoPlano());
        header.add(btnRefrescarTxs, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);

        String[] cols = {"TIPO", "MONTO DOGE", "ESTADO / CONFIRMACIONES", "FECHA", "TX HASH (ID)"};
        txTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        txTable = new JTable(txTableModel);
        txTable.setRowHeight(36);
        txTable.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines: true; selectionBackground: #FEF3C7; selectionForeground: #78350F");
        txTable.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font: bold 12; background: #FDFBF0");

        txTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        txTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        txTable.getColumnModel().getColumn(2).setPreferredWidth(210);
        txTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        txTable.getColumnModel().getColumn(4).setPreferredWidth(220);

        txTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                if ("ENTRANTE".equals(value)) {
                    l.setForeground(new Color(22, 163, 74));
                } else if ("SALIENTE".equals(value)) {
                    l.setForeground(new Color(220, 38, 38));
                }
                return l;
            }
        });

        JScrollPane scroll = new JScrollPane(txTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 225, 210)));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // --- PANEL 4: BÓVEDA DE SEMILLAS EN LA NUBE ---

    private JPanel crearPanelBovedaSemillas() {
        JPanel p = new JPanel(new BorderLayout(0, 15));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 4));
        info.setOpaque(false);
        JLabel title = new JLabel("BÓVEDA DE SEMILLAS DOGECOIN (NUBE FIREBASE & BD LOCAL)");
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold 15; foreground: #2D250E");
        JLabel desc = new JLabel("Frases semilla Dogecoin protegidas con AES-256-GCM y sincronizadas en la Nube Firebase.");
        desc.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #786D55");
        info.add(title);
        info.add(desc);
        top.add(info, BorderLayout.CENTER);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tools.setOpaque(false);

        JButton btnGuardarActual = new JButton("💾 GUARDAR EN LA NUBE");
        btnGuardarActual.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #D97706; foreground: #FFFFFF; font: bold 12");
        btnGuardarActual.addActionListener(e -> guardarSemillaActualEnNubeFlujo());
        tools.add(btnGuardarActual);

        JButton btnGuardarManual = new JButton("➕ GUARDAR OTRA SEMILLA");
        btnGuardarManual.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #FFFFFF; foreground: #334155; font: bold 12; border: 1,1,1,1,#CBD5E1");
        btnGuardarManual.addActionListener(e -> guardarSemillaManualEnNubeFlujo());
        tools.add(btnGuardarManual);

        JButton btnRefrescar = new JButton("🔄 ACTUALIZAR");
        btnRefrescar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F1F5F9; font: bold 12");
        btnRefrescar.addActionListener(e -> cargarTablaSemillas());
        tools.add(btnRefrescar);

        top.add(tools, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        String[] cols = {"CÓDIGO WALLET", "RED", "DIRECCIÓN DOGECOIN", "ESTADO CIFRADO", "FECHA REGISTRO"};
        seedTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        tblSeeds = new JTable(seedTableModel);
        tblSeeds.setRowHeight(34);
        tblSeeds.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines: true; selectionBackground: #FEF3C7; selectionForeground: #78350F");
        tblSeeds.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font: bold 12; background: #FDFBF0");
        tblSeeds.getColumnModel().getColumn(0).setPreferredWidth(160);
        tblSeeds.getColumnModel().getColumn(1).setPreferredWidth(90);
        tblSeeds.getColumnModel().getColumn(2).setPreferredWidth(320);
        tblSeeds.getColumnModel().getColumn(3).setPreferredWidth(170);
        tblSeeds.getColumnModel().getColumn(4).setPreferredWidth(140);

        JScrollPane scroll = new JScrollPane(tblSeeds);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 225, 210)));
        p.add(scroll, BorderLayout.CENTER);

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
        currentSeedList = LocalDatabaseService.obtenerSemillasDogeWallet(userEmail);
        for (DogeSeedEntry se : currentSeedList) {
            seedTableModel.addRow(new Object[]{
                    se.codigoWallet,
                    se.red,
                    (se.direccion != null && !se.direccion.isEmpty()) ? se.direccion : "(No registrada)",
                    "🔒 AES-256 GCM (Nube Firebase)",
                    se.getFormattedDate()
            });
        }
    }

    // --- PANEL 5: SEGURIDAD ---

    private JPanel crearPanelSeguridad() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        int r = 0;

        JLabel title = new JLabel("Operaciones Críticas de Seguridad de la Billetera Dogecoin");
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold 16; foreground: #1E293B");
        gbc.insets = new Insets(0, 0, 8, 0); gbc.gridy = r++;
        panel.add(title, gbc);

        JLabel desc = new JLabel("<html>Aquí puede respaldar su frase semilla mnemónica o realizar la destrucción permanente y segura de la billetera activa de la memoria y la base de datos.</html>");
        desc.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #64748B");
        gbc.insets = new Insets(0, 0, 25, 0); gbc.gridy = r++;
        panel.add(desc, gbc);

        JButton btnVerSemillaActual = new JButton("👁️ REVELAR FRASE SEMILLA MNEMÓNICA (12 PALABRAS)");
        btnVerSemillaActual.setPreferredSize(new Dimension(380, 44));
        btnVerSemillaActual.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FEF3C7; foreground: #92400E; font: bold 13");
        btnVerSemillaActual.addActionListener(e -> revelarSemillaActualFlujo());
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = r++;
        panel.add(btnVerSemillaActual, gbc);

        JButton btnGuardarNube = new JButton("💾 GUARDAR SEMILLA EN LA NUBE (CON CÓDIGO)");
        btnGuardarNube.setPreferredSize(new Dimension(380, 44));
        btnGuardarNube.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FEF9C3; foreground: #854D0E; font: bold 13");
        btnGuardarNube.addActionListener(e -> guardarSemillaActualEnNubeFlujo());
        gbc.insets = new Insets(0, 0, 25, 0); gbc.gridy = r++;
        panel.add(btnGuardarNube, gbc);

        JSeparator sep = new JSeparator();
        gbc.insets = new Insets(0, 0, 25, 0); gbc.gridy = r++;
        panel.add(sep, gbc);

        JLabel lblWarn = new JLabel("Zona de Peligro: Destrucción de Llaves");
        lblWarn.putClientProperty(FlatClientProperties.STYLE, "font: bold 13; foreground: #DC2626");
        gbc.insets = new Insets(0, 0, 8, 0); gbc.gridy = r++;
        panel.add(lblWarn, gbc);

        JButton btnEliminar = new JButton("🗑️ DESTRUIR Y ELIMINAR BILLETERA DOGECOIN PERMANENTEMENTE");
        btnEliminar.setPreferredSize(new Dimension(380, 44));
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #DC2626; foreground: #FFFFFF; font: bold 13");
        btnEliminar.addActionListener(e -> eliminarBilleteraFlujo());
        gbc.insets = new Insets(0, 0, 0, 0); gbc.gridy = r++;
        panel.add(btnEliminar, gbc);

        return panel;
    }

    // --- ACCIONES Y FLUJOS ---

    private void crearNuevaBilleteraFlujo() {
        JPasswordField pfPin = new JPasswordField(4);
        pfPin.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true; font: 14");
        Object[] msg = {
                "Defina su PIN de autorización de 4 dígitos para cifrar la nueva billetera Dogecoin:",
                pfPin
        };
        int opt = JOptionPane.showConfirmDialog(this, msg, "Crear Billetera Dogecoin", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION) return;

        char[] pin = pfPin.getPassword();
        char[] pinCheck = Arrays.copyOf(pin, pin.length);
        if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pinCheck)) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto. Operación cancelada.", "Error", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            return;
        }

        try {
            DogeWalletHolder nueva = DogecoinWalletService.crearBilletera(isTestnet);
            DogecoinWalletService.guardarBilleteraCifrada(nueva, userEmail, pin, isTestnet);
            this.currentWallet = nueva;

            mostrarVentanaPalabrasSemilla(nueva.getMnemonic(), "NUEVA BILLETERA DOGECOIN CREADA CON ÉXITO", () -> {
                renderizarPantallaDashboard();
                ToastNotification.show(this, "Billetera Dogecoin creada y asegurada.", ToastNotification.Type.SUCCESS);
                AuditService.log(userEmail, "DOGE_WALLET_CREATED", "Nueva billetera Dogecoin generada (Testnet: " + isTestnet + ").");
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al crear billetera: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(pin, '0');
        }
    }

    private void restaurarBilleteraFlujo() {
        JTextField txtPalabras = new JTextField();
        txtPalabras.putClientProperty(FlatClientProperties.STYLE, "font: 13; background: #F8F9FA");
        JPasswordField pfPin = new JPasswordField(4);
        pfPin.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true; font: 14");

        Object[] msg = {
                "Ingrese sus 12 o 24 palabras mnemónicas separadas por un espacio:",
                txtPalabras,
                "Ingrese su PIN de autorización para cifrar la billetera en su cuenta:",
                pfPin
        };
        int opt = JOptionPane.showConfirmDialog(this, msg, "Restaurar Billetera Dogecoin", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION) return;

        String raw = txtPalabras.getText().trim();
        if (raw.isEmpty()) return;
        List<String> palabras = Arrays.asList(raw.split("\\s+"));

        char[] pin = pfPin.getPassword();
        char[] pinCheck = Arrays.copyOf(pin, pin.length);
        if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pinCheck)) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            return;
        }

        try {
            DogeWalletHolder rest = DogecoinWalletService.restaurarBilletera(palabras, isTestnet);
            DogecoinWalletService.guardarBilleteraCifrada(rest, userEmail, pin, isTestnet);
            this.currentWallet = rest;
            renderizarPantallaDashboard();
            ToastNotification.show(this, "Billetera Dogecoin restaurada con éxito.", ToastNotification.Type.SUCCESS);
            AuditService.log(userEmail, "DOGE_WALLET_RESTORED", "Billetera Dogecoin restaurada desde semilla.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al restaurar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(pin, '0');
        }
    }

    private void generarNuevaDireccionFlujo() {
        if (currentWallet == null) return;
        try {
            String newAddr = DogecoinWalletService.generarNuevaDireccion(currentWallet);
            actualizarDireccionYQr(newAddr);
            ToastNotification.show(this, "Nueva dirección generada: " + newAddr, ToastNotification.Type.SUCCESS);
        } catch (Exception ex) {
            ToastNotification.show(this, "Error al generar dirección: " + ex.getMessage(), ToastNotification.Type.ERROR);
        }
    }

    private void agregarDireccionEtiquetadaFlujo() {
        JTextField txtTitulo = new JTextField();
        txtTitulo.putClientProperty(FlatClientProperties.STYLE, "font: 13");
        JTextField txtAddr = new JTextField();
        txtAddr.putClientProperty(FlatClientProperties.STYLE, "font: 13");

        if (currentWallet != null) {
            txtAddr.setText(currentWallet.currentAddress);
        }

        Object[] msg = {
                "Título / Identificador (ej: 'Pago de Amigo', 'Ahorros Doge'):",
                txtTitulo,
                "Dirección Dogecoin (dejar actual o ingresar externa):",
                txtAddr
        };
        int opt = JOptionPane.showConfirmDialog(this, msg, "Guardar Dirección Etiquetada", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION) return;

        String addr = txtAddr.getText().trim();
        String titulo = txtTitulo.getText().trim();
        if (addr.isEmpty()) return;

        DogecoinWalletService.guardarDireccionEtiquetada(userEmail, isTestnet, addr, titulo);
        cargarTablaDirecciones();
        ToastNotification.show(this, "Dirección guardada en la libreta.", ToastNotification.Type.SUCCESS);
    }

    private void copiarDireccionSeleccionada() {
        int r = addressTable.getSelectedRow();
        if (r < 0) {
            ToastNotification.show(this, "Seleccione una fila primero.", ToastNotification.Type.WARNING);
            return;
        }
        String addr = (String) addressTableModel.getValueAt(r, 1);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(addr), null);
        ToastNotification.show(this, "Copiada al portapapeles.", ToastNotification.Type.SUCCESS);
    }

    private void eliminarDireccionSeleccionada() {
        int r = addressTable.getSelectedRow();
        if (r < 0) {
            ToastNotification.show(this, "Seleccione una fila primero.", ToastNotification.Type.WARNING);
            return;
        }
        String addr = (String) addressTableModel.getValueAt(r, 1);
        DogecoinWalletService.eliminarDireccionEtiquetada(userEmail, isTestnet, addr);
        cargarTablaDirecciones();
        ToastNotification.show(this, "Dirección eliminada de la libreta.", ToastNotification.Type.SUCCESS);
    }

    private void revelarSemillaActualFlujo() {
        if (currentWallet == null) return;
        JPasswordField pfPin = new JPasswordField(4);
        int opt = JOptionPane.showConfirmDialog(this, new Object[]{"Ingrese su PIN para autorizar la revelación:", pfPin}, "Autorización", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION) return;
        char[] pin = pfPin.getPassword();
        if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pin)) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            return;
        }
        Arrays.fill(pin, '0');
        mostrarVentanaPalabrasSemilla(currentWallet.getMnemonic(), "SEMILLA DE LA BILLETERA DOGECOIN ACTIVA", null);
        AuditService.log(userEmail, "DOGE_SEED_REVEAL", "Semilla Dogecoin revelada por usuario.");
    }

    private void guardarSemillaActualEnNubeFlujo() {
        if (currentWallet == null) {
            ToastNotification.show(this, "No hay una billetera activa actualmente.", ToastNotification.Type.WARNING);
            return;
        }

        List<String> palabras = currentWallet.getMnemonic();
        String dir = currentWallet.currentAddress;

        JTextField txtCodigo = new JTextField("DOGE-" + (isTestnet ? "TESTNET" : "MAINNET") + "-01");
        txtCodigo.putClientProperty(FlatClientProperties.STYLE, "font: 13; background: #F8F9FA");
        JPasswordField pfPin = new JPasswordField(4);

        Object[] msg = {
                "Asigne un Código / Nombre para identificar esta billetera Dogecoin en la nube (Firebase):",
                txtCodigo,
                "Ingrese su PIN para autorizar y cifrar la semilla en Firebase Firestore:",
                pfPin
        };

        int opt = JOptionPane.showConfirmDialog(this, msg, "Guardar Semilla Doge en la Nube (Firebase)", JOptionPane.OK_CANCEL_OPTION);
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
            boolean guardado = LocalDatabaseService.guardarSemillaDogeWallet(
                    userEmail, codigo, isTestnet ? "TESTNET" : "MAINNET", dir, frase, pin
            );
            if (guardado) {
                ToastNotification.show(this, "Semilla Doge sincronizada en Firebase con código: " + codigo.toUpperCase(), ToastNotification.Type.SUCCESS);
                AuditService.log(userEmail, "DOGE_SEED_SAVED_CLOUD", "Semilla de billetera Doge (" + codigo + ") guardada en Firebase.");
                cargarTablaSemillas();
            } else {
                ToastNotification.show(this, "No se pudo guardar la semilla Doge en la nube.", ToastNotification.Type.ERROR);
            }
        } finally {
            Arrays.fill(pin, '0');
        }
    }

    private void guardarSemillaManualEnNubeFlujo() {
        JTextField txtCodigo = new JTextField();
        txtCodigo.putClientProperty(FlatClientProperties.STYLE, "font: 13; background: #F8F9FA");
        JComboBox<String> cmbRed = new JComboBox<>(new String[]{"MAINNET", "TESTNET"});
        JTextField txtPalabras = new JTextField();
        txtPalabras.putClientProperty(FlatClientProperties.STYLE, "font: 13; background: #F8F9FA");
        JPasswordField pfPin = new JPasswordField(4);

        Object[] msg = {
                "Código / Identificador de la Billetera Doge (ej: DOGE-FONDOS):",
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
            DogeWalletHolder tempWallet = DogecoinWalletService.restaurarBilletera(words, isRedTestnet);
            dir = tempWallet.currentAddress;
        } catch (Exception ignored) {}

        try {
            boolean guardado = LocalDatabaseService.guardarSemillaDogeWallet(
                    userEmail, codigo, redSelec, dir, palabrasRaw, pin
            );
            if (guardado) {
                ToastNotification.show(this, "Semilla guardada y sincronizada en Firebase con código: " + codigo.toUpperCase(), ToastNotification.Type.SUCCESS);
                AuditService.log(userEmail, "DOGE_SEED_SAVED_CLOUD", "Semilla externa guardada en Firebase (" + codigo + ")");
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
        DogeSeedEntry entry = currentSeedList.get(idx);

        JPasswordField pfPin = new JPasswordField(4);
        int opc = JOptionPane.showConfirmDialog(this,
                new Object[]{"Ingrese su PIN para autorizar la revelación:", pfPin},
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
            String frase = LocalDatabaseService.descifrarSemillaDogeWallet(entry, pin);
            List<String> palabras = Arrays.asList(frase.trim().split("\\s+"));
            mostrarVentanaPalabrasSemilla(palabras, "SEMILLA DESCIFRADA: " + entry.codigoWallet + " (" + entry.red + ")", null);
            AuditService.log(userEmail, "DOGE_SEED_REVEAL_DB", "Semilla (" + entry.codigoWallet + ") revelada desde BD.");
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
        DogeSeedEntry entry = currentSeedList.get(idx);

        boolean entryIsTestnet = "TESTNET".equalsIgnoreCase(entry.red);

        int conf = JOptionPane.showConfirmDialog(this,
                "¿Desea restaurar y establecer la billetera [" + entry.codigoWallet + "] (" + entry.red + ") como la billetera activa?\n",
                "Restaurar Billetera Doge desde Bóveda",
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
            String frase = LocalDatabaseService.descifrarSemillaDogeWallet(entry, pin);
            List<String> palabras = Arrays.asList(frase.trim().split("\\s+"));
            DogeWalletHolder restored = DogecoinWalletService.restaurarBilletera(palabras, entryIsTestnet);

            this.isTestnet = entryIsTestnet;
            if (cbNetworkSelector != null) {
                cbNetworkSelector.setSelectedIndex(entryIsTestnet ? 1 : 0);
            }
            DogecoinWalletService.guardarBilleteraCifrada(restored, userEmail, pin, entryIsTestnet);
            this.currentWallet = restored;
            renderizarPantallaDashboard();
            ToastNotification.show(this, "Billetera [" + entry.codigoWallet + "] restaurada como activa con éxito.", ToastNotification.Type.SUCCESS);
            AuditService.log(userEmail, "DOGE_WALLET_RESTORED_DB", "Billetera restaurada desde semilla (" + entry.codigoWallet + ").");
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
        DogeSeedEntry entry = currentSeedList.get(idx);

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
            boolean ok = LocalDatabaseService.eliminarSemillaDogeWallet(entry.id);
            if (ok) {
                ToastNotification.show(this, "Semilla eliminada de la nube y la base de datos.", ToastNotification.Type.SUCCESS);
                AuditService.log(userEmail, "DOGE_SEED_DELETED_CLOUD", "Semilla (" + entry.codigoWallet + ") eliminada de Firebase y BD.");
                cargarTablaSemillas();
            } else {
                ToastNotification.show(this, "No se pudo eliminar la semilla.", ToastNotification.Type.ERROR);
            }
        } finally {
            Arrays.fill(pin, '0');
        }
    }

    private void eliminarBilleteraFlujo() {
        int conf = JOptionPane.showConfirmDialog(this,
                "⚠️ ADVERTENCIA CRÍTICA: ¿Está seguro de eliminar y destruir esta billetera Dogecoin?\n\n" +
                        "Se eliminarán permanentemente las llaves de Firebase y de la base de datos local.\n" +
                        "¿Aseguró su frase semilla antes de continuar?",
                "Destrucción Segura de Billetera Dogecoin",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;

        JPasswordField pfPin = new JPasswordField(4);
        int opt = JOptionPane.showConfirmDialog(this, new Object[]{"Ingrese su PIN para autorizar la destrucción:", pfPin}, "Confirmación", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION) return;

        char[] pin = pfPin.getPassword();
        if (SecuritySession.hasUserPin() && !SecuritySession.verifyUserPin(pin)) {
            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pin, '0');
            return;
        }
        Arrays.fill(pin, '0');

        DogecoinWalletService.eliminarBilletera(userEmail, isTestnet);
        this.currentWallet = null;
        renderizarPantallaBienvenida();
        ToastNotification.show(this, "Billetera Dogecoin eliminada con éxito.", ToastNotification.Type.SUCCESS);
        AuditService.log(userEmail, "DOGE_WALLET_DESTROYED", "Billetera Dogecoin destruida de forma segura.");
    }

    private void mostrarVentanaPalabrasSemilla(List<String> palabras, String titulo, Runnable onFinish) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), titulo, JDialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());
        dlg.setSize(580, 480);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new BorderLayout(0, 15));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel lblWarn = new JLabel("<html><b>⚠️ GUARDE ESTAS PALABRAS EN UN LUGAR SEGURO.</b><br/>Son la única forma de recuperar sus fondos de Dogecoin. Nadie puede recuperarlas por usted.</html>");
        lblWarn.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #92400E; background: #FEF3C7; border: 10,12,10,12, #F59E0B, 1, 10");
        p.add(lblWarn, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 3, 10, 10));
        grid.setOpaque(false);
        for (int i = 0; i < palabras.size(); i++) {
            JLabel box = new JLabel((i + 1) + ". " + palabras.get(i), SwingConstants.CENTER);
            box.putClientProperty(FlatClientProperties.STYLE, "font: bold 14; background: #FFFBEB; foreground: #78350F; border: 8,8,8,8, #FDE68A, 1, 8");
            grid.add(box);
        }
        p.add(grid, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);

        JButton btnCopy = new JButton("📋 COPIAR");
        btnCopy.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 12");
        btnCopy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(String.join(" ", palabras)), null);
            ToastNotification.show(dlg, "Palabras copiadas al portapapeles.", ToastNotification.Type.SUCCESS);
        });
        bottom.add(btnCopy);

        JButton btnClose = new JButton("HE RESPALDADO MI SEMILLA");
        btnClose.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #D97706; foreground: #FFFFFF; font: bold 12");
        btnClose.addActionListener(e -> {
            dlg.dispose();
            if (onFinish != null) onFinish.run();
        });
        bottom.add(btnClose);

        p.add(bottom, BorderLayout.SOUTH);
        dlg.add(p);
        dlg.setVisible(true);
    }

    private void mostrarDialogoBovedaSemillas() {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Bóveda de Semillas Dogecoin (Nube Firebase & BD)", JDialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());
        dlg.setSize(880, 520);
        dlg.setLocationRelativeTo(this);
        dlg.add(crearPanelBovedaSemillas(), BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    private void actualizarDireccionYQr(String address) {
        if (txtMainAddress != null) {
            txtMainAddress.setText(address);
        }
        if (lblMainQr != null) {
            try {
                BufferedImage qr = QRCodeService.generateQRCode("dogecoin:" + address, 160, 160);
                lblMainQr.setIcon(new ImageIcon(qr));
            } catch (Exception ex) {
                lblMainQr.setText("QR No Disponible");
            }
        }
    }

    private void refrescarDatosEnSegundoPlano() {
        if (currentWallet == null) return;
        String addr = currentWallet.currentAddress;
        actualizarDireccionYQr(addr);
        cargarTablaDirecciones();
        refrescarBalanceEnSegundoPlano();
        refrescarTransaccionesEnSegundoPlano();
    }

    private void refrescarBalanceEnSegundoPlano() {
        if (currentWallet == null || lblBalanceDoge == null) return;
        String addr = currentWallet.currentAddress;
        lblBalanceDoge.setText("Consultando...");

        SwingWorker<Long, Void> worker = new SwingWorker<>() {
            @Override
            protected Long doInBackground() {
                return DogecoinWalletService.consultarBalanceKoinus(addr, isTestnet);
            }

            @Override
            protected void done() {
                try {
                    long koinus = get();
                    if (koinus >= 0) {
                        lblBalanceDoge.setText(DogecoinWalletService.koinusToDoge(koinus));
                        lblBalanceKoinus.setText("≈ " + String.format("%,d", koinus) + " Koinus");
                    } else {
                        lblBalanceDoge.setText("0.0000 DOGE");
                        lblBalanceKoinus.setText("Sin conexión de red directa a explorer");
                    }
                } catch (Exception ignored) {
                    lblBalanceDoge.setText("0.0000 DOGE");
                }
            }
        };
        worker.execute();
    }

    private void refrescarTransaccionesEnSegundoPlano() {
        if (currentWallet == null || txTableModel == null) return;
        String addr = currentWallet.currentAddress;

        SwingWorker<List<DogeTxInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<DogeTxInfo> doInBackground() {
                return DogecoinWalletService.consultarTransacciones(addr, isTestnet);
            }

            @Override
            protected void done() {
                try {
                    List<DogeTxInfo> txs = get();
                    txTableModel.setRowCount(0);
                    for (DogeTxInfo tx : txs) {
                        txTableModel.addRow(new Object[]{
                                tx.isIncoming ? "ENTRANTE" : "SALIENTE",
                                tx.getDogeAmount(),
                                tx.getStatusDescription(),
                                tx.getFormattedDate(),
                                tx.txid
                        });
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void cargarTablaDirecciones() {
        if (addressTableModel == null) return;
        addressTableModel.setRowCount(0);
        List<DogeAddressEntry> list = DogecoinWalletService.cargarDireccionesEtiquetadas(userEmail, isTestnet);
        for (DogeAddressEntry e : list) {
            addressTableModel.addRow(new Object[]{e.label, e.address, e.date});
        }
    }
}
