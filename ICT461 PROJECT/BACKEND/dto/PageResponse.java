package zm.ac.mu.registry.dto;

import java.util.List;

public record PageREsponse<T>(
       List<T> content,
       int page,
       int size,
       long totalElements) {}