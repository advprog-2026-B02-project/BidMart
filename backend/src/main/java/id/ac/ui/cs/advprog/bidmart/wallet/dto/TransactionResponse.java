package id.ac.ui.cs.advprog.bidmart.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID id;
    private String type;
    private long amount;
    private String description;
    private UUID referenceId;
    private long balanceAfter;
    private LocalDateTime createdAt;
}
