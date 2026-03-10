package com.extractor.unraveldocs.wordexport.interfaces;

import org.springframework.core.io.InputStreamResource;

/**
 * Service interface for preparing and providing OCR results for download.
 */
public interface DownloadOcrResultService {
    /**
     * A record to hold the file data for download.
     *
     * @param fileName The name of the file to be suggested to the client.
     * @param resource The binary content of the file as a streamable resource.
     */
    record DownloadableFile(String fileName, InputStreamResource resource) {}

    /**
     * Retrieves OCR data for a document, generates a DOCX file, and wraps it for download.
     *
     * @param collectionId The ID of the document's collection.
     * @param documentId   The ID of the document to process.
     * @param type         The type of text to download (original or edited).
     * @param userId       The ID of the user requesting the download.
     * @return A {@link DownloadableFile} record containing the file name and resource.
     */
    DownloadableFile downloadAsDocx(String collectionId, String documentId, String type, String userId);
}
