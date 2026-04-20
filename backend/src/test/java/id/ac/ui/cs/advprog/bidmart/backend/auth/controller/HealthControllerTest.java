package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    void health() {
        ResponseEntity<String> response = controller.health();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("OK", response.getBody());
    }
}
