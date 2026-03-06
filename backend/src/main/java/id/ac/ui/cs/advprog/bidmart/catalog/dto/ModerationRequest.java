package id.ac.ui.cs.advprog.bidmart.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ModerationRequest {

    public enum Action { APPROVE, REJECT, DELETE }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModerateListingRequest {

        private Action action;

        private String reason;
    }
}
