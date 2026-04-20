package id.ac.ui.cs.advprog.bidmart.backend.auth.security;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@SpringBootTest(classes = {SecurityConfig.class, SecurityConfigTest.TestConfig.class, SecurityConfigTest.DummyController.class})
@AutoConfigureMockMvc
@Import(SecurityConfigTest.TestConfig.class)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class
})
@TestPropertySource(properties = "app.frontend.url=http://frontend.test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityConfig securityConfig;

    @Test
    void corsConfigurationSource_ContainsFrontendAndLocalhost() {
        ReflectionTestUtils.setField(securityConfig, "frontendUrl", "http://frontend.test");

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration cfg = source.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertTrue(cfg.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(cfg.getAllowedOrigins().contains("http://frontend.test"));
        assertEquals(List.of("Authorization"), cfg.getExposedHeaders());
    }

    @Test
    void securityRules_PublicAndProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
        mockMvc.perform(options("/anything"));

        mockMvc.perform(get("/me")).andExpect(status().isForbidden());
        mockMvc.perform(put("/me").with(user("u").roles("BUYER"))).andExpect(status().isOk());

        mockMvc.perform(get("/admin/x").with(user("u").roles("BUYER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/x").with(user("u").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Configuration
    static class TestConfig {
        @Bean
        JwtAuthFilter jwtAuthFilter() {
            id.ac.ui.cs.advprog.bidmart.backend.auth.config.AuthProperties props =
                    new id.ac.ui.cs.advprog.bidmart.backend.auth.config.AuthProperties();
            props.setSecret("my-super-secret-key-that-is-at-least-32-bytes");
            props.setAccessTokenExpiration(3600000L);
            props.setRefreshTokenExpiration(7200000L);
            return new JwtAuthFilter(props);
        }
    }

    @RestController
    @RequestMapping
    static class DummyController {
        @GetMapping("/health")
        public Map<String, String> health() {
            return Map.of("status", "ok");
        }

        @PostMapping("/auth/login")
        public Map<String, String> login() {
            return Map.of("status", "ok");
        }

        @GetMapping("/me")
        public Map<String, String> me() {
            return Map.of("status", "ok");
        }

        @PutMapping("/me")
        public Map<String, String> mePut() {
            return Map.of("status", "ok");
        }

        @GetMapping("/admin/x")
        public Map<String, String> admin() {
            return Map.of("status", "ok");
        }
    }
}
