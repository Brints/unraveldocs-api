package com.extractor.unraveldocs.ocrprocessing.utils;

import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

@Component
public class ExtractImageURL {
    public static void extractImageURL(FileEntry fileEntry, OcrData ocrData, String tesseractDataPath) throws IOException, TesseractException {
        URL imageUrl = URI.create(fileEntry.getFileUrl()).toURL();
        BufferedImage image = ImageIO.read(imageUrl);
        if (image == null) {
            throw new IOException("Failed to read image from URL: " + fileEntry.getFileUrl());
        }

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tesseractDataPath);
        tesseract.setLanguage("eng");

        String extractedText = tesseract.doOCR(image);

        ocrData.setExtractedText(extractedText);
        ocrData.setStatus(OcrStatus.COMPLETED);
    }
}
