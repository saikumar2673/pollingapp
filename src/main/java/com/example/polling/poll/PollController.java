package com.example.polling.poll;

import com.example.polling.auth.CurrentUserService;
import com.example.polling.invitation.InvitationService;
import com.example.polling.results.ResultsService;
import com.example.polling.vote.VoteService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PollController {
    private final CurrentUserService currentUserService;
    private final PollService polls;
    private final ResultsService results;
    private final VoteService votes;
    private final InvitationService invitations;
    private final PollAccessService access;

    public PollController(CurrentUserService currentUserService, PollService polls, ResultsService results,
                          VoteService votes, InvitationService invitations, PollAccessService access) {
        this.currentUserService = currentUserService;
        this.polls = polls;
        this.results = results;
        this.votes = votes;
        this.invitations = invitations;
        this.access = access;
    }

    @GetMapping("/polls")
    public String feed(@RequestParam(required = false) PollStatus status,
                       @RequestParam(defaultValue = "createdAt") String sort,
                       @RequestParam(defaultValue = "desc") String dir,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<Poll> pollPage = polls.publicFeed(status, sort, dir, page);
        model.addAttribute("polls", pollPage);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("view", "feed");
        model.addAttribute("listPath", "/polls");
        model.addAttribute("respondentCounter", polls);
        return "polls/feed";
    }

    @GetMapping("/polls/my")
    public String myPolls(@RequestParam(defaultValue = "0") int page, Authentication auth, Model model) {
        var user = currentUserService.requireUser(auth);
        model.addAttribute("polls", polls.myPolls(user, page));
        model.addAttribute("view", "my");
        model.addAttribute("listPath", "/polls/my");
        model.addAttribute("respondentCounter", polls);
        return "polls/feed";
    }

    @GetMapping("/polls/shared")
    public String shared(@RequestParam(defaultValue = "0") int page, Authentication auth, Model model) {
        var user = currentUserService.requireUser(auth);
        model.addAttribute("polls", polls.sharedWithMe(user, page));
        model.addAttribute("view", "shared");
        model.addAttribute("listPath", "/polls/shared");
        model.addAttribute("respondentCounter", polls);
        return "polls/feed";
    }

    @GetMapping("/polls/new")
    public String newPoll(Model model) {
        model.addAttribute("form", new PollForm());
        model.addAttribute("mode", "create");
        return "polls/form";
    }

    @PostMapping("/polls")
    public String create(@ModelAttribute("form") PollForm form, Authentication auth) {
        var user = currentUserService.requireUser(auth);
        Poll poll = polls.createDraft(form, user);
        return "redirect:/polls/" + poll.getShareToken() + "/edit?created";
    }

    @GetMapping("/polls/{token}")
    public String detail(@PathVariable String token, Authentication auth, Model model,
                         jakarta.servlet.http.HttpServletRequest request) {
        var user = currentUserService.requireUser(auth);
        Poll poll = polls.detailByToken(token, user);
        boolean creator = access.isCreator(poll, user);
        boolean canSeeResults = results.canSeeResults(poll, user);
        model.addAttribute("poll", poll);
        model.addAttribute("creator", creator);
        model.addAttribute("canSeeResults", canSeeResults);
        model.addAttribute("results", canSeeResults ? results.resultsFor(poll) : null);
        model.addAttribute("currentSelectionIds", votes.currentSelectionIds(poll, user));
        model.addAttribute("hasVoted", votes.hasVoted(poll, user));
        model.addAttribute("invitations", creator ? invitations.listForCreator(poll, user) : List.of());
        model.addAttribute("shareUrl", request.getRequestURL().toString());
        return "polls/detail";
    }

    @GetMapping("/polls/{token}/edit")
    public String edit(@PathVariable String token, Authentication auth, Model model) {
        var user = currentUserService.requireUser(auth);
        Poll poll = polls.detailByToken(token, user);
        access.requireCreator(poll, user);
        model.addAttribute("poll", poll);
        model.addAttribute("form", polls.toForm(poll));
        model.addAttribute("mode", "edit");
        return "polls/form";
    }

    @PostMapping("/polls/{token}/edit")
    public String update(@PathVariable String token, @ModelAttribute("form") PollForm form, Authentication auth) {
        var user = currentUserService.requireUser(auth);
        polls.updateDraft(token, form, user);
        return "redirect:/polls/" + token + "/edit";
    }

    @PostMapping("/polls/{token}/delete")
    public String delete(@PathVariable String token, Authentication auth) {
        var user = currentUserService.requireUser(auth);
        polls.deleteDraft(token, user);
        return "redirect:/polls/my";
    }

    @PostMapping("/polls/{token}/publish")
    public String publish(@PathVariable String token, Authentication auth) {
        var user = currentUserService.requireUser(auth);
        polls.publish(token, user);
        return "redirect:/polls/" + token;
    }

    @PostMapping("/polls/{token}/close")
    public String close(@PathVariable String token, Authentication auth) {
        var user = currentUserService.requireUser(auth);
        polls.close(token, user);
        return "redirect:/polls/" + token;
    }

    @PostMapping("/polls/{token}/end-at")
    public String extendEndAt(@PathVariable String token,
                              @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
                              @RequestParam(required = false) LocalDateTime endAt,
                              Authentication auth) {
        var user = currentUserService.requireUser(auth);
        polls.extendEndAt(token, endAt, user);
        return "redirect:/polls/" + token;
    }

    @PostMapping("/polls/{token}/vote")
    public String vote(@PathVariable String token,
                       @RequestParam(name = "optionIds", required = false) List<Long> optionIds,
                       Authentication auth) {
        var user = currentUserService.requireUser(auth);
        Poll poll = polls.detailByToken(token, user);
        votes.submitVote(poll, user, optionIds);
        return "redirect:/polls/" + token;
    }

    @PostMapping("/polls/{token}/withdraw")
    public String withdraw(@PathVariable String token, Authentication auth) {
        var user = currentUserService.requireUser(auth);
        Poll poll = polls.detailByToken(token, user);
        votes.withdrawVote(poll, user);
        return "redirect:/polls/" + token;
    }

    @PostMapping("/polls/{token}/invitations")
    public String invite(@PathVariable String token, @RequestParam String email, Authentication auth) {
        var user = currentUserService.requireUser(auth);
        Poll poll = polls.detailByToken(token, user);
        invitations.invite(poll, user, email);
        return "redirect:/polls/" + token;
    }

    @PostMapping("/polls/{token}/invitations/{invitationId}/revoke")
    public String revoke(@PathVariable String token, @PathVariable Long invitationId, Authentication auth) {
        var user = currentUserService.requireUser(auth);
        Poll poll = polls.detailByToken(token, user);
        invitations.revoke(poll, user, invitationId);
        return "redirect:/polls/" + token;
    }
}
