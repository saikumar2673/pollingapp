package com.example.polling.poll;

import com.example.polling.user.AppUser;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "polls")
public class Poll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PollType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PollVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ResultsVisibility resultsVisibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PollStatus status = PollStatus.DRAFT;

    private Instant endAt;

    @Column(nullable = false, unique = true, length = 80)
    private String shareToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private AppUser creator;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PollOption> options = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant closedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isDraft() { return status == PollStatus.DRAFT; }
    public boolean isOpen() { return status == PollStatus.OPEN; }
    public boolean isClosed() { return status == PollStatus.CLOSED; }
    public boolean isPrivate() { return visibility == PollVisibility.PRIVATE; }

    public void replaceOptions(List<String> labels) {
        options.clear();
        for (int i = 0; i < labels.size(); i++) {
            PollOption option = new PollOption();
            option.setPoll(this);
            option.setLabel(labels.get(i));
            option.setPosition(i);
            options.add(option);
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PollType getType() { return type; }
    public void setType(PollType type) { this.type = type; }
    public PollVisibility getVisibility() { return visibility; }
    public void setVisibility(PollVisibility visibility) { this.visibility = visibility; }
    public ResultsVisibility getResultsVisibility() { return resultsVisibility; }
    public void setResultsVisibility(ResultsVisibility resultsVisibility) { this.resultsVisibility = resultsVisibility; }
    public PollStatus getStatus() { return status; }
    public void setStatus(PollStatus status) { this.status = status; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    public AppUser getCreator() { return creator; }
    public void setCreator(AppUser creator) { this.creator = creator; }
    public List<PollOption> getOptions() { return options; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
