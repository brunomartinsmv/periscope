package br.ufmt.periscope.api.dto;

public record LoginResponse(String token, UserDTO user) {
}
