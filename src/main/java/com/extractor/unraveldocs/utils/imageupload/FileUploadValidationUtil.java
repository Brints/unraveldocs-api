package com.extractor.unraveldocs.utils.imageupload;

import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

import static com.extractor.unraveldocs.documents.utils.ValidateFileCheck.validateFileCheck;

public final class FileUploadValidationUtil {

    private FileUploadValidationUtil() {
    }

    public static void validateTotalFileSize(MultipartFile[] files) {
        long totalSize = Arrays.stream(files)
                .mapToLong(MultipartFile::getSize)
                .sum();
        if (!FileSize.isValidFileSize(totalSize, true)) {
            throw new BadRequestException(FileSize.getFileSizeLimitMessage(true));
        }
    }

    public static void validateIndividualFile(MultipartFile file) {
        validateIndividualFile(file, FileType.FILE);
    }

    public static void validateIndividualFile(MultipartFile file, FileType fileType) {
        validateFileCheck(file, fileType);
        if (!FileSize.isValidFileSize(file.getSize(), false)) {
            throw new BadRequestException(FileSize.getFileSizeLimitMessage(false));
        }
    }
}
