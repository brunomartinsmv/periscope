package br.ufmt.periscope.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PDFTextParser {

    String pdfToText(String fileName) throws IOException {
        File file = new File(fileName);
        try (PDDocument pdDoc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(pdDoc);
        }
    }

    void writeTextToFile(String pdfText, String fileName) {

        try {
            PrintWriter pw = new PrintWriter(fileName);
            pw.print(pdfText);
            pw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PDFTextParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
