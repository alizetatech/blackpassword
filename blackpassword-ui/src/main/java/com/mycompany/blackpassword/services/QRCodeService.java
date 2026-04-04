package com.mycompany.blackpassword.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para la generación de códigos QR v1.0.
 * Utiliza la librería ZXing para transformar texto en imágenes escaneables.
 */
public class QRCodeService {

    /**
     * Genera una imagen de código QR a partir de un texto.
     * @param text El contenido del QR (ej: una contraseña).
     * @param width Ancho de la imagen.
     * @param height Alto de la imagen.
     * @return BufferedImage con el código QR.
     * @throws Exception Si ocurre un error en la codificación.
     */
    public static BufferedImage generateQRCode(String text, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // Alta corrección de errores
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix matrix = new MultiFormatWriter().encode(
                text, 
                BarcodeFormat.QR_CODE, 
                width, 
                height, 
                hints
        );

        return MatrixToImageWriter.toBufferedImage(matrix);
    }
}
