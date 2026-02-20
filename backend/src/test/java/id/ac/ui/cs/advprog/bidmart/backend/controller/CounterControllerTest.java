package id.ac.ui.cs.advprog.bidmart.backend.controller;

import id.ac.ui.cs.advprog.bidmart.backend.model.PageView;
import id.ac.ui.cs.advprog.bidmart.backend.repository.CounterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CounterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CounterRepository repo;

    @Test
    void testGetAndIncrement() throws Exception {
        PageView view = new PageView("main_page", 5);
        when(repo.findById("main_page")).thenReturn(Optional.of(view));
        when(repo.save(any(PageView.class))).thenReturn(view);

        mockMvc.perform(get("/api/counter"))
                .andExpect(status().isOk())
                .andExpect(content().string("6"));
    }
}