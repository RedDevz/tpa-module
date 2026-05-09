package de.reddev.tpamodule;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores pending TPA requests keyed by the TARGET's UUID.
 * Only one incoming request per target is tracked at a time —
 * a new request to the same target overwrites the previous one.
 */
public class TpaManager {

    // target UUID → pending request directed at them
    private final Map<UUID, TpaRequest> pendingRequests = new ConcurrentHashMap<>();
    private final int timeoutSeconds;

    public TpaManager(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /** Stores a new request, replacing any existing one for this target. */
    public void addRequest(TpaRequest request) {
        pendingRequests.put(request.getTarget().getUniqueId(), request);
    }

    /**
     * Returns the pending request for the given target player, if one exists
     * and hasn't expired yet. Expired requests are removed automatically.
     */
    public Optional<TpaRequest> getRequest(Player target) {
        TpaRequest req = pendingRequests.get(target.getUniqueId());
        if (req == null) return Optional.empty();
        if (req.isExpired(timeoutSeconds)) {
            pendingRequests.remove(target.getUniqueId());
            return Optional.empty();
        }
        return Optional.of(req);
    }

    /** Removes and returns the pending request for the given target, if any. */
    public Optional<TpaRequest> consumeRequest(Player target) {
        TpaRequest req = pendingRequests.remove(target.getUniqueId());
        if (req == null || req.isExpired(timeoutSeconds)) return Optional.empty();
        return Optional.of(req);
    }

    /** Removes any request where the given player is the sender. */
    public void cancelBySender(Player sender) {
        pendingRequests.values().removeIf(r -> r.getSender().getUniqueId().equals(sender.getUniqueId()));
    }

    /** Removes any request where the given player is involved (sender or target). */
    public void removeAll(Player player) {
        UUID id = player.getUniqueId();
        pendingRequests.values().removeIf(r ->
            r.getSender().getUniqueId().equals(id) || r.getTarget().getUniqueId().equals(id)
        );
    }

    public Collection<TpaRequest> getAll() {
        return pendingRequests.values();
    }

    public void clear() {
        pendingRequests.clear();
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
