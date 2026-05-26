package com.upsglam.controller;
import com.upsglam.dto.request.CreateCommentRequest;
import com.upsglam.dto.response.CommentResponse;
import com.upsglam.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;
@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    @GetMapping
    public Flux<CommentResponse> getComments(@PathVariable UUID postId) {
        return commentService.getComments(postId);
    }
    @PostMapping
    public Mono<ResponseEntity<CommentResponse>> addComment(
            @PathVariable UUID postId,
            @RequestBody CreateCommentRequest req,
            Authentication auth) {
        return commentService.addComment((UUID) auth.getPrincipal(), postId, req)
            .map(ResponseEntity::ok);
    }
    @DeleteMapping("/{commentId}")
    public Mono<ResponseEntity<Void>> deleteComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            Authentication auth) {
        return commentService.deleteComment((UUID) auth.getPrincipal(), commentId)
            .thenReturn(ResponseEntity.<Void>noContent().build());
    }
}
