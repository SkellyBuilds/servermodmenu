package com.skellybuilds.servermodmenu.gui;

import com.skellybuilds.servermodmenu.config.ModMenuConfig;
import com.skellybuilds.servermodmenu.config.ModMenuConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ModMenuOptionsScreen extends GameOptionsScreen {

	private Screen previous;
	private OptionListWidget list;

	@SuppressWarnings("resource")
	public ModMenuOptionsScreen(Screen previous) {
		super(previous, MinecraftClient.getInstance().options, Text.translatable("modmenu.options"));
		this.previous = previous;
	}


	protected void init() {
		this.list = new OptionListWidget(this.client, this.width, this.height, 32, this.height - 32, 25);
		this.list.addAll(ModMenuConfig.asOptions());
		this.addSelectableChild(this.list);
		this.addDrawableChild(
				ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
							ModMenuConfigManager.save();
							this.client.setScreen(this.previous);
						}).position(this.width / 2 - 100, this.height - 27)
						.size(200, 20)
						.build());
	}

	@Override
	public void render(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
		this.renderBackgroundTexture(DrawContext);
		this.list.render(DrawContext, mouseX, mouseY, delta);
		DrawContext.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 5, 0xfffff);
		super.render(DrawContext, mouseX, mouseY, delta);
	}

	public void removed() {
		ModMenuConfigManager.save();
	}
}
