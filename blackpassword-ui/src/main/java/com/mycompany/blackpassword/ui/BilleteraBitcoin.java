package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.core.util.ToastNotification;
import com.mycompany.blackpassword.services.AuditService;
import com.mycompany.blackpassword.services.BitcoinWalletService;
import com.mycompany.blackpassword.services.BitcoinWalletService.BtcAddressEntry;
import com.mycompany.blackpassword.services.BitcoinWalletService.BtcTxInfo;
import com.mycompany.blackpassword.services.QRCodeService;
import com.mycompany.blackpassword.services.SecuritySession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
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

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnGroup.setOpaque(false);

        JButton btnCrear = new JButton("✨ CREAR NUEVA BILLETERA");
        btnCrear.setPreferredSize(new Dimension(240, 46));
        btnCrear.putClientProperty(FlatClientProperties.STYLE, "arc: 14; background: #0078D4; foreground: #FFFFFF; font: bold 13");
        btnCrear.addActionListener(e -> crearNuevaBilleteraFlujo());
        btnGroup.add(btnCrear);

        JButton btnRestaurar = new JButton("📥 RESTAURAR DESDE 12 PALABRAS");
        btnRestaurar.setPreferredSize(new Dimension(260, 46));
        btnRestaurar.putClientProperty(FlatClientProperties.STYLE, "arc: 14; background: #FFFFFF; foreground: #333333; font: bold 13; border: 1,1,1,1,#CCCCCC");
        btnRestaurar.addActionListener(e -> restaurarBilleteraFlujo());
        btnGroup.add(btnRestaurar);

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
        tabbedPane.addTab("⚙️ SEGURIDAD", crearPanelSeguridad());

        contentContainer.add(tabbedPane, BorderLayout.CENTER);
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

        JButton btnSemilla = new JButton("🔑 REVELAR FRASE SEMILLA (12 PALABRAS)");
        btnSemilla.setPreferredSize(new Dimension(320, 44));
        btnSemilla.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FFF4CE; foreground: #795B00; font: bold 13");
        btnSemilla.addActionListener(e -> revelarSemillaConPin());
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = r++;
        panel.add(btnSemilla, gbc);

        JButton btnEliminar = new JButton("🗑️ DESVINCULAR BILLETERA DE ESTA RED");
        btnEliminar.setPreferredSize(new Dimension(320, 44));
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FDE7E9; foreground: #A80000; font: bold 13");
        btnEliminar.addActionListener(e -> desvincularBilleteraConPin());
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
                    if (sats >= 0 && lblBalanceBtc != null && lblBalanceSats != null) {
                        lblBalanceBtc.setText(BitcoinWalletService.satsToBtc(sats));
                        lblBalanceSats.setText(String.format("%,d Satoshis", sats));
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
        if (!SecuritySession.verifyUserPin(pin)) {
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

    private void desvincularBilleteraConPin() {
        int conf = JOptionPane.showConfirmDialog(this,
                "⚠️ ¿Está completamente seguro de desvincular esta billetera (" + (isTestnet ? "Testnet" : "Mainnet") + ")?\n" +
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
            BitcoinWalletService.eliminarBilletera(userEmail, isTestnet);
            currentWallet = null;
            renderizarPantallaBienvenida();
            ToastNotification.show(this, "Billetera (" + (isTestnet ? "Testnet" : "Mainnet") + ") desvinculada exitosamente.", ToastNotification.Type.SUCCESS);
            AuditService.log(userEmail, "BTC_WALLET_DELETED", "Billetera eliminada por el usuario.");
        } else {
            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        Arrays.fill(pin, '0');
    }
}
