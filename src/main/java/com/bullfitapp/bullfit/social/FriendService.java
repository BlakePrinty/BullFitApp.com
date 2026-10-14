package com.bullfitapp.bullfit.social;

import com.bullfitapp.bullfit.user.User;
import com.bullfitapp.bullfit.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private static final int MAX_PENDING_OUTGOING = 25;
    private static final int DECLINE_COOLDOWN_DAYS = 7;

    private final FriendRequestRepository requestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserBlockRepository blockRepository;
    private final UserRepository userRepository;

    public FriendService(FriendRequestRepository requestRepository,
            FriendshipRepository friendshipRepository,
            UserBlockRepository blockRepository, UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.friendshipRepository = friendshipRepository;
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
    }

    private static Long lo(Long a, Long b) {
        return a < b ? a : b;
    }

    private static Long hi(Long a, Long b) {
        return a < b ? b : a;
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public boolean areFriends(Long a, Long b) {
        return friendshipRepository.existsByUserAIdAndUserBId(lo(a, b), hi(a, b));
    }

    // ----- requests -----

    @Transactional
    public void sendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new SocialException("You can't add yourself.");
        }
        userRepository.findById(receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (blockRepository.existsByBlockerIdAndBlockedId(senderId, receiverId)) {
            throw new SocialException("You've blocked this person. Unblock them first.");
        }
        if (blockRepository.existsByBlockerIdAndBlockedId(receiverId, senderId)) {
            throw new SocialException("You can't send a request to this person.");
        }
        if (areFriends(senderId, receiverId)) {
            throw new SocialException("You're already friends.");
        }

        // They already asked you: both sides want it, so just accept theirs.
        Optional<FriendRequest> reverse = requestRepository.findFirstBySenderIdAndReceiverIdAndStatus(
                receiverId, senderId, FriendRequestStatus.PENDING);
        if (reverse.isPresent()) {
            acceptInternal(reverse.get());
            return;
        }

        if (requestRepository.findFirstBySenderIdAndReceiverIdAndStatus(
                senderId, receiverId, FriendRequestStatus.PENDING).isPresent()) {
            throw new SocialException("You've already sent this person a request.");
        }
        if (requestRepository.countBySenderIdAndStatus(senderId, FriendRequestStatus.PENDING) >= MAX_PENDING_OUTGOING) {
            throw new SocialException("You have " + MAX_PENDING_OUTGOING
                    + " pending requests. Wait for replies or cancel some first.");
        }
        if (requestRepository.existsBySenderIdAndReceiverIdAndStatusAndRespondedAtAfter(
                senderId, receiverId, FriendRequestStatus.DECLINED, now().minusDays(DECLINE_COOLDOWN_DAYS))) {
            throw new SocialException("You can send this person another request later.");
        }

        FriendRequest request = new FriendRequest();
        request.setSenderId(senderId);
        request.setReceiverId(receiverId);
        request.setCreatedAt(now());
        requestRepository.save(request);
    }

    @Transactional
    public void accept(Long userId, Long requestId) {
        acceptInternal(pendingForReceiver(userId, requestId));
    }

    @Transactional
    public void decline(Long userId, Long requestId) {
        FriendRequest request = pendingForReceiver(userId, requestId);
        request.setStatus(FriendRequestStatus.DECLINED);
        request.setRespondedAt(now());
        requestRepository.save(request);
    }

    @Transactional
    public void cancel(Long userId, Long requestId) {
        FriendRequest request = requestRepository.findByIdAndSenderId(requestId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new SocialException("That request is no longer pending.");
        }
        request.setStatus(FriendRequestStatus.CANCELLED);
        request.setRespondedAt(now());
        requestRepository.save(request);
    }

    private FriendRequest pendingForReceiver(Long userId, Long requestId) {
        FriendRequest request = requestRepository.findByIdAndReceiverId(requestId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new SocialException("That request is no longer pending.");
        }
        return request;
    }

    private void acceptInternal(FriendRequest request) {
        Long a = lo(request.getSenderId(), request.getReceiverId());
        Long b = hi(request.getSenderId(), request.getReceiverId());
        if (!friendshipRepository.existsByUserAIdAndUserBId(a, b)) {
            Friendship friendship = new Friendship();
            friendship.setUserAId(a);
            friendship.setUserBId(b);
            friendship.setCreatedAt(now());
            friendshipRepository.save(friendship);
        }
        request.setStatus(FriendRequestStatus.ACCEPTED);
        request.setRespondedAt(now());
        requestRepository.save(request);
    }

    // ----- unfriending and blocking -----

    @Transactional
    public void unfriend(Long userId, Long otherId) {
        friendshipRepository.deleteByUserAIdAndUserBId(lo(userId, otherId), hi(userId, otherId));
    }

    @Transactional
    public void block(Long userId, Long otherId) {
        if (userId.equals(otherId)) {
            throw new SocialException("You can't block yourself.");
        }
        userRepository.findById(otherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!blockRepository.existsByBlockerIdAndBlockedId(userId, otherId)) {
            UserBlock block = new UserBlock();
            block.setBlockerId(userId);
            block.setBlockedId(otherId);
            block.setCreatedAt(now());
            blockRepository.save(block);
        }
        unfriend(userId, otherId);
        cancelPending(userId, otherId);
        cancelPending(otherId, userId);
    }

    private void cancelPending(Long senderId, Long receiverId) {
        requestRepository.findFirstBySenderIdAndReceiverIdAndStatus(
                senderId, receiverId, FriendRequestStatus.PENDING).ifPresent(r -> {
                    r.setStatus(FriendRequestStatus.CANCELLED);
                    r.setRespondedAt(now());
                    requestRepository.save(r);
                });
    }

    @Transactional
    public void unblock(Long userId, Long otherId) {
        blockRepository.deleteByBlockerIdAndBlockedId(userId, otherId);
    }

    // ----- reading -----

    private record Context(Set<Long> friends, Map<Long, Long> outgoing, Map<Long, Long> incoming,
            Set<Long> blockedByMe) {
    }

    private Context context(Long viewerId) {
        Set<Long> friends = friendshipRepository.findByUserAIdOrUserBId(viewerId, viewerId).stream()
                .map(f -> f.getUserAId().equals(viewerId) ? f.getUserBId() : f.getUserAId())
                .collect(Collectors.toSet());
        Map<Long, Long> outgoing = requestRepository
                .findBySenderIdAndStatusOrderByCreatedAtDesc(viewerId, FriendRequestStatus.PENDING).stream()
                .collect(Collectors.toMap(FriendRequest::getReceiverId, FriendRequest::getId, (x, y) -> x));
        Map<Long, Long> incoming = requestRepository
                .findByReceiverIdAndStatusOrderByCreatedAtDesc(viewerId, FriendRequestStatus.PENDING).stream()
                .collect(Collectors.toMap(FriendRequest::getSenderId, FriendRequest::getId, (x, y) -> x));
        Set<Long> blockedByMe = blockRepository.findByBlockerIdOrderByCreatedAtDesc(viewerId).stream()
                .map(UserBlock::getBlockedId).collect(Collectors.toSet());
        return new Context(friends, outgoing, incoming, blockedByMe);
    }

    private PersonItem toPerson(Context c, Long viewerId, User user) {
        Long id = user.getId();
        if (id.equals(viewerId))
            return new PersonItem(user, Relationship.SELF, null);
        if (c.blockedByMe().contains(id))
            return new PersonItem(user, Relationship.BLOCKED_BY_ME, null);
        if (c.friends().contains(id))
            return new PersonItem(user, Relationship.FRIENDS, null);
        if (c.outgoing().containsKey(id))
            return new PersonItem(user, Relationship.REQUEST_SENT, c.outgoing().get(id));
        if (c.incoming().containsKey(id))
            return new PersonItem(user, Relationship.REQUEST_RECEIVED, c.incoming().get(id));
        return new PersonItem(user, Relationship.NONE, null);
    }

    @Transactional(readOnly = true)
    public PersonItem personFor(Long viewerId, User other) {
        return toPerson(context(viewerId), viewerId, other);
    }

    @Transactional(readOnly = true)
    public List<PersonItem> search(Long viewerId, String query) {
        Context c = context(viewerId);
        Set<Long> blockedMe = blockRepository.findByBlockedId(viewerId).stream()
                .map(UserBlock::getBlockerId).collect(Collectors.toSet());
        return userRepository.findTop20ByUsernameContainingIgnoreCaseOrderByUsername(query).stream()
                .filter(u -> !blockedMe.contains(u.getId()))
                .map(u -> toPerson(c, viewerId, u)).toList();
    }

    @Transactional(readOnly = true)
    public List<User> friends(Long viewerId) {
        Set<Long> ids = context(viewerId).friends();
        return userRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Transactional(readOnly = true)
    public List<User> blocked(Long viewerId) {
        Set<Long> ids = context(viewerId).blockedByMe();
        return userRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Transactional(readOnly = true)
    public List<RequestItem> incoming(Long viewerId) {
        List<FriendRequest> requests = requestRepository
                .findByReceiverIdAndStatusOrderByCreatedAtDesc(viewerId, FriendRequestStatus.PENDING);
        Map<Long, User> users = userRepository.findAllById(
                requests.stream().map(FriendRequest::getSenderId).toList()).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return requests.stream()
                .map(r -> new RequestItem(r.getId(), users.get(r.getSenderId()), r.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<RequestItem> outgoing(Long viewerId) {
        List<FriendRequest> requests = requestRepository
                .findBySenderIdAndStatusOrderByCreatedAtDesc(viewerId, FriendRequestStatus.PENDING);
        Map<Long, User> users = userRepository.findAllById(
                requests.stream().map(FriendRequest::getReceiverId).toList()).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return requests.stream()
                .map(r -> new RequestItem(r.getId(), users.get(r.getReceiverId()), r.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public long pendingIncomingCount(Long viewerId) {
        return requestRepository.countByReceiverIdAndStatus(viewerId, FriendRequestStatus.PENDING);
    }
}
