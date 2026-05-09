package de.reddev.tpamodule;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class TpaCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin core;
    private final TpaManager manager;

    public TpaCommand(JavaPlugin core, TpaManager manager) {
        this.core    = core;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        switch (label.toLowerCase()) {
            case "tpa"       -> handleTpa(player, args, TpaRequest.Type.TPA);
            case "tpahere"   -> handleTpa(player, args, TpaRequest.Type.TPAHERE);
            case "tpaccept"  -> handleAccept(player);
            case "tpdeny"    -> handleDeny(player);
            case "tpacancel" -> handleCancel(player);
        }

        return true;
    }

    // ─────────────────────────────────────────────────────────────

    private void handleTpa(Player sender, String[] args, TpaRequest.Type type) {
        if (args.length < 1) {
            sender.sendMessage(err("Usage: /" + (type == TpaRequest.Type.TPA ? "tpa" : "tpahere") + " <player>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(err("Player '" + args[0] + "' is not online."));
            return;
        }
        if (target.equals(sender)) {
            sender.sendMessage(err("You can't send a TPA request to yourself."));
            return;
        }

        TpaRequest request = new TpaRequest(sender, target, type);
        manager.addRequest(request);

        int timeout = manager.getTimeoutSeconds();

        if (type == TpaRequest.Type.TPA) {
            // /tpa — sender wants to teleport TO target
            sender.sendMessage(info("Teleport request sent to ")
                .append(Component.text(target.getName(), NamedTextColor.WHITE))
                .append(info(". Expires in " + timeout + "s.")));

            target.sendMessage(info(sender.getName() + " wants to teleport to you. ").color(NamedTextColor.YELLOW)
                .append(Component.text("/tpaccept", NamedTextColor.GREEN))
                .append(info(" or "))
                .append(Component.text("/tpdeny", NamedTextColor.RED)));
        } else {
            // /tpahere — sender wants target to teleport TO sender
            sender.sendMessage(info("Teleport-here request sent to ")
                .append(Component.text(target.getName(), NamedTextColor.WHITE))
                .append(info(". Expires in " + timeout + "s.")));

            target.sendMessage(Component.text(sender.getName(), NamedTextColor.WHITE)
                .append(info(" wants you to teleport to them. "))
                .append(Component.text("/tpaccept", NamedTextColor.GREEN))
                .append(info(" or "))
                .append(Component.text("/tpdeny", NamedTextColor.RED)));
        }

        // Auto-expire notification
        core.getServer().getScheduler().runTaskLater(core, () -> {
            // Only notify if the request is still pending (wasn't accepted/denied)
            manager.getRequest(target).ifPresent(r -> {
                if (r.getSender().equals(sender)) {
                    sender.sendMessage(err("Your TPA request to " + target.getName() + " expired."));
                    target.sendMessage(err("TPA request from " + sender.getName() + " expired."));
                }
            });
        }, timeout * 20L);
    }

    private void handleAccept(Player target) {
        Optional<TpaRequest> opt = manager.consumeRequest(target);
        if (opt.isEmpty()) {
            target.sendMessage(err("You have no pending TPA request."));
            return;
        }

        TpaRequest request = opt.get();
        Player sender = request.getSender();

        if (!sender.isOnline()) {
            target.sendMessage(err("That player is no longer online."));
            return;
        }

        target.sendMessage(ok("Teleport request accepted."));
        sender.sendMessage(ok(target.getName() + " accepted your teleport request."));

        // Small delay before teleporting (feels more natural, gives time to read message)
        core.getServer().getScheduler().runTaskLater(core, () -> {
            if (request.getType() == TpaRequest.Type.TPA) {
                // sender teleports to target
                sender.teleport(target.getLocation());
                sender.sendMessage(ok("Teleported to " + target.getName() + "."));
            } else {
                // target teleports to sender (/tpahere)
                target.teleport(sender.getLocation());
                target.sendMessage(ok("Teleported to " + sender.getName() + "."));
            }
        }, 20L); // 1 second delay
    }

    private void handleDeny(Player target) {
        Optional<TpaRequest> opt = manager.consumeRequest(target);
        if (opt.isEmpty()) {
            target.sendMessage(err("You have no pending TPA request."));
            return;
        }

        Player sender = opt.get().getSender();
        target.sendMessage(info("Teleport request denied."));
        if (sender.isOnline()) {
            sender.sendMessage(err(target.getName() + " denied your teleport request."));
        }
    }

    private void handleCancel(Player sender) {
        manager.cancelBySender(sender);
        sender.sendMessage(info("Your outgoing TPA request has been cancelled."));
    }

    // ─────────────────────────────────────────────────────────────
    //  Tab completion — suggest online player names
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String label,
                                      @NotNull String[] args) {
        if (args.length == 1 && (label.equalsIgnoreCase("tpa") || label.equalsIgnoreCase("tpahere"))) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equalsIgnoreCase(((Player) sender).getName()))
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
        }
        return List.of();
    }

    // ─────────────────────────────────────────────────────────────

    private Component ok(String text)   { return Component.text(text, NamedTextColor.GREEN); }
    private Component err(String text)  { return Component.text(text, NamedTextColor.RED); }
    private Component info(String text) { return Component.text(text, NamedTextColor.YELLOW); }
}
