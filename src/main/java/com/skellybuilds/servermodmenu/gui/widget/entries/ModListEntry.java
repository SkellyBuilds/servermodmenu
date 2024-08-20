package com.skellybuilds.servermodmenu.gui.widget.entries;
import com.skellybuilds.servermodmenu.config.ModMenuConfigManager;
import com.skellybuilds.servermodmenu.gui.EntryButton;
import net.minecraft.client.sound.PositionedSoundInstance;
import com.mojang.blaze3d.systems.RenderSystem;
import com.skellybuilds.servermodmenu.ModMenu;
import com.skellybuilds.servermodmenu.config.ModMenuConfig;
import com.skellybuilds.servermodmenu.db.SMod;
import com.skellybuilds.servermodmenu.gui.widget.ModListWidget;
import com.skellybuilds.servermodmenu.gui.widget.UpdateAvailableBadge;
import com.skellybuilds.servermodmenu.util.DrawingUtil;
import com.skellybuilds.servermodmenu.util.Networking;
import com.skellybuilds.servermodmenu.util.TexturesManager;
import com.skellybuilds.servermodmenu.util.mod.Mod;
import com.skellybuilds.servermodmenu.util.mod.ModBadgeRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.skellybuilds.servermodmenu.ModMenu.LOGGER;
import static com.skellybuilds.servermodmenu.ModMenu.MainNetwork;

public class ModListEntry extends AlwaysSelectedEntryListWidget.Entry<ModListEntry> {
	public static final Identifier UNKNOWN_ICON = new Identifier("textures/misc/unknown_pack.png");
	public static final Identifier DOWNLOAD_ICON = new Identifier("servermodmenu", "textures/gui/download_button.png");
	public static final Identifier RELOAD_ICON = new Identifier("servermodmenu", "textures/gui/reload_servers.png");
	public static final Identifier HIDE_ICON = new Identifier("servermodmenu", "textures/gui/hide_button.png");
	//private static final Identifier MOD_CONFIGURATION_ICON = new Identifier("servermodmenu", "textures/gui/mod_configuration.png");
	private static final Identifier ERROR_ICON = new Identifier("minecraft", "textures/gui/world_selection.png");

	protected final MinecraftClient client;
	public Mod mod;
	protected final ModListWidget list;
	protected Identifier iconLocation;
	protected static final int FULL_ICON_SIZE = 32;
	protected static final int COMPACT_ICON_SIZE = 19;
	protected long sinceLastClick;
	private boolean useSMod;
	public SMod smod;
	public String serverName;
	public boolean isFirst = false;
	public Thread serverStat;
	public Networking.SocketStatusLoop serverStatR;
	public boolean renderSvnNO = false;
	private int ButtonX;
	private int ButtonY;
	private int ButtonSX = 20;
	private int ButtonSY = 20;
//	private boolean isPrevHB = false;
//	private boolean disableB = false;
//	private boolean isDone = false;
	public boolean moreY = false;

	public void testF(EntryButton button){
		button.setActive(false);
(new Thread(() -> {
			List<SMod> tList = new ArrayList<>(ModMenu.SMODS.get(serverName).values());

			while(!tList.isEmpty()) {
				ModMenu.LOGGER.info("processing chunk");
				this.processChunk(tList);

				try {
					Thread.sleep(1250L);
				} catch (InterruptedException var3) {
					Thread.currentThread().interrupt();
				}
			}
	if(!ModMenu.isAllDFB){
		List<Boolean> isAllt = new ArrayList<>();
		ModMenu.buttonEntries.forEach((name, mButton) -> {
			if(!mButton.visible)
				isAllt.add(true);
		});

		if(isAllt.size() == ModMenu.buttonEntries.size())
			ModMenu.isAllDFB = true;
	}
	button.setVisible(false);
	button.SOUNDMANAGER.play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F));
})).start();


		if(!ModMenu.isAllDFB){
			List<Boolean> isAllt = new ArrayList<>();
			ModMenu.buttonEntries.forEach((name, mButton) -> {
				if(!mButton.visible)
					isAllt.add(true);
			});

			if(isAllt.size() == ModMenu.buttonEntries.size())
				ModMenu.isAllDFB = true;
		}

	}

	public void setHiddenS(EntryButton button){
		AtomicBoolean isExisting = new AtomicBoolean(false);
		ModMenuConfig.HIDDEN_SERVERS.getValue().forEach((name) -> {
			if(Objects.equals(name, serverName)){
				isExisting.set(true);
			}
		});
		if(isExisting.get()){
			ModMenuConfig.HIDDEN_SERVERS.getValue().remove(serverName);
		} else {
			ModMenuConfig.HIDDEN_SERVERS.getValue().add(serverName);
		}
		ModMenuConfigManager.save();
		list.reloadFilters();
		list.getParent().calcServersSize();
		list.setSelected(null);
		button.SOUNDMANAGER.play(PositionedSoundInstance.master(SoundEvents.BLOCK_PORTAL_TRIGGER, 1.0F));
	}

	public boolean downloadA(EntryButton button){
		button.setActive(false);
		Thread t0 = new Thread(() -> {
			//List<SMod> tList = this.list.smods;
			List<SMod> tList = new ArrayList<>(ModMenu.SMODS.get(serverName).values());

			while(!tList.isEmpty()) {
				ModMenu.LOGGER.info("processing chunk");
				this.processChunk(tList);

				try {
					Thread.sleep(1250L);
				} catch (InterruptedException var3) {
					Thread.currentThread().interrupt();
				}
			}

			if(!ModMenu.isAllDFB){
				List<Boolean> isAllt = new ArrayList<>();
				ModMenu.buttonEntries.forEach((name, mButton) -> {
					if(!mButton.visible)
						isAllt.add(true);
				});

				if(isAllt.size() == ModMenu.buttonEntries.size())
					ModMenu.isAllDFB = true;
			}
			button.setVisible(false);
			button.SOUNDMANAGER.play(PositionedSoundInstance.master(SoundEvents.ENTITY_ARROW_HIT_PLAYER, 1.0F));
		});
		t0.start();
		boolean a = false;
		while(true){
			if(t0.getState() != Thread.State.RUNNABLE){
				a = true;
				break;
			}
		}
		return a;
	}

	private void reloadServer(EntryButton button){
		MainNetwork.reloadServer(serverName);
		button.active = false;
		new Thread(() -> {
			while (true) {
				if (MainNetwork.isNthreadsDone()) {
					this.list.reloadFilters();
					button.active = true;
					break;
				}
				try {
					Thread.sleep(750);
				} catch (InterruptedException e) {
					LOGGER.error(e.toString());
				}
			}
		}).start();
	}

	EntryButton testB; // Download Button
	EntryButton reloadB = new EntryButton(RELOAD_ICON, this::reloadServer); //
	EntryButton hideB = new EntryButton(HIDE_ICON, this::setHiddenS);
	public ModListEntry(SMod smod, ModListWidget list){
		useSMod = true;
		this.smod = smod;
		this.list = list;
		this.client = MinecraftClient.getInstance();

	}

	public ModListEntry(SMod smod, ModListWidget list, String svn){
		useSMod = true;
		this.smod = smod;
		this.list = list;
		this.client = MinecraftClient.getInstance();
		this.serverName = svn;
	}

	public ModListEntry(SMod smod, ModListWidget list, String svn, boolean isF, boolean renSvn){
		useSMod = true;
		this.smod = smod;
		this.list = list;
		this.client = MinecraftClient.getInstance();
		this.serverName = svn;
		this.isFirst = isF;
		if(isF){
			if(ModMenu.socketLoops.get(svn) != null){
				serverStatR = ModMenu.socketLoops.get(svn);
				serverStat = new Thread(serverStatR);
				serverStat.start();
			} else {
				serverStatR = new Networking.SocketStatusLoop(svn);
				ModMenu.socketLoops.put(svn, serverStatR);
				serverStat = new Thread(serverStatR);
				serverStat.start();
			}
		}
		this.renderSvnNO = renSvn;
		if(isF) {
			if (ModMenu.buttonEntries.get(svn) != null) testB = ModMenu.buttonEntries.get(svn);
			else {
				testB = new EntryButton(DOWNLOAD_ICON, this::testF, ButtonX, ButtonY, ButtonSX, ButtonSY);
				ModMenu.buttonEntries.put(svn, testB);
			}
		}
	}

	public ModListEntry(SMod smod, ModListWidget list, String svn, boolean isF, boolean renSvn, boolean moreY){
		useSMod = true;
		this.smod = smod;
		this.list = list;
		this.client = MinecraftClient.getInstance();
		this.serverName = svn;
		this.moreY = moreY;
		this.isFirst = isF;
		if(isF){
			if(ModMenu.socketLoops.get(svn) != null){
				serverStatR = ModMenu.socketLoops.get(svn);
				serverStat = new Thread(serverStatR);
				serverStat.start();
			} else {
				serverStatR = new Networking.SocketStatusLoop(svn);
				ModMenu.socketLoops.put(svn, serverStatR);
				serverStat = new Thread(ModMenu.socketLoops.get(svn));
				serverStat.start();
			}
		}
		this.renderSvnNO = renSvn;
		if(isF) {
			if (ModMenu.buttonEntries.get(svn) != null) testB = ModMenu.buttonEntries.get(svn);
			else {
				testB = new EntryButton(DOWNLOAD_ICON, this::testF, ButtonX, ButtonY, ButtonSX, ButtonSY);
				ModMenu.buttonEntries.put(svn, testB);
			}
		}
	}

	public ModListEntry(Mod mod, ModListWidget list) {
		this.mod = mod;
		this.list = list;
		this.client = MinecraftClient.getInstance();
	}

	@Override
	public Text getNarration() {
		if(useSMOD() && smod.meta == null){
			return Text.literal("NO NAME");
		}
		if(useSMOD())return Text.literal(smod.meta.name);
		else return Text.literal(mod.getTranslatedName());
	}

	@Override
	public void render(DrawContext DrawContext, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float delta) {
		if(useSMod) {
//			if(moreY){
//				y = y + 14;
//			}

			int iconSize = ModMenuConfig.COMPACT_LIST.getValue() ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			if(isFirst){

				Text svName = Text.literal(serverName);
				StringVisitable trimmedName = svName;
				int maxNameWidth = rowWidth - iconSize - 3;
				TextRenderer font = this.client.textRenderer;
				if (font.getWidth(svName) > maxNameWidth) {
					StringVisitable ellipsis = StringVisitable.plain("...");
					trimmedName = StringVisitable.concat(font.trimToWidth(svName, maxNameWidth - font.getWidth(ellipsis)), ellipsis);
				}
				testB.ButtonX = x + font.getWidth(svName) + 15;
				reloadB.ButtonX = x + font.getWidth(svName) + 35;
				hideB.ButtonX = x + font.getWidth(svName) + 55;
				int wa = y;
				testB.ButtonY = wa-20;
				reloadB.ButtonY = wa - 20;
				hideB.ButtonY = wa-20;
				DrawContext.drawText(font, Language.getInstance().reorder(trimmedName), x - 5, wa -11, 0xFFFFFF, false);
				if(!renderSvnNO) {
					testB.render(DrawContext, mouseX, mouseY);
				}
				reloadB.render(DrawContext, mouseX, mouseY);
				hideB.render(DrawContext, mouseX, mouseY);
				//DrawContext.drawTexture(DOWNLOAD_ICON, ButtonX, ButtonY, 0, 0,ButtonSX, ButtonSY, 32, 64);
				if(serverStatR != null) {
					if (serverStatR.status == 0) {
						DrawContext.fill(font.getWidth(trimmedName) + 8, wa - 11, font.getWidth(trimmedName) + 6, wa - 12, 0xFF00FF00);
					}
						else if (serverStatR.status == 1) {
						DrawContext.fill(font.getWidth(trimmedName) - 16, wa - 11, font.getWidth(trimmedName) - 15, wa - 10, 0xFF808080);
					}else {
						DrawContext.fill(font.getWidth(trimmedName) - 16, wa - 11, font.getWidth(trimmedName) - 15, wa - 10, 0xFFFF0000);
						}

				}

				y = y+2;
			} else 	y = y + 3;
			if(renderSvnNO){
				return;
			}
			x += getXOffset();
			rowWidth -= getXOffset();
			String modId = smod.getId();
			if ("java".equals(modId)) { // maybe modmenu settings
				DrawingUtil.drawRandomVersionBackgroundS(smod, DrawContext, x, y, iconSize, iconSize);
			}
			RenderSystem.enableBlend();
			DrawContext.drawTexture(this.getIconTexture(), x, y, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
			RenderSystem.disableBlend();
			Text name = Text.literal(smod.meta.name);
			StringVisitable trimmedName = name;
			int maxNameWidth = rowWidth - iconSize - 3;
			TextRenderer font = this.client.textRenderer;
			if (font.getWidth(name) > maxNameWidth) {
				StringVisitable ellipsis = StringVisitable.plain("...");
				trimmedName = StringVisitable.concat(font.trimToWidth(name, maxNameWidth - font.getWidth(ellipsis)), ellipsis);
			}
			DrawContext.drawText(font, Language.getInstance().reorder(trimmedName), x + iconSize + 3, y + 1, 0xFFFFFF, false);
//			if(enableDownloads){
//				DrawContext.drawTexture(DOWNLOAD_ICON, x + iconSize - 12, y+1, 0.0F, 0.0F, DLICON_SIZE, DLICON_SIZE, 16, 16);
//			}
			//var updateBadgeXOffset = 0;

//			if(isFirst && !renderSvnNO && !isDone) {
//				//ButtonSY = 19;
//				if (isMouseOverButton(mouseX, mouseY) && !disableB) {
//					isPrevHB = true;
//
//					DrawContext.drawTexture(DOWNLOAD_ICON, ButtonX, ButtonY, 0, 21, ButtonSX, ButtonSY, 32, 64);
//					// disabled - DrawContext.drawTexture(DOWNLOAD_ICON, ButtonX, ButtonY, 0, 42,ButtonSX, ButtonSY, 32, 64);
//				} else {
//					if(disableB) DrawContext.drawTexture(DOWNLOAD_ICON, ButtonX, ButtonY, 0, 42,ButtonSX, ButtonSY, 32, 64);
//					else DrawContext.drawTexture(DOWNLOAD_ICON, ButtonX, ButtonY, 0, 0,ButtonSX, ButtonSY, 32, 64);
//				}
//			}

			final int textureSize = ModMenuConfig.COMPACT_LIST.getValue() ? (int) (256 / (FULL_ICON_SIZE / (double) COMPACT_ICON_SIZE)) : 256;
			if (this.client.options.getTouchscreen().getValue() || hovered) {
//				if(enableDownloads){
//					DrawContext.fill(x, y, x + iconSize, y + iconSize, -1601138544);
//				} else DrawContext.fill(x, y, x + iconSize, y + iconSize, 0xFF0000);
//
//				boolean hoveringIcon = mouseX - x < iconSize;
//				int v = hoveringIcon ? iconSize : 0;
//
//				if (hoveringIcon && !enableDownloads) {
//					if(smod.meta.authors.length > 0){
//					this.list.getParent().setTooltip(this.client.textRenderer.wrapLines(Text.translatable("modmenu.download.error", modId, modId).copy().append("\n\n").append(smod.meta.authors[0]).formatted(Formatting.RED), 175));
//					DrawContext.drawTexture(ERROR_ICON, x, y, 0.0F, (float) v, iconSize, iconSize, textureSize, textureSize);
//					}
//				} else {
//					DrawContext.drawTexture(DOWNLOAD_ICON, x, y, 0.0F, (float) v, iconSize, iconSize, textureSize, textureSize);
//				}

//				if (this.list.getParent().modScreenErrors.containsKey(modId)) {
//					DrawContext.drawTexture(ERROR_ICON, x, y, 96.0F, (float) v, iconSize, iconSize, textureSize, textureSize);
//					if (hoveringIcon) {
//						Throwable e = this.list.getParent().modScreenErrors.get(modId);
//						this.list.getParent().setTooltip(this.client.textRenderer.wrapLines(Text.translatable("modmenu.configure.error", modId, modId).copy().append("\n\n").append(e.toString()).formatted(Formatting.RED), 175));
//					}
//				} else {

				//}
			}
		}
		else {
			x += getXOffset();
			rowWidth -= getXOffset();
			int iconSize = ModMenuConfig.COMPACT_LIST.getValue() ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;
			String modId = mod.getId();
			if ("java".equals(modId)) {
				DrawingUtil.drawRandomVersionBackground(mod, DrawContext, x, y, iconSize, iconSize);
			}
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.enableBlend();
			DrawContext.drawTexture(this.getIconTexture(), x, y, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
			RenderSystem.disableBlend();
			Text name = Text.literal(mod.getTranslatedName());
			StringVisitable trimmedName = name;
			int maxNameWidth = rowWidth - iconSize - 3;
			TextRenderer font = this.client.textRenderer;
			if (font.getWidth(name) > maxNameWidth) {
				StringVisitable ellipsis = StringVisitable.plain("...");
				trimmedName = StringVisitable.concat(font.trimToWidth(name, maxNameWidth - font.getWidth(ellipsis)), ellipsis);
			}
			DrawContext.drawText(font, Language.getInstance().reorder(trimmedName), x + iconSize + 3, y + 1, 0xFFFFFF, false);
			var updateBadgeXOffset = 0;
			if (ModMenuConfig.UPDATE_CHECKER.getValue() && !ModMenuConfig.DISABLE_UPDATE_CHECKER.getValue().contains(modId) && (mod.getModrinthData() != null || mod.getChildHasUpdate())) {
				UpdateAvailableBadge.renderBadge(DrawContext, x + iconSize + 3 + font.getWidth(name) + 2, y);
				updateBadgeXOffset = 11;
			}
			if (!ModMenuConfig.HIDE_BADGES.getValue()) {
				new ModBadgeRenderer(x + iconSize + 3 + font.getWidth(name) + 2 + updateBadgeXOffset, y, x + rowWidth, mod, list.getParent()).draw(DrawContext, mouseX, mouseY);
			}
			if (!ModMenuConfig.COMPACT_LIST.getValue()) {
				String summary = mod.getSummary();
				DrawingUtil.drawWrappedString(DrawContext, summary, (x + iconSize + 3 + 4), (y + client.textRenderer.fontHeight + 2), rowWidth - iconSize - 7, 2, 0x808080);
			} else {
				DrawingUtil.drawWrappedString(DrawContext, mod.getPrefixedVersion(), (x + iconSize + 3), (y + client.textRenderer.fontHeight + 2), rowWidth - iconSize - 7, 2, 0x808080);
			}

			if (!(this instanceof ParentEntry) && ModMenuConfig.QUICK_CONFIGURE.getValue() && (this.list.getParent().getModHasConfigScreen().get(modId) || this.list.getParent().modScreenErrors.containsKey(modId))) {
				final int textureSize = ModMenuConfig.COMPACT_LIST.getValue() ? (int) (256 / (FULL_ICON_SIZE / (double) COMPACT_ICON_SIZE)) : 256;
				if (this.client.options.getTouchscreen().getValue() || hovered) {
					DrawContext.fill(x, y, x + iconSize, y + iconSize, -1601138544);
					boolean hoveringIcon = mouseX - x < iconSize;
					int v = hoveringIcon ? iconSize : 0;
					if (this.list.getParent().modScreenErrors.containsKey(modId)) {
						DrawContext.drawTexture(ERROR_ICON, x, y, 96.0F, (float) v, iconSize, iconSize, textureSize, textureSize);
						if (hoveringIcon) {
							Throwable e = this.list.getParent().modScreenErrors.get(modId);
							this.list.getParent().setTooltip(this.client.textRenderer.wrapLines(Text.translatable("modmenu.configure.error", modId, modId).copy().append("\n\n").append(e.toString()).formatted(Formatting.RED), 175));
						}
					} else {
						//DrawContext.drawTexture(MOD_CONFIGURATION_ICON, x, y, 0.0F, (float) v, iconSize, iconSize, textureSize, textureSize);
					}
				}
			}
		}
	}

	private static final int CHUNK_SIZE = 3; // Number of items to process per chunk
	private static final int SLEEP_TIME_MS = 1250; // Delay between chunks in milliseconds

	private boolean processChunk(List<SMod> list) {
		int chunkSize = Math.min(CHUNK_SIZE, list.size());

		List<SMod> chunk = new ArrayList<>(list.subList(0, chunkSize));
		list.subList(0, chunkSize).clear(); // Remove processed items from the list

		for (SMod item : chunk) {
			MainNetwork.requestNDownload(this.serverName, item.getId());
			ModMenu.idsDLD.add(item.getId());
		}

		return true;
	}

	private boolean isMouseOverButton(int mouseX, int mouseY) {
		return mouseX >= ButtonX && mouseX < ButtonX + ButtonSX && mouseY >= ButtonY && mouseY < ButtonY + ButtonSY;
	}
		@Override
		public boolean mouseClicked ( double mouseX, double mouseY, int delta){
			list.select(smod.getId(), serverName);
			if(this.useSMOD()){
				if(this.isFirst && !renderSvnNO) {
					testB.handleOnClickEvent((int)mouseX, (int)mouseY);
					reloadB.handleOnClickEvent((int)mouseX, (int)mouseY);
					hideB.handleOnClickEvent((int) mouseX, (int)mouseY);
				} else if(this.isFirst){
					reloadB.handleOnClickEvent((int)mouseX, (int)mouseY);
					hideB.handleOnClickEvent((int) mouseX, (int)mouseY);
				}





// uNUSED VARIABLE
//				if (ModMenuConfig.QUICK_CONFIGURE.getValue() && this.list.getParent().getModHasConfigScreen().get(this.smod.getId())) {
//					int iconSize = ModMenuConfig.COMPACT_LIST.getValue() ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;
////					if (mouseX - list.getRowLeft() <= iconSize) {
////						Thread downloadT = new Thread(() -> {
////							isBusy = true;
////							Networking.HTTPS.downloadMod(smod.getId(), smod.getVersion(), "./mods");
////							isBusy = false;
////						});
////						downloadT.start();
////					} else if (Util.getMeasuringTimeMs() - this.sinceLastClick < 250) {
////						Thread downloadT = new Thread(() -> {
////							isBusy = true;
////							Networking.HTTPS.downloadMod(smod.getId(), smod.getVersion(), "./mods");
////							isBusy = false;
////						});
////						downloadT.start();
////					}
//				}
			} else {
				if (ModMenuConfig.QUICK_CONFIGURE.getValue() && this.list.getParent().getModHasConfigScreen().get(this.mod.getId())) {
					int iconSize = ModMenuConfig.COMPACT_LIST.getValue() ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;
//					if (mouseX - list.getRowLeft() <= iconSize) {
//						Thread downloadT = new Thread(() -> {
//							isBusy = true;
//							Networking.HTTPS.downloadMod(smod.getId(), smod.getVersion(), "./mods");
//							isBusy = false;
//						});
//						downloadT.start();
//					} else if (Util.getMeasuringTimeMs() - this.sinceLastClick < 250) {
//						Thread downloadT = new Thread(() -> {
//							isBusy = true;
//							Networking.HTTPS.downloadMod(smod.getId(), smod.getVersion(), "./mods");
//							isBusy = false;
//						});
//						downloadT.start();
//					}
				}
			}
			this.sinceLastClick = Util.getMeasuringTimeMs();
			return true;
		}


	public void openConfig() {
		MinecraftClient.getInstance().setScreen(ModMenu.getConfigScreen(mod.getId(), list.getParent()));
	}

	public Mod getMod() {
		return mod;
	}

	public boolean useSMOD() {
		return useSMod;
	}

	public SMod getSMod() {
		return smod;
	}

	public Identifier getIconTexture() {
		if (this.iconLocation == null) {
			if(useSMod) {

				Identifier modIcon = null;
				if (smod.meta != null && smod.meta.icon != null) {
					modIcon = TexturesManager.lFromBase64(smod.meta.icon, "/textures/logo"+smod.id);
				} else {
					this.iconLocation = UNKNOWN_ICON;
				}
				if(modIcon == null){
					this.iconLocation =  UNKNOWN_ICON; // icon data
				} else {
					this.iconLocation = modIcon;
				}

			} else {
				this.iconLocation = new Identifier(ModMenu.MOD_ID, mod.getId() + "_icon");
				NativeImageBackedTexture icon = mod.getIcon(list.getFabricIconHandler(), 64 * this.client.options.getGuiScale().getValue());
				if (icon != null) {
					this.client.getTextureManager().registerTexture(this.iconLocation, icon);
				} else {
					this.iconLocation = UNKNOWN_ICON;
				}
			}
		}
		return iconLocation;
	}

	public int getXOffset() {
		return 0;
	}
}
