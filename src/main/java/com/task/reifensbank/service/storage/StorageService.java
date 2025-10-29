package com.task.reifensbank.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String put(String objectKey, MultipartFile file) throws Exception;

    void delete(String objectKey) throws Exception;

    default String buildObjectKey(String publicId, String extension) {
        String safeExt = extension == null ? "" : extension.replaceAll("[^a-zA-Z0-9]", "");
        return safeExt.isBlank() ? "documents/%s".formatted(publicId)
                : "documents/%s.%s".formatted(publicId, safeExt.toLowerCase());
    }
}
