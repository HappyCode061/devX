package com.happy.devx.chunk.controller;

import com.happy.devx.chunk.dto.ChunkSearchResponse;
import com.happy.devx.chunk.service.DocumentChunkService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/chunks")
@RequiredArgsConstructor
public class ChunkController {

    private final DocumentChunkService documentChunkService;

    @GetMapping("/search")
    public List<ChunkSearchResponse> searchChunks(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit
    ) {
        return documentChunkService.searchChunks(query, limit);
    }
}
