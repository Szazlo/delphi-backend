package com.davidwilson.delphi.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class SecurityController {

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;
    @Value("${spring.security.oauth2.client.provider.keycloak.token-uri}")
    private String tokenUri;
    @Value("${keycloak.registerUri}")
    private String registerUri;
    private String clientToken;

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> login(@RequestParam Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "password");
        body.add("username", username);
        body.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(tokenUri, request, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return ResponseEntity.ok(response.getBody());
        } else {
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        }
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> register(@RequestBody Map<String, Object> registerRequest) {
        RestTemplate restTemplate = new RestTemplate();
        String accessToken = getCurrentAccessToken();

        // Forward the register request to Keycloak
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(registerRequest, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(registerUri, request, String.class);

        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            // Obtain a new bearer token
            accessToken = obtainNewAccessToken();
            headers.setBearerAuth(accessToken);
            request = new HttpEntity<>(registerRequest, headers);
            response = restTemplate.postForEntity(registerUri, request, String.class);
        }

        if (response.getStatusCode() == HttpStatus.OK) {
            return ResponseEntity.ok(response.getBody());
        }
        else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.getBody());
        } else {
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        }
    }

    private String getCurrentAccessToken() {
        if (clientToken == null) {
            return obtainNewAccessToken();
        }
        return clientToken;
    }

    private String obtainNewAccessToken() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> tokenBody = new LinkedMultiValueMap<>();
        tokenBody.add("client_id", clientId);
        tokenBody.add("client_secret", clientSecret);
        tokenBody.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenBody, tokenHeaders);
        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUri, tokenRequest, Map.class);

        if (tokenResponse.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to obtain access token");
        }
        clientToken = (String) tokenResponse.getBody().get("access_token");
        return clientToken;
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateUser(@PathVariable String id, @RequestBody Map<String, Object> updateRequest) {
        RestTemplate restTemplate = new RestTemplate();
        String accessToken = getCurrentAccessToken();

        // Forward the update request to Keycloak
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(updateRequest, headers);
        String updateUri = registerUri + "/" + id;
        ResponseEntity<String> response = restTemplate.exchange(updateUri, HttpMethod.PUT, request, String.class);

        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            // Obtain a new bearer token
            accessToken = obtainNewAccessToken();
            headers.setBearerAuth(accessToken);
            request = new HttpEntity<>(updateRequest, headers);
            response = restTemplate.exchange(updateUri, HttpMethod.PUT, request, String.class);
        }

        if (response.getStatusCode() == HttpStatus.OK) {
            return ResponseEntity.ok(response.getBody());
        } else {
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        }
    }
}