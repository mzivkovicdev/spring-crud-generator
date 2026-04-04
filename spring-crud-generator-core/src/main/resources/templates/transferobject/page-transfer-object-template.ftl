import java.util.List;

public record PageTO<T>(
    int totalPages, long totalElements, int size, int number, List<T> content
) {
    
}
