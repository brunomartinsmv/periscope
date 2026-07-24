package br.ufmt.periscope.api.dto;

public record PatentUpdateRequest(
        String title,
        String abstractText,
        Boolean blacklisted,
        Boolean completed
) {
}
