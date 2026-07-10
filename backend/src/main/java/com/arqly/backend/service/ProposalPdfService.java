package com.arqly.backend.service;

import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.ClientPersonType;
import com.arqly.backend.entity.Proposal;
import com.arqly.backend.entity.ProposalItem;
import com.arqly.backend.entity.ProposalPaymentCondition;
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
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ProposalPdfService {
    private static final Color PRIMARY = new Color(0, 121, 107);
    private static final Color PRIMARY_DARK = new Color(0, 84, 78);
    private static final Color TEXT = new Color(15, 23, 42);
    private static final Color MUTED = new Color(82, 97, 116);
    private static final Color SOFT = new Color(242, 248, 247);
    private static final Color BORDER = new Color(219, 231, 228);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generate(Proposal proposal) {
        try {
            var output = new ByteArrayOutputStream();
            var document = new Document(PageSize.A4, 40, 40, 44, 46);
            var writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new FooterEvent());
            document.open();

            document.add(hero(proposal));
            document.add(spacer(12));
            document.add(parties(proposal));
            document.add(spacer(14));
            document.add(sectionTitle("Serviços contratados"));
            document.add(itemsTable(proposal));
            document.add(spacer(12));
            document.add(financialSummary(proposal));
            document.add(spacer(14));
            document.add(sectionTitle("Condições de pagamento"));
            document.add(paymentTable(proposal));
            addTextSection(document, "Escopo", proposal.getScope());
            addTextSection(document, "Exclusões", proposal.getExclusions());
            addTextSection(document, "Observações para o cliente", proposal.getClientNotes());
            document.add(spacer(18));
            document.add(closingBlock());
            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível gerar o PDF da proposta.", ex);
        }
    }

    private PdfPTable hero(Proposal proposal) {
        var table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1.45f, 1f });

        var title = new Paragraph();
        title.add(new Chunk("Arqly\n", font(26, Font.BOLD, PRIMARY_DARK)));
        title.add(new Chunk("Proposta comercial\n", font(10, Font.BOLD, PRIMARY)));
        title.add(new Chunk(nullToDash(proposal.getTitle()), font(18, Font.BOLD, TEXT)));

        var titleCell = cell(title, Rectangle.NO_BORDER, 0, 0, Element.ALIGN_LEFT);
        titleCell.setPadding(0);
        table.addCell(titleCell);

        var meta = new PdfPTable(1);
        meta.setWidthPercentage(100);
        meta.addCell(infoLine("Número", proposal.getNumber()));
        meta.addCell(infoLine("Status", statusLabel(proposal.getStatus().name())));
        meta.addCell(infoLine("Validade", proposal.getValidUntil() == null ? "-" : proposal.getValidUntil().format(DATE)));
        meta.addCell(infoLine("Emissão", proposal.getCreatedAt() == null ? "-" : DATE.format(proposal.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate())));

        var metaCell = cell(meta, Rectangle.NO_BORDER);
        metaCell.setPadding(0);
        table.addCell(metaCell);
        return table;
    }

    private PdfPTable parties(Proposal proposal) {
        var table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1f, 1f });
        table.addCell(partyCard("Cliente", clientLines(proposal.getClient())));
        table.addCell(partyCard("Escritório", officeLines(proposal)));
        return table;
    }

    private PdfPTable itemsTable(Proposal proposal) {
        var table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 3.2f, .75f, .95f, 1.05f, 1.1f });
        table.setHeaderRows(1);
        addHeader(table, "Serviço", "Qtd.", "Unidade", "Unitário", "Total");
        for (ProposalItem item : proposal.getItems()) {
            var description = firstNotBlank(item.getCustomDescription(), item.getServiceDescription());
            var service = new Paragraph(nullToDash(item.getServiceName()), font(9, Font.BOLD, TEXT));
            if (description != null) {
                service.add(new Chunk("\n" + description, font(8, Font.NORMAL, MUTED)));
            }
            table.addCell(bodyCell(service, Element.ALIGN_LEFT));
            table.addCell(bodyCell(number(item.getQuantity()), Element.ALIGN_CENTER));
            table.addCell(bodyCell(unitLabel(item.getUnit().name()), Element.ALIGN_CENTER));
            table.addCell(bodyCell(money(item.getUnitValue()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(money(item.getTotal()), Element.ALIGN_RIGHT));
        }
        table.setSplitLate(false);
        return table;
    }

    private PdfPTable financialSummary(Proposal proposal) {
        var table = new PdfPTable(2);
        table.setWidthPercentage(48);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setWidths(new float[] { 1.2f, 1f });
        addTotal(table, "Subtotal", proposal.getSubtotal(), false);
        addTotal(table, "Desconto", proposal.getDiscount(), false);
        addTotal(table, "Acréscimo", proposal.getAddition(), false);
        addTotal(table, "Valor total", proposal.getTotal(), true);
        return table;
    }

    private PdfPTable paymentTable(Proposal proposal) {
        var table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 2.4f, .9f, 1f, 1f });
        table.setHeaderRows(1);
        addHeader(table, "Descrição", "%", "Valor", "Vencimento");
        if (proposal.getPaymentConditions().isEmpty()) {
            var empty = bodyCell("Condições a combinar.", Element.ALIGN_LEFT);
            empty.setColspan(4);
            table.addCell(empty);
            return table;
        }
        for (ProposalPaymentCondition condition : proposal.getPaymentConditions()) {
            table.addCell(bodyCell(condition.getDescription(), Element.ALIGN_LEFT));
            table.addCell(bodyCell(condition.getPercentage() == null ? "-" : number(condition.getPercentage()) + "%", Element.ALIGN_CENTER));
            table.addCell(bodyCell(money(condition.getValue()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(condition.getDueDate() == null ? "-" : condition.getDueDate().format(DATE), Element.ALIGN_CENTER));
        }
        return table;
    }

    private void addTextSection(Document document, String title, String value) throws Exception {
        if (value == null || value.isBlank()) return;
        document.add(spacer(12));
        document.add(sectionTitle(title));
        var paragraph = new Paragraph(value.trim(), font(9, Font.NORMAL, TEXT));
        paragraph.setLeading(13);
        paragraph.setSpacingBefore(4);
        document.add(paragraph);
    }

    private PdfPTable closingBlock() {
        var table = new PdfPTable(1);
        table.setWidthPercentage(100);
        var paragraph = new Paragraph();
        paragraph.add(new Chunk("Próximos passos\n", font(10, Font.BOLD, PRIMARY_DARK)));
        paragraph.add(new Chunk("Após a aprovação, esta proposta poderá originar um projeto dentro do Arqly, mantendo a rastreabilidade entre negociação comercial e execução.", font(9, Font.NORMAL, MUTED)));
        var cell = cell(paragraph, Rectangle.BOX, 10, 10, Element.ALIGN_LEFT);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(SOFT);
        table.addCell(cell);
        return table;
    }

    private PdfPCell partyCard(String title, String[] lines) {
        var paragraph = new Paragraph();
        paragraph.add(new Chunk(title + "\n", font(9, Font.BOLD, PRIMARY)));
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                paragraph.add(new Chunk(line + "\n", font(9, Font.NORMAL, TEXT)));
            }
        }
        var cell = cell(paragraph, Rectangle.BOX, 10, 10, Element.ALIGN_LEFT);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(Color.WHITE);
        return cell;
    }

    private PdfPCell infoLine(String label, String value) {
        var paragraph = new Paragraph();
        paragraph.add(new Chunk(label + "\n", font(7, Font.BOLD, MUTED)));
        paragraph.add(new Chunk(nullToDash(value), font(10, Font.BOLD, TEXT)));
        var cell = cell(paragraph, Rectangle.BOX, 8, 6, Element.ALIGN_LEFT);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(SOFT);
        return cell;
    }

    private void addHeader(PdfPTable table, String... titles) {
        for (String title : titles) {
            var cell = new PdfPCell(new Phrase(title, font(8, Font.BOLD, Color.WHITE)));
            cell.setBackgroundColor(PRIMARY);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }
    }

    private PdfPCell bodyCell(String text, int alignment) {
        return bodyCell(new Paragraph(nullToDash(text), font(8, Font.NORMAL, TEXT)), alignment);
    }

    private PdfPCell bodyCell(Paragraph paragraph, int alignment) {
        var cell = new PdfPCell(paragraph);
        cell.setPadding(8);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(BORDER);
        cell.setUseAscender(true);
        cell.setUseDescender(true);
        return cell;
    }

    private void addTotal(PdfPTable table, String label, BigDecimal value, boolean highlight) {
        var left = bodyCell(label, Element.ALIGN_LEFT);
        var right = bodyCell(money(value), Element.ALIGN_RIGHT);
        if (highlight) {
            left.setBackgroundColor(PRIMARY_DARK);
            right.setBackgroundColor(PRIMARY_DARK);
            left.setPhrase(new Phrase(label, font(9, Font.BOLD, Color.WHITE)));
            right.setPhrase(new Phrase(money(value), font(10, Font.BOLD, Color.WHITE)));
        }
        table.addCell(left);
        table.addCell(right);
    }

    private Paragraph sectionTitle(String text) {
        var paragraph = new Paragraph(text, font(11, Font.BOLD, PRIMARY_DARK));
        paragraph.setSpacingAfter(6);
        return paragraph;
    }

    private Paragraph spacer(float height) {
        var paragraph = new Paragraph(" ");
        paragraph.setSpacingBefore(height);
        paragraph.setLeading(0);
        return paragraph;
    }

    private PdfPCell cell(PdfPTable table, int border) {
        var cell = new PdfPCell(table);
        cell.setBorder(border);
        return cell;
    }

    private PdfPCell cell(Paragraph paragraph, int border, float horizontalPadding, float verticalPadding, int alignment) {
        var cell = new PdfPCell(paragraph);
        cell.setBorder(border);
        cell.setHorizontalAlignment(alignment);
        cell.setPaddingLeft(horizontalPadding);
        cell.setPaddingRight(horizontalPadding);
        cell.setPaddingTop(verticalPadding);
        cell.setPaddingBottom(verticalPadding);
        return cell;
    }

    private Font font(int size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    private String[] clientLines(Client client) {
        var document = client.getPersonType() == ClientPersonType.NATURAL_PERSON ? client.getCpf() : client.getCnpj();
        return new String[] {
                displayName(client),
                client.getEmail(),
                client.getPhone(),
                document == null || document.isBlank() ? null : "Documento: " + document,
                address(client)
        };
    }

    private String[] officeLines(Proposal proposal) {
        var tenant = proposal.getTenant();
        return new String[] {
                tenant.getTradeName(),
                tenant.getPrimaryEmail(),
                tenant.getPhone(),
                tenant.getCnpj() == null || tenant.getCnpj().isBlank() ? null : "Documento: " + tenant.getCnpj(),
                String.join(" - ", nonBlank(tenant.getAddress()), nonBlank(tenant.getCity()), nonBlank(tenant.getState()))
        };
    }

    private String address(Client client) {
        var firstLine = String.join(", ", nonBlank(client.getStreet()), nonBlank(client.getNumber()));
        var secondLine = String.join(" - ", nonBlank(client.getDistrict()), nonBlank(client.getCity()), nonBlank(client.getState()));
        var zip = nonBlank(client.getZipCode());
        return String.join(" | ", nonBlank(firstLine), nonBlank(secondLine), zip.isBlank() ? "" : "CEP " + zip);
    }

    private String nonBlank(String value) {
        return value == null || value.isBlank() ? "" : value;
    }

    private String firstNotBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first.trim();
        if (second != null && !second.isBlank()) return second.trim();
        return null;
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String displayName(Client client) {
        return client.getPersonType() == ClientPersonType.NATURAL_PERSON ? client.getName() : client.getTradeName();
    }

    private String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(value == null ? BigDecimal.ZERO : value);
    }

    private String number(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String unitLabel(String unit) {
        return switch (unit) {
            case "M2" -> "m²";
            case "HOUR" -> "Hora";
            case "DAY" -> "Dia";
            case "MONTH" -> "Mês";
            case "PROJECT" -> "Projeto";
            case "VISIT" -> "Visita";
            case "OTHER" -> "Outro";
            default -> unit;
        };
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "DRAFT" -> "Rascunho";
            case "SENT" -> "Enviada";
            case "VIEWED" -> "Visualizada";
            case "ACCEPTED" -> "Aceita";
            case "REJECTED" -> "Recusada";
            case "EXPIRED" -> "Expirada";
            case "CANCELLED" -> "Cancelada";
            default -> status;
        };
    }

    private static class FooterEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            var table = new PdfPTable(2);
            try {
                table.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                table.setWidths(new float[] { 1.5f, 1f });
                table.addCell(footerCell("Arqly · proposta gerada automaticamente", Element.ALIGN_LEFT));
                table.addCell(footerCell("Página " + writer.getPageNumber(), Element.ALIGN_RIGHT));
                table.writeSelectedRows(0, -1, document.leftMargin(), document.bottomMargin() - 8, writer.getDirectContent());
            } catch (Exception ignored) {
            }
        }

        private PdfPCell footerCell(String text, int alignment) {
            var cell = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED)));
            cell.setBorder(Rectangle.TOP);
            cell.setBorderColor(BORDER);
            cell.setHorizontalAlignment(alignment);
            cell.setPaddingTop(6);
            return cell;
        }
    }
}
