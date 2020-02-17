package com.example.apigateway;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfiguration {
    private final StringKeyGenerator secureKeyGenerator = new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveClientRegistrationRepository clientRegistrationRepository) {
        ServerOAuth2AuthorizationRequestResolver resolver = new DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrationRepository) {
            @Override
            public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange) {
                return customOAuth2AuthorizationRequest(super.resolve(exchange));
            }

            @Override
            public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange, String clientRegistrationId) {
                return customOAuth2AuthorizationRequest(super.resolve(exchange, clientRegistrationId));
            }
        };

        http
            .authorizeExchange()
            .anyExchange()
            .authenticated()

            .and()
            .oauth2Login()
            .authorizationRequestResolver(resolver)

            .and()
            .oauth2ResourceServer()
            .jwt();

        return http.build();
    }

    @Bean
    CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of("*"));
        corsConfig.setMaxAge(3600L);
        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    private Mono<OAuth2AuthorizationRequest> customOAuth2AuthorizationRequest(Mono<OAuth2AuthorizationRequest> requestMono) {
        return requestMono.map(request -> {
            Map<String, Object> attributes = new HashMap<>(request.getAttributes());
            Map<String, Object> additionalParameters = new HashMap<>(request.getAdditionalParameters());
            addPkceParameters(attributes, additionalParameters);
            return OAuth2AuthorizationRequest.from(request)
                .attributes(attributes)
                .additionalParameters(additionalParameters)
                .build();
        });
    }

    private void addPkceParameters(Map<String, Object> attributes, Map<String, Object> additionalParameters) {
        String codeVerifier = this.secureKeyGenerator.generateKey();
        attributes.put(PkceParameterNames.CODE_VERIFIER, codeVerifier);
        try {
            String codeChallenge = createHash(codeVerifier);
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE, codeChallenge);
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256");
        } catch (NoSuchAlgorithmException e) {
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE, codeVerifier);
        }
    }

    private static String createHash(String value) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(value.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
