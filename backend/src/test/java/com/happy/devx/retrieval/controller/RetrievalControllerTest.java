package com.happy.devx.retrieval.controller;

import com.happy.devx.retrieval.dto.RetrievalSearchResponse;
import com.happy.devx.retrieval.dto.RetrievedChunkResponse;
import com.happy.devx.retrieval.dto.RetrievedDocumentResponse;
import com.happy.devx.retrieval.service.RetrievalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RetrievalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RetrievalService retrievalService;

    @Test
    void returnsRetrievalSearchResults() throws Exception {
        RetrievedChunkResponse chunk = new RetrievedChunkResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                0,
                "Install Java 21 and start PostgreSQL with Docker Compose.",
                58
        );

        RetrievedDocumentResponse document = new RetrievedDocumentResponse(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Onboarding Guide",
                "sample-knowledge-base/onboarding-guide.md",List.of("postgresql", "database"),
                1,
                List.of(chunk)
        );

        RetrievalSearchResponse response = new RetrievalSearchResponse(
                "postgresql",
                List.of("postgresql", "database"),
                5,
                1,
                1,
                List.of(document)
        );

        given(retrievalService.search("postgresql", 5)).willReturn(response);

        mockMvc.perform(get("/api/retrieval/search")
                        .param("query", "postgresql")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.query").value("postgresql"))
                .andExpect(jsonPath("$.matchedChunkCount").value(1))
                .andExpect(jsonPath("$.matchedDocumentCount").value(1))
                .andExpect(jsonPath("$.documents[0].documentTitle").value("Onboarding Guide"))
                .andExpect(jsonPath("$.documents[0].matchedChunks[0].chunkIndex").value(0));
    }

    @Test
    void rejectsInvalidRetrievalLimit() throws Exception {
        mockMvc.perform(get("/api/retrieval/search")
                        .param("query", "postgresql")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}
