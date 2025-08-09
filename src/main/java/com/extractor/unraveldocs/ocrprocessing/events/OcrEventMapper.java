package com.extractor.unraveldocs.ocrprocessing.events;

import com.extractor.unraveldocs.documents.model.FileEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OcrEventMapper {

    @Mapping(target = "collectionId", source = "collectionId")
    @Mapping(target = "documentId", source = "fileEntry.documentId")
    OcrRequestedEvent toOcrRequestedEvent(FileEntry fileEntry, String collectionId);
}
