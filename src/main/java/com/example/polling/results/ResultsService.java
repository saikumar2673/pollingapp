package com.example.polling.results;

import com.example.polling.poll.Poll;
import com.example.polling.poll.PollAccessService;
import com.example.polling.poll.PollStatus;
import com.example.polling.poll.ResultsVisibility;
import com.example.polling.user.AppUser;
import com.example.polling.vote.VoteRepository;
import com.example.polling.vote.VoteSelectionRepository;
import java.util.HashMap;
import org.springframework.stereotype.Service;

@Service
public class ResultsService {
    private final VoteRepository votes;
    private final VoteSelectionRepository selections;
    private final PollAccessService access;

    public ResultsService(VoteRepository votes, VoteSelectionRepository selections, PollAccessService access) {
        this.votes = votes;
        this.selections = selections;
        this.access = access;
    }

    public boolean canSeeResults(Poll poll, AppUser user) {
        if (poll.getStatus() == PollStatus.CLOSED) {
            return true;
        }
        if (poll.getResultsVisibility() == ResultsVisibility.ALWAYS_VISIBLE) {
            return true;
        }
        return access.isCreator(poll, user) || votes.existsByPollAndVoter(poll, user);
    }

    public PollResults resultsFor(Poll poll) {
        long total = votes.countByPoll(poll);
        var counts = new HashMap<Long, Long>();
        for (Object[] row : selections.countsByOption(poll)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        var optionResults = poll.getOptions().stream()
            .map(option -> {
                long count = counts.getOrDefault(option.getId(), 0L);
                double percentage = total == 0 ? 0 : (count * 100.0) / total;
                return new OptionResult(option.getId(), option.getLabel(), count, percentage);
            })
            .toList();
        return new PollResults(total, optionResults);
    }
}
