package com.happy.devx.document.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedDocumentResponse(
        List<DocumentResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedDocumentResponse from(Page<DocumentResponse> documentPage) {
        return new PagedDocumentResponse(
                documentPage.getContent(),
                documentPage.getNumber(),
                documentPage.getSize(),
                documentPage.getTotalElements(),
                documentPage.getTotalPages()
        );
    }
}
