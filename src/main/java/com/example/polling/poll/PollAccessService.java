package com.example.polling.poll;

import com.example.polling.common.ForbiddenException;
import com.example.polling.invitation.InvitationRepository;
import com.example.polling.user.AppUser;
import org.springframework.stereotype.Service;

@Service
public class PollAccessService {
    private final InvitationRepository invitations;

    public PollAccessService(InvitationRepository invitations) {
        this.invitations = invitations;
    }

    public boolean isCreator(Poll poll, AppUser user) {
        return poll.getCreator().getId().equals(user.getId());
    }

    public boolean canAccess(Poll poll, AppUser user) {
        if (isCreator(poll, user)) {
            return true;
        }
        if (poll.getStatus() == PollStatus.DRAFT) {
            return false;
        }
        if (poll.getVisibility() == PollVisibility.PUBLIC) {
            return true;
        }
        return invitations.existsByPollAndInvitedUser(poll, user);
    }

    public void requireAccess(Poll poll, AppUser user) {
        if (!canAccess(poll, user)) {
            if (poll.getVisibility() == PollVisibility.PRIVATE) {
                throw new ForbiddenException("You do not have access to this poll.");
            }
            throw new ForbiddenException("You do not have permission to view this poll.");
        }
    }

    public void requireCreator(Poll poll, AppUser user) {
        if (!isCreator(poll, user)) {
            throw new ForbiddenException("Only the poll creator can perform this action.");
        }
    }
}
