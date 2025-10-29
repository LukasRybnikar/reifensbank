package com.task.reifensbank.controller;

import com.task.reifensbank.api.DocumentsApi;
import com.task.reifensbank.usecase.DocumentsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class DocumentsController implements DocumentsApi {

    private final DocumentsAppService documentsAppService;

    @Override
    public ResponseEntity<com.task.reifensbank.model.Document> documentsCreate(
            MultipartFile file,
            String name,
            String type
    ) {
        return documentsAppService.create(file, name, type);
    }
}
