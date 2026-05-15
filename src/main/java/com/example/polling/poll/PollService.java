package com.example.polling.poll;

import com.example.polling.common.AppException;
import com.example.polling.common.NotFoundException;
import com.example.polling.user.AppUser;
import com.example.polling.vote.VoteRepository;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PollService {
    private static final int PAGE_SIZE = 20;
    private final PollRepository polls;
    private final PollAccessService access;
    private final VoteRepository votes;
    private final SecureRandom random = new SecureRandom();

    public PollService(PollRepository polls, PollAccessService access, VoteRepository votes) {
        this.polls = polls;
        this.access = access;
        this.votes = votes;
    }

    @Transactional
    public Poll createDraft(PollForm form, AppUser creator) {
        Poll poll = new Poll();
        applyDraftFields(poll, form);
        poll.setCreator(creator);
        poll.setStatus(PollStatus.DRAFT);
        poll.setShareToken(generateToken());
        poll.replaceOptions(cleanOptions(form.getOptions()));
        validateDraft(poll);
        return polls.save(poll);
    }

    @Transactional
    public Poll updateDraft(String token, PollForm form, AppUser user) {
        Poll poll = requireByToken(token);
        access.requireCreator(poll, user);
        if (!poll.isDraft()) {
            throw new AppException("Only draft polls can be edited.");
        }
        applyDraftFields(poll, form);
        poll.replaceOptions(cleanOptions(form.getOptions()));
        validateDraft(poll);
        return poll;
    }

    @Transactional
    public void deleteDraft(String token, AppUser user) {
        Poll poll = requireByToken(token);
        access.requireCreator(poll, user);
        if (!poll.isDraft()) {
            throw new AppException("Published polls cannot be deleted. Close the poll instead.");
        }
        polls.delete(poll);
    }

    @Transactional
    public Poll publish(String token, AppUser user) {
        Poll poll = requireByToken(token);
        access.requireCreator(poll, user);
        if (!poll.isDraft()) {
            throw new AppException("Only draft polls can be published.");
        }
        validatePublish(poll);
        poll.setStatus(PollStatus.OPEN);
        return poll;
    }

    @Transactional
    public Poll close(String token, AppUser user) {
        Poll poll = requireByToken(token);
        access.requireCreator(poll, user);
        autoClose(poll);
        if (poll.isDraft()) {
            throw new AppException("Draft polls cannot be closed. Publish or delete the draft.");
        }
        if (!poll.isClosed()) {
            poll.setStatus(PollStatus.CLOSED);
            poll.setClosedAt(Instant.now());
        }
        return poll;
    }

    @Transactional
    public Poll extendEndAt(String token, LocalDateTime newEndAt, AppUser user) {
        Poll poll = requireByToken(token);
        access.requireCreator(poll, user);
        autoClose(poll);
        if (!poll.isOpen()) {
            throw new AppException("Only open polls can have their closing time extended.");
        }
        if (newEndAt == null) {
            throw new AppException("Provide a future closing time.");
        }
        Instant proposed = toInstant(newEndAt);
        if (!proposed.isAfter(Instant.now())) {
            throw new AppException("The new closing time must be in the future.");
        }
        if (poll.getEndAt() != null && proposed.isBefore(poll.getEndAt())) {
            throw new AppException("You can extend the closing time, but cannot move it earlier.");
        }
        poll.setEndAt(proposed);
        return poll;
    }

    @Transactional
    public Poll detailByToken(String token, AppUser user) {
        Poll poll = requireByToken(token);
        autoClose(poll);
        access.requireAccess(poll, user);
        return poll;
    }

    public Poll requireByToken(String token) {
        return polls.findByShareToken(token).orElseThrow(() -> new NotFoundException("Poll was not found."));
    }

    @Transactional
    public void autoClose(Poll poll) {
        if (poll.isOpen() && poll.getEndAt() != null && !poll.getEndAt().isAfter(Instant.now())) {
            poll.setStatus(PollStatus.CLOSED);
            poll.setClosedAt(Instant.now());
        }
    }

    public Page<Poll> publicFeed(PollStatus status, String sort, String dir, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, sortFor(sort, dir));
        if ("respondents".equals(sort)) {
            Pageable unsorted = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
            return "asc".equalsIgnoreCase(dir)
                ? polls.publicFeedByRespondentsAsc(status, unsorted)
                : polls.publicFeedByRespondentsDesc(status, unsorted);
        }
        return polls.publicFeed(status, pageable);
    }

    public Page<Poll> myPolls(AppUser user, int page) {
        return polls.findByCreator(user, PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public Page<Poll> sharedWithMe(AppUser user, int page) {
        return polls.sharedWithMe(user, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    public long respondentCount(Poll poll) {
        return votes.countByPoll(poll);
    }

    public PollForm toForm(Poll poll) {
        PollForm form = new PollForm();
        form.setTitle(poll.getTitle());
        form.setDescription(poll.getDescription());
        form.setType(poll.getType());
        form.setVisibility(poll.getVisibility());
        form.setResultsVisibility(poll.getResultsVisibility());
        form.setEndAt(poll.getEndAt() == null ? null : LocalDateTime.ofInstant(poll.getEndAt(), ZoneId.systemDefault()));
        form.setOptions(poll.getOptions().stream().map(PollOption::getLabel).toList());
        return form;
    }

    private void applyDraftFields(Poll poll, PollForm form) {
        poll.setTitle(trim(form.getTitle()));
        poll.setDescription(blankToNull(form.getDescription()));
        poll.setType(form.getType());
        poll.setVisibility(form.getVisibility());
        poll.setResultsVisibility(form.getResultsVisibility());
        poll.setEndAt(form.getEndAt() == null ? null : toInstant(form.getEndAt()));
    }

    private void validateDraft(Poll poll) {
        if (isBlank(poll.getTitle())) {
            throw new AppException("Poll title is required.");
        }
        if (poll.getType() == null || poll.getVisibility() == null || poll.getResultsVisibility() == null) {
            throw new AppException("Poll type, visibility, and results visibility are required.");
        }
        if (poll.getOptions().size() < 2) {
            throw new AppException("A poll must have at least two options.");
        }
    }

    private void validatePublish(Poll poll) {
        validateDraft(poll);
        if (poll.getEndAt() != null && !poll.getEndAt().isAfter(Instant.now())) {
            throw new AppException("Closing time must be in the future when publishing.");
        }
    }

    private List<String> cleanOptions(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> cleaned = raw.stream().map(this::trim).filter(s -> !isBlank(s)).toList();
        if (cleaned.size() < 2) {
            throw new AppException("A poll must have at least two non-empty options.");
        }
        return cleaned;
    }

    private Sort sortFor(String sort, String dir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property = switch (sort == null ? "" : sort) {
            case "endAt" -> "endAt";
            case "createdAt" -> "createdAt";
            default -> "createdAt";
        };
        return Sort.by(direction, property);
    }

    private String generateToken() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Instant toInstant(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        String trimmed = trim(value);
        return isBlank(trimmed) ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
