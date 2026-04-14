package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgument() {
        ResponseEntity<Map<String, String>> res = handler.handleIllegalArgument(new IllegalArgumentException("msg"));
        assertEquals(400, res.getStatusCode().value());
        assertEquals("msg", res.getBody().get("message"));
    }

    @Test
    void handleIllegalState() {
        ResponseEntity<Map<String, String>> res = handler.handleIllegalState(new IllegalStateException("msg"));
        assertEquals(403, res.getStatusCode().value());
        assertEquals("msg", res.getBody().get("message"));
    }

    @Test
    void handleGeneralException() {
        ResponseEntity<Map<String, String>> res = handler.handleGeneralException(new Exception("msg"));
        assertEquals(500, res.getStatusCode().value());
        assertEquals("Terjadi kesalahan internal pada server.", res.getBody().get("message"));
    }
}
