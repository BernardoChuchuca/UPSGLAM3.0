package com.upsglam.service;
import com.upsglam.dto.request.CreateCommentRequest;
import com.upsglam.dto.response.CommentResponse;
import com.upsglam.dto.response.ProfileResponse;
import com.upsglam.model.Comment;
import com.upsglam.repository.CommentRepository;
import com.upsglam.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.OffsetDateTime;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepo;
    private final ProfileRepository profileRepo;
    public Mono<CommentResponse> addComment(UUID authUserId, UUID postId, CreateCommentRequest req) {
        return profileRepo.findByAuthUserId(authUserId).flatMap(profile ->
            commentRepo.save(Comment.builder().postId(postId).userId(profile.getId())
                .content(req.getContent()).createdAt(OffsetDateTime.now()).build())
            .flatMap(c -> profileRepo.findById(c.getUserId()).map(p ->
                CommentResponse.builder().id(c.getId()).content(c.getContent())
                    .createdAt(c.getCreatedAt())
                    .author(ProfileResponse.builder().id(p.getId()).username(p.getUsername())
                        .avatarUrl(p.getAvatarUrl()).build())
                    .build())));
    }
    public Flux<CommentResponse> getComments(UUID postId) {
        return commentRepo.findByPostIdOrderByCreatedAtAsc(postId)
            .flatMap(c -> profileRepo.findById(c.getUserId()).map(p ->
                CommentResponse.builder().id(c.getId()).content(c.getContent())
                    .createdAt(c.getCreatedAt())
                    .author(ProfileResponse.builder().id(p.getId()).username(p.getUsername())
                        .avatarUrl(p.getAvatarUrl()).build())
                    .build()));
    }
    public Mono<Void> deleteComment(UUID authUserId, UUID commentId) {
        return profileRepo.findByAuthUserId(authUserId).flatMap(profile ->
            commentRepo.findById(commentId)
                .filter(c -> c.getUserId().equals(profile.getId()))
                .flatMap(commentRepo::delete));
    }
}
