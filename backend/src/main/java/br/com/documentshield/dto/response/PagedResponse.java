package br.com.documentshield.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record PagedResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last) {
    public static <S, T> PagedResponse<T> from(Page<S> p, java.util.function.Function<S, T> f) {
        return new PagedResponse<>(
                p.map(f).getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.isFirst(),
                p.isLast());
    }
}
