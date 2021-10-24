package net.pavela.suspicious;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.delay.StaticDelay;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.io.IOException;

public class suspiciousIRC extends ListenerAdapter implements Runnable {
    public static PircBotX bot;

    public suspiciousIRC() {
    }

    public static void main(String[] args) {
        try {
            Configuration configuration = new Configuration.Builder()
                    .setName(args[0]) // Set the nick of the bot.
                    .addServer(args[1]) // Join the Libera.chat network by default.
                    .addAutoJoinChannel(args[2]) // Join the test channel.
                    .setMessageDelay(new StaticDelay(500L)) // half a second delay
                    .addListener((Listener) new suspiciousIRC()) // Add our listener that will be called on Events
                    .setAutoReconnect(true)
                    .buildConfiguration();

            //Create our bot with the configuration
            bot = new PircBotX(configuration);
            //Connect to the server
            bot.startBot();
        } catch (IOException | IrcException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGenericMessage(GenericMessageEvent event) {
        // Bukkit.getConsoleSender().sendMessage(event.getMessage());
        if (event.getMessage().startsWith("?help")) {
            event.respond("Help menu");
        }
    }

    @Override
    public void onConnect(ConnectEvent event){
        System.out.println("Connected!");
    }

    @Override
    public void run() {
    }
}
