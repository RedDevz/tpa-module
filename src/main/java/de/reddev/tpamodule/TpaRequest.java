package de.reddev.tpamodule;

import org.bukkit.entity.Player;

public class TpaRequest {

    public enum Type { TPA, TPAHERE }

    private final Player sender;
    private final Player target;
    private final Type type;
    private final long createdAt;

    public TpaRequest(Player sender, Player target, Type type) {
        this.sender    = sender;
        this.target    = target;
        this.type      = type;
        this.createdAt = System.currentTimeMillis();
    }

    public Player getSender()  { return sender; }
    public Player getTarget()  { return target; }
    public Type getType()      { return type; }
    public long getCreatedAt() { return createdAt; }

    /** Returns true if this request is older than the given timeout in seconds. */
    public boolean isExpired(int timeoutSeconds) {
        return System.currentTimeMillis() - createdAt > timeoutSeconds * 1000L;
    }
}
