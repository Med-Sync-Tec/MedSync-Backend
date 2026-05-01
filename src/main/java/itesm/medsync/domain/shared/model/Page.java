package itesm.medsync.domain.shared.model;

import java.util.List;

public record Page<T>(List<T> items, long total, int page, int size) {

    public static <T> Page<T> of(List<T> items, long total, int page, int size) {
        return new Page<>(List.copyOf(items), total, page, size);
    }
}
