package com.happy.devx.document.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.happy.devx.document.dto.CreateDocumentRequest;
import com.happy.devx.document.dto.DocumentResponse;
import com.happy.devx.document.dto.PagedDocumentResponse;
import com.happy.devx.document.dto.UpdateDocumentStatusRequest;
import com.happy.devx.document.entity.DocumentOrigin;
import com.happy.devx.document.entity.DocumentStatus;
import com.happy.devx.document.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    @Test
    void returnsDocuments() throws Exception {
        DocumentResponse response = new DocumentResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "API Standards",
                "sample-knowledge-base/api-standards.md",
                DocumentStatus.INGESTED,
                DocumentOrigin.SAMPLE_KNOWLEDGE_BASE,
                OffsetDateTime.parse("2026-03-28T09:00:00Z"),
                OffsetDateTime.parse("2026-03-28T09:30:00Z")
        );

        given(documentService.getDocuments(0, 20, null, null))
                .willReturn(new PagedDocumentResponse(List.of(response), 0, 20, 1, 1));

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].title").value("API Standards"))
                .andExpect(jsonPath("$.items[0].status").value("INGESTED"))
                .andExpect(jsonPath("$.items[0].origin").value("SAMPLE_KNOWLEDGE_BASE"))
                .andExpect(jsonPath("$.items[0].sourcePath").value("sample-knowledge-base/api-standards.md"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void returnsDocumentById() throws Exception {
        UUID documentId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        DocumentResponse response = new DocumentResponse(
                documentId,
                "Runbook",
                "sample-knowledge-base/runbook.md",
                DocumentStatus.DISCOVERED,
                DocumentOrigin.SAMPLE_KNOWLEDGE_BASE,
                OffsetDateTime.parse("2026-03-28T11:00:00Z"),
                OffsetDateTime.parse("2026-03-28T11:00:00Z")
        );

        given(documentService.getDocumentById(documentId)).willReturn(response);

        mockMvc.perform(get("/api/documents/{id}", documentId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.title").value("Runbook"));
    }

    @Test
    void createsDocument() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Onboarding Guide",
                "sample-knowledge-base/onboarding-guide.md"
        );

        DocumentResponse response = new DocumentResponse(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Onboarding Guide",
                "sample-knowledge-base/onboarding-guide.md",
                DocumentStatus.DISCOVERED,
                DocumentOrigin.MANUAL,
                OffsetDateTime.parse("2026-03-28T10:00:00Z"),
                OffsetDateTime.parse("2026-03-28T10:00:00Z")
        );

        given(documentService.createDocument(any(CreateDocumentRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Onboarding Guide"))
                .andExpect(jsonPath("$.status").value("DISCOVERED"))
                .andExpect(jsonPath("$.sourcePath").value("sample-knowledge-base/onboarding-guide.md"));
    }

    @Test
    void updatesDocumentStatus() throws Exception {
        UUID documentId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UpdateDocumentStatusRequest request = new UpdateDocumentStatusRequest(DocumentStatus.INGESTED);

        DocumentResponse response = new DocumentResponse(
                documentId,
                "Onboarding Guide",
                "sample-knowledge-base/onboarding-guide.md",
                DocumentStatus.INGESTED,
                DocumentOrigin.MANUAL,
                OffsetDateTime.parse("2026-03-28T10:00:00Z"),
                OffsetDateTime.parse("2026-03-28T12:00:00Z")
        );

        given(documentService.updateDocumentStatus(documentId, request)).willReturn(response);

        mockMvc.perform(patch("/api/documents/{id}/status", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.status").value("INGESTED"));
    }

    @Test
    void filtersDocumentsByOriginAndStatus() throws Exception {
        DocumentResponse response = new DocumentResponse(
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                "Security Guide",
                "sample-knowledge-base/security-guide.md",
                DocumentStatus.DISCOVERED,
                DocumentOrigin.SAMPLE_KNOWLEDGE_BASE,
                OffsetDateTime.parse("2026-03-28T13:00:00Z"),
                OffsetDateTime.parse("2026-03-28T13:00:00Z")
        );

        given(documentService.getDocuments(0, 10, DocumentOrigin.SAMPLE_KNOWLEDGE_BASE, DocumentStatus.DISCOVERED))
                .willReturn(new PagedDocumentResponse(List.of(response), 0, 10, 1, 1));

        mockMvc.perform(get("/api/documents")
                        .param("page", "0")
                        .param("size", "10")
                        .param("origin", "SAMPLE_KNOWLEDGE_BASE")
                        .param("status", "DISCOVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].origin").value("SAMPLE_KNOWLEDGE_BASE"))
                .andExpect(jsonPath("$.items[0].status").value("DISCOVERED"))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void returnsValidationErrorForInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/documents").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]").exists());
    }
}
