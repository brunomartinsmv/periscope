package br.ufmt.periscope.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates PDFBox 3 text extraction used by {@link PDFTextParser}.
 */
class PDFTextParserTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsTextFromGeneratedPdf() throws IOException {
        Path pdf = tempDir.resolve("sample.pdf");
        writeSimplePdf(pdf, "Periscope patent abstract sample text");

        PDFTextParser parser = new PDFTextParser();
        String text = parser.pdfToText(pdf.toString());

        assertThat(text).contains("Periscope patent abstract sample text");
    }

    @Test
    void writeTextToFilePersistsExtractedContent() throws IOException {
        Path pdf = tempDir.resolve("sample2.pdf");
        Path out = tempDir.resolve("out.txt");
        writeSimplePdf(pdf, "UFMT Periscope PDFBox");

        PDFTextParser parser = new PDFTextParser();
        String extracted = parser.pdfToText(pdf.toString());
        parser.writeTextToFile(extracted, out.toString());

        String written = Files.readString(out);
        assertThat(written).contains("UFMT Periscope PDFBox");
    }

    private static void writeSimplePdf(Path target, String content) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(content);
                stream.endText();
            }
            document.save(target.toFile());
        }
    }
}
