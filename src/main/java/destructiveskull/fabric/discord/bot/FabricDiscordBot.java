package destructiveskull.fabric.discord.bot;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.GatewayDiscordClient;
import reactor.core.publisher.Mono;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.core.DiscordClient;
import discord4j.common.util.Snowflake;
import net.minecraft.util.ActionResult;

import destructiveskull.fabric.discord.bot.api.event.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;

public class FabricDiscordBot implements ModInitializer, Runnable{

	public static final Logger LOGGER = LogManager.getLogger("fabric-discord-bot");

	@Override
	public void onInitialize() {

		File f = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fabricdiscordbot.channel");

		try {
			if(!f.exists()){
				LOGGER.debug("Creating fabricdiscordbot.channel");
				f.createNewFile();
			}
			else{
				LOGGER.debug("Loading fabricdiscordbot.channel");
			}
		} catch (Exception e) {

			LOGGER.error(e.getMessage());
		}

		FabricDiscordBot obj = new FabricDiscordBot();
		Thread thread = new Thread(obj);
		thread.start();
		LOGGER.debug("Started Discord Bot");

	}
	public void run() {
		LOGGER.info("Discord Bot Initialized");

		File f = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fabricdiscordbot.channel");
		
		DiscordClient client = DiscordClient.create("OTE5NDE2MDEzNzYwNjU5NDU2.YbVe4w.5Iq_SNwmCU5kFQTSCMzQVlWSJfs");

		PlayerJoinCallback.EVENT.register((player, server) -> {
			
			LOGGER.debug(player.getName().getString() + "joined");

			try {
				FileInputStream fi = new FileInputStream(f);
				ObjectInputStream oi = new ObjectInputStream(fi);

				String sel_channel = (String) oi.readObject();

				oi.close();
				fi.close();

				Snowflake ch_id = Snowflake.of(sel_channel);
				
				RestChannel ch = client.getChannelById(ch_id);

				EmbedCreateSpec embed = EmbedCreateSpec.builder()
					.color(Color.GREEN)
					.title("\n\nPlayer Joined - " + player.getName().getString())
					.url("https://github.com/DestructiveSkull/fabric-discord-bot")
					.author("Fabric Discord Bot by DestructiveSkull", "https://github.com/DestructiveSkull/fabric-discord-bot", "https://i.imgur.com/tOvtyVX.png")
					.description(player.getName().getString() + " has Joined the Server!")
					.thumbnail("https://mc-heads.net/avatar/" + player.getName().getString() + "/100/nohelm.png")
					.timestamp(Instant.now())
					.addField("Buy me Coffee!","https://paypal.me/DSkul", false)
					.url("https://paypal.me/DSkul")
					.footer("fabric-discord-bot", "https://i.imgur.com/tOvtyVX.png")
					.build();

				ch.createMessage(embed.asRequest()).subscribe();

			} catch (ClassNotFoundException | IOException e) {
				LOGGER.error(e.getMessage());
			}

			return ActionResult.FAIL;
		});


		Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) -> {
			Mono<Void> printOnLogin = gateway.on(ReadyEvent.class, event ->
				Mono.fromRunnable(() -> {
					LOGGER.debug("Logged in as " + event.getSelf().getUsername() + "#" + event.getSelf().getDiscriminator());
				}))
				.then();
		  
			Mono<Void> set_channel = gateway.on(MessageCreateEvent.class, event -> {
			  Message message = event.getMessage();
		  
			  if (message.getContent().equalsIgnoreCase("!set_channel")) {
				try {
					FileOutputStream fo = new FileOutputStream(f);
					ObjectOutputStream o = new ObjectOutputStream(fo);

					String sel_channel = message.getChannelId().asString();

					o.writeObject(sel_channel);

					o.close();
					fo.close();

					return message.getChannel()
					.flatMap(channel -> channel.createMessage("Selected this channel for MC Server Messages!"));
					
				} catch (Exception e) {
					LOGGER.error(e.getMessage());
				}
			  }
		  
			  return Mono.empty();
			}).then();

			Mono<Void> handlePingCommand = gateway.on(MessageCreateEvent.class, event -> {
				Message message = event.getMessage();
			
				if (message.getContent().equalsIgnoreCase("!ping")) {
				  return message.getChannel()
					  .flatMap(channel -> channel.createMessage("pong!"));
				}
			
				return Mono.empty();
			}).then();

			Mono<Void> handleHelpCommand = gateway.on(MessageCreateEvent.class, event -> {
				Message message = event.getMessage();
			
				if (message.getContent().equalsIgnoreCase("!help")) {
					EmbedCreateSpec embed = EmbedCreateSpec.builder()
					.color(Color.GREEN)
					.title("\n\nHelp Section")
					.url("https://github.com/DestructiveSkull/fabric-discord-bot")
					.author("Fabric Discord Bot by DestructiveSkull", "https://github.com/DestructiveSkull/fabric-discord-bot", "https://i.imgur.com/tOvtyVX.png")
					.description("This bot is part of the Fabric Discord Bot Mod for Fabric servers, that notifies player join events on discord ")
					.thumbnail("https://i.imgur.com/tOvtyVX.png")
					.addField("Commands", "\u200B", false)
					.addField("!ping","Bot replies with pong! (Test to check if bot is working)", false)
					.addField("!set_channel", "Sets the current channel to receive bot's player event messages", false)
					.addField("!help","Displays this help section", false)
					.image("https://i.imgur.com/tOvtyVX.png")
					.timestamp(Instant.now())
					.addField("\u200B", "\u200B", false)
					.addField("Buy me Coffee!","https://paypal.me/DSkul", false)
					.url("https://paypal.me/DSkul")
					.footer("fabric-discord-bot", "https://i.imgur.com/tOvtyVX.png")
					.build();

					return message.getChannel()
						.flatMap(channel -> channel.createMessage(embed));
				}
			
				return Mono.empty();
			}).then();
		  
			return set_channel.and(handlePingCommand).and(handleHelpCommand).and(printOnLogin);
		  });

		login.block();
	}
}
