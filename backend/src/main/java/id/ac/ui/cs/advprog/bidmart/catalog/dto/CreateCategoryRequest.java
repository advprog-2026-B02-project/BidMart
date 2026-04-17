package id.ac.ui.cs.advprog.bidmart.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCategoryRequest {

        @NotBlank
        @Size(max = 100)
        private String name;

        @NotBlank
        @Size(max = 120)
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug can only have lowercase alphabet, numbers, and hyphen"
        )
        private String slug;

        private UUID parentId;
}