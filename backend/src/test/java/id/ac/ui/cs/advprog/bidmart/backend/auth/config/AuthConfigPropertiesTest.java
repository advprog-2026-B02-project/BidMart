package id.ac.ui.cs.advprog.bidmart.backend.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AuthConfigPropertiesTest {

    @Test
    void authProperties_GetterSetter() {
        AuthProperties props = new AuthProperties();
        props.setSecret("secret");
        props.setAccessTokenExpiration(1000L);
        props.setRefreshTokenExpiration(2000L);

        assertEquals("secret", props.getSecret());
        assertEquals(1000L, props.getAccessTokenExpiration());
        assertEquals(2000L, props.getRefreshTokenExpiration());
    }

    @Test
    void appProperties_GetterSetter() {
        AppProperties props = new AppProperties();
        props.setBaseUrl("http://backend.local");
        props.setFrontendUrl("http://frontend.local");

        assertEquals("http://backend.local", props.getBaseUrl());
        assertEquals("http://frontend.local", props.getFrontendUrl());
    }
}
