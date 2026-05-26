package com.upsglam.service;

import com.upsglam.dto.response.CudaProcessingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Llama al servicio FastAPI/PyCUDA en http://cuda-service:8001
 * El nombre 'cuda-service' es el hostname del contenedor en Docker Compose.
 */
@Service
@RequiredArgsConstructor
public class CudaService {

    @Qualifier("cudaWebClient")
    private final WebClient cudaWebClient;

    /**
     * Envía la imagen al servicio CUDA y devuelve la respuesta con
     * la imagen procesada en base64 y las métricas GPU.
     */
    public Mono<CudaProcessingResponse> processImage(byte[] imageBytes,
                                                      String filterName) {
        var resource = new ByteArrayResource(imageBytes) {
            @Override public String getFilename() { return "image.jpg"; }
        };

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", resource).contentType(MediaType.IMAGE_JPEG);
        builder.part("filter_name", filterName);

        return cudaWebClient.post()
                .uri("/filter")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(CudaProcessingResponse.class);
    }
}
