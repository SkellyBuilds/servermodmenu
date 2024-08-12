package com.skellybuilds.servermodmenu.gui.widget;

import com.skellybuilds.servermodmenu.ModMenu;
import com.skellybuilds.servermodmenu.config.ModMenuConfig;
import com.skellybuilds.servermodmenu.gui.ModsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;


public class ModMenuButtonWidget extends ButtonWidget {
	public ModMenuButtonWidget(int x, int y, int width, int height, Text text, Screen screen) {
		super(x, y, width, height, text, button -> MinecraftClient.getInstance().setScreen(new ModsScreen(screen)), ButtonWidget.DEFAULT_NARRATION_SUPPLIER);

	}

	@Override
	public void render(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
		super.render(DrawContext, mouseX, mouseY, delta);
		if (ModMenuConfig.BUTTON_UPDATE_BADGE.getValue() && ModMenu.areModUpdatesAvailable()) {
			UpdateAvailableBadge.renderBadge(DrawContext, this.width + this.getX() - 16, this.height / 2 + this.getY() - 4);
		}
	}
}
