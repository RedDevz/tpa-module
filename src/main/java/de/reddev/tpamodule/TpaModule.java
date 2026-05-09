package de.reddev.tpamodule;

import de.reddev.api.Module;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class TpaModule implements Module {

    private JavaPlugin core;
    private TpaManager manager;
    private final List<Command> registeredCommands = new ArrayList<>();

    @Override
    public String getName()    { return "tpa-module"; }

    @Override
    public String getVersion() { return "1.0.0"; }

    @Override
    public void onEnable(JavaPlugin core) {
        this.core = core;

        manager = new TpaManager(30); // 30 second request timeout

        TpaCommand handler = new TpaCommand(core, manager);
        core.getServer().getPluginManager().registerEvents(new TpaListener(manager), core);

        CommandMap commandMap = core.getServer().getCommandMap();
        registerCmd(commandMap, "tpa",       "Request to teleport to a player.",     handler);
        registerCmd(commandMap, "tpahere",   "Request a player to teleport to you.", handler);
        registerCmd(commandMap, "tpaccept",  "Accept an incoming TPA request.",      handler);
        registerCmd(commandMap, "tpdeny",    "Deny an incoming TPA request.",        handler);
        registerCmd(commandMap, "tpacancel", "Cancel your outgoing TPA request.",    handler);

        core.getLogger().info("[TpaModule] Enabled. Timeout: 30s");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.clear();

        // Unregister commands from Bukkit's command map
        CommandMap commandMap = core.getServer().getCommandMap();
        for (Command cmd : registeredCommands) {
            cmd.unregister(commandMap);
        }
        registeredCommands.clear();

        core.getLogger().info("[TpaModule] Disabled.");
    }

    // ─────────────────────────────────────────────────────────────

    private void registerCmd(CommandMap map, String name, String desc, TpaCommand handler) {
        Command command = new Command(name) {
            { setDescription(desc); }

            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                return handler.onCommand(sender, this, label, args);
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                return handler.onTabComplete(sender, this, alias, args);
            }
        };
        map.register("tpamodule", command);
        registeredCommands.add(command);
    }
}
