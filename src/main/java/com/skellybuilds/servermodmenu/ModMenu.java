package com.skellybuilds.servermodmenu;

import com.google.common.collect.LinkedListMultimap;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skellybuilds.servermodmenu.api.ConfigScreenFactory;
import com.skellybuilds.servermodmenu.config.ModMenuConfig;
import com.skellybuilds.servermodmenu.config.ModMenuConfigManager;
import com.skellybuilds.servermodmenu.db.SMod;
import com.skellybuilds.servermodmenu.event.ModMenuEventHandler;
import com.skellybuilds.servermodmenu.gui.EntryButton;
import com.skellybuilds.servermodmenu.util.ModrinthUtil;
import com.skellybuilds.servermodmenu.util.Networking;
import com.skellybuilds.servermodmenu.util.mod.Mod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.*;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.text.NumberFormat;
import java.util.*;

public class ModMenu implements ClientModInitializer {
	public static final String MOD_ID = "servermodmenu";
	public static final String GITHUB_REF = "SkellyBuilds/ServerModMenu";
	public static final Logger LOGGER = LoggerFactory.getLogger("Server Mod Menu");
	public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create();
	public static final Gson GSON_MINIFIED = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	public static final List<String> idsDLD = new ArrayList<>();
	public static final Map<String, EntryButton> buttonEntries = new HashMap<>();
	public static final Map<String, Networking.SocketStatusLoop> socketLoops = new HashMap<>();
	public static final Map<String, Map<String, SMod>> SMODS = new HashMap<>(); // Server Name - Mod Map
	public static Map<String, SMod> SMODSA = new HashMap<>();
	public static final Map<String, Mod> MODS = new HashMap<>();
	public static final Map<String, Mod> ROOT_MODS = new HashMap<>();
	public static final LinkedListMultimap<Mod, Mod> PARENT_MAP = LinkedListMultimap.create();
	public final static Networking MainNetwork = new Networking();
	private static Map<String, ConfigScreenFactory<?>> configScreenFactories = new HashMap<>();
	private static List<Map<String, ConfigScreenFactory<?>>> delayedScreenFactoryProviders = new ArrayList<>();
	public static boolean isAllDFB = false;
	private static int cachedDisplayedModCount = -1;
	public static boolean runningQuilt = FabricLoader.getInstance().isModLoaded("quilt_loader");
	public static boolean devEnvironment = FabricLoader.getInstance().isDevelopmentEnvironment();



	public static Screen getConfigScreen(String modid, Screen menuScreen) {
		if (!delayedScreenFactoryProviders.isEmpty()) {
			delayedScreenFactoryProviders.forEach(map -> map.forEach(configScreenFactories::putIfAbsent));
			delayedScreenFactoryProviders.clear();
		}
		if (ModMenuConfig.HIDDEN_CONFIGS.getValue().contains(modid)) {
			return null;
		}
		ConfigScreenFactory<?> factory = configScreenFactories.get(modid);
		if (factory != null) {
			return factory.create(menuScreen);
		}
		return null;
	}

	public static void sendmodstonetwork(ServerList serverList, MinecraftClient client){
		serverList.loadFile();
		for (int i = 0; i < serverList.size(); i++) {
			ServerInfo serverInfo = serverList.get(i);

			if(serverInfo.address.contains(":")){
				serverInfo.address = serverInfo.address.substring(0, serverInfo.address.indexOf(":"));
			}

			Networking.ServerAddress parsedAd = Networking.ServerAddress.parse(serverInfo.address);

			Optional<InetSocketAddress> optAddress = Networking.AllowedAddressResolver.DEFAULT.resolve(parsedAd).map(Address::getInetSocketAddress);
			if(optAddress.isPresent()) {
				final InetSocketAddress inetSocketAddress = (InetSocketAddress) optAddress.get();

				List<String> stringArray = new ArrayList<>();
				for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
					stringArray.add(mod.getMetadata().getId());
				}

				String testS = inetSocketAddress.getAddress().getHostAddress();

				String data = "{\"playerN\":" + client.getSession().getUsername() + ", \"data\":" + stringArray.toString() + "}";
				MainNetwork.connect(testS, inetSocketAddress.getPort());

				MainNetwork.sendDataToServer(testS, "addpmods|" + data);
			} else {
				List<String> stringArray = new ArrayList<>();
				for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
					stringArray.add(mod.getMetadata().getId());
				}

				String data = "{\"playerN\":" + client.getSession().getUsername() + ", \"data\":" + stringArray.toString() + "}";
				MainNetwork.connect(serverInfo.address, 27752);
				MainNetwork.sendDataToServer(serverInfo.address, "addpmods|" + data);
			}
		}


	}

	@Override
	public void onInitializeClient() {

		MinecraftClient client = MinecraftClient.getInstance();
		ServerList serverList = new ServerList(client);
		MainNetwork.reloadAllServers(serverList);


		serverList.loadFile();
		for (int i = 0; i < serverList.size(); i++) {
			ServerInfo serverInfo = serverList.get(i);

			Networking.ServerAddress parsedAd = Networking.ServerAddress.parse(serverInfo.address);

			Optional<InetSocketAddress> optAddress = Networking.AllowedAddressResolver.DEFAULT.resolve(parsedAd).map(Address::getInetSocketAddress);
			if(optAddress.isPresent()) {
				final InetSocketAddress inetSocketAddress = (InetSocketAddress) optAddress.get();
				if (socketLoops.get(inetSocketAddress.getAddress().getHostAddress()) != null) {
					new Thread(socketLoops.get(serverInfo.address)).start();
				} else {
					Networking.SocketStatusLoop loop = new Networking.SocketStatusLoop(serverInfo.address, inetSocketAddress.getPort());
					socketLoops.put(serverInfo.address, loop);
					new Thread(loop).start();
				}
			} else {
				if (socketLoops.get(serverInfo.address) != null) {
					new Thread(socketLoops.get(serverInfo.address)).start();
				} else {
					Networking.SocketStatusLoop loop = new Networking.SocketStatusLoop(serverInfo.address);
					socketLoops.put(serverInfo.address, loop);
					new Thread(loop).start();
				}
			}
		}

		ModMenuConfigManager.initializeConfig();
		Set<String> modpackMods = new HashSet<>();

		// uh get server list and see if the server has any stuf
//		FabricLoader.getInstance().getEntrypointContainers("modmenu", ModMenuApi.class).forEach(entrypoint -> {
//			ModMetadata metadata = entrypoint.getProvider().getMetadata();
//			String modId = metadata.getId();
//			try {
//				ModMenuApi api = entrypoint.getEntrypoint();
//				configScreenFactories.put(modId, api.getModConfigScreenFactory());
//				delayedScreenFactoryProviders.add(api.getProvidedConfigScreenFactories());
//				api.attachModpackBadges(modpackMods::add);
//			} catch (Throwable e) {
//				LOGGER.error("Mod {} provides a broken implementation of ModMenuApi", modId, e);
//			}
//		});




		// Fill mods map
//		for (ModContainer modContainer : FabricLoader.getInstance().getAllMods()) {
//			Mod mod;
//
//			if (runningQuilt) {
//				mod = new QuiltMod(modContainer, modpackMods);
//			} else {
		//		mod = new FabricMod(modContainer, modpackMods);
//			}
//
//			MODS.put(mod.getId(), mod);
//		}

		ModrinthUtil.checkForUpdates();

		Map<String, Mod> dummyParents = new HashMap<>();

//		// Initialize parent map
//		for (Mod mod : MODS.values()) {
//			String parentId = mod.getParent();
//			if (parentId != null) {
//				Mod parent = MODS.getOrDefault(parentId, dummyParents.get(parentId));
//				if (parent == null) {
//					if (mod instanceof FabricMod) {
//						parent = new FabricDummyParentMod((FabricMod) mod, parentId);
//						dummyParents.put(parentId, parent);
//					}
//				}
//				PARENT_MAP.put(parent, mod);
//			} else {
//				ROOT_MODS.put(mod.getId(), mod);
//			}
//		}
//		MODS.putAll(dummyParents);

		sendmodstonetwork(serverList, client);
		ModMenuEventHandler.register();
	}

	public static void clearModCountCache() {
		cachedDisplayedModCount = -1;
	}

	public static boolean areModUpdatesAvailable() {
		if (!ModMenuConfig.UPDATE_CHECKER.getValue()) {
			return false;
		}

		for (Mod mod : MODS.values()) {
			if (mod.isHidden()) {
				continue;
			}

			if (!ModMenuConfig.SHOW_LIBRARIES.getValue() && mod.getBadges().contains(Mod.Badge.LIBRARY)) {
				continue;
			}

			if (mod.getModrinthData() != null || mod.getChildHasUpdate()) {
				return true; // At least one currently visible mod has an update
			}
		}

		return false;
	}

	public static String getDisplayedModCount() {
		if (cachedDisplayedModCount == -1) {
			// listen, if you have >= 2^32 mods then that's on you
			cachedDisplayedModCount = Math.toIntExact(MODS.values().stream().filter(mod ->
					(ModMenuConfig.COUNT_CHILDREN.getValue() || mod.getParent() == null) &&
							(ModMenuConfig.COUNT_LIBRARIES.getValue() || !mod.getBadges().contains(Mod.Badge.LIBRARY)) &&
							(ModMenuConfig.COUNT_HIDDEN_MODS.getValue() || !mod.isHidden())
			).count());
		}
		return NumberFormat.getInstance().format(cachedDisplayedModCount);
	}

	public static Text createModsButtonText(boolean title) {
		var titleStyle = ModMenuConfig.MODS_BUTTON_STYLE.getValue();
		var gameMenuStyle = ModMenuConfig.GAME_MENU_BUTTON_STYLE.getValue();
		var isIcon = title ? titleStyle == ModMenuConfig.TitleMenuButtonStyle.ICON : gameMenuStyle == ModMenuConfig.GameMenuButtonStyle.ICON;
		var isShort = title ? titleStyle == ModMenuConfig.TitleMenuButtonStyle.SHRINK : gameMenuStyle == ModMenuConfig.GameMenuButtonStyle.REPLACE_BUGS;
		MutableText modsText = Text.translatable("servermodmenu.title");
		if (ModMenuConfig.MOD_COUNT_LOCATION.getValue().isOnModsButton() && !isIcon) {
			String count = ModMenu.getDisplayedModCount();
			if (isShort) {
				modsText.append(Text.literal(" ")).append(Text.translatable("modmenu.loaded.short", count));
			} else {
				String specificKey = "modmenu.loaded." + count;
				String key = I18n.hasTranslation(specificKey) ? specificKey : "modmenu.loaded";
				if (ModMenuConfig.EASTER_EGGS.getValue() && I18n.hasTranslation(specificKey + ".secret")) {
					key = specificKey + ".secret";
				}
				modsText.append(Text.literal(" ")).append(Text.translatable(key, count));
			}
		}
		return modsText;
	}
}
