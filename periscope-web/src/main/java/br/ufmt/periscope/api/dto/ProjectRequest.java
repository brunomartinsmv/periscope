package br.ufmt.periscope.api.dto;

public record ProjectRequest(
        String title,
        String description,
        Boolean isPublic
) {
}
