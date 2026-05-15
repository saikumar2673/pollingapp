# Implementation Plan: Online Polling Platform

## 1. Goal

Build a working end-to-end online polling platform using:

- Backend: Spring Boot
- Frontend: HTML, CSS, and JavaScript
- Persistence: SQL Server with Spring Data JPA
- Authentication: Email/password login with hashed passwords

The priority is correctness over visual polish:

- Authenticated-only access
- Correct poll lifecycle handling
- Correct public/private access enforcement
- One vote-set per user per poll
- Correct results visibility behavior
- Reliable persistence across restarts

## 2. Proposed Technology Stack

### Backend

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Security
- Spring Data JPA
- Bean Validation
- Thymeleaf or REST controllers with static frontend pages
- BCrypt password hashing

### Database

Use SQL Server for durable relational persistence.

Recommended local setup:

- SQL Server running locally or through Docker
- Database name: `polling_platform`
- Spring Data JPA with Hibernate schema updates during development

SQL Server is a good fit because the project needs reliable persistence, constraints, indexing, and realistic query behavior for pagination and sorting.

### Frontend

- Server-rendered HTML pages or static HTML calling JSON APIs
- CSS for layout and state styling
- Vanilla JavaScript for form interactions, voting, pagination, filtering, and share URL copy behavior

Recommended approach: Spring Boot serves HTML pages plus JSON endpoints where useful. This keeps the project simple while still allowing clean client-side interactions.

## 3. Project Structure

```text
polling-platform/
  pom.xml
  README.md
  implementation_plan.md
  src/
    main/
      java/
        com/example/polling/
          PollingApplication.java
          config/
            SecurityConfig.java
          auth/
            AuthController.java
            CustomUserDetailsService.java
          user/
            User.java
            UserRepository.java
          poll/
            Poll.java
            PollOption.java
            PollStatus.java
            PollType.java
            PollVisibility.java
            ResultsVisibility.java
            PollController.java
            PollService.java
            PollRepository.java
            PollOptionRepository.java
          invitation/
            Invitation.java
            InvitationController.java
            InvitationService.java
            InvitationRepository.java
          vote/
            Vote.java
            VoteSelection.java
            VoteController.java
            VoteService.java
            VoteRepository.java
            VoteSelectionRepository.java
          results/
            ResultsService.java
            PollResultsDto.java
          common/
            AccessDeniedException.java
            NotFoundException.java
            GlobalExceptionHandler.java
            PageRequestFactory.java
      resources/
        application.yml
        templates/
          auth/
            login.html
            register.html
          polls/
            feed.html
            my-polls.html
            shared-with-me.html
            create.html
            edit.html
            detail.html
        static/
          css/
            styles.css
          js/
            polls.js
            voting.js
```

## 4. Core Domain Model

### User

Fields:

- id
- name
- email
- passwordHash
- createdAt
- updatedAt

Rules:

- Email must be unique.
- Email must be syntactically valid.
- Password must be stored with BCrypt.
- Plaintext password must never be persisted.

### Poll

Fields:

- id
- title
- description
- type: SINGLE_CHOICE or MULTI_CHOICE
- visibility: PUBLIC or PRIVATE
- resultsVisibility: ALWAYS_VISIBLE or VISIBLE_AFTER_VOTING
- status: DRAFT, OPEN, CLOSED
- endAt
- shareToken
- creator
- createdAt
- updatedAt
- closedAt

Rules:

- New polls start as DRAFT.
- shareToken is generated on creation and never changes.
- Draft polls are visible only to the creator.
- Published polls lock type, options, and visibility.
- Closed polls are read-only.

### PollOption

Fields:

- id
- poll
- label
- position
- createdAt

Rules:

- A poll must have at least two options before publishing.
- Option labels must be non-empty.
- Draft options may be added, edited, reordered, or deleted.
- Published options are locked.

### Invitation

Fields:

- id
- poll
- invitedUser
- invitedBy
- invitedAt

Rules:

- Invitations apply only to private polls.
- Only the creator can add or revoke invitations.
- Invitee must already be a registered user.
- Revoking an invitation removes future access but does not delete historical votes.

Recommended database constraint:

- Unique pair: poll_id + invited_user_id

### Vote

Fields:

- id
- poll
- voter
- createdAt
- updatedAt

Rules:

- One vote-set per user per poll.
- Resubmitting replaces prior selections.
- Withdrawing deletes the vote selections and may delete the vote row or mark it withdrawn.

Recommended approach:

- Keep a Vote row only while the user has an active vote.
- On withdraw, delete VoteSelection rows and delete the Vote row.
- Historical privacy is preserved because individual choices are never exposed through UI or API.

Recommended database constraint:

- Unique pair: poll_id + voter_id

### VoteSelection

Fields:

- id
- vote
- option

Rules:

- Selected option must belong to the same poll as the vote.
- Single-choice votes require exactly one selection.
- Multi-choice votes require one or more selections.

Recommended database constraint:

- Unique pair: vote_id + option_id

## 5. Database Design

Tables:

- users
- polls
- poll_options
- invitations
- votes
- vote_selections

Important indexes:

- users.email unique
- polls.share_token unique
- polls.status
- polls.visibility
- polls.created_at
- polls.end_at
- polls.creator_id
- invitations.poll_id
- invitations.invited_user_id
- invitations.poll_id + invitations.invited_user_id unique
- votes.poll_id
- votes.voter_id
- votes.poll_id + votes.voter_id unique
- vote_selections.vote_id
- vote_selections.option_id

For total respondents sorting:

- Start with a query using grouped vote counts.
- If performance becomes a concern, add a cached respondent_count column on polls and update it transactionally when votes are created or withdrawn.

## 6. Security Model

### Authentication

Implement Spring Security with:

- Register page
- Login page
- Logout endpoint
- BCryptPasswordEncoder
- Session-based authentication

All polling routes require authentication.

Public unauthenticated paths:

- GET /login
- POST /login
- GET /register
- POST /register
- Static assets

Everything else must require an authenticated user.

### Authorization

Centralize poll access checks in PollService or a dedicated PollAccessService.

Access rules:

- Draft poll: creator only
- Public open/closed poll: any authenticated user
- Private poll: creator or active invitee only
- Closed private poll: creator or active invitee only
- Revoked invitee: no access, even with share URL

Mutation rules:

- Create poll: authenticated user
- Edit draft: creator only
- Delete draft: creator only
- Publish draft: creator only
- Extend endAt after publish: creator only
- Manual close: creator only
- Invite/revoke: creator only, private polls only
- Vote/withdraw: authenticated user with access, poll must be OPEN

Important privacy rule:

- When denying private poll access, do not reveal the title, options, creator, or any detail.

## 7. Poll Lifecycle Rules

### Draft

Allowed:

- Creator can edit all fields.
- Creator can edit options.
- Creator can change visibility.
- Creator can invite users if the poll is private.
- Creator can delete the poll.
- Creator can publish if valid.

Denied:

- Other users cannot see it.
- Voting is not allowed.
- Drafts do not appear in public feed or shared-with-me.

### Open

Allowed:

- Accessible according to visibility.
- Eligible users can vote, update vote, or withdraw vote.
- Creator can extend endAt to a future time.
- Creator can manually close.
- Creator can add/revoke private invitations.

Denied:

- Options, type, and visibility cannot change.
- Poll cannot be deleted.
- Vote attempts by users without access are rejected.

### Closed

Allowed:

- Accessible according to visibility.
- Results visible to all users with access.
- Creator can add/revoke private invitations, but this only affects access.

Denied:

- No reopening.
- No voting.
- No editing.
- No deleting.

### Auto-close

Auto-closing can happen lazily:

- Every read or write that loads a poll checks endAt.
- If status is OPEN and endAt is before or equal to now, transition to CLOSED.
- Save the poll before returning the result.

This satisfies the requirement without a background scheduler.

## 8. Main User Flows

### Register and Sign In

1. User opens /register.
2. User submits name, email, and password.
3. Backend validates email uniqueness and password requirements.
4. Backend stores user with BCrypt hash.
5. User signs in at /login.
6. Authenticated user lands on public feed.

### Create Draft Poll

1. User opens /polls/new.
2. User enters title, description, type, visibility, results visibility, optional endAt, and at least two options.
3. Backend validates required fields.
4. Backend generates shareToken.
5. Backend saves poll as DRAFT.
6. User is redirected to draft detail/edit page.

### Edit Draft Poll

1. Creator opens draft edit page.
2. Creator changes any field or option.
3. Backend confirms poll is DRAFT and current user is creator.
4. Backend saves changes.

### Publish Poll

1. Creator clicks Publish.
2. Backend validates title, at least two options, valid option labels, and future endAt if present.
3. Backend changes status from DRAFT to OPEN.
4. Backend locks options, type, and visibility by enforcing service-level mutation rules.

### Vote

1. Authenticated user opens an accessible OPEN poll.
2. UI renders radio buttons for single-choice and checkboxes for multi-choice.
3. User submits selection.
4. Backend validates access, status, option ownership, and selection count.
5. Backend replaces any prior vote-set in one transaction.
6. Results are recalculated from persisted votes.

### Withdraw Vote

1. User clicks Withdraw vote on an accessible OPEN poll where they already voted.
2. Backend validates access and status.
3. Backend removes the user's active vote-set.
4. Counts decrement immediately.

### Private Invitation

1. Creator opens private poll manage page.
2. Creator enters registered user's email.
3. Backend validates poll is private and user exists.
4. Backend creates invitation.
5. Invitee can see poll in Shared with me and access via share URL.

### Revoke Invitation

1. Creator removes invitee.
2. Backend deletes invitation row.
3. User immediately loses access.
4. Existing vote remains counted if one exists.

## 9. Routes and Endpoints

### Page Routes

- GET /register
- POST /register
- GET /login
- POST /login
- POST /logout
- GET /polls
- GET /polls/my
- GET /polls/shared
- GET /polls/new
- POST /polls
- GET /polls/{shareToken}
- GET /polls/{shareToken}/edit
- POST /polls/{shareToken}/edit
- POST /polls/{shareToken}/publish
- POST /polls/{shareToken}/delete
- POST /polls/{shareToken}/close
- POST /polls/{shareToken}/end-at
- POST /polls/{shareToken}/vote
- POST /polls/{shareToken}/withdraw
- POST /polls/{shareToken}/invitations
- POST /polls/{shareToken}/invitations/{invitationId}/revoke

### Optional JSON API Endpoints

Use these only where JavaScript needs asynchronous behavior:

- GET /api/polls/{shareToken}/results
- POST /api/polls/{shareToken}/vote
- POST /api/polls/{shareToken}/withdraw

Keep authorization behavior identical between page routes and API routes.

## 10. UI Pages

### Public Feed

Shows:

- Public OPEN and CLOSED polls only
- Title
- Status
- Creator name
- Created date
- End date
- Total respondents
- Pagination controls
- Status filter
- Sort controls for createdAt, endAt, and total respondents

Must never show:

- Draft polls
- Private polls

### My Polls

Shows:

- All polls created by current user
- Draft, Open, Closed
- Public and Private
- Actions based on status

### Shared With Me

Shows:

- Private OPEN and CLOSED polls where current user is an invitee
- Excludes polls created by current user
- Excludes drafts

### Poll Detail

Shows if user has access:

- Poll title and description
- Status
- Voting form if OPEN and user can vote
- User's current vote if present
- Withdraw button if user has voted and poll is OPEN
- Results section if results are visible to this viewer
- Share URL
- Creator controls if current user is creator

For denied private access:

- Show only a clear no-access message.
- Do not show title, options, creator, or status.

### Poll Edit

Available only for creator-owned drafts.

Fields:

- Title
- Description
- Type
- Visibility
- Results visibility
- EndAt
- Options

### Invitation Management

Visible only to creator of private poll.

Shows:

- Invited user name
- Invited user email
- Invited at
- Invited by
- Revoke action

## 11. Results Calculation

The results service should calculate:

- totalRespondents = count of distinct Vote rows for poll
- per option count = count of VoteSelection rows for that option
- percentage = option count / totalRespondents * 100

Rules:

- If totalRespondents is zero, all percentages are 0.
- For single-choice polls, percentages should sum to 100 except for rounding differences.
- For multi-choice polls, percentages may sum above 100.
- Individual vote choices are only returned for the current authenticated user, never for other voters.

Results visibility:

- Closed poll: visible to all users with access.
- Always visible: visible to all users with access.
- Visible after voting: visible to creator or users who have an active vote.

## 12. Validation Plan

### Poll Validation

- Title is required and trimmed.
- At least two options are required.
- Option labels are required and trimmed.
- EndAt must be in the future at publish time.
- Draft-only fields cannot be modified after publish.
- Closed polls cannot be modified.

### Vote Validation

- Poll must be OPEN.
- User must have access.
- Option IDs must belong to the poll.
- Single-choice requires exactly one option ID.
- Multi-choice requires at least one option ID.
- Duplicate option IDs should be normalized or rejected.

### Invitation Validation

- Poll must be private.
- Current user must be creator.
- Email must belong to existing registered user.
- Creator should not need to invite themselves.
- Duplicate invitations should show a clear message.

### Authentication Validation

- Registration requires name, valid unique email, and password.
- Login rejects invalid credentials with a generic clear message.
- All protected routes redirect to login if unauthenticated.

## 13. Transaction Boundaries

Use @Transactional for:

- Creating polls with options
- Editing draft polls and options
- Publishing polls
- Auto-closing polls
- Voting and replacing selections
- Withdrawing votes
- Adding/revoking invitations
- Manual close
- Extending endAt

Voting replacement should be atomic:

1. Load poll with access and status checks.
2. Validate submitted option IDs.
3. Find existing vote by poll and user.
4. Delete existing selections.
5. Save new vote if needed.
6. Save new selections.

This prevents duplicated or fragmented vote counts.

## 14. Sorting, Filtering, and Pagination

Default page size:

- 20

Public feed filters:

- status=OPEN
- status=CLOSED
- no status means OPEN and CLOSED

Public feed sorting:

- createdAt asc/desc
- endAt asc/desc
- totalRespondents asc/desc

My Polls:

- Include all statuses and visibilities for current creator.
- Paginate.
- Optional status filter can be added for usability.

Shared With Me:

- Include private OPEN and CLOSED polls where current user is invited.
- Exclude drafts.
- Paginate.

Implementation detail:

- Use Spring Data Page and Pageable for standard sorts.
- Use a custom query for total respondents sorting.

## 15. Testing Strategy

### Unit Tests

PollService:

- Create draft poll.
- Edit draft poll.
- Reject edit after publish.
- Publish valid draft.
- Reject publish with fewer than two options.
- Auto-close expired open poll.
- Reject reopening closed poll.

VoteService:

- Single-choice accepts exactly one option.
- Single-choice rejects zero or multiple options.
- Multi-choice accepts one or more options.
- Multi-choice rejects zero options.
- Vote replacement updates counts correctly.
- Withdraw removes counts correctly.
- Reject vote on draft or closed poll.
- Reject option IDs from another poll.

InvitationService:

- Invite existing user to private poll.
- Reject invite to public poll.
- Reject non-existent email.
- Revoke invitation removes access.
- Revoked user's historical vote remains counted.

ResultsService:

- Correct single-choice percentages.
- Correct multi-choice percentages.
- Zero respondent behavior.
- Results visibility for always visible.
- Results visibility for visible after voting.
- Closed poll overrides visibility setting.

AccessService:

- Draft creator-only access.
- Public access for authenticated users.
- Private access for creator and invitee.
- Private denial does not leak poll details.

### Integration Tests

Use Spring Boot tests with MockMvc:

- Register, logout, login flow.
- Unauthenticated users redirected or rejected.
- Public feed excludes private and draft polls.
- Share URL works for public poll.
- Share URL does not grant private access.
- Invitee can access private poll.
- Revoked invitee cannot access private poll.
- API tampering attempts are rejected.

### Manual Acceptance Test Script

Create test users:

- alice@example.com
- bob@example.com
- charlie@example.com

Scenarios:

1. Alice registers, creates public poll, publishes it.
2. Bob sees public poll in feed and votes.
3. Bob changes vote and counts update.
4. Bob withdraws vote and counts decrement.
5. Alice creates private poll and invites Bob.
6. Bob sees private poll in Shared with me.
7. Charlie cannot access private poll even with URL.
8. Alice revokes Bob.
9. Bob can no longer access private poll.
10. Existing Bob vote remains counted.
11. Poll with endAt in the past auto-closes on next access.
12. Results visibility after voting behaves correctly.

## 16. README Requirements

The README should include:

- Project overview
- Tech stack and why it was chosen
- Setup instructions
- How to run the app
- How to run tests
- Database configuration
- Default local URLs
- Explanation of poll lifecycle
- Explanation of private poll access rules
- Explanation that revoked invitee votes remain counted
- Explanation of multi-choice percentages
- Security notes about password hashing and private access enforcement

## 17. Implementation Phases

### Phase 1: Project Foundation

- Generate Spring Boot project.
- Add dependencies.
- Configure database.
- Add basic app layout.
- Add global error handling.
- Add README skeleton.

### Phase 2: Authentication

- Implement User entity and repository.
- Implement registration.
- Implement login/logout with Spring Security.
- Hash passwords with BCrypt.
- Protect all polling routes.

### Phase 3: Poll Draft Management

- Implement Poll and PollOption entities.
- Implement create draft.
- Implement edit draft.
- Implement delete draft.
- Generate stable shareToken.
- Add My Polls page.

### Phase 4: Lifecycle

- Implement publish.
- Lock type/options/visibility after publish.
- Implement manual close.
- Implement endAt extension.
- Implement lazy auto-close.
- Enforce read-only behavior for closed polls.

### Phase 5: Visibility and Access

- Implement public/private access rules.
- Implement share URL lookup by token.
- Implement public feed.
- Ensure private polls never appear in public feed.
- Ensure private access denial leaks no details.

### Phase 6: Invitations

- Implement invitations for private polls.
- Add invite by registered email.
- Add revoke invitation.
- Add creator-only invitee list.
- Add Shared with me page.

### Phase 7: Voting

- Implement vote submission.
- Enforce one vote-set per user per poll.
- Implement vote replacement.
- Implement vote withdrawal.
- Validate option ownership and poll type rules.

### Phase 8: Results

- Implement aggregate results service.
- Implement result visibility rules.
- Display total respondents.
- Display counts and percentages.
- Add multi-choice percentage note in UI.

### Phase 9: Filtering, Sorting, Pagination

- Add pagination to public feed, My Polls, and Shared with me.
- Add public feed status filter.
- Add public feed sorting by createdAt, endAt, and total respondents.
- Verify filters apply before pagination.

### Phase 10: Hardening and Acceptance Testing

- Add unit tests.
- Add integration tests.
- Run manual acceptance script.
- Fix edge cases.
- Complete README.

## 18. Risks and Mitigations

### Risk: Private poll details leak through errors or list views

Mitigation:

- Use a single access service for all poll reads and writes.
- Denied private access returns a generic no-access page/message.
- Tests must cover direct URL and API attempts.

### Risk: Vote counts become inconsistent when users change votes

Mitigation:

- Enforce unique poll/user vote constraint.
- Replace selections transactionally.
- Calculate aggregate counts from vote selections.

### Risk: Published poll fields are accidentally editable

Mitigation:

- Service layer rejects mutations unless status is DRAFT.
- UI hides controls, but backend remains authoritative.
- Tests cover direct POST/API tampering.

### Risk: Expired polls continue accepting votes

Mitigation:

- Call lazy auto-close before detail rendering, voting, editing, and result retrieval.
- Reject votes after auto-close.

### Risk: Sorting by total respondents becomes slow

Mitigation:

- Start with grouped query.
- Add indexes.
- If needed, add cached respondent_count later.

## 19. Definition of Done

The project is complete when:

- All acceptance criteria AC1 through AC25 pass.
- Users can register, login, logout, and access only authorized pages.
- Polls can be created, edited as drafts, published, closed, and auto-closed.
- Public and private access rules are enforced on every route.
- Invitations work and revoked invitees lose access.
- Voting, changing votes, and withdrawing votes update counts correctly.
- Results visibility behaves exactly as configured.
- Lists are paginated, filtered, sorted, and scoped to viewer permissions.
- Data persists after application restart.
- README documents setup, behavior decisions, and edge cases.
- Automated tests cover the most important lifecycle, access, voting, and results rules.
