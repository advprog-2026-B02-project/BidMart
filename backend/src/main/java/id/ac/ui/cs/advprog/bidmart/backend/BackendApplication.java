package id.ac.ui.cs.advprog.bidmart.backend;

import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AppProperties;
import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AuthProperties;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@EnableConfigurationProperties({AuthProperties.class, AppProperties.class})
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();

        Map<String, Object> defaults = new HashMap<>();
        dotenv.entries().forEach(e -> {
            // jangan override env beneran kalau sudah ada
            if (System.getenv(e.getKey()) == null && System.getProperty(e.getKey()) == null) {
                defaults.put(e.getKey(), e.getValue());
            }
        });

        SpringApplication app = new SpringApplication(BackendApplication.class);
        app.setDefaultProperties(defaults);
        app.run(args);
    }
}