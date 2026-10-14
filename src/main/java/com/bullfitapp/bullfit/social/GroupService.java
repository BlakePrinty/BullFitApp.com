package com.bullfitapp.bullfit.social;

import com.bullfitapp.bullfit.common.ZoneOptions;
import com.bullfitapp.bullfit.user.User;
import com.bullfitapp.bullfit.user.UserRepository;
import com.bullfitapp.bullfit.weight.WeightService;
import com.bullfitapp.bullfit.workout.WorkoutRepository;
import com.bullfitapp.bullfit.workout.WorkoutStatus;
import com.bullfitapp.bullfit.workout.WorkoutTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class GroupService {

    private final AccountabilityGroupRepository groups;
    private final GroupMemberRepository members;
    private final UserRepository users;
    private final WorkoutRepository workouts;
    private final FriendService friendService;

    public GroupService(AccountabilityGroupRepository groups, GroupMemberRepository members,
            UserRepository users, WorkoutRepository workouts, FriendService friendService) {
        this.groups = groups;
        this.members = members;
        this.users = users;
        this.workouts = workouts;
        this.friendService = friendService;
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private AccountabilityGroup ownedGroup(Long groupId, Long userId) {
        AccountabilityGroup group = groups.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!group.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND); // don't reveal the group to non-owners
        }
        return group;
    }

    // ----- lists -----

    @Transactional(readOnly = true)
    public List<GroupListItem> myGroups(Long userId) {
        List<GroupMember> mine = members.findByUserIdAndStatus(userId, GroupMemberStatus.ACTIVE);
        if (mine.isEmpty())
            return List.of();
        List<Long> ids = mine.stream().map(GroupMember::getGroupId).toList();
        Map<Long, Long> counts = members.countByGroups(ids, GroupMemberStatus.ACTIVE).stream()
                .collect(Collectors.toMap(GroupCount::groupId, GroupCount::total));
        return groups.findAllById(ids).stream()
                .sorted(Comparator.comparing(AccountabilityGroup::getName, String.CASE_INSENSITIVE_ORDER))
                .map(g -> new GroupListItem(g.getId(), g.getName(), counts.getOrDefault(g.getId(), 0L),
                        g.getWeeklyGoal(), g.getOwnerId().equals(userId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupInvite> myInvites(Long userId) {
        List<GroupMember> invites = members.findByUserIdAndStatus(userId, GroupMemberStatus.INVITED);
        if (invites.isEmpty())
            return List.of();
        Map<Long, AccountabilityGroup> byId = groups.findAllById(
                invites.stream().map(GroupMember::getGroupId).toList()).stream()
                .collect(Collectors.toMap(AccountabilityGroup::getId, g -> g));
        Map<Long, User> inviters = users.findAllById(
                invites.stream().map(GroupMember::getInvitedBy).toList()).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return invites.stream()
                .map(m -> new GroupInvite(m.getGroupId(), byId.get(m.getGroupId()).getName(),
                        inviters.get(m.getInvitedBy())))
                .toList();
    }

    @Transactional(readOnly = true)
    public long pendingInviteCount(Long userId) {
        return members.countByUserIdAndStatus(userId, GroupMemberStatus.INVITED);
    }

    @Transactional(readOnly = true)
    public long ownedCount(Long userId) {
        return groups.countByOwnerId(userId);
    }

    // ----- creating and managing -----

    @Transactional
    public Long create(User owner, GroupForm form) {
        int maxOwned = GroupLimits.maxOwned(owner.getRole());
        if (groups.countByOwnerId(owner.getId()) >= maxOwned) {
            throw new SocialException("You've reached your limit of " + GroupLimits.describe(maxOwned)
                    + " groups you own.");
        }
        int maxJoined = GroupLimits.maxJoined(owner.getRole());
        if (members.countByUserIdAndStatus(owner.getId(), GroupMemberStatus.ACTIVE) >= maxJoined) {
            throw new SocialException("You're in the maximum of " + GroupLimits.describe(maxJoined)
                    + " groups. Leave one first.");
        }
        AccountabilityGroup group = new AccountabilityGroup();
        group.setOwnerId(owner.getId());
        group.setName(form.getName().trim());
        group.setDescription(blankToNull(form.getDescription()));
        group.setWeeklyGoal(form.getWeeklyGoal());
        group.setCreatedAt(now());
        groups.save(group);

        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(owner.getId());
        member.setStatus(GroupMemberStatus.ACTIVE);
        member.setInvitedBy(owner.getId());
        member.setCreatedAt(now());
        member.setJoinedAt(now());
        members.save(member);
        return group.getId();
    }

    @Transactional
    public void updateSettings(Long ownerId, Long groupId, String name, String description, Integer goal) {
        AccountabilityGroup group = ownedGroup(groupId, ownerId);
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty() || cleanName.length() > 50) {
            throw new SocialException("Name must be 1 to 50 characters.");
        }
        String cleanDescription = blankToNull(description);
        if (cleanDescription != null && cleanDescription.length() > 300) {
            throw new SocialException("Description can be up to 300 characters.");
        }
        if (goal == null || goal < 1 || goal > 7) {
            throw new SocialException("The weekly goal must be between 1 and 7 days.");
        }
        group.setName(cleanName);
        group.setDescription(cleanDescription);
        group.setWeeklyGoal(goal);
        groups.save(group);
    }

    @Transactional
    public void delete(Long ownerId, Long groupId) {
        groups.delete(ownedGroup(groupId, ownerId)); // the database cascades the members
    }

    // ----- invites and membership -----

    @Transactional
    public void invite(Long ownerId, Long groupId, Long inviteeId) {
        ownedGroup(groupId, ownerId);
        User owner = users.findById(ownerId).orElseThrow();
        users.findById(inviteeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ownerId.equals(inviteeId)) {
            throw new SocialException("You're already in your own group.");
        }
        if (!friendService.areFriends(ownerId, inviteeId)) {
            throw new SocialException("You can only invite your friends.");
        }
        Optional<GroupMember> existing = members.findByGroupIdAndUserId(groupId, inviteeId);
        if (existing.isPresent()) {
            throw new SocialException(existing.get().getStatus() == GroupMemberStatus.ACTIVE
                    ? "They're already in this group."
                    : "They've already been invited.");
        }
        int max = GroupLimits.maxMembers(owner.getRole());
        if (members.countByGroupId(groupId) >= max) {
            throw new SocialException("This group is full (" + GroupLimits.describe(max)
                    + " spots, counting pending invites).");
        }
        GroupMember invite = new GroupMember();
        invite.setGroupId(groupId);
        invite.setUserId(inviteeId);
        invite.setInvitedBy(ownerId);
        invite.setCreatedAt(now());
        members.save(invite);
    }

    @Transactional
    public void acceptInvite(User user, Long groupId) {
        GroupMember member = members.findByGroupIdAndUserId(groupId, user.getId())
                .filter(m -> m.getStatus() == GroupMemberStatus.INVITED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        int maxJoined = GroupLimits.maxJoined(user.getRole());
        if (members.countByUserIdAndStatus(user.getId(), GroupMemberStatus.ACTIVE) >= maxJoined) {
            throw new SocialException("You're in the maximum of " + GroupLimits.describe(maxJoined)
                    + " groups. Leave one first.");
        }
        member.setStatus(GroupMemberStatus.ACTIVE);
        member.setJoinedAt(now());
        members.save(member);
    }

    @Transactional
    public void declineInvite(Long userId, Long groupId) {
        members.findByGroupIdAndUserId(groupId, userId)
                .filter(m -> m.getStatus() == GroupMemberStatus.INVITED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        members.deleteByGroupIdAndUserId(groupId, userId);
    }

    @Transactional
    public void leave(Long userId, Long groupId) {
        AccountabilityGroup group = groups.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        members.findByGroupIdAndUserId(groupId, userId)
                .filter(m -> m.getStatus() == GroupMemberStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (group.getOwnerId().equals(userId)) {
            throw new SocialException("Owners can't leave. Delete the group instead.");
        }
        members.deleteByGroupIdAndUserId(groupId, userId);
    }

    /** Removes an active member or cancels a pending invite. */
    @Transactional
    public void removeMember(Long ownerId, Long groupId, Long userId) {
        ownedGroup(groupId, ownerId);
        if (ownerId.equals(userId)) {
            throw new SocialException("You can't remove yourself. Delete the group instead.");
        }
        members.deleteByGroupIdAndUserId(groupId, userId);
    }

    // ----- the group page -----

    @Transactional(readOnly = true)
    public GroupView view(Long groupId, User viewer) {
        AccountabilityGroup group = groups.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        members.findByGroupIdAndUserId(groupId, viewer.getId())
                .filter(m -> m.getStatus() == GroupMemberStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<GroupMember> all = members.findByGroupIdOrderByCreatedAtAsc(groupId);
        List<GroupMember> active = all.stream().filter(m -> m.getStatus() == GroupMemberStatus.ACTIVE).toList();
        List<GroupMember> invitedRows = all.stream().filter(m -> m.getStatus() == GroupMemberStatus.INVITED).toList();
        Map<Long, User> userById = users.findAllById(all.stream().map(GroupMember::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // Dates are shown from the viewer's calendar; each member's workouts are placed
        // on *their* local day.
        LocalDate today = WeightService.todayFor(viewer.getTimeZone());
        List<LocalDate> dates = IntStream.rangeClosed(0, 13).mapToObj(i -> today.minusDays(13 - i)).toList();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime from = today.minusDays(15).atStartOfDay(); // generous UTC bound, trimmed per member below

        Map<Long, Set<LocalDate>> checkIns = new HashMap<>();
        for (WorkoutTime t : workouts.findTimes(active.stream().map(GroupMember::getUserId).toList(),
                WorkoutStatus.COMPLETED, from)) {
            ZoneId zone = ZoneOptions.zoneOf(userById.get(t.userId()).getTimeZone());
            LocalDate day = t.startedAt().atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalDate();
            checkIns.computeIfAbsent(t.userId(), k -> new HashSet<>()).add(day);
        }

        List<GroupMemberRow> rows = new ArrayList<>();
        for (GroupMember m : active) {
            User user = userById.get(m.getUserId());
            ZoneId zone = ZoneOptions.zoneOf(user.getTimeZone());
            // Only days from the day this member joined are shared.
            LocalDate joinedDay = m.getJoinedAt().atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalDate();
            Set<LocalDate> days = checkIns.getOrDefault(m.getUserId(), Set.of()).stream()
                    .filter(d -> !d.isBefore(joinedDay)).collect(Collectors.toSet());
            int weekCount = (int) days.stream().filter(d -> !d.isBefore(monday) && !d.isAfter(today)).count();
            rows.add(new GroupMemberRow(user, group.getOwnerId().equals(m.getUserId()),
                    days.contains(today), weekCount, weekCount >= group.getWeeklyGoal(),
                    dates.stream().map(days::contains).toList()));
        }
        rows.sort(Comparator.comparing(GroupMemberRow::owner).reversed()
                .thenComparing(r -> r.user().getUsername(), String.CASE_INSENSITIVE_ORDER));

        boolean isOwner = group.getOwnerId().equals(viewer.getId());
        List<User> invited = invitedRows.stream().map(m -> userById.get(m.getUserId())).toList();
        List<User> invitable = List.of();
        if (isOwner) {
            Set<Long> taken = all.stream().map(GroupMember::getUserId).collect(Collectors.toSet());
            invitable = friendService.friends(viewer.getId()).stream()
                    .filter(f -> !taken.contains(f.getId())).toList();
        }

        User owner = users.findById(group.getOwnerId()).orElseThrow();
        String capacity = all.size() + " of " + GroupLimits.describe(GroupLimits.maxMembers(owner.getRole()))
                + " spots used";
        return new GroupView(group.getId(), group.getName(), group.getDescription(), group.getWeeklyGoal(),
                isOwner, capacity, dates, rows, invited, invitable);
    }
}
