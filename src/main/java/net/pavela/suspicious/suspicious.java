/*
  Discord bot for scanning malicious or advertising urls and files.
  Scans in block lists and more.
  Collection of blocklists: https://firebog.net/

  ### Blocklists ###
  High severity - Malicious (Phishing, scams, malware)
  https://urlhaus.abuse.ch/downloads/text/
  https://github.com/Spam404/lists/blob/master/main-blacklist.txt

  Medium severity - Advertising (Trackers, advertisements, etc.)
  https://adaway.org/hosts.txt
  https://v.firebog.net/hosts/Easyprivacy.txt
  https://v.firebog.net/hosts/Prigent-Ads.txt

  Low severity - Unwanted content (Link shortners, junk, nsfw)
 */

package net.pavela.suspicious;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class suspicious extends ListenerAdapter {
    public static JDA jda;
    public static final String URL_REGEX = "(http(s)?:\\/\\/.)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)";

    public static String maliciousMessage = ":x: **The link in this message is __malicious__** :x:\n" +
            "The link sent in the message above has been found to be in a **malicious** URL blocklist. " +
            "This link could attempt to steal your data, run an exploit or get you to download malware. " +
            "Avoid the URL and the message contents. If you wish to visit the link, do so using the Tor " +
            "browser and do not download anything or enter any personal info. Please use your common sense!";

    public static String advertisementMessage = "⚠️ **The link in this message is known as an advertising link** ⚠️\n" +
            "The link sent in the message above has been found in an advertising blocklist. The purpose of this link is to collect telemetry and serve advertisemsnts. Please use caution if browsing these links.";

    public static void main(String[] args) throws LoginException {
        Map<String, String> env = System.getenv();
        jda = JDABuilder.createDefault(args[0])
                .addEventListeners(new suspicious()).build();

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

    public List<String> search(String fileName, String stringToSearch) {
        List<String> susURLs = null;
        try {
            String pathToSearch = getDomainName(stringToSearch);
            System.out.println(pathToSearch);

            susURLs = Files.lines(Paths.get(ClassLoader.getSystemResource(fileName).toURI()))
                    // findFirst() can be used get the first match and stop.
                    .filter(line -> line.contentEquals(pathToSearch.toLowerCase()))
                    .collect(Collectors.toList());

//            for (String sus : susURLs ) {
//                System.out.println(sus);
//            }

        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return susURLs;
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("API is ready!");
        jda.getPresence().setActivity(Activity.playing("Among Us"));

    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if (event.isFromType(ChannelType.PRIVATE))
        {
            System.out.printf("[PM] %s: %s\n", event.getAuthor().getName(),
                    event.getMessage().getContentDisplay());
        }
        else
        {
            System.out.printf("[%s][%s] %s: %s\n", event.getGuild().getName(),
                    event.getTextChannel().getName(), event.getMember().getEffectiveName(),
                    event.getMessage().getContentDisplay());

            boolean malicious = false;
            boolean advertising = false;

            Pattern p = Pattern.compile(URL_REGEX);
            Matcher m = p.matcher(event.getMessage().getContentDisplay());
            List<String> sus = new ArrayList<>();


            while (m.find()) {
                List<String> search1 = search("blocklists/malicious/spam404.txt", m.group(0));
                List<String> search2 = search("blocklists/malicious/urlhaus.txt", m.group(0));
                //System.out.println(m.group(0));
                if (!search1.isEmpty()) {
                    malicious = true;
                }
                if (!search2.isEmpty()) {
                    malicious = true;
                }
                sus.add(m.group(0));
            }

            if (malicious) {
                event.getMessage().reply(maliciousMessage)
                        .setActionRow(
                                Button.danger("Delete", "Delete"),
                                Button.secondary("Dismiss", "Dismiss")
                        )
                        .queue();
            }
        }
    }

    @Override
    public void onButtonClick(ButtonClickEvent event) {
        System.out.println(event.getMember().getEffectiveName());
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            if (event.getComponentId().equals("Delete")) {
                event.deferEdit().queue();
                Message message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
                message.getReferencedMessage().delete().queue();
                event.getMessage().delete().queue();

            } else if (event.getComponentId().equals("Dismiss")) {
                event.deferEdit().queue();
                event.getMessage().delete().queue();
            }
        } else {
            event.deferEdit().queue();
            PrivateChannel channel = event.getUser().openPrivateChannel().complete();
            channel.sendMessage("You do not have permission to run this command!").queue();
        }

    }

}