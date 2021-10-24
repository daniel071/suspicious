package net.pavela.suspicious;

import net.dv8tion.jda.api.interactions.components.Button;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.delay.StaticDelay;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class suspiciousIRC extends ListenerAdapter implements Runnable {
    public static PircBotX bot;

    public static final String URL_REGEX = "(http(s)?:\\/\\/.)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)";

    public static List<String> maliciousMessage = Arrays.asList("❌ \u000305\u0002The link in this message is \u001Fmalicious\u000F\u0002 ❌",
            "The link sent in the message above has been found to be in a \u0002malicious\u0002 URL blocklist. ",
            "This link could attempt to steal your data, run an exploit or get you to download malware. ",
            "Avoid the URL and the message contents. If you wish to visit the link, do so using the Tor ",
            "browser and do not download anything or enter any personal info. Please use your common sense!");

    public static List<String> advertisementMessage = Arrays.asList("⚠️ \u000308\u0002The link in this message is an advertising link ⚠ ️️",
            "The link sent in the message above has been found in an advertising blocklist. ",
            "The purpose of this link is to collect telemetry and serve advertisements. ",
            "Please use caution if browsing these links.");


    public static String getDomainName(String givenURL) throws URISyntaxException {
        String url = givenURL;
        if (!url.toLowerCase().matches("^\\w+://.*")) {
            url = "http://" + givenURL;
        }
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    public List<String> search(Path fileName, String stringToSearch) {
        List<String> susURLs = null;
        try {
            String pathToSearch = getDomainName(stringToSearch);
            // System.out.println(pathToSearch);

            susURLs = Files.lines(fileName)
                    // findFirst() can be used get the first match and stop.
                    .filter(line -> line.contentEquals(pathToSearch.toLowerCase()))
                    .collect(Collectors.toList());


        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return susURLs;
    }

    public boolean searchDirectory(String searchText, String directory) {
        boolean status = false;
        File dir = null;
        try {
            dir = new File(getClass().getResource(directory).toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                //System.out.println(child.getAbsolutePath());
                if (!search(Paths.get(child.getAbsolutePath()), searchText).isEmpty()) {
                    status = true;
                }
            }
        } else {
            System.out.println("Directory specified does not exist!");
        }
        return status;
    }

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
            event.respond("\u0002Suspicious? IRC Bot");
            event.respond("This bot scans messages and warns if there are any suspicious links.");
            event.respond("\u000303\u0002Commands:");
            event.respond("\u0002\u000308✅ ?help\u000F displays this menu");
            event.respond("\u0002\u000308✅ ?scan <url>\u000F checks if <url> is in any malicious or advertising blocklists");
        }
        if (event.getMessage().startsWith("?scan")) {
            event.respond("Feature not yet implemented.");
        }

        boolean malicious = false;
        boolean advertising = false;

        Pattern p = Pattern.compile(URL_REGEX);
        Matcher m = p.matcher(event.getMessage());


        while (m.find()) {
            malicious = searchDirectory(m.group(0), "/blocklists/malicious/");
            advertising = searchDirectory(m.group(0), "/blocklists/advertising/");
        }

        if (malicious) {
            for (String msg : maliciousMessage) {
                event.respond(msg);
            }
        }
        if (advertising) {
            for (String msg : advertisementMessage) {
                event.respond(msg);
            }
        }

    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent event) {
    }

    @Override
    public void onConnect(ConnectEvent event){
        System.out.println("Connected!");
    }

    @Override
    public void run() {
    }
}
