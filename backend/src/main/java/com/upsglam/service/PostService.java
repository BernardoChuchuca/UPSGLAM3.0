package com.upsglam.service;

import com.upsglam.dto.request.CreatePostRequest;
import com.upsglam.dto.response.CudaProcessingResponse;
import com.upsglam.dto.response.PostResponse;
import com.upsglam.dto.response.ProfileResponse;
import com.upsglam.model.GpuMetrics;
import com.upsglam.model.Post;
import com.upsglam.model.ProcessingHistory;
import com.upsglam.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Orquesta el flujo completo de una publicación:
 * 1. Sube imagen original a Supabase Storage
 * 2. Llama al servicio CUDA y obtiene imagen procesada + métricas
 * 3. Sube imagen procesada a Supabase Storage
 * 4. Persiste processing_history y gpu_metrics
 * 5. Persiste el post
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository              postRepo;
    private final ProfileRepository           profileRepo;
    private final FilterRepository            filterRepo;
    private final LikeRepository              likeRepo;
    private final ProcessingHistoryRepository historyRepo;
    private final GpuMetricsRepository        metricsRepo;
    private final CudaService                 cudaService;
    private final StorageService              storageService;

    // ─────────────────────────────────────────────────────────────
    // Crear post con procesamiento GPU
    // ─────────────────────────────────────────────────────────────
    public Mono<PostResponse> createPost(UUID authUserId,
                                          CreatePostRequest req,
                                          byte[] imageBytes,
                                          String contentType) {
        return profileRepo.findByAuthUserId(authUserId)
            .flatMap(profile ->
                filterRepo.findByName(req.getFilterName())
                    .flatMap(filter ->

                        // 1. Subir imagen original
                        storageService.uploadOriginal(imageBytes, contentType)
                            .flatMap(originalUrl ->

                                // 2. Procesar en GPU
                                cudaService.processImage(imageBytes, req.getFilterName())
                                    .flatMap(cudaResp -> {

                                        byte[] processedBytes = Base64.getDecoder()
                                                .decode(cudaResp.getImageBase64());

                                        // 3. Subir imagen procesada
                                        return storageService.uploadProcessed(processedBytes)
                                            .flatMap(processedUrl -> {

                                                // 4. Guardar processing_history
                                                ProcessingHistory history = ProcessingHistory.builder()
                                                    .userId(profile.getId())
                                                    .filterId(filter.getId())
                                                    .originalImageUrl(originalUrl)
                                                    .processedImageUrl(processedUrl)
                                                    .status(cudaResp.getStatus())
                                                    .createdAt(OffsetDateTime.now())
                                                    .build();

                                                return historyRepo.save(history)
                                                    .flatMap(savedHistory -> {

                                                        // 5. Guardar gpu_metrics
                                                        GpuMetrics metrics = buildMetrics(
                                                                savedHistory.getId(), cudaResp);

                                                        return metricsRepo.save(metrics)
                                                            .then(
                                                                // 6. Guardar post
                                                                postRepo.save(Post.builder()
                                                                    .userId(profile.getId())
                                                                    .filterId(filter.getId())
                                                                    .caption(req.getCaption())
                                                                    .originalImageUrl(originalUrl)
                                                                    .processedImageUrl(processedUrl)
                                                                    .createdAt(OffsetDateTime.now())
                                                                    .build())
                                                            )
                                                            .flatMap(post -> buildPostResponse(
                                                                    post, profile, filter.getName(),
                                                                    authUserId));
                                                    });
                                            });
                                    })
                            )
                    )
            );
    }

    // ─────────────────────────────────────────────────────────────
    // Previsualización de filtro GPU
    // ─────────────────────────────────────────────────────────────
    public Mono<CudaProcessingResponse> previewFilter(String filterName, byte[] imageBytes) {
        return cudaService.processImage(imageBytes, filterName);
    }

    // ─────────────────────────────────────────────────────────────
    // Feed público
    // ─────────────────────────────────────────────────────────────
    public Flux<PostResponse> getFeed(UUID authUserId) {
        return postRepo.findAllByOrderByCreatedAtDesc()
                .flatMap(post -> buildPostResponse(post, null, null, authUserId));
    }

    // ─────────────────────────────────────────────────────────────
    // Posts de un usuario (perfil)
    // ─────────────────────────────────────────────────────────────
    public Flux<PostResponse> getUserPosts(UUID profileId, UUID authUserId) {
        return postRepo.findByUserIdOrderByCreatedAtDesc(profileId)
                .flatMap(post -> buildPostResponse(post, null, null, authUserId));
    }

    // ─────────────────────────────────────────────────────────────
    // Eliminar post (solo el dueño)
    // ─────────────────────────────────────────────────────────────
    public Mono<Void> deletePost(UUID authUserId, UUID postId) {
        return profileRepo.findByAuthUserId(authUserId)
                .flatMap(profile -> postRepo.findById(postId)
                        .filter(post -> post.getUserId().equals(profile.getId()))
                        .flatMap(postRepo::delete));
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers privados
    // ─────────────────────────────────────────────────────────────
    private Mono<PostResponse> buildPostResponse(Post post, Object ignoredProfile,
                                                   Object ignoredFilter, UUID authUserId) {
        Mono<ProfileResponse> authorMono = profileRepo.findById(post.getUserId())
                .map(p -> ProfileResponse.builder()
                        .id(p.getId()).username(p.getUsername())
                        .avatarUrl(p.getAvatarUrl()).bio(p.getBio())
                        .createdAt(p.getCreatedAt()).build());

        Mono<Long>    likeCountMono = likeRepo.countByPostId(post.getId());
        Mono<String>  filterNameMono = filterRepo.findById(post.getFilterId())
                .map(f -> f.getName()).defaultIfEmpty("unknown");

        Mono<Boolean> likedMono = authUserId == null
                ? Mono.just(false)
                : profileRepo.findByAuthUserId(authUserId)
                    .flatMap(p -> likeRepo.existsByPostIdAndUserId(post.getId(), p.getId()))
                    .defaultIfEmpty(false);

        return Mono.zip(authorMono, likeCountMono, filterNameMono, likedMono)
                .map(t -> PostResponse.builder()
                        .id(post.getId())
                        .caption(post.getCaption())
                        .originalImageUrl(post.getOriginalImageUrl())
                        .processedImageUrl(post.getProcessedImageUrl())
                        .filterName(t.getT3())
                        .author(t.getT1())
                        .likeCount(t.getT2())
                        .likedByMe(t.getT4())
                        .createdAt(post.getCreatedAt())
                        .build());
    }

    private GpuMetrics buildMetrics(UUID processingId, CudaProcessingResponse r) {
        return GpuMetrics.builder()
                .processingId(processingId)
                .blockDim(r.getBlockDim())
                .gridDim(r.getGridDim())
                .totalThreads(r.getTotalThreads())
                .kernelTimeMs(r.getKernelTimeMs())
                .imageWidth(r.getWidth())
                .imageHeight(r.getHeight())
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
