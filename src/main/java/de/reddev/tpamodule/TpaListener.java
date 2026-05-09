package de.reddev.tpamodule;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class TpaListener implements Listener {

    private final TpaManager manager;

    public TpaListener(TpaManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remove any requests the leaving player was part of
        manager.removeAll(event.getPlayer());
    }
}
