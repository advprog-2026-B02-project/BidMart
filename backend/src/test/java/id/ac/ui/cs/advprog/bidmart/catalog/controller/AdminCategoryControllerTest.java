package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.CategoryResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.CreateCategoryRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private AdminCategoryController controller;

    @Test
    void findAll() {
        when(categoryService.findRoots()).thenReturn(Collections.emptyList());
        ResponseEntity<List<CategoryResponse>> res = controller.findAll();
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
    }

    @Test
    void findById() {
        UUID id = UUID.randomUUID();
        CategoryResponse cr = mock(CategoryResponse.class);
        when(categoryService.findById(id)).thenReturn(cr);
        
        ResponseEntity<CategoryResponse> res = controller.findById(id);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void create() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        CategoryResponse cr = mock(CategoryResponse.class);
        when(categoryService.create(req)).thenReturn(cr);

        ResponseEntity<CategoryResponse> res = controller.create(req);
        assertEquals(201, res.getStatusCode().value());
    }

    @Test
    void deleteCategory() {
        UUID id = UUID.randomUUID();
        doNothing().when(categoryService).delete(id);

        ResponseEntity<Void> res = controller.delete(id);
        assertEquals(204, res.getStatusCode().value());
    }
}
