package id.ac.ui.cs.advprog.bidmart.bidding.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BidRequestDTO {
    @NotNull(message = "jumlah penawaran tidak boleh kosong")
    @Positive(message = "jumlah penawaran harus bernilai positif")
    private BigDecimal amount;
}