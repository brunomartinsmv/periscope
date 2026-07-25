package br.ufmt.periscope.api.dto;

import java.util.List;

public record ImportResultDTO(int imported, String importer, String fileName, List<String> messages) {
}
