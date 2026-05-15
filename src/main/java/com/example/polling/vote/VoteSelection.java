package com.example.polling.vote;

import com.example.polling.poll.PollOption;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "vote_selections", uniqueConstraints = {
    @UniqueConstraint(name = "uk_vote_selection_vote_option", columnNames = {"vote_id", "option_id"})
})
public class VoteSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private PollOption option;

    public Long getId() { return id; }
    public Vote getVote() { return vote; }
    public void setVote(Vote vote) { this.vote = vote; }
    public PollOption getOption() { return option; }
    public void setOption(PollOption option) { this.option = option; }
}
