package com.arqly.backend.service;

import com.arqly.backend.entity.GeneratedDocument;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeneratedDocumentPdfService {
    private static final Color PRIMARY = new Color(0, 121, 107);
    private static final Color PRIMARY_DARK = new Color(0, 84, 78);
    private static final Color TEXT = new Color(15, 23, 42);
    private static final Color MUTED = new Color(82, 97, 116);
    private static final Color SOFT = new Color(242, 248, 247);
    private static final Color BORDER = new Color(219, 231, 228);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final Pattern INLINE = Pattern.compile("(\\*\\*[^*]+\\*\\*|\\*[^*]+\\*|\\[[^]]+]\\(https?://[^)]+\\))");

    private final DocumentGenerationService generationService;

    public GeneratedDocumentPdfService(DocumentGenerationService generationService) {
        this.generationService = generationService;
    }

    @Transactional(readOnly = true)
    public byte[] generate(UUID tenantId, UUID documentId) {
        var generated = generationService.find(tenantId, documentId);
        try {
            var output = new ByteArrayOutputStream();
            var document = new Document(PageSize.A4, 48, 48, 50, 48);
            var writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new FooterEvent());
            document.open();
            document.add(header(generated));
            document.add(spacer(18));
            addMarkdown(document, generated.getContent());
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível exportar o documento em PDF.", exception);
        }
    }

    private PdfPTable header(GeneratedDocument generated) {
        var table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1.6f, 1f });

        var title = new Paragraph();
        title.add(new Chunk(generated.getTenant().getTradeName() + "\n", font(10, Font.BOLD, PRIMARY)));
        title.add(new Chunk(generated.getTitle(), font(22, Font.BOLD, PRIMARY_DARK)));
        var titleCell = new PdfPCell(title);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(0);
        table.addCell(titleCell);

        var metadata = new PdfPTable(1);
        metadata.setWidthPercentage(100);
        metadata.addCell(metadataCell("Categoria", categoryLabel(generated.getTemplate().getCategory().name())));
        metadata.addCell(metadataCell("Versão", "v" + generated.getVersion()));
        metadata.addCell(metadataCell("Gerado em", DATE_TIME.format(generated.getGeneratedAt().atZone(ZoneId.systemDefault()))));
        var metadataCell = new PdfPCell(metadata);
        metadataCell.setBorder(Rectangle.NO_BORDER);
        metadataCell.setPadding(0);
        table.addCell(metadataCell);
        return table;
    }

    private void addMarkdown(Document document, String markdown) throws Exception {
        boolean listOpen = false;
        for (String rawLine : (markdown == null ? "" : markdown).split("\\R", -1)) {
            var line = rawLine.strip();
            if (line.isEmpty()) {
                if (listOpen) listOpen = false;
                document.add(spacer(4));
                continue;
            }
            if (line.startsWith("### ")) {
                document.add(text(line.substring(4), 13, Font.BOLD, PRIMARY_DARK, 12, 5));
            } else if (line.startsWith("## ")) {
                document.add(text(line.substring(3), 16, Font.BOLD, PRIMARY_DARK, 16, 6));
            } else if (line.startsWith("# ")) {
                document.add(text(line.substring(2), 20, Font.BOLD, TEXT, 18, 8));
            } else if (line.startsWith("- ")) {
                var paragraph = inline(line.substring(2), 10);
                paragraph.setIndentationLeft(14);
                paragraph.add(0, new Chunk("•  ", font(10, Font.BOLD, PRIMARY)));
                paragraph.setSpacingAfter(5);
                document.add(paragraph);
                listOpen = true;
            } else {
                var paragraph = inline(line, 10);
                paragraph.setLeading(15);
                paragraph.setSpacingAfter(7);
                document.add(paragraph);
                listOpen = false;
            }
        }
    }

    private Paragraph inline(String value, int size) {
        var paragraph = new Paragraph();
        var matcher = INLINE.matcher(value);
        int position = 0;
        while (matcher.find()) {
            if (matcher.start() > position) paragraph.add(new Chunk(value.substring(position, matcher.start()), font(size, Font.NORMAL, TEXT)));
            var token = matcher.group();
            if (token.startsWith("**")) {
                paragraph.add(new Chunk(token.substring(2, token.length() - 2), font(size, Font.BOLD, TEXT)));
            } else if (token.startsWith("*")) {
                paragraph.add(new Chunk(token.substring(1, token.length() - 1), font(size, Font.ITALIC, TEXT)));
            } else {
                int separator = token.indexOf("](");
                var link = new Chunk(token.substring(1, separator), font(size, Font.UNDERLINE, PRIMARY));
                link.setAnchor(token.substring(separator + 2, token.length() - 1));
                paragraph.add(link);
            }
            position = matcher.end();
        }
        if (position < value.length()) paragraph.add(new Chunk(value.substring(position), font(size, Font.NORMAL, TEXT)));
        return paragraph;
    }

    private Paragraph text(String value, int size, int style, Color color, float before, float after) {
        var paragraph = new Paragraph(value, font(size, style, color));
        paragraph.setSpacingBefore(before);
        paragraph.setSpacingAfter(after);
        return paragraph;
    }

    private PdfPCell metadataCell(String label, String value) {
        var paragraph = new Paragraph();
        paragraph.add(new Chunk(label + "\n", font(7, Font.BOLD, MUTED)));
        paragraph.add(new Chunk(value, font(9, Font.BOLD, TEXT)));
        var cell = new PdfPCell(paragraph);
        cell.setPadding(7);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(SOFT);
        return cell;
    }

    private Paragraph spacer(float spacing) {
        var paragraph = new Paragraph(" ");
        paragraph.setLeading(0);
        paragraph.setSpacingBefore(spacing);
        return paragraph;
    }

    private Font font(int size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    private String categoryLabel(String category) {
        return switch (category) {
            case "CONTRACT" -> "Contrato";
            case "PROPOSAL" -> "Proposta";
            case "MEMORIAL" -> "Memorial";
            case "DECLARATION" -> "Declaração";
            case "RECEIPT" -> "Recibo";
            case "REPORT" -> "Relatório";
            case "CHECKLIST" -> "Checklist";
            default -> "Outro";
        };
    }

    private static class FooterEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            var footer = new PdfPTable(2);
            try {
                footer.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                footer.setWidths(new float[] { 1.5f, 1f });
                footer.addCell(footerCell("Arqly · documento gerado automaticamente", Element.ALIGN_LEFT));
                footer.addCell(footerCell("Página " + writer.getPageNumber(), Element.ALIGN_RIGHT));
                footer.writeSelectedRows(0, -1, document.leftMargin(), document.bottomMargin() - 8, writer.getDirectContent());
            } catch (Exception ignored) {
            }
        }

        private PdfPCell footerCell(String value, int alignment) {
            var cell = new PdfPCell(new Phrase(value, new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED)));
            cell.setBorder(Rectangle.TOP);
            cell.setBorderColor(BORDER);
            cell.setHorizontalAlignment(alignment);
            cell.setPaddingTop(6);
            return cell;
        }
    }
}
