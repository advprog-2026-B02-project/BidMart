package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.request.CreateCategoryRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.response.CategoryResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.CategoryRepository;
import id.ac.ui.cs.advprog.bidmart.catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Slug already has been used: " + request.slug());
        }

        Category parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Parent category not found: " + request.parentId()));
        }

        Category category = Category.builder()
                .name(request.name())
                .slug(request.slug())
                .parent(parent)
                .build();

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category not found: " + id));
        return CategoryResponse.from(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findRoots() {
        return categoryRepository.findByParentIsNull()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category not found: " + id));

        if (!category.getChildren().isEmpty()) {
            throw new IllegalStateException("Cannot delete category that has sub-category");
        }

        categoryRepository.delete(category);
    }
}