package com.example.polling.vote;

import com.example.polling.common.AppException;
import com.example.polling.poll.Poll;
import com.example.polling.poll.PollAccessService;
import com.example.polling.poll.PollOption;
import com.example.polling.poll.PollOptionRepository;
import com.example.polling.poll.PollService;
import com.example.polling.poll.PollType;
import com.example.polling.user.AppUser;
import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VoteService {
    private final VoteRepository votes;
    private final PollOptionRepository options;
    private final PollAccessService access;
    private final PollService polls;

    public VoteService(VoteRepository votes, PollOptionRepository options, PollAccessService access, PollService polls) {
        this.votes = votes;
        this.options = options;
        this.access = access;
        this.polls = polls;
    }

    @Transactional
    public void submitVote(Poll poll, AppUser voter, Collection<Long> optionIds) {
        polls.autoClose(poll);
        access.requireAccess(poll, voter);
        if (!poll.isOpen()) {
            throw new AppException("Only open polls can accept votes.");
        }

        List<Long> uniqueIds = optionIds == null ? List.of() : new HashSet<>(optionIds).stream().toList();
        validateSelectionCount(poll, uniqueIds);
        List<PollOption> selected = options.findByPollAndIdIn(poll, uniqueIds);
        if (selected.size() != uniqueIds.size()) {
            throw new AppException("Every selected option must belong to this poll.");
        }

        Vote vote = votes.findByPollAndVoter(poll, voter).orElseGet(() -> {
            Vote newVote = new Vote();
            newVote.setPoll(poll);
            newVote.setVoter(voter);
            return newVote;
        });
        vote.replaceSelections(selected);
        votes.save(vote);
    }

    @Transactional
    public void withdrawVote(Poll poll, AppUser voter) {
        polls.autoClose(poll);
        access.requireAccess(poll, voter);
        if (!poll.isOpen()) {
            throw new AppException("Only open polls allow vote withdrawal.");
        }
        votes.findByPollAndVoter(poll, voter).ifPresent(votes::delete);
    }

    @Transactional
    public List<Long> currentSelectionIds(Poll poll, AppUser voter) {
        return votes.findByPollAndVoter(poll, voter)
            .map(v -> v.getSelections().stream().map(s -> s.getOption().getId()).toList())
            .orElse(List.of());
    }

    public boolean hasVoted(Poll poll, AppUser voter) {
        return votes.existsByPollAndVoter(poll, voter);
    }

    private void validateSelectionCount(Poll poll, List<Long> optionIds) {
        if (poll.getType() == PollType.SINGLE_CHOICE && optionIds.size() != 1) {
            throw new AppException("Single-choice polls require exactly one selected option.");
        }
        if (poll.getType() == PollType.MULTI_CHOICE && optionIds.isEmpty()) {
            throw new AppException("Multi-choice polls require at least one selected option.");
        }
    }
}
