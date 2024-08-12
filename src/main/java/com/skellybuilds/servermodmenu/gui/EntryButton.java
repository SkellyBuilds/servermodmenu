package com.skellybuilds.servermodmenu.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class EntryButton {
	private final Identifier Texture;
	private final Consumer<EntryButton> callback;
	public int ButtonX = 0;
	public int ButtonY = 0;
	protected int ButtonSX = 20;
	protected int ButtonSY = 20;
	public boolean active = true;
	public boolean visible = true;
	private boolean pauseRender = false;
	private final Logger LOGGER = LoggerFactory.getLogger("Server Mod Menu");
	public SoundManager SOUNDMANAGER = MinecraftClient.getInstance().getSoundManager();;
	public EntryButton(Identifier text, Consumer<EntryButton> cb){
		this.Texture = text;
		this.callback = cb;
	}

	public EntryButton(Identifier text, Consumer<EntryButton> cb, int X, int Y, int XS, int XY){
		this.Texture = text;
		this.callback = cb;
		this.ButtonX = X;
		this.ButtonY = Y;
		this.ButtonSX = XS;
		this.ButtonSY = XY;
	}

	public void render(DrawContext dc, int mouseX, int mouseY){
		if (pauseRender || !visible) {
			return;
		}

		if(active) {
			if (!hoverHandler(mouseX, mouseY))
				dc.drawTexture(Texture, ButtonX, ButtonY, 0, 0, ButtonSX, ButtonSY, 32, 64);
			else
				dc.drawTexture(Texture, ButtonX, ButtonY, 0, 21, ButtonSX, ButtonSY, 32, 64);
		} else
			dc.drawTexture(Texture, ButtonX, ButtonY, 0, 42,ButtonSX, ButtonSY, 32, 64);

	}

	private boolean hoverHandler(int mouseX, int mouseY){
		return mouseX >= ButtonX && mouseX < ButtonX + ButtonSX
			&& mouseY >= ButtonY && mouseY < ButtonY + ButtonSY;
	}

	public void handleOnClickEvent(int mouseX, int mouseY){
		if(hoverHandler(mouseX, mouseY) && active) {
			SOUNDMANAGER.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));

			callback.accept(this);
		}
	}

	public void setActive(boolean a){
		this.active = a;
	}
	public void setVisible(boolean v){
		this.visible = v;
	}
	public void setX(int X){
		this.ButtonX =X;
	}
	public void setY(int Y){
		this.ButtonY = Y;
	}
	public void setSX(int X){
		this.ButtonSX =X;
	}
	public void setSY(int Y){
		this.ButtonSY = Y;
	}
}
