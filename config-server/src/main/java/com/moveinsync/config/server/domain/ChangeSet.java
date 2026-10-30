package com.moveinsync.config.server.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An atomic versioned collection of setting modifications published together.
 */
@Entity
@Table(name = "change_sets", indexes = {
        @Index(name = "idx_changeset_tenant_ver", columnList = "tenant_id, version")
})
public class ChangeSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "changeSet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ChangeSetItem> items = new ArrayList<>();

    // Default constructor for JPA.
    public ChangeSet() {}

    // Constructs a change set with tenant, version, author, and reason.
    public ChangeSet(String tenantId, Long version, String author, String reason) {
        this.tenantId = tenantId;
        this.version = version;
        this.author = author;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    // Adds a change item to this atomic change set.
    public void addItem(ChangeSetItem item) {
        items.add(item);
        item.setChangeSet(this);
    }

    // Returns the database ID.
    public Long getId() {
        return id;
    }

    // Returns the target tenant identifier.
    public String getTenantId() {
        return tenantId;
    }

    // Returns the version number of this atomic change set.
    public Long getVersion() {
        return version;
    }

    // Returns the author of this change set.
    public String getAuthor() {
        return author;
    }

    // Returns the reason documented for this change set.
    public String getReason() {
        return reason;
    }

    // Returns the creation timestamp.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Returns the list of changed items in this change set.
    public List<ChangeSetItem> getItems() {
        return items;
    }
}
