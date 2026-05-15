package com.example.polling.invitation;

import com.example.polling.poll.Poll;
import com.example.polling.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "invitations", uniqueConstraints = {
    @UniqueConstraint(name = "uk_invitation_poll_user", columnNames = {"poll_id", "invited_user_id"})
})
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_user_id", nullable = false)
    private AppUser invitedUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_id", nullable = false)
    private AppUser invitedBy;

    @Column(nullable = false, updatable = false)
    private Instant invitedAt;

    @PrePersist
    void prePersist() {
        invitedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Poll getPoll() { return poll; }
    public void setPoll(Poll poll) { this.poll = poll; }
    public AppUser getInvitedUser() { return invitedUser; }
    public void setInvitedUser(AppUser invitedUser) { this.invitedUser = invitedUser; }
    public AppUser getInvitedBy() { return invitedBy; }
    public void setInvitedBy(AppUser invitedBy) { this.invitedBy = invitedBy; }
    public Instant getInvitedAt() { return invitedAt; }
}
