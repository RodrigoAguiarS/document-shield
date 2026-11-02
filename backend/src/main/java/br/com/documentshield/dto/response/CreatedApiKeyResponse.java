package br.com.documentshield.dto.response;

public record CreatedApiKeyResponse(AdminApiKeyResponse apiKey, String token) {}
