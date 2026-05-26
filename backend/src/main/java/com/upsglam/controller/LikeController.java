package com.upsglam.controller;
import com.upsglam.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/posts/{postId}/likes")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;
    @PostMapping
    public Mono<ResponseEntity<Void>> toggleLike(
            @PathVariable UUID postId, Authentication auth) {
        return likeService.toggleLike((UUID) auth.getPrincipal(), postId)
            .thenReturn(ResponseEntity.<Void>ok().build());
    }
    @GetMapping("/count")
    public Mono<Map<String,Long>> getLikeCount(@PathVariable UUID postId) {
        return likeService.getLikeCount(postId).map(c -> Map.of("count", c));
    }
}
