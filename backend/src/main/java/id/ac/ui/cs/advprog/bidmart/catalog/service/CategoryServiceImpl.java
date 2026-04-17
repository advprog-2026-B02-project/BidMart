package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.CreateCategoryRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.CategoryResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.CategoryRepository;
import id.ac.ui.cs.advprog.bidmart.catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Slug sudah digunakan: " + request.getSlug());
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Parent category tidak ditemukan: " + request.getParentId()));
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .parent(parent)
                .build();

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        return categoryRepository.findById(id)
                .map(CategoryResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category tidak ditemukan: " + id));
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category tidak ditemukan: " + id));

        if (!category.getChildren().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tidak bisa menghapus kategori yang masih punya sub-kategori.");
        }

        categoryRepository.delete(category);
    }
}