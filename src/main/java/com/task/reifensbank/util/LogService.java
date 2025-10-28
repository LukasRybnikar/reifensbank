package com.task.reifensbank.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class LogService {

    public void logEndpoint(String method, String path) {
        log.info("📥 Incoming {} request to: {}", method, path);
    }

    public void logError(String errorCode, String requestURI, Throwable ex) {
        log.error("[{}] Error exception at {}: {}", errorCode, requestURI, ex.getMessage(), ex);
    }

}
