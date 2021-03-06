package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitPlayer extends FawePlayer<Player> {

    private static ConsoleCommandSender console;

    public BukkitPlayer(final Player parent) {
        super(parent);
    }

    @Override
    public String getName() {
        return this.parent.getName();
    }

    @Override
    public UUID getUUID() {
        return this.parent.getUniqueId();
    }

    @Override
    public boolean hasPermission(final String perm) {
        return this.parent.hasPermission(perm);
    }

    @Override
    public boolean isSneaking() {
        return parent.isSneaking();
    }

    @Override
    public void setPermission(final String perm, final boolean flag) {
        /*
         *  Permissions are used to managing WorldEdit region restrictions
         *   - The `/wea` command will give/remove the required bypass permission
         */
        if (Fawe.<FaweBukkit> imp().getVault() == null || Fawe.<FaweBukkit> imp().getVault().permission == null) {
            this.parent.addAttachment(Fawe.<FaweBukkit> imp().getPlugin()).setPermission(perm, flag);
        } else if (flag) {
            if (!Fawe.<FaweBukkit> imp().getVault().permission.playerAdd(this.parent, perm)) {
                this.parent.addAttachment(Fawe.<FaweBukkit> imp().getPlugin()).setPermission(perm, flag);
            }
        } else {
            if (!Fawe.<FaweBukkit> imp().getVault().permission.playerRemove(this.parent, perm)) {
                this.parent.addAttachment(Fawe.<FaweBukkit> imp().getPlugin()).setPermission(perm, flag);
            }
        }
    }


    @Override
    public void resetTitle() {
        parent.resetTitle();
    }

    public void sendTitle(String title, String sub) {
        parent.sendTitle(ChatColor.GOLD + title, ChatColor.GOLD + sub, 0, 70, 20);
        if (console == null) {
            console = Bukkit.getConsoleSender();
            Bukkit.getServer().dispatchCommand(console, "gamerule sendCommandFeedback false");
            Bukkit.getServer().dispatchCommand(console, "title " + getName() + " times 0 60 20");
        }
        Bukkit.getServer().dispatchCommand(console, "title " + getName() + " subtitle [{\"text\":\"" + sub + "\",\"color\":\"gold\"}]");
        Bukkit.getServer().dispatchCommand(console, "title " + getName() + " title [{\"text\":\"" + title + "\",\"color\":\"gold\"}]");
    }

    @Override
    public void sendMessage(final String message) {
        this.parent.sendMessage(BBC.color(message));
    }

    @Override public void printError(String msg) {
        this.sendMessage(msg);
    }

    @Override
    public void executeCommand(final String cmd) {
        Bukkit.getServer().dispatchCommand(this.parent, cmd);
    }

    @Override
    public com.sk89q.worldedit.entity.Player toWorldEditPlayer() {
        return WorldEditPlugin.getInstance().wrapPlayer(this.parent);
    }

}
