package com.upsglam.service;
import com.upsglam.model.Like;
import com.upsglam.repository.LikeRepository;
import com.upsglam.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.OffsetDateTime;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepo;
    private final ProfileRepository profileRepo;
    public Mono<Void> toggleLike(UUID authUserId, UUID postId) {
        return profileRepo.findByAuthUserId(authUserId).flatMap(profile ->
            likeRepo.findByPostIdAndUserId(postId, profile.getId())
                .flatMap(like -> likeRepo.delete(like))
                .switchIfEmpty(
                    likeRepo.save(Like.builder().postId(postId)
                        .userId(profile.getId()).createdAt(OffsetDateTime.now()).build())
                    .then()
                )
        );
    }
    public Mono<Long> getLikeCount(UUID postId) {
        return likeRepo.countByPostId(postId);
    }
}
