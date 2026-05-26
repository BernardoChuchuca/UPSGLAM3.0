package com.upsglam.controller;
import com.upsglam.dto.response.ProcessingHistoryResponse;
import com.upsglam.service.ProcessingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.UUID;
@RestController
@RequestMapping("/api/processing")
@RequiredArgsConstructor
public class ProcessingHistoryController {
    private final ProcessingHistoryService historyService;
    @GetMapping("/history")
    public Flux<ProcessingHistoryResponse> getMyHistory(Authentication auth) {
        return historyService.getMyHistory((UUID) auth.getPrincipal());
    }
}
