package com.task.reifensbank.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogService {

    public void logEndpoint(String method, String path) {
        log.info("📥 Incoming {} request to: {}", method, path);
    }

}
