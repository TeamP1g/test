package me.earth.pingbypass.commons.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.earth.pingbypass.api.command.CommandManager;
import me.earth.pingbypass.api.command.CommandSource;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.commons.event.chat.ChatEvent;
import me.earth.pingbypass.commons.event.chat.CommandSuggestionEvent;
import net.minecraft.network.chat.ComponentUtils;

public class CommandEventService extends SubscriberImpl {
    public CommandEventService(CommandManager commandManager, CommandSource source) {
        listen(new Listener<ChatEvent>() {
            @Override
            public void onEvent(ChatEvent event) {
                String prefix = commandManager.getPrefix();
                if (event.getMessage().startsWith(prefix) && !event.isCancelled()) {
                    try {
                        commandManager.execute(event.getMessage().substring(prefix.length()), source);
                    } catch (CommandSyntaxException e) {
                        source.getMinecraft().gui.getChat().addMessage(ComponentUtils.fromMessage(e.getRawMessage()));
                    }

                    event.setCancelled(true);
                }
            }
        });
        listen(new Listener<CommandSuggestionEvent>() {
            @Override
            public void onEvent(CommandSuggestionEvent event) {
                String prefix = commandManager.getPrefix();
                int length = prefix.length();
                StringReader r = event.getStringReader();
                if (!event.isCancelled() && r.canRead(length) && r.getString().startsWith(prefix, r.getCursor())) {
                    r.setCursor(r.getCursor() + length);
                    if (event.getCurrentParse() == null || event.getCustomParse() == null) {
                        event.setCustomParse(commandManager.parse(r, source));
                        event.setCurrentParse(ParseResultUtil.dummy());
                    }

                    int cursor = event.getInput().getCursorPosition();
                    if (cursor >= 1 && (event.getSuggestions() == null || !event.isKeepSuggestions())) {
                        var suggestions = commandManager.getCompletionSuggestions(event.getCustomParse(), cursor);
                        event.setUpdatingPendingSuggestions(true);
                        event.setPendingSuggestions(suggestions);
                    }

                    event.setCancelled(true);
                }
            }
        });
    }

}
