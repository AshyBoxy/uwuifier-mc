package xyz.ashyboxy.uwuifiermc;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;

import io.netty.buffer.Unpooled;
import lol.bai.badpackets.api.event.PacketSenderReadyCallback;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import xyz.ashyboxy.Uwuifier;
import xyz.ashyboxy.uwuifiermc.UwuifierModConfig.UserMode;

public class UwuifierMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("uwuifier");

	public static final Identifier SERVER_INFORM_ID = new Identifier("uwuifier", "serverinform");

	@Override
	public void onInitialize() {
		LOGGER.info(Uwuifier.uwuify("Hello Fabric world!"));

		UwuifierModConfig.load();

		PacketSenderReadyCallback.registerServer((handler, sender, server) -> {
			sender.send(SERVER_INFORM_ID, new PacketByteBuf(Unpooled.buffer().writeBoolean(true)));
		});

		// run in the default phase, we wanna mess with
		// other mods' content changes
		ServerMessageDecoratorEvent.EVENT.register((sender, message) -> {
			if (UwuifierModConfig.getMode() == UwuifierModConfig.UserMode.DISABLED
					|| (UwuifierModConfig.getMode() == UwuifierModConfig.UserMode.BLACKLIST
							&& UwuifierModConfig.blacklist
									.indexOf(Uuids.getUuidFromProfile(sender.getGameProfile()).toString()) > -1)
					|| (UwuifierModConfig.getMode() == UwuifierModConfig.UserMode.WHITELIST
							&& UwuifierModConfig.whitelist
									.indexOf(Uuids.getUuidFromProfile(sender.getGameProfile()).toString()) < 0))
				return CompletableFuture.completedFuture(message);
			return CompletableFuture.completedFuture(Text.literal(Uwuifier.uwuify(message.getString())));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("uwuconfig")
					.requires(environment.integrated ? (source) -> true : (source) -> source.hasPermissionLevel(2))
					.executes((context) -> {
						context.getSource().sendMessage(
								Text.literal(String.format("mode: %s\nblacklist: %s\nwhitelist: %s",
										UwuifierModConfig.getMode().getString(),
										uuidListToString(UwuifierModConfig.blacklist, context.getSource().getServer()),
										uuidListToString(UwuifierModConfig.whitelist,
												context.getSource().getServer()))));
						return 1;
					})
					.then(CommandManager.literal("mode").executes((context) -> {
						context.getSource()
								.sendMessage(Text.literal("mode: " + UwuifierModConfig.getMode().getString()));
						return 1;
					}).then(CommandManager.argument("mode", StringArgumentType.word())
							.suggests((context, builder) -> CommandSource
									.suggestMatching(UwuifierModConfig.getUserModeStrings().keySet(),
											builder))
							.executes((context) -> {
								String modeStr = StringArgumentType.getString(context, "mode");
								UserMode mode = UserMode.fromString(modeStr);
								if (mode == null) {
									context.getSource().sendError(Text.literal("Invalid mode"));
									return -1;
								}
								UwuifierModConfig.setMode(mode);
								context.getSource().sendFeedback(
										() -> Text.literal("Uwuifier: changed mode to " + mode.getString()), true);
								return 1;
							})))
					.then(CommandManager.literal("blacklist").executes((context) -> {
						context.getSource()
								.sendMessage(Text.literal("blacklist: " + uuidListToString(UwuifierModConfig.blacklist,
										context.getSource().getServer())));
						return 1;
					})
							.then(CommandManager.literal("add")
									.then(CommandManager.argument("players", GameProfileArgumentType.gameProfile())
											.executes((context) -> {
												int i = 0;
												for (GameProfile player : GameProfileArgumentType
														.getProfileArgument(context, "players")) {
													String uuid = player.getId().toString();
													if (UwuifierModConfig.blacklistAdd(uuid) == -1)
														continue;
													context.getSource().sendFeedback(
															() -> Text.literal("Uwuifier: blacklisted "
																	+ player.getName()),
															true);
													i++;
												}
												return i;
											})))
							.then(CommandManager.literal("remove")
									.then(CommandManager.argument("players", GameProfileArgumentType.gameProfile())
											.suggests((context, builder) -> CommandSource
													.suggestMatching(UwuifierModConfig.blacklist.stream()
															.map((s) -> context.getSource().getServer().getUserCache()
																	.getByUuid(UUID.fromString(s))
																	// this is 100% buggy
																	// don't put unencountered uuids in your config
																	.orElse(new GameProfile(UUID.fromString(s), s))
																	.getName()),
															builder))
											.executes(context -> {
												int i = 0;
												for (GameProfile player : GameProfileArgumentType
														.getProfileArgument(context, "players")) {
													String uuid = player.getId().toString();
													if (UwuifierModConfig.blacklistRemove(uuid) == -1)
														continue;
													context.getSource().sendFeedback(
															() -> Text.literal("Uwuifier: unblacklisted "
																	+ player.getName()),
															true);
													i++;
												}
												return i;
											}))))
					.then(CommandManager.literal("whitelist").executes((context) -> {
						context.getSource()
								.sendMessage(Text.literal("whitelist: " + uuidListToString(UwuifierModConfig.whitelist,
										context.getSource().getServer())));
						return 1;
					})
							.then(CommandManager.literal("add")
									.then(CommandManager.argument("players", GameProfileArgumentType.gameProfile())
											.executes((context) -> {
												int i = 0;
												for (GameProfile player : GameProfileArgumentType
														.getProfileArgument(context, "players")) {
													String uuid = player.getId().toString();
													if (UwuifierModConfig.whitelistAdd(uuid) == -1)
														continue;
													context.getSource().sendFeedback(
															() -> Text.literal("Uwuifier: whitelisted "
																	+ player.getName()),
															true);
													i++;
												}
												return i;
											})))
							.then(CommandManager.literal("remove")
									.then(CommandManager.argument("players", GameProfileArgumentType.gameProfile())
											.suggests((context, builder) -> CommandSource
													.suggestMatching(UwuifierModConfig.whitelist.stream()
															.map((s) -> context.getSource().getServer().getUserCache()
																	.getByUuid(UUID.fromString(s))
																	// this is 100% buggy
																	// don't put unencountered uuids in your config
																	.orElse(new GameProfile(UUID.fromString(s), s))
																	.getName()),
															builder))
											.executes(context -> {
												int i = 0;
												for (GameProfile player : GameProfileArgumentType
														.getProfileArgument(context, "players")) {
													String uuid = player.getId().toString();
													if (UwuifierModConfig.whitelistRemove(uuid) == -1)
														continue;
													context.getSource().sendFeedback(
															() -> Text.literal("Uwuifier: unwhitelisted "
																	+ player.getName()),
															true);
													i++;
												}
												return i;
											}))))
					.then(CommandManager.literal("reload").executes((context) -> {
						int result = UwuifierModConfig.load();
						if (result >= 0) {
							context.getSource().sendFeedback(() -> Text.literal("Uwuifier: reloaded config"), true);
							return 1;
						}
						context.getSource().sendError(Text.literal(
								"There was an error reloading the config, check your logs or delete the config file"));
						return -1;
					})));
		});
	}

	private static String uuidListToString(List<String> list, MinecraftServer server) {
		String s = "";
		boolean f = true;
		for (String uuid : list) {
			GameProfile u = server.getUserCache()
					.getByUuid(UUID.fromString(uuid)).orElse(null);
			String name = null;
			if (u != null)
				name = u.getName();
			if (name == null)
				name = uuid;
			if (f) {
				s = s + name;
				f = false;
			} else
				s = s + ", " + name;
		}
		return s;
	}
}
