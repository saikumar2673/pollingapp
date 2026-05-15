package com.example.polling.invitation;

import com.example.polling.common.AppException;
import com.example.polling.common.NotFoundException;
import com.example.polling.poll.Poll;
import com.example.polling.poll.PollAccessService;
import com.example.polling.poll.PollVisibility;
import com.example.polling.user.AppUser;
import com.example.polling.user.AppUserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InvitationService {
    private final InvitationRepository invitations;
    private final AppUserRepository users;
    private final PollAccessService access;

    public InvitationService(InvitationRepository invitations, AppUserRepository users, PollAccessService access) {
        this.invitations = invitations;
        this.users = users;
        this.access = access;
    }

    @Transactional
    public void invite(Poll poll, AppUser creator, String email) {
        access.requireCreator(poll, creator);
        if (poll.getVisibility() != PollVisibility.PRIVATE) {
            throw new AppException("Invitations can only be added to private polls.");
        }
        String normalized = email == null ? "" : email.trim().toLowerCase();
        AppUser invited = users.findByEmail(normalized)
            .orElseThrow(() -> new AppException("No registered user exists with that email."));
        if (invited.getId().equals(creator.getId())) {
            throw new AppException("The creator already has access and does not need an invitation.");
        }
        if (invitations.existsByPollAndInvitedUser(poll, invited)) {
            throw new AppException("That user is already invited.");
        }
        Invitation invitation = new Invitation();
        invitation.setPoll(poll);
        invitation.setInvitedUser(invited);
        invitation.setInvitedBy(creator);
        invitations.save(invitation);
    }

    @Transactional
    public void revoke(Poll poll, AppUser creator, Long invitationId) {
        access.requireCreator(poll, creator);
        if (poll.isClosed()) {
            throw new AppException("Invitations cannot be revoked after a poll is closed.");
        }
        Invitation invitation = invitations.findById(invitationId)
            .orElseThrow(() -> new NotFoundException("Invitation was not found."));
        if (!invitation.getPoll().getId().equals(poll.getId())) {
            throw new AppException("Invitation does not belong to this poll.");
        }
        invitations.delete(invitation);
    }

    public List<Invitation> listForCreator(Poll poll, AppUser user) {
        access.requireCreator(poll, user);
        return poll.getVisibility() == PollVisibility.PRIVATE
            ? invitations.findByPollOrderByInvitedAtDesc(poll)
            : List.of();
    }
}
