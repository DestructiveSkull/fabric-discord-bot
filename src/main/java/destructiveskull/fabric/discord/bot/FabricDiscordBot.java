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
import destructiveskull.fabric.discord.bot.api.ConfigFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FabricDiscordBot implements ModInitializer, Runnable{

	public static final Logger LOGGER = LogManager.getLogger("fabric-discord-bot");
	private  String discord_token;

	@Override
	public void onInitialize() {

		//File f = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fabricdiscordbot.channel");
		File config_file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fabricdiscordbot.json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		//final String discord_token;

		try {
			if(!config_file.exists()){
				LOGGER.debug("Creating fabricdiscordbot.channel");
				config_file.createNewFile();

				ConfigFile config = new ConfigFile("insert-your-token-here", "");
				//String test = gson.toJson(config);

				//LOGGER.info(config_file.getPath());

				Writer writer = new FileWriter(config_file.getPath());
				gson.toJson(config, writer);

				writer.flush();
				writer.close();


				LOGGER.debug("jsonString written to file");

				LOGGER.error("PLEASE add your discord token to the config file and restart the server!");
			}
			else{
				LOGGER.debug("Loading fabricdiscordbot.channel");

				BufferedReader bReader = new BufferedReader(new FileReader(config_file));

				ConfigFile config = gson.fromJson(bReader, ConfigFile.class);

				discord_token = config.GetToken();

				if(discord_token!="insert-your-token-here"){
					FabricDiscordBot obj = new FabricDiscordBot();
					Thread thread = new Thread(obj);
					thread.start();
					LOGGER.debug("Started Discord Bot");
				}
				else{
					LOGGER.error("PLEASE add your discord token to the config file and restart the server!");
					throw new Exception("Discord Token not updated");
				}
	
			}
		} catch (Exception e) {

			LOGGER.error(e.getMessage());
		}

		

	}
	public void run() {
		LOGGER.info("Discord Bot Initialized");

		try {
			File config_file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fabricdiscordbot.json");
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			BufferedReader bReader = new BufferedReader(new FileReader(config_file));
			ConfigFile config = gson.fromJson(bReader, ConfigFile.class);
			discord_token = config.GetToken();
			
			DiscordClient client = DiscordClient.create(discord_token);

			PlayerJoinCallback.EVENT.register((player, server) -> {
				
				LOGGER.debug(player.getName().getString() + "joined");

				try {

					//Gson gson = new GsonBuilder().setPrettyPrinting().create();
					BufferedReader bReader2 = new BufferedReader(new FileReader(config_file));
					ConfigFile updated_config = gson.fromJson(bReader2, ConfigFile.class);

					//ConfigFile config = gson.fromJson(jsonString, ConfigFile.class);

					String channel_id = updated_config.GetChannelID();

					if(channel_id == ""){
						LOGGER.error("Server Channel has not been set. Please run the !set_channel command on discord at least once.");
					}

					else{

						Snowflake ch_id = Snowflake.of(channel_id);
						
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
					}

				} catch (Exception e) {
					LOGGER.error(e.getMessage());
					LOGGER.error("Server Channel may not be set. Please run the !set_channel command on discord at least once.");
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

						BufferedReader bReader3 = new BufferedReader(new FileReader(config_file));
						ConfigFile old_config = gson.fromJson(bReader3, ConfigFile.class);
						discord_token = old_config.GetToken();

						String channel_id = message.getChannelId().asString();

						ConfigFile new_config = new ConfigFile(discord_token, channel_id);

						Writer writer = new FileWriter(config_file.getPath());
						gson.toJson(new_config, writer);

						writer.flush();
						writer.close();

						LOGGER.debug("newJsonString writted to file");

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

		catch (Exception e){
			LOGGER.error(e.getStackTrace().toString());
		}
	}
}
