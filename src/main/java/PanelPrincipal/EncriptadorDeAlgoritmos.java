package PanelPrincipal;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Hex;
import javax.swing.JInternalFrame; // Cambiado de JFrame a JInternalFrame

/**

 * @author Josue Daniel Roldan Ochoa
 */

public class EncriptadorDeAlgoritmos extends JInternalFrame { // Cambiado de JFrame a JInternalFrame
    public EncriptadorDeAlgoritmos() {
        initComponents();
        this.setResizable(false);
        // this.setLocationRelativeTo(null); // Eliminado, ya que JInternalFrame se gestiona en un JDesktopPane
        this.setTitle("ENCRIPTADOR DE ALGORITMOS"); // Establecer título aquí
    }

    public void encriptacion(String texto) throws UnsupportedEncodingException{
    try{
    MessageDigest md = null;
    
       
            if(cbalgoritmo.getSelectedItem().toString().equals("SHA-512")){
                try {
            txencripcion.setText("");
            //SHA-512
            String encripcion;
            md= MessageDigest.getInstance("SHA-512");
            md.update(texto.getBytes());
            byte[] mb = md.digest();
          //  System.out.println((Hex.encodeHex(mb)));
            char [] dds=(Hex.encodeHex(mb));
            String s = new String(dds);
           
            txencripcion.setText(s);
          //  System.out.println((Arrays.toString(Hex.encodeHex(mb))));
            } catch (NoSuchAlgorithmException e) {
            //Error
        } 
            
            } else if(cbalgoritmo.getSelectedItem().toString().equals("SHA-1")){
                try{
            //SHA-1
            String encripcion;
            md= MessageDigest.getInstance("SHA-1");
            md.update(texto.getBytes());
            byte[]  mb = md.digest();
            char [] dds=(Hex.encodeHex(mb));
            String s = new String(dds);
            txencripcion.setText(s);
            } catch (NoSuchAlgorithmException e) {
            //Error
        }
            }else if(cbalgoritmo.getSelectedItem().toString().equals("MD5")){
                try{
            //MD5
            String encripcion;
            md= MessageDigest.getInstance("MD5");
            md.update(texto.getBytes());
            byte[]  mb = md.digest();
            char [] dds=(Hex.encodeHex(mb));
            String s = new String(dds);
            txencripcion.setText(s);
            } catch (NoSuchAlgorithmException e) {
            //Error
        }
                
            } else if(cbalgoritmo.getSelectedItem().toString().equals("SHA-256")){
                try{
            //MD5
            String encripcion;
            md= MessageDigest.getInstance("SHA-256");
           
            md.update(texto.getBytes("UTF-8")); // Change this to "UTF-16" if needed
            byte[] mb = md.digest();
            char [] dds=(Hex.encodeHex(mb));
            String s = new String(dds);
            txencripcion.setText(s);
            } catch (NoSuchAlgorithmException e) {
            //Error
        }
                
            } 
    }catch(Exception e){
    
    }
      //      MessageDigest md = MessageDigest.getInstance("SHA-256");

    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        txtexto = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txencripcion = new javax.swing.JTextField();
        cbalgoritmo = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE); // Cambiado a DISPOSE_ON_CLOSE
        // setTitle("ENCRIPTADOR DE ALGORITMOS"); // Movido al constructor

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));
        jPanel1.setPreferredSize(new java.awt.Dimension(617, 191));

        txtexto.setFont(new java.awt.Font("Lucida Sans", 0, 10)); // NOI18N
        txtexto.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel1.setFont(new java.awt.Font("Lucida Sans", 0, 10)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("TEXTO:");

        jLabel2.setFont(new java.awt.Font("Lucida Sans", 0, 10)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("ENCRIPTACION:");

        txencripcion.setEditable(false);
        txencripcion.setFont(new java.awt.Font("Lucida Sans", 0, 11)); // NOI18N
        txencripcion.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        cbalgoritmo.setFont(new java.awt.Font("Lucida Handwriting", 0, 10)); // NOI18N
        cbalgoritmo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "MD5", "SHA-1", "SHA-512", "SHA-256" }));
        cbalgoritmo.setSelectedItem(null);

        jLabel3.setFont(new java.awt.Font("Lucida Sans", 0, 10)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("ALGORITMO:");

        jButton1.setFont(new java.awt.Font("Lucida Handwriting", 0, 10)); // NOI18N
        jButton1.setText("Encriptar");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Lucida Handwriting", 0, 10)); // NOI18N
        jButton2.setText("Cancelar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txencripcion))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbalgoritmo, 0, 519, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtexto))
                    .addComponent(jButton2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbalgoritmo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtexto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txencripcion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            encriptacion(txtexto.getText());        // TODO add your handling code here:
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(EncriptadorDeAlgoritmos.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
    txencripcion.setText("");
    txtexto.setText("");// TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> cbalgoritmo;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField txencripcion;
    private javax.swing.JTextField txtexto;
    // End of variables declaration//GEN-END:variables
}
