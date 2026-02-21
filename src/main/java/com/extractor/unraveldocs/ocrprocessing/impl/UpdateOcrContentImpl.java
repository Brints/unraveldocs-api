package com.extractor.unraveldocs.ocrprocessing.impl;

import com.extractor.unraveldocs.documents.dto.response.DocumentCollectionResponse;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.documents.model.FileEntry;
import com.extractor.unraveldocs.documents.repository.DocumentCollectionRepository;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.elasticsearch.events.IndexAction;
import com.extractor.unraveldocs.elasticsearch.service.ElasticsearchIndexingService;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.ocrprocessing.datamodel.ContentFormat;
import com.extractor.unraveldocs.ocrprocessing.datamodel.OcrStatus;
import com.extractor.unraveldocs.ocrprocessing.dto.response.FileResultData;
import com.extractor.unraveldocs.ocrprocessing.interfaces.UpdateOcrContentService;
import com.extractor.unraveldocs.ocrprocessing.model.OcrData;
import com.extractor.unraveldocs.ocrprocessing.repository.OcrDataRepository;
import com.extractor.unraveldocs.ocrprocessing.utils.FindAndValidateFileEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateOcrContentImpl implements UpdateOcrContentService {
    private final DocumentCollectionRepository documentCollectionRepository;
    private final OcrDataRepository ocrDataRepository;
    private final FindAndValidateFileEntry validateFileEntry;
    private final Optional<ElasticsearchIndexingService> elasticsearchIndexingService;
    private final SanitizeLogging sanitizer;

    /**
     * Safelist for HTML sanitization.
     * Allows common formatting tags while stripping scripts, event handlers, etc.
     */
    private static final Safelist HTML_SAFELIST = Safelist.relaxed()
            .addTags("span", "div", "br", "hr", "pre", "code", "mark", "sub", "sup", "u", "s")
            .addAttributes("a", "href", "title", "target", "rel")
            .addAttributes("span", "style")
            .addAttributes("p", "style")
            .addAttributes("div", "style")
            .addProtocols("a", "href", "http", "https", "mailto");

    @Override
    @Transactional
    public DocumentCollectionResponse<FileResultData> updateOcrContent(
            String collectionId, String documentId, String userId,
            String editedContent, ContentFormat contentFormat) {

        FileEntry fileEntry = validateFileEntry
                .findAndValidateFileEntry(collectionId, documentId, userId, documentCollectionRepository);

        OcrData ocrData = ocrDataRepository.findByDocumentId(fileEntry.getDocumentId())
                .orElseThrow(() -> new NotFoundException(
                        "OCR data not found for document ID: " + fileEntry.getDocumentId()));

        if (ocrData.getStatus() != OcrStatus.COMPLETED) {
            throw new BadRequestException(
                    "Cannot edit content for a document that has not completed OCR processing. Current status: "
                            + ocrData.getStatus().name());
        }

        String sanitizedContent = sanitizeContent(editedContent, contentFormat);

        ocrData.setEditedContent(sanitizedContent);
        ocrData.setContentFormat(contentFormat);
        ocrData.setEditedBy(userId);
        ocrData.setEditedAt(OffsetDateTime.now());
        ocrDataRepository.save(ocrData);

        // Re-index in Elasticsearch with the edited content
        reindexDocument(collectionId, fileEntry, ocrData);

        FileResultData fileResultData = buildFileResultData(fileEntry, ocrData);

        return DocumentCollectionResponse.<FileResultData>builder()
                .statusCode(HttpStatus.OK.value())
                .status("success")
                .message("Document content updated successfully.")
                .data(fileResultData)
                .build();
    }

    /**
     * Sanitizes the edited content based on the format.
     * HTML content is sanitized using Jsoup to prevent XSS.
     * Markdown content is stored as-is since it has no executable content.
     */
    private String sanitizeContent(String content, ContentFormat format) {
        if (format == ContentFormat.HTML) {
            return Jsoup.clean(content, HTML_SAFELIST);
        }
        return content;
    }

    /**
     * Re-indexes the document in Elasticsearch with updated content.
     */
    private void reindexDocument(String collectionId, FileEntry fileEntry, OcrData ocrData) {
        try {
            documentCollectionRepository.findById(collectionId).ifPresent(collection -> elasticsearchIndexingService.ifPresent(
                    service -> service.indexDocument(collection, fileEntry, ocrData, IndexAction.UPDATE)));
        } catch (Exception e) {
            log.error("Failed to re-index document {} after content update: {}",
                    sanitizer.sanitizeLogging(fileEntry.getDocumentId()), e.getMessage());
        }
    }

    /**
     * Builds the FileResultData response from the file entry and OCR data.
     */
    private static FileResultData buildFileResultData(FileEntry fileEntry, OcrData ocrData) {
        FileResultData fileResultData = new FileResultData();
        fileResultData.setDocumentId(fileEntry.getDocumentId());
        fileResultData.setOriginalFileName(fileEntry.getOriginalFileName());
        fileResultData.setStatus(ocrData.getStatus().name());
        fileResultData.setErrorMessage(ocrData.getErrorMessage());
        fileResultData.setCreatedAt(ocrData.getCreatedAt());
        fileResultData.setExtractedText(ocrData.getExtractedText());
        fileResultData.setEditedContent(ocrData.getEditedContent());
        fileResultData.setContentFormat(
                ocrData.getContentFormat() != null ? ocrData.getContentFormat().name() : null);
        fileResultData.setEditedBy(ocrData.getEditedBy());
        fileResultData.setEditedAt(ocrData.getEditedAt());
        fileResultData.setAiSummary(ocrData.getAiSummary());
        fileResultData.setDocumentType(ocrData.getDocumentType());

        if (ocrData.getAiTags() != null && !ocrData.getAiTags().isBlank()) {
            fileResultData.setAiTags(
                    Arrays.stream(ocrData.getAiTags().split(","))
                            .map(String::trim)
                            .filter(tag -> !tag.isEmpty())
                            .toList());
        }

        return fileResultData;
    }
}
