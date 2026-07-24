package br.ufmt.periscope.api.dto;

import java.util.List;

public record PageDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageDTO<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);
        return new PageDTO<>(content, page, size, totalElements, totalPages);
    }
}
