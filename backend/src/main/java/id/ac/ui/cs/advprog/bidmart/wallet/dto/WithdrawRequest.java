package id.ac.ui.cs.advprog.bidmart.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawRequest {

    @Min(value = 10000, message = "Minimum withdrawal amount is 10,000")
    private long amount;

    @NotBlank(message = "Bank code is required")
    private String bankCode;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Account name is required")
    private String accountName;
}
