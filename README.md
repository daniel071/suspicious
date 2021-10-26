# Suspicious?
A discord and IRC bot that scans and notifies of any suspicious links

## Features (Discord)
Real-time scanning of all new messages and easy buttons for moderation (admins only)

[video example](https://user-images.githubusercontent.com/45705145/138825294-53cb44aa-708d-4afb-ba17-5b65de388aca.mp4)

Discord Slash-commands intergration

![image](https://user-images.githubusercontent.com/45705145/138826006-f52eba3f-0e43-4f6c-ba88-d81b71566e51.png)

Easy to use scan command

![image](https://user-images.githubusercontent.com/45705145/138825910-cf0da31c-a14e-4e50-8232-6a13a36e2a05.png)

Helpful help menu

![image](https://user-images.githubusercontent.com/45705145/138826137-92d1863e-57c0-46f5-b3ac-e7f016de937d.png)


## Features (IRC)

Real-time scanning of all new messages

![image](https://user-images.githubusercontent.com/45705145/138826856-a5e3e1b4-b073-462f-8435-36c98cef52b9.png)

Easy to use scan comamnd

![image](https://user-images.githubusercontent.com/45705145/138826688-098e36a8-8ca7-43df-bad4-2f297212f70d.png)

Displays all channels the bot is in

![image](https://user-images.githubusercontent.com/45705145/138826977-361757bc-e34c-4d15-aab5-b7617c9c4b06.png)

Any channel. PM the bot with ?invite <channel> or run /invite Suspicious on any channel and it will join.

![image](https://user-images.githubusercontent.com/45705145/138827116-8c2e3f41-3f98-47c3-9187-9f3822e48f4a.png)

Helpful help menu

![image](https://user-images.githubusercontent.com/45705145/138826505-509df582-493c-47fe-82f0-07e402b606da.png)

## Invite
You can use this bot by simply inviting the publicly hosted bots. 

Discord: https://discord.com/api/oauth2/authorize?client_id=890820136183947275&permissions=8192&scope=bot%20applications.commands \
IRC: PM the nick "Suspicious" with `?invite <channel>`. You can also run `/invite Suspicious` on any channel.

## Compiling
You can use an IDE to make this process simpler. In my case, I used intellij. Clone the repository (from IDE or commandline). 
1. From the main menu, select Run | Edit Configurations.
2. Click on the plus button and click "Application"
3. Select the main class.
4. Enter the CLI arguments. For Discord it's `<token>`. For IRC, it's `<nick> <server> <savefile>`
