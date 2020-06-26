package io.sked.docs.core.util.format;

import com.google.common.base.Charsets;
import com.google.common.io.Closer;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import io.sked.docs.core.model.context.AppContext;
import io.sked.util.mime.MimeType;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Text plain format handler.
 *
 * @author bgamard
 */
public class TextPlainFormatHandler implements FormatHandler {
    @Override
    public boolean accept(String mimeType) {
        return mimeType.equals(MimeType.TEXT_CSV) || mimeType.equals(MimeType.TEXT_PLAIN);
    }

    @Override
    public BufferedImage generateThumbnail(Path file) throws Exception {
        Document output = new Document(PageSize.A4, 40, 40, 40, 40);
        Path tempFile = AppContext.getInstance().getFileService().createTemporaryFile();
        OutputStream pdfOutputStream = Files.newOutputStream(tempFile);
        PdfWriter.getInstance(output, pdfOutputStream);

        output.open();
        String content = new String(Files.readAllBytes(file), Charsets.UTF_8);
        Font font = FontFactory.getFont("LiberationMono-Regular");
        Paragraph paragraph = new Paragraph(content, font);
        paragraph.setAlignment(Element.ALIGN_LEFT);
        output.add(paragraph);
        output.close();

        // Use the PDF format handler
        return new PdfFormatHandler().generateThumbnail(tempFile);
    }

    @Override
    public String extractContent(String language, Path file) throws Exception {
        return new String(Files.readAllBytes(file), "UTF-8");
    }

    @Override
    public void appendToPdf(Path file, PDDocument doc, boolean fitImageToPage, int margin, MemoryUsageSetting memUsageSettings, Closer closer) {
        // TODO Append the text file to the PDF
    }
}
