package com.extractor.unraveldocs.wordexport.impl;

import com.extractor.unraveldocs.wordexport.interfaces.DocxExportService;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class DocxExportServiceImpl implements DocxExportService {

    @Override
    public ByteArrayInputStream generateDocxFromText(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            if (isHtml(text)) {
                parseHtmlToDocx(document, text);
            } else {
                String[] paragraphs = text.split("\\r?\\n");
                for (String p : paragraphs) {
                    XWPFParagraph paragraph = document.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.setText(p);
                }
            }

            document.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private boolean isHtml(String text) {
        return text != null
                && (text.contains("<p>") || text.contains("<h3>") || text.contains("<ul>") || text.contains("<li>"));
    }

    private void parseHtmlToDocx(XWPFDocument document, String html) {
        Document jsoupDoc = Jsoup.parseBodyFragment(html);
        Element body = jsoupDoc.body();

        for (Element element : body.children()) {
            processElement(document, element);
        }
    }

    private void processElement(XWPFDocument document, Element element) {
        String tagName = element.tagName().toLowerCase();

        switch (tagName) {
            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
                XWPFParagraph header = document.createParagraph();
                header.setStyle("Heading" + tagName.substring(1));
                XWPFRun headerRun = header.createRun();
                headerRun.setBold(true);
                // Simple font size fallback if styles aren't fully defined
                headerRun.setFontSize(tagName.equals("h1") ? 20 : (tagName.equals("h2") ? 18 : 16));
                renderChildren(header, element);
                break;

            case "p":
                XWPFParagraph p = document.createParagraph();
                renderChildren(p, element);
                break;

            case "ul":
            case "ol":
                for (Element li : element.children()) {
                    if (li.tagName().equalsIgnoreCase("li")) {
                        XWPFParagraph liPara = document.createParagraph();
                        liPara.setNumID(tagName.equals("ul") ? getBulletNumId(document) : getDecimalNumId(document));

                        // Safely set indentation to prevent NPE
                        CTPPr pPr = liPara.getCTP().getPPr();
                        if (pPr == null)
                            pPr = liPara.getCTP().addNewPPr();
                        CTInd ind = pPr.getInd();
                        if (ind == null)
                            ind = pPr.addNewInd();
                        ind.setLeft(java.math.BigInteger.valueOf(720));

                        renderChildren(liPara, li);
                    }
                }
                break;

            default:
                // Fallback for nested or unknown tags
                if (!element.children().isEmpty()) {
                    for (Element child : element.children()) {
                        processElement(document, child);
                    }
                } else {
                    XWPFParagraph fallback = document.createParagraph();
                    renderChildren(fallback, element);
                }
                break;
        }
    }

    private void renderChildren(XWPFParagraph paragraph, Element parent) {
        for (Node node : parent.childNodes()) {
            if (node instanceof TextNode) {
                XWPFRun run = paragraph.createRun();
                run.setText(((TextNode) node).text());
            } else if (node instanceof Element) {
                Element child = (Element) node;
                processInlineElement(paragraph, child);
            }
        }
    }

    private void processInlineElement(XWPFParagraph paragraph, Element element) {
        String tagName = element.tagName().toLowerCase();

        switch (tagName) {
            case "strong":
            case "b":
                XWPFRun boldRun = paragraph.createRun();
                boldRun.setBold(true);
                boldRun.setText(element.text());
                break;
            case "em":
            case "i":
                XWPFRun italicRun = paragraph.createRun();
                italicRun.setItalic(true);
                italicRun.setText(element.text());
                break;
            case "u":
                XWPFRun underRun = paragraph.createRun();
                underRun.setUnderline(UnderlinePatterns.SINGLE);
                underRun.setText(element.text());
                break;
            case "br":
                paragraph.createRun().addBreak();
                break;
            case "span":
            case "p": // Nested p handling
                renderChildren(paragraph, element);
                break;
            default:
                XWPFRun normalRun = paragraph.createRun();
                normalRun.setText(element.text());
                break;
        }
    }

    // Simplified numbering helpers - in a real app these would use a numbering.xml
    // template
    private java.math.BigInteger getBulletNumId(XWPFDocument document) {
        // This is a simplified approach. Ideally, you define numbering styles.
        return java.math.BigInteger.valueOf(1);
    }

    private java.math.BigInteger getDecimalNumId(XWPFDocument document) {
        return java.math.BigInteger.valueOf(2);
    }
}
