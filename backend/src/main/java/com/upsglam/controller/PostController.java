package com.upsglam.controller;

import com.upsglam.dto.request.CreatePostRequest;
import com.upsglam.dto.response.PostResponse;
import com.upsglam.dto.response.CudaProcessingResponse;
import com.upsglam.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PostController {

    private final PostService postService;

    @GetMapping("/feed")
    public Flux<PostResponse> getFeed(Authentication auth) {
        UUID userId = auth != null ? (UUID) auth.getPrincipal() : null;
        return postService.getFeed(userId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<PostResponse>> createPost(
            @RequestPart("data") @Valid CreatePostRequest req,
            @RequestPart("image") FilePart image,
            Authentication auth) {
        UUID userId = auth != null ? (UUID) auth.getPrincipal() : UUID.fromString("9ffcf888-ff9d-4cb3-b28c-36f18e363443");
        return image.content()
            .reduce(new byte[0], (acc, buf) -> {
                byte[] b = new byte[buf.readableByteCount()];
                buf.read(b);
                byte[] combined = new byte[acc.length + b.length];
                System.arraycopy(acc, 0, combined, 0, acc.length);
                System.arraycopy(b, 0, combined, acc.length, b.length);
                return combined;
            })
            .flatMap(bytes -> postService.createPost(userId, req, bytes,
                image.headers().getContentType().toString()))
            .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<CudaProcessingResponse>> previewPost(
            @RequestParam("filterName") String filterName,
            @RequestPart("image") FilePart image) {
        return image.content()
            .reduce(new byte[0], (acc, buf) -> {
                byte[] b = new byte[buf.readableByteCount()];
                buf.read(b);
                byte[] combined = new byte[acc.length + b.length];
                System.arraycopy(acc, 0, combined, 0, acc.length);
                System.arraycopy(b, 0, combined, acc.length, b.length);
                return combined;
            })
            .flatMap(bytes -> postService.previewFilter(filterName, bytes))
            .map(ResponseEntity::ok);
    }

    @GetMapping("/user/{profileId}")
    public Flux<PostResponse> getUserPosts(@PathVariable UUID profileId, Authentication auth) {
        UUID userId = auth != null ? (UUID) auth.getPrincipal() : null;
        return postService.getUserPosts(profileId, userId);
    }

    @DeleteMapping("/{postId}")
    public Mono<ResponseEntity<Void>> deletePost(@PathVariable UUID postId, Authentication auth) {
        return postService.deletePost((UUID) auth.getPrincipal(), postId)
            .thenReturn(ResponseEntity.<Void>noContent().build());
    }

    // ─── ENDPOINTS DE REPOSTS ────────────────────────────────────

    @PostMapping("/{postId}/repost")
    public Mono<ResponseEntity<Void>> toggleRepost(@PathVariable UUID postId, Authentication auth) {
        return postService.toggleRepost((UUID) auth.getPrincipal(), postId)
                .thenReturn(ResponseEntity.ok().<Void>build());
    }

    @GetMapping("/reposts/user/{profileId}")
    public Flux<PostResponse> getUserReposts(@PathVariable UUID profileId, Authentication auth) {
        UUID userId = auth != null ? (UUID) auth.getPrincipal() : null;
        return postService.getUserReposts(profileId, userId);
    }
}