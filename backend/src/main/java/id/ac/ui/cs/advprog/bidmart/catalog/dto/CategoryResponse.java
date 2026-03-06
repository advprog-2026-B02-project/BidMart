package id.ac.ui.cs.advprog.bidmart.catalog.dto;

import id.ac.ui.cs.advprog.bidmart.catalog.model.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CategoryResponse {

    private UUID id;
    private String name;
    private String slug;
    private UUID parentId;
    private List<CategoryResponse> children;

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getChildren().stream()
                        .map(CategoryResponse::from)
                        .toList()
        );
    }

    public static CategoryResponse flat(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getId() : null,
                List.of()
        );
    }
}