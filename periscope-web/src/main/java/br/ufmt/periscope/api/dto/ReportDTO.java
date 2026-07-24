package br.ufmt.periscope.api.dto;

import java.util.List;

public record ReportDTO(String name, String label, List<ReportItemDTO> items) {
}
