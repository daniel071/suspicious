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
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class suspicious extends ListenerAdapter {
    public static JDA jda;
    public static final String URL_REGEX = "(http(s)?:\\/\\/.)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)";

    public static void main(String[] args) throws LoginException {
        Map<String, String> env = System.getenv();
        jda = JDABuilder.createDefault(args[0])
                .addEventListeners(new suspicious()).build();

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

            Pattern p = Pattern.compile(URL_REGEX);
            Matcher m = p.matcher(event.getMessage().getContentDisplay());
            List<String> sus = new ArrayList<>();

            while (m.find()) {
                System.out.println(m.group(0));
                sus.add(m.group(0));
            }
            if (!sus.isEmpty()) {
                event.getTextChannel().sendMessage("Oh my fucking god, did you just fucking post a link? I'm going to have to delete your reddit account! :angry:").queue();
            }

        }
    }

}