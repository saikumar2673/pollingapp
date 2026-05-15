package com.example.polling.vote;

import com.example.polling.poll.Poll;
import com.example.polling.user.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByPollAndVoter(Poll poll, AppUser voter);
    boolean existsByPollAndVoter(Poll poll, AppUser voter);
    long countByPoll(Poll poll);

    @Query("select count(v) from Vote v where v.poll.id = :pollId")
    long countRespondents(@Param("pollId") Long pollId);
}
