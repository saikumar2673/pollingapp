package com.example.polling.poll;

import com.example.polling.user.AppUser;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollRepository extends JpaRepository<Poll, Long> {
    @EntityGraph(attributePaths = {"options", "creator"})
    Optional<Poll> findByShareToken(String shareToken);

    Page<Poll> findByCreator(AppUser creator, Pageable pageable);

    @Query("""
        select p from Poll p
        where p.visibility = com.example.polling.poll.PollVisibility.PUBLIC
          and p.status <> com.example.polling.poll.PollStatus.DRAFT
          and (:status is null or p.status = :status)
        """)
    Page<Poll> publicFeed(@Param("status") PollStatus status, Pageable pageable);

    @Query(
        value = """
            select p from Poll p
            left join Vote v on v.poll = p
            where p.visibility = com.example.polling.poll.PollVisibility.PUBLIC
              and p.status <> com.example.polling.poll.PollStatus.DRAFT
              and (:status is null or p.status = :status)
            group by p
            order by count(v.id) asc
            """,
        countQuery = """
            select count(p) from Poll p
            where p.visibility = com.example.polling.poll.PollVisibility.PUBLIC
              and p.status <> com.example.polling.poll.PollStatus.DRAFT
              and (:status is null or p.status = :status)
            """)
    Page<Poll> publicFeedByRespondentsAsc(@Param("status") PollStatus status, Pageable pageable);

    @Query(
        value = """
            select p from Poll p
            left join Vote v on v.poll = p
            where p.visibility = com.example.polling.poll.PollVisibility.PUBLIC
              and p.status <> com.example.polling.poll.PollStatus.DRAFT
              and (:status is null or p.status = :status)
            group by p
            order by count(v.id) desc
            """,
        countQuery = """
            select count(p) from Poll p
            where p.visibility = com.example.polling.poll.PollVisibility.PUBLIC
              and p.status <> com.example.polling.poll.PollStatus.DRAFT
              and (:status is null or p.status = :status)
            """)
    Page<Poll> publicFeedByRespondentsDesc(@Param("status") PollStatus status, Pageable pageable);

    @Query("""
        select p from Invitation i
        join i.poll p
        where i.invitedUser = :user
          and p.creator <> :user
          and p.visibility = com.example.polling.poll.PollVisibility.PRIVATE
          and p.status <> com.example.polling.poll.PollStatus.DRAFT
        order by p.createdAt desc
        """)
    Page<Poll> sharedWithMe(@Param("user") AppUser user, Pageable pageable);
}
