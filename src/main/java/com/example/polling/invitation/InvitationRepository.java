package com.example.polling.invitation;

import com.example.polling.poll.Poll;
import com.example.polling.user.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    boolean existsByPollAndInvitedUser(Poll poll, AppUser invitedUser);
    Optional<Invitation> findByPollAndInvitedUser(Poll poll, AppUser invitedUser);

    @EntityGraph(attributePaths = {"invitedUser", "invitedBy"})
    List<Invitation> findByPollOrderByInvitedAtDesc(Poll poll);
}
