package com.extractor.unraveldocs.ocrprocessing.events;

import com.extractor.unraveldocs.documents.model.FileEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OcrEventMapper {

    @Mapping(target = "collectionId", source = "collectionId")
    @Mapping(target = "documentId", source = "fileEntry.documentId")
    @Mapping(target = "startPage", ignore = true)
    @Mapping(target = "endPage", ignore = true)
    @Mapping(target = "pages", ignore = true)
    OcrRequestedEvent toOcrRequestedEvent(FileEntry fileEntry, String collectionId);

    @Mapping(target = "collectionId", source = "collectionId")
    @Mapping(target = "documentId", source = "fileEntry.documentId")
    @Mapping(target = "startPage", source = "startPage")
    @Mapping(target = "endPage", source = "endPage")
    @Mapping(target = "pages", source = "pages")
    OcrRequestedEvent toOcrRequestedEvent(FileEntry fileEntry, String collectionId,
                                          Integer startPage, Integer endPage, List<Integer> pages);
}
