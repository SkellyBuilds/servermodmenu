package com.skellybuilds.servermodmenu.util.mod;

import com.skellybuilds.servermodmenu.db.SMod;
import com.skellybuilds.servermodmenu.gui.ModsScreen;
import com.skellybuilds.servermodmenu.util.DrawingUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;

import java.util.Set;

public class ModBadgeRenderer {
	protected int startX, startY, badgeX, badgeY, badgeMax;
	protected Mod mod;
	protected SMod smod;
	protected boolean useSMOD;
	protected MinecraftClient client;
	protected final ModsScreen screen;

	public ModBadgeRenderer(int startX, int startY, int endX, SMod mod, ModsScreen screen) {
		this.startX = startX;
		this.startY = startY;
		this.badgeMax = endX;
		this.smod = mod;
		useSMOD = true;
		this.screen = screen;
		this.client = MinecraftClient.getInstance();
	}

	public ModBadgeRenderer(int startX, int startY, int endX, Mod mod, ModsScreen screen) {
		this.startX = startX;
		this.startY = startY;
		this.badgeMax = endX;
		this.mod = mod;
		this.screen = screen;
		this.client = MinecraftClient.getInstance();
	}

	public void draw(DrawContext DrawContext, int mouseX, int mouseY) {
		this.badgeX = startX;
		this.badgeY = startY;
		Set<Mod.Badge> badges = mod.getBadges();
		badges.forEach(badge -> drawBadge(DrawContext, badge, mouseX, mouseY));
	}

	public void drawBadge(DrawContext DrawContext, Mod.Badge badge, int mouseX, int mouseY) {
		this.drawBadge(DrawContext, badge.getText().asOrderedText(), badge.getOutlineColor(), badge.getFillColor(), mouseX, mouseY);
	}

	public void drawBadge(DrawContext DrawContext, OrderedText text, int outlineColor, int fillColor, int mouseX, int mouseY) {
		int width = client.textRenderer.getWidth(text) + 6;
		if (badgeX + width < badgeMax) {
			DrawingUtil.drawBadge(DrawContext, badgeX, badgeY, width, text, outlineColor, fillColor, 0xCACACA);
			badgeX += width + 3;
		}
	}

	public Mod getMod() {
		return mod;
	}
	public SMod getSMod() {
		return smod;
	}
}
