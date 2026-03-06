package id.ac.ui.cs.advprog.bidmart.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication(scanBasePackages = "id.ac.ui.cs.advprog.bidmart")
@EnableJpaRepositories(basePackages = "id.ac.ui.cs.advprog.bidmart")
@EntityScan(basePackages = "id.ac.ui.cs.advprog.bidmart")
public class BackendApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));

        SpringApplication.run(BackendApplication.class, args);
    }

}
