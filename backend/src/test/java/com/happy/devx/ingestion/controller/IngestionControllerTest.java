package com.happy.devx.ingestion.controller;

import com.happy.devx.ingestion.dto.IngestionScanResponse;
import com.happy.devx.ingestion.service.SampleKnowledgeBaseIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SampleKnowledgeBaseIngestionService sampleKnowledgeBaseIngestionService;

    @Test
    void scansSampleKnowledgeBase() throws Exception {
        given(sampleKnowledgeBaseIngestionService.ingestSampleKnowledgeBase())
                .willReturn(new IngestionScanResponse(3, 2, 1));

        mockMvc.perform(post("/api/ingestion/scan"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.filesFound").value(3))
                .andExpect(jsonPath("$.documentsCreated").value(2))
                .andExpect(jsonPath("$.documentsSkipped").value(1));
    }
}
