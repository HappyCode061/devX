package com.happy.devx.chunk.controller;

import com.happy.devx.chunk.dto.ChunkSearchResponse;
import com.happy.devx.chunk.service.DocumentChunkService;
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
class ChunkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentChunkService documentChunkService;

    @Test
    void searchesChunks() throws Exception {
        ChunkSearchResponse response = new ChunkSearchResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Onboarding Guide",
                "sample-knowledge-base/onboarding-guide.md",
                0,
                "Install Java 21 and start PostgreSQL with Docker Compose.",
                58
        );

        given(documentChunkService.searchChunks("postgresql", 5)).willReturn(List.of(response));

        mockMvc.perform(get("/api/chunks/search")
                        .param("query", "postgresql")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].documentTitle").value("Onboarding Guide"))
                .andExpect(jsonPath("$[0].documentSourcePath").value("sample-knowledge-base/onboarding-guide.md"))
                .andExpect(jsonPath("$[0].chunkIndex").value(0));
    }

    @Test
    void rejectsInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/chunks/search")
                        .param("query", "postgresql")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}
