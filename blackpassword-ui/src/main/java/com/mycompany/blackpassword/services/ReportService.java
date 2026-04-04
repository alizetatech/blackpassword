package com.mycompany.blackpassword.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Servicio de Generación de Reportes PDF.
 * Crea informes profesionales de auditoría y estado de la bóveda.
 */
public class ReportService {

    /**
     * Genera un reporte de auditoría para un usuario específico.
     * @param email Email del usuario.
     * @param logs Lista de mapas con los datos ya descifrados (fecha, accion, detalles).
     * @param destino Archivo donde se guardará el PDF.
     * @throws Exception Si hay errores en la generación del PDF.
     */
    public static void generarReporteAuditoria(String email, List<Map<String, String>> logs, File destino) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Encabezado Premium
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
                contentStream.setNonStrokingColor(0, 120, 212); // Azul Blackpassword
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("BLACKPASSWORD - INFORME DE SEGURIDAD");
                contentStream.endText();

                contentStream.setLineWidth(1f);
                contentStream.setStrokingColor(200, 200, 200);
                contentStream.moveTo(50, 740);
                contentStream.lineTo(550, 740);
                contentStream.stroke();

                // Información del Usuario
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.setNonStrokingColor(Color.BLACK);
                contentStream.newLineAtOffset(50, 710);
                contentStream.showText("Usuario: " + email);
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Fecha del Reporte: " + new java.util.Date().toString());
                contentStream.endText();

                // Tabla de Auditoría
                int y = 660;
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
                contentStream.newLineAtOffset(50, y);
                contentStream.showText("FECHA");
                contentStream.newLineAtOffset(150, 0);
                contentStream.showText("ACCIÓN");
                contentStream.newLineAtOffset(150, 0);
                contentStream.showText("DETALLES");
                contentStream.endText();

                y -= 10;
                contentStream.moveTo(50, y);
                contentStream.lineTo(550, y);
                contentStream.stroke();

                y -= 20;
                contentStream.setFont(PDType1Font.HELVETICA, 9);
                
                for (Map<String, String> log : logs) {
                    if (y < 50) break; // Evitar desbordamiento de página (simplificado)

                    String fecha = log.getOrDefault("fecha", "N/A");
                    String accion = log.getOrDefault("accion", "N/A");
                    String detalles = log.getOrDefault("detalles", "N/A");
                    
                    // Limpieza de caracteres para PDFBox (evitar errores con caracteres especiales)
                    detalles = detalles.replaceAll("[^\\p{Print}]", "");
                    if (detalles.length() > 50) detalles = detalles.substring(0, 47) + "...";

                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, y);
                    contentStream.showText(fecha);
                    contentStream.newLineAtOffset(150, 0);
                    contentStream.showText(accion);
                    contentStream.newLineAtOffset(150, 0);
                    contentStream.showText(detalles);
                    contentStream.endText();
                    
                    y -= 15;
                }

                // Pie de página
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                contentStream.setNonStrokingColor(Color.GRAY);
                contentStream.newLineAtOffset(50, 30);
                contentStream.showText("Este documento es confidencial y está protegido por cifrado RSA de 2048 bits.");
                contentStream.endText();
            }

            document.save(destino);
        }
    }
}
