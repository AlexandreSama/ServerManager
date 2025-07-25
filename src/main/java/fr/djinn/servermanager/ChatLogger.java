package fr.djinn.servermanager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ChatLogger implements Listener {

    private static final LinkedList<String> messages = new LinkedList<>();

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (messages.size() >= 20) messages.removeFirst();
        messages.add("[" + event.getPlayer().getName() + "] " + event.getMessage());
    }

    public static List<String> getLastMessages() {
        return new ArrayList<>(messages);
    }
}