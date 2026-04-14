package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.CategoryResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.CreateCategoryRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void create_SlugConflict() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setSlug("slug");
        req.setName("name");

        when(categoryRepository.existsBySlug("slug")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> categoryService.create(req));
    }

    @Test
    void create_ParentNotFound() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setSlug("slug");
        req.setName("name");
        req.setParentId(UUID.randomUUID());

        when(categoryRepository.existsBySlug("slug")).thenReturn(false);
        when(categoryRepository.findById(req.getParentId())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> categoryService.create(req));
    }

    @Test
    void create_Success() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setSlug("slug");
        req.setName("name");

        when(categoryRepository.existsBySlug("slug")).thenReturn(false);
        
        Category saved = new Category();
        saved.setId(UUID.randomUUID());
        saved.setName("name");
        saved.setSlug("slug");

        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse res = categoryService.create(req);
        assertNotNull(res);
        assertEquals("name", res.getName());
    }

    @Test
    void create_WithParent_Success() {
        UUID parentId = UUID.randomUUID();
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setSlug("slug");
        req.setName("name");
        req.setParentId(parentId);

        when(categoryRepository.existsBySlug("slug")).thenReturn(false);
        
        Category parent = new Category();
        parent.setId(parentId);
        
        Category saved = new Category();
        saved.setId(UUID.randomUUID());
        saved.setName("name");
        saved.setSlug("slug");
        saved.setParent(parent);

        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse res = categoryService.create(req);
        assertNotNull(res);
        assertEquals("name", res.getName());
    }

    @Test
    void findById_NotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> categoryService.findById(id));
    }

    @Test
    void findById_Success() {
        UUID id = UUID.randomUUID();
        Category c = new Category();
        c.setId(id);
        c.setName("name");
        c.setSlug("slug");

        when(categoryRepository.findById(id)).thenReturn(Optional.of(c));
        
        CategoryResponse res = categoryService.findById(id);
        assertNotNull(res);
        assertEquals(id, res.getId());
    }

    @Test
    void findRoots() {
        when(categoryRepository.findByParentIsNull()).thenReturn(Collections.emptyList());
        List<CategoryResponse> res = categoryService.findRoots();
        assertTrue(res.isEmpty());
    }

    @Test
    void delete_NotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> categoryService.delete(id));
    }

    @Test
    void delete_HasChildren() {
        UUID id = UUID.randomUUID();
        Category c = new Category();
        c.setId(id);
        c.setChildren(List.of(new Category()));

        when(categoryRepository.findById(id)).thenReturn(Optional.of(c));
        assertThrows(ResponseStatusException.class, () -> categoryService.delete(id));
    }

    @Test
    void delete_Success() {
        UUID id = UUID.randomUUID();
        Category c = new Category();
        c.setId(id);
        c.setChildren(new ArrayList<>());

        when(categoryRepository.findById(id)).thenReturn(Optional.of(c));
        doNothing().when(categoryRepository).delete(c);
        
        categoryService.delete(id);
        verify(categoryRepository).delete(c);
    }
}
