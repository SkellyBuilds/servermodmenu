package com.skellybuilds.servermodmenu;

import com.google.common.collect.ImmutableMap;
import com.skellybuilds.servermodmenu.api.ConfigScreenFactory;
import com.skellybuilds.servermodmenu.api.ModMenuApi;
import com.skellybuilds.servermodmenu.gui.ModMenuOptionsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.OptionsScreen;

import java.util.Map;

public class ModMenuModMenuCompat implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ModMenuOptionsScreen::new;
	}

	@Override
	public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
		return ImmutableMap.of("minecraft", parent -> new OptionsScreen(parent, MinecraftClient.getInstance().options));
	}
}
