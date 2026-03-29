package com.happy.devx.retrieval.controller;

import com.happy.devx.retrieval.dto.RetrievalSearchResponse;
import com.happy.devx.retrieval.service.RetrievalService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/retrieval")
@RequiredArgsConstructor
public class RetrievalController {

    private final RetrievalService retrievalService;

    @GetMapping("/search")
    public RetrievalSearchResponse search(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit
    ) {
        return retrievalService.search(query, limit);
    }
}
