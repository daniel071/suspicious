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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class suspicious extends ListenerAdapter {
    public static JDA jda;
    public static final String URL_REGEX = "(http(s)?:\\/\\/.)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)";

    public static String maliciousMessage = ":x: **The link in this message is __malicious__** :x:\n" +
            "The link sent in the message above has been found to be in a **malicious** URL blocklist. " +
            "This link could attempt to steal your data, run an exploit or get you to download malware. " +
            "Avoid the URL and the message contents. If you wish to visit the link, do so using the Tor " +
            "browser and do not download anything or enter any personal info. Please use your common sense!";

    public static String advertisementMessage = "⚠️ **The link in this message is an advertising link** ⚠️\n" +
            "The link sent in the message above has been found in an advertising blocklist. The purpose of this link is to collect telemetry and serve advertisements. Please use caution if browsing these links.";

    public static void main(String[] args) throws LoginException {
        Map<String, String> env = System.getenv();
        jda = JDABuilder.createDefault(args[0])
                .addEventListeners(new suspicious()).build();

        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(
                new CommandData("scan", "Scan a link to see if it is malicious")
                        .addOptions(new OptionData(STRING, "link", "The user to ban")
                                .setRequired(true)) // This command requires a parameter
        );
        commands.addCommands(
                new CommandData("help", "Displays the help menu")
        );
//        commands.queue();

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

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("API is ready!");
        jda.getPresence().setActivity(Activity.playing("Among Us"));

    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            System.out.printf("[PM] %s: %s\n", event.getAuthor().getName(),
                    event.getMessage().getContentDisplay());
        } else {
            if (event.getMessage().getMentionedUsers().contains(jda.getSelfUser())) {
                event.getMessage().reply("Run /help for more info.").queue();
            }

            System.out.printf("[%s][%s] %s: %s\n", event.getGuild().getName(),
                    event.getTextChannel().getName(), event.getMember().getEffectiveName(),
                    event.getMessage().getContentDisplay());

            boolean malicious = false;
            boolean advertising = false;

            Pattern p = Pattern.compile(URL_REGEX);
            Matcher m = p.matcher(event.getMessage().getContentDisplay());


            while (m.find()) {
                malicious = searchDirectory(m.group(0), "/blocklists/malicious/");
                advertising = searchDirectory(m.group(0), "/blocklists/advertising/");
            }

            if (malicious) {
                event.getMessage().reply(maliciousMessage)
                        .setActionRow(
                                Button.danger("Delete", "Delete"),
                                Button.secondary("Dismiss", "Dismiss")
                        )
                        .queue();
            }
            if (advertising) {
                event.getMessage().reply(advertisementMessage)
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

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        switch (event.getName()) {
            case "scan":
                boolean malicious = false;
                boolean advertising = false;
                String link = event.getOption("link").getAsString();

                malicious = searchDirectory(link, "/blocklists/malicious/");
                advertising = searchDirectory(link, "/blocklists/advertising/");

                if (malicious) {
                    event.reply(maliciousMessage).queue();
                } else if (advertising) {
                    event.reply(advertisementMessage).queue();
                } else {
                    event.reply(":white_check_mark: Link is not in blocklists").queue();
                }
                break;

            case "help":
                // Embed info: https://gist.github.com/zekroTJA/c8ed671204dafbbdf89c36fc3a1827e1
                EmbedBuilder eb = new EmbedBuilder();

                eb.setTitle("Suspicious? Help", null);
                eb.setColor(Color.red);
                eb.setDescription("I'm definitely not an imposter, If I was, then I wouldn't be one 'cause I'm not one");

                eb.addField("/scan <link>", "Scan a link in blocklists and VirusTotal", false);
                eb.addField("/help", "Shows this menu", false);

                eb.addBlankField(false);

                eb.setAuthor("Suspicious?", "https://pavela.net/", "https://pbs.twimg.com/profile_images/1404881883901554689/VR-3lR3B_400x400.jpg");
                eb.setFooter(" Source code\nhttps://github.com/daniel071/suspicious");

                eb.setImage("https://cdn2.unrealengine.com/amoguslandscape-2560x1440-2c59395f3208.jpg");
                eb.setThumbnail("https://pbs.twimg.com/profile_images/1404881883901554689/VR-3lR3B_400x400.jpg");
                event.replyEmbeds(eb.build()).queue();

                break;
            default:
                event.reply("I can't handle that command right now :(").setEphemeral(true).queue();
        }
    }

}