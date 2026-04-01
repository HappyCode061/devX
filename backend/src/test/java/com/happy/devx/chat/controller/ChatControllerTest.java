package com.happy.devx.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.happy.devx.chat.dto.ChatAskRequest;
import com.happy.devx.chat.dto.ChatAskResponse;
import com.happy.devx.chat.dto.ChatCitationResponse;
import com.happy.devx.chat.service.ChatService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @Test
    void returnsGroundedDraftAnswer() throws Exception {
        ChatAskRequest request = new ChatAskRequest("How do we validate PostgreSQL setup?", 5, false);

        ChatAskResponse response = new ChatAskResponse(
                "How do we validate PostgreSQL setup?",
                "Draft grounded answer for: How do we validate PostgreSQL setup?",
                "draft",
                "retrieval-draft-v1",
                true,
                List.of("postgresql", "setup"),
                1,
                2,
                List.of(new ChatCitationResponse(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "Onboarding Guide",
                        "sample-knowledge-base/onboarding-guide.md",
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        0
                )),
                null
        );

        given(chatService.ask(request.question(), request.retrievalLimit(), request.includeDebug())).willReturn(response);

        mockMvc.perform(post("/api/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.question").value("How do we validate PostgreSQL setup?"))
                .andExpect(jsonPath("$.matchedDocumentCount").value(1))
                .andExpect(jsonPath("$.citations[0].documentTitle").value("Onboarding Guide"));
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        ChatAskRequest request = new ChatAskRequest("", 5, false);

        mockMvc.perform(post("/api/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void returnsDebugBlockWhenRequested() throws Exception {
        ChatAskRequest request = new ChatAskRequest("How do we validate PostgreSQL setup?", 5, true);

        ChatAskResponse response = new ChatAskResponse(
                "How do we validate PostgreSQL setup?",
                "Draft grounded answer for: How do we validate PostgreSQL setup?",
                "draft",
                "retrieval-draft-v1",
                true,
                List.of("postgresql", "setup"),
                1,
                2,
                List.of(),
                new com.happy.devx.chat.dto.ChatDebugResponse(
                        "fallback",
                        "Question:\nHow do we validate PostgreSQL setup?",
                        43,
                        List.of(new com.happy.devx.chat.dto.ChatDebugSourceResponse(
                                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                                "Onboarding Guide",
                                "sample-knowledge-base/onboarding-guide.md",
                                List.of("postgresql"),
                                List.of(0)
                        ))
                )
        );

        given(chatService.ask(request.question(), request.retrievalLimit(), request.includeDebug())).willReturn(response);

        mockMvc.perform(post("/api/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debug.generationMode").value("fallback"))
                .andExpect(jsonPath("$.debug.selectedSources[0].documentTitle").value("Onboarding Guide"));
    }
}
