package com.extractor.unraveldocs.documents.utils;

import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.utils.imageupload.FileType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ValidateFileCheck {
    public static void validateFileCheck(MultipartFile file, FileType fileType) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty: " + file.getOriginalFilename());
        }
        String contentType = file.getContentType();
        if (!fileType.isValid(contentType)) {
            throw new BadRequestException(
                    "Invalid file type: " + contentType +
                            " for file " + file.getOriginalFilename() +
                            ". Allowed types are: " +
                            String.join(", ", fileType.getMimeTypes()));
        }
    }
}
