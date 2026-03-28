package com.happy.devx.document.controller;

import com.happy.devx.document.dto.CreateDocumentRequest;
import com.happy.devx.document.dto.DocumentResponse;
import com.happy.devx.document.dto.PagedDocumentResponse;
import com.happy.devx.document.dto.UpdateDocumentStatusRequest;
import com.happy.devx.document.entity.DocumentOrigin;
import com.happy.devx.document.entity.DocumentStatus;
import com.happy.devx.document.service.DocumentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public PagedDocumentResponse getDocuments(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) DocumentOrigin origin,
            @RequestParam(required = false) DocumentStatus status
    ) {
        return documentService.getDocuments(page, size, origin, status);
    }

    @GetMapping("/{id}")
    public DocumentResponse getDocument(@PathVariable UUID id) {
        return documentService.getDocumentById(id);
    }

    @PatchMapping("/{id}/status")
    public DocumentResponse updateDocumentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDocumentStatusRequest request
    ) {
        return documentService.updateDocumentStatus(id, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        return documentService.createDocument(request);
    }
}
