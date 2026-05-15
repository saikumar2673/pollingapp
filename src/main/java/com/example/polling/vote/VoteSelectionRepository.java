package com.example.polling.vote;

import com.example.polling.poll.Poll;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteSelectionRepository extends JpaRepository<VoteSelection, Long> {
    @Query("""
        select vs.option.id, count(vs.id)
        from VoteSelection vs
        where vs.vote.poll = :poll
        group by vs.option.id
        """)
    List<Object[]> countsByOption(@Param("poll") Poll poll);
}
