package com.upsglam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * WebClient para el servicio CUDA/PyCUDA.
     * Usa el nombre del contenedor Docker — nunca localhost.
     */
    @Bean("cudaWebClient")
    public WebClient cudaWebClient(
            @Value("${cuda.service-url}") String cudaServiceUrl) {
        return WebClient.builder()
                .baseUrl(cudaServiceUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)) // 100 MB
                .build();
    }

    /**
     * WebClient para la REST API de Supabase Storage.
     * Incluye la Service Key para operaciones administrativas (subir imágenes).
     */
    @Bean("supabaseWebClient")
    public WebClient supabaseWebClient(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-key}") String serviceKey) {
        return WebClient.builder()
                .baseUrl(supabaseUrl)
                .defaultHeader("apikey", serviceKey)
                .defaultHeader("Authorization", "Bearer " + serviceKey)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)) // 100 MB
                .build();
    }
}
