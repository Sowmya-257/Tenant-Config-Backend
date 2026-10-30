package com.moveinsync.config.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request payload for submitting an atomic batch of configuration changes.
 */
public class ChangeSetRequest {

    @NotBlank(message = "Author is required")
    private String author;

    @NotBlank(message = "Reason for configuration change is required")
    private String reason;

    @NotEmpty(message = "Change set must contain at least one item")
    @Valid
    private List<ChangeSetItemRequest> items;

    // Default constructor.
    public ChangeSetRequest() {}

    // Constructs a change set request with author, reason, and items.
    public ChangeSetRequest(String author, String reason, List<ChangeSetItemRequest> items) {
        this.author = author;
        this.reason = reason;
        this.items = items;
    }

    // Returns the author of this change set.
    public String getAuthor() {
        return author;
    }

    // Sets author.
    public void setAuthor(String author) {
        this.author = author;
    }

    // Returns reason for this change.
    public String getReason() {
        return reason;
    }

    // Sets reason.
    public void setReason(String reason) {
        this.reason = reason;
    }

    // Returns list of items in this atomic change set.
    public List<ChangeSetItemRequest> getItems() {
        return items;
    }

    // Sets items.
    public void setItems(List<ChangeSetItemRequest> items) {
        this.items = items;
    }
}
