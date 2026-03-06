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
public class WalletResponse {

    private UUID userId;
    private long availableBalance;
    private long heldBalance;
    private long totalBalance;
    private LocalDateTime updatedAt;
}