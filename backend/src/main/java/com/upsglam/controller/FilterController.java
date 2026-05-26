package com.upsglam.controller;
import com.upsglam.model.Filter;
import com.upsglam.repository.FilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
@RestController
@RequestMapping("/api/filters")
@RequiredArgsConstructor
public class FilterController {
    private final FilterRepository filterRepo;
    @GetMapping
    public Flux<Filter> getActiveFilters() {
        return filterRepo.findByIsActiveTrue();
    }
}
