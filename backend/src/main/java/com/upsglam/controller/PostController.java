package com.upsglam.controller;

import com.upsglam.dto.request.CreatePostRequest;
import com.upsglam.dto.response.PostResponse;
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
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // CORS libre para tu Ionic
public class PostController {
    private final PostService postService;

    @GetMapping("/feed")
    public Flux<Map<String, Object>> getFeed(Authentication auth) {
        // HACK SUPREMO: Creamos mapas genéricos que Spring Boot convertirá automáticamente a JSON
        // Esto se salta cualquier restricción de constructores de PostResponse
        
        Map<String, Object> post1 = new HashMap<>();
        post1.put("id", UUID.randomUUID().toString());
        post1.put("username", "paul_art");
        post1.put("description", "Filtro Laplacian acelerado en GPU con PyCUDA 🚀");
        post1.put("imageUrl", "https://picsum.photos/id/10/600/400");
        post1.put("mimeType", "image/jpeg");
        post1.put("likesCount", 124);
        post1.put("likedByCurrentUser", false);
        post1.put("createdAt", java.time.OffsetDateTime.now().toString());

        Map<String, Object> post2 = new HashMap<>();
        post2.put("id", UUID.randomUUID().toString());
        post2.put("username", "ana_dev");
        post2.put("description", "Servidor reactivo con Spring WebFlux volando de forma no bloqueante.");
        post2.put("imageUrl", "https://picsum.photos/id/20/600/400");
        post2.put("mimeType", "image/jpeg");
        post2.put("likesCount", 89);
        post2.put("likedByCurrentUser", true);
        post2.put("createdAt", java.time.OffsetDateTime.now().toString());

        return Flux.just(post1, post2);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<PostResponse>> createPost(
            @RequestPart("data") @Valid CreatePostRequest req,
            @RequestPart("image") FilePart image,
            Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
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
}