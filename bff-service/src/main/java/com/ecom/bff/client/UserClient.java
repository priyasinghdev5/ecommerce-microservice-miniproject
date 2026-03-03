package com.ecom.bff.client;
import com.ecom.bff.dto.UserProfileDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

    private final WebClient.Builder webClientBuilder;

    public Mono<UserProfileDto> getProfile(String userId, String token) {
        return webClientBuilder.baseUrl("http://user-service").build()
                .get().uri("/api/users/profile")
                .header("X-User-Id", userId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(ProfileResponse.class)
                .map(r -> r.data() != null ? r.data() : UserProfileDto.empty())
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(e -> {
                    log.warn("user-service unavailable: {}", e.getMessage());
                    return Mono.just(UserProfileDto.empty());
                });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProfileResponse(boolean success, UserProfileDto data) {}
}
