package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.CreateCategoryRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse create(CreateCategoryRequest request);

    CategoryResponse findById(UUID id);

    List<CategoryResponse> findRoots();

    void delete(UUID id);
}