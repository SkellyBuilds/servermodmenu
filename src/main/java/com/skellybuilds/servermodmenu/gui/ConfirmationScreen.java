package com.skellybuilds.servermodmenu.gui;

import com.skellybuilds.servermodmenu.config.ModMenuConfig;
import com.skellybuilds.servermodmenu.config.ModMenuConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConfirmationScreen extends Screen {
	private final Consumer<ConfirmationScreen> YesCB;
	private final Consumer<ConfirmationScreen> NoCB;
	public Screen prevS;
	public Text mainT;

	ConfirmationScreen(Screen previousScreen, Consumer<ConfirmationScreen> ycb, Consumer<ConfirmationScreen> ncb, Text text) {
		super(Text.translatable("servermodmenu.conf.title"));
		YesCB = ycb;
		NoCB = ncb;
		prevS = previousScreen;
		mainT = text;
	}


	@Override
	protected void init() {
		this.addDrawableChild(new ButtonWidget(8, 138, 75, 20, Text.translatable("servermodmenu.conf.y"), button -> {
			YesCB.accept(this);
		}, Supplier::get) {
			@Override
			public void render(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
				super.render(DrawContext, mouseX, mouseY, delta);
			}
		});
		this.addDrawableChild(new ButtonWidget(346, 138, 75, 20, Text.translatable("servermodmenu.conf.n"), button -> {
			NoCB.accept(this);
		}, Supplier::get) {
			@Override
			public void render(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
				super.render(DrawContext, mouseX, mouseY, delta);
			}
		});
	}

	@Override
	public void render(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
		this.renderBackgroundTexture(DrawContext);
		DrawContext.drawCenteredTextWithShadow(this.textRenderer, this.mainT, 215, 45, 0xFFFFFF);

		super.render(DrawContext, mouseX, mouseY, delta);
	}

}
