package br.ufmt.periscope.api.dto;

public record UserRequest(
        String username,
        String password,
        String firstname,
        String lastname,
        String email,
        String userLevel
) {
}
