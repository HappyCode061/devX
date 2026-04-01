package com.happy.devx.chat.controller;

import com.happy.devx.chat.dto.ChatAskRequest;
import com.happy.devx.chat.dto.ChatAskResponse;
import com.happy.devx.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/ask")
    public ChatAskResponse ask(@Valid @RequestBody ChatAskRequest request) {
        return chatService.ask(request.question(), request.retrievalLimit(), request.includeDebug());
    }
}
