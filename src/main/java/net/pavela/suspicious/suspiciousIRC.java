package net.pavela.suspicious;

import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.delay.StaticDelay;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class suspiciousIRC extends ListenerAdapter implements Runnable {
    public static PircBotX bot;

    public static final String URL_REGEX = "(http(s)?:\\/\\/.)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)";

    public static List<String> maliciousMessage = Arrays.asList("❌ \u000305\u0002The link in this message is \u001Fmalicious\u000F\u0002 ❌",
            "\u000FThe link sent in the message above has been found to be in a \u0002malicious\u0002 URL blocklist. ",
            "\u000FThis link could attempt to steal your data, run an exploit or get you to download malware. ",
            "\u000FAvoid the URL and the message contents. If you wish to visit the link, do so using the Tor ",
            "\u000Fbrowser and do not download anything or enter any personal info. Please use your common sense!");

    public static List<String> advertisementMessage = Arrays.asList("⚠️ \u000308\u0002The link in this message is an advertising link ⚠ ️️",
            "\u000FThe link sent in the message above has been found in an advertising blocklist. ",
            "\u000FThe purpose of this link is to collect telemetry and serve advertisements. ",
            "\u000FPlease use caution if browsing these links.");


    public static List<String> Channels = new ArrayList<String>();

    public static String fileName;

    public static void save(String fileName) {
        try {
            FileOutputStream fout= new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(Channels);
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static void read(String fileName) throws IOException, ClassNotFoundException {
        FileInputStream fin= new FileInputStream (fileName);
        ObjectInputStream ois = new ObjectInputStream(fin);
        Channels = (ArrayList<String>)ois.readObject();
        fin.close();
    }

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
                    .setMessageDelay(new StaticDelay(500L)) // half a second delay
                    .addListener((Listener) new suspiciousIRC()) // Add our listener that will be called on Events
                    .setAutoReconnect(true)
                    .buildConfiguration();

            //Create our bot with the configuration
            bot = new PircBotX(configuration);

            fileName = args[2];
            try {
                read(fileName);
            } catch (FileNotFoundException | EOFException e) {
                File file = new File(fileName);
                file.createNewFile();
            }


            //Connect to the server
            bot.startBot();
        } catch (IOException | IrcException | ClassNotFoundException e) {
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
            event.respond("\u0002\u000308✅ ?join <channel>\u000F joins a channel, can also join using /invite");
            event.respond("\u0002\u000308✅ ?list\u000F displays all channels the bot is in");
        }
        if (event.getMessage().startsWith("?join")) {
            event.respond("✅ Joining channel");
            String tempChannel = event.getMessage().replace("?join ", "");
            System.out.println("Joining channel:");
            System.out.println(tempChannel);
            bot.sendIRC().joinChannel(tempChannel);
            Channels.add(tempChannel);
            save(fileName);
        }
        if (event.getMessage().startsWith("?list")) {
            List<String> currentChannels = new ArrayList<String>();
            for (Channel channel : bot.getUserBot().getChannels()) {
                 currentChannels.add(channel.getName());
            }

            String listString = currentChannels.stream().map(Object::toString)
                    .collect(Collectors.joining(", "));
            event.respond(String.format("Channels: %s", listString));
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

        if (event.getMessage().startsWith("?scan")) {
            if (!malicious & !advertising) {
                event.respond("✅\u000303\u0002 Link has not been found in blocklists!");
            }
        }
    }

    @Override
    public void onInvite(InviteEvent event) {
        System.out.println("Joining channel:");
        System.out.println(event.getChannel());
        bot.sendIRC().joinChannel(event.getChannel());
        Channels.add(event.getChannel());
        save(fileName);
    }

    @Override
    public void onConnect(ConnectEvent event){
        System.out.println("Connected!");
        for (String channel : Channels) {
            bot.sendIRC().joinChannel(channel);
        }
    }

    @Override
    public void onKick(KickEvent event){
        // If we got kicked from a channel, remember to not rejoin it automatically
        if (Objects.equals(event.getRecipient().getNick(), bot.getNick())) {
            Channels.removeIf(channel -> channel.equals(event.getChannel().getName()));
            System.out.println(String.format("%s has kicked us from %s for reason %s", event.getUser().getNick(), event.getChannel().getName(), event.getReason()));
            save(fileName);
        }

    }


    @Override
    public void run() {
    }
}
