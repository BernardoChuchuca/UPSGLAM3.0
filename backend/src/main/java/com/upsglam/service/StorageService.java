package com.upsglam.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Sube imágenes a Supabase Storage y devuelve la URL pública.
 * Llama directamente a la REST API de Supabase — no hay SDK Java oficial.
 */
@Service
@RequiredArgsConstructor
public class StorageService {

    @Qualifier("supabaseWebClient")
    private final WebClient supabaseWebClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.storage.bucket-originals}")
    private String bucketOriginals;

    @Value("${supabase.storage.bucket-processed}")
    private String bucketProcessed;

    /** Sube la imagen original y devuelve su URL pública. */
    public Mono<String> uploadOriginal(byte[] imageBytes, String contentType) {
        String path = UUID.randomUUID() + ".jpg";
        return upload(bucketOriginals, path, imageBytes, contentType)
                .map(p -> buildPublicUrl(bucketOriginals, p));
    }

    /** Sube la imagen procesada (decodificada de base64) y devuelve su URL pública. */
    public Mono<String> uploadProcessed(byte[] imageBytes) {
        String path = UUID.randomUUID() + ".jpg";
        return upload(bucketProcessed, path, imageBytes, "image/jpeg")
                .map(p -> buildPublicUrl(bucketProcessed, p));
    }

    private Mono<String> upload(String bucket, String path,
                                byte[] bytes, String contentType) {
        var resource = new ByteArrayResource(bytes) {
            @Override public String getFilename() { return path; }
        };

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", resource)
               .contentType(MediaType.parseMediaType(contentType));

        return supabaseWebClient.post()
                .uri("/storage/v1/object/{bucket}/{path}", bucket, path)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(String.class)
                .thenReturn(path);
    }

    private String buildPublicUrl(String bucket, String path) {
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }
}
