package com.skellybuilds.servermodmenu.gui;

import com.google.common.base.Joiner;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skellybuilds.servermodmenu.config.ModMenuConfig;
import com.skellybuilds.servermodmenu.config.ModMenuConfigManager;
import com.skellybuilds.servermodmenu.db.SMod;
import com.skellybuilds.servermodmenu.gui.widget.ModListWidget;
import com.skellybuilds.servermodmenu.gui.widget.entries.ModListEntry;
import com.skellybuilds.servermodmenu.ModMenu;
import com.skellybuilds.servermodmenu.gui.widget.DescriptionListWidget;
import com.skellybuilds.servermodmenu.util.DrawingUtil;
import com.skellybuilds.servermodmenu.util.Networking;
import com.skellybuilds.servermodmenu.util.TranslationUtil;
import com.skellybuilds.servermodmenu.util.mod.Mod;
import com.skellybuilds.servermodmenu.util.mod.ModBadgeRenderer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.render.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static com.skellybuilds.servermodmenu.ModMenu.MainNetwork;


public class ModsScreen extends Screen {
	private static final Identifier FILTERS_BUTTON_LOCATION = new Identifier(ModMenu.MOD_ID, "textures/gui/filters_button.png");
	private static final Identifier DOWNLOSD_BUTTON_LOCATION = new Identifier(ModMenu.MOD_ID, "textures/gui/download_button.png");
	private static final Identifier RELOADS_BUTTON_LOCATION = new Identifier(ModMenu.MOD_ID, "textures/gui/reload_servers.png");
	private static final Text OptModT = Text.translatable("modmenu.isOpt");
	private static final Text ReqModT = Text.translatable("modmenu.isReq");
	private static final Text TOGGLE_FILTER_OPTIONS = Text.translatable("modmenu.toggleFilterOptions");
	private static final Text RELOAD_ALLSERV_T = Text.translatable("modmenu.reloadAllServers");
	private static final Text DOWNLOADALLSERV_T = Text.translatable("modmenu.downloadsAll");
	private static final Text CONFIGURE = Text.translatable("modmenu.configure");
	private static final Logger LOGGER = LoggerFactory.getLogger("Mod Menu | ModsScreen");
	private TextFieldWidget searchBox;
	private DescriptionListWidget descriptionListWidget;
	private final Screen previousScreen;
	public ModListWidget modList;
	private ModListEntry selected;
	private ModBadgeRenderer modBadgeRenderer;
	private double scrollPercent = 0;
	private boolean init = false;
	private boolean filterOptionsShown = false;
	private int paneY;
	private static final int RIGHT_PANE_Y = 48;
	private int paneWidth;
	private int rightPaneX;
	private int searchBoxX;
	private int filtersX;
	private int filtersWidth;
	private int searchRowWidth;
	public final Set<String> showModChildren = new HashSet<>();
	public SMod[] ModsA = {};
	public final Map<String, Boolean> modHasConfigScreen = new HashMap<>();
	public final Map<String, Throwable> modScreenErrors = new HashMap<>();
	private MinecraftClient client = MinecraftClient.getInstance();
	private ServerList serverList;
	public AtomicInteger amountofvmods = new AtomicInteger();

	public ModsScreen(Screen previousScreen) {
		super(Text.translatable("servermodmenu.title"));
		this.previousScreen = previousScreen;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (modList.isMouseOver(mouseX, mouseY)) {
			return this.modList.mouseScrolled(mouseX, mouseY, amount);
		}
		if (descriptionListWidget.isMouseOver(mouseX, mouseY)) {
			return this.descriptionListWidget.mouseScrolled(mouseX, mouseY, amount);
		}
		return false;
	}



	private boolean NThreadsFinished(){
		final boolean[] isAllDone = {true};
		MainNetwork.networkThreads.forEach((d, a) -> {
			if(!isAllDone[0]) return;
			if(a.getState() == Thread.State.RUNNABLE) {
				isAllDone[0] = false;
			}
		});

		return isAllDone[0];
	}

	public void switchToConfirm(){
			this.client.execute(() -> {
				 this.client.setScreen(new ConfirmationScreen(this, this::resCB, this::backCB, Text.literal("All of your mods have finished downloading! Do you wish to close the game?")
					.formatted(Formatting.ITALIC).formatted(Formatting.GREEN)));
			});

	}

	public void switchToConfirmCS(String CSText){
		this.client.execute(() -> {
			this.client.setScreen(new ConfirmationScreen(this, this::resCB, this::backCB, Text.literal(CSText)
				.formatted(Formatting.ITALIC).formatted(Formatting.GREEN)));
		});

	}

	@Override
	public void tick() {
		this.searchBox.tick();
	}

	private void backCB(ConfirmationScreen bla){
		MinecraftClient.getInstance().setScreen(bla.prevS);
	}
	private void resCB(ConfirmationScreen bla){
		LOGGER.info("Your game didn't crash, you intentionally (or by mistake, you never know) closed the game. Restart the game manually");
		MinecraftClient.getInstance().scheduleStop();
	}

	public void calcServersSize(){
		amountofvmods.set(0);
		modList.children().forEach((entry) -> {
			if(entry.isFirst) {
				AtomicBoolean isHidden = new AtomicBoolean(false);
				if (!entry.renderSvnNO && ModMenu.SMODS.get(entry.serverName).size() > 1) {
					ModMenuConfig.HIDDEN_SERVERS.getValue().forEach((name) -> {
							if (Objects.equals(name, entry.serverName)) {
								isHidden.set(true);
							}
						});

					if(isHidden.get()){
						if(ModMenuConfig.SHOWHIDDENSERVERS.getValue()) amountofvmods.getAndIncrement();
					} else {
						amountofvmods.getAndIncrement();
					}
				}
			}
		});
	}

	@Override
	protected void init() {

		serverList = new ServerList(client);
		serverList.loadFile();

		paneY = ModMenuConfig.CONFIG_MODE.getValue() ? 48 : 48 + 19;
		paneWidth = this.width / 2 - 8;
		rightPaneX = width - paneWidth;

		int filtersButtonSize = (ModMenuConfig.CONFIG_MODE.getValue() ? 0 : 22);
		int searchWidthMax = paneWidth - 32 - filtersButtonSize;
		int searchBoxWidth = ModMenuConfig.CONFIG_MODE.getValue() ? Math.min(200, searchWidthMax) : searchWidthMax;
		searchBoxX = paneWidth / 2 - searchBoxWidth / 2 - filtersButtonSize / 2;
		this.searchBox = new TextFieldWidget(this.textRenderer, searchBoxX, 22, searchBoxWidth, 20, this.searchBox, Text.translatable("modmenu.search"));
		this.searchBox.setChangedListener((string_1) -> this.modList.filter(string_1, false));

		for (Mod mod : ModMenu.MODS.values()) {
			String id = mod.getId();
			if (!modHasConfigScreen.containsKey(id)) {
				try {
					Screen configScreen = ModMenu.getConfigScreen(id, this);
					modHasConfigScreen.put(id, configScreen != null);
				} catch (java.lang.NoClassDefFoundError e) {
					LOGGER.warn("The '" + id + "' mod config screen is not available because " + e.getLocalizedMessage() + " is missing.");
					modScreenErrors.put(id, e);
					modHasConfigScreen.put(id, false);
				} catch (Throwable e) {
					LOGGER.error("Error from mod '" + id + "'", e);
					modScreenErrors.put(id, e);
					modHasConfigScreen.put(id, false);
				}
			}
		}
		ModMenu.SMODS.forEach((d, w) -> {
			w.forEach((e, mod) -> {
				String id = mod.id;
				if (!modHasConfigScreen.containsKey(id)) {
					try {
						Screen configScreen = ModMenu.getConfigScreen(id, this);
						modHasConfigScreen.put(id, configScreen != null);
					} catch (java.lang.NoClassDefFoundError wa) {
						LOGGER.warn("The '" + id + "' mod config screen is not available because " + wa.getLocalizedMessage() + " is missing.");
						modScreenErrors.put(id, wa);
						modHasConfigScreen.put(id, false);
					} catch (Throwable wa) {
						LOGGER.error("Error from mod '" + id + "'", wa);
						modScreenErrors.put(id, wa);
						modHasConfigScreen.put(id, false);
					}
				}
			});

			});



		this.modList = new ModListWidget(this.client, paneWidth, this.height, paneY, this.height - 36, ModMenuConfig.COMPACT_LIST.getValue() ? 23 : 36, this.searchBox.getText(), this.modList, this);
		if(ModMenu.MODS.isEmpty() && !ModMenu.SMODS.isEmpty()){
			this.modList.useSMod = true;
		}
		this.modList.setLeftPos(0);
		modList.reloadFilters();

		// Downloads all from each server. Yep, may take time!
		ButtonWidget downloadAllSButton = new TexturedButtonWidget(paneWidth / 2 + searchBoxWidth / 2 - 20 / 2 + 41, 22, 20, 20, 0, 0, 20, DOWNLOSD_BUTTON_LOCATION, 32, 64, button -> {

			final SoundManager[] tempmgr = new SoundManager[1];
			boolean change = false;
button.active = false;
Thread finalT = new Thread(() -> {
	AtomicBoolean isERRORD = new AtomicBoolean(false);
	AtomicBoolean isSUCONCE = new AtomicBoolean(false);
	modList.children().forEach((child) -> {
		if(child.isFirst){
			EntryButton mButton = ModMenu.buttonEntries.get(child.serverName);
			mButton.active = false;
		}
	});

	modList.children().forEach((child) -> {
		if(child.isFirst) {
			EntryButton mButton = ModMenu.buttonEntries.get(child.serverName);
			if (!change) {
				tempmgr[0] = mButton.SOUNDMANAGER;
			}
			child.downloadA(mButton);
			boolean isDT0 = false;
			boolean isDTFW = false;
			while (true) {
				if(MainNetwork.isDthreadDone(child.serverName)){
					isDT0 = true;
				}
				if(isDT0 && !isDTFW){
					try {
						Thread.sleep(2950);
					} catch (InterruptedException e) {
						LOGGER.error("Interrupted: {}", e.toString());
					}
					if(MainNetwork.isDthreadDone(child.serverName)){
						if(Objects.equals(MainNetwork.networkErrors.get(child.serverName), "ERR")){
							button.active = true;
							button.visible = true;
							mButton.active = true;
							mButton.visible = true;
							tempmgr[0].play(PositionedSoundInstance.master(SoundEvents.ENTITY_VILLAGER_NO, 1.0F));
							isERRORD.set(true);
							break;
						} else {
							isDTFW = true;
							isSUCONCE.set(true);
						}
					} else isDT0 = false;
				} else {
					if(!isDTFW) {
						try {
							Thread.sleep(750);
						} catch (InterruptedException e) {
							LOGGER.error("Interrupted: {}", e.toString());
						}
					} else {
						break;
					}
				}
			}
			if(!isERRORD.get() && !isSUCONCE.get()) {
				mButton.visible = false;
			}
		}
	});

	// if no errors & downloaded a server sucessfully
	if(!isERRORD.get() && isSUCONCE.get()) {
		tempmgr[0].play(PositionedSoundInstance.master(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
		button.visible = false;
		switchToConfirm();
	} // a server downlaoded successfully but another one failed!
	else if(isERRORD.get() && isSUCONCE.get()) {
		tempmgr[0].play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_BIG_FALL, 1.0F));
		//button.visible = false;
		switchToConfirmCS("A server's mod successfully were downloaded but another one failed!!! Do you wish to close the game?");
	} // All servers failed to download!!!
	else if(isERRORD.get() && !isSUCONCE.get()){
		tempmgr[0].play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_DEATH, 1.0F));
	}
});

finalT.start();



		}){
			@Override
			public void render(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
			if(amountofvmods.get() == 0){
				visible = false;
				active = false;
				return;
			}

				if(selected == null) {
				visible = false;
				active = false;
				return;
			}



				if(ModMenu.isAllDFB){
					visible = false;
					active = false;
					return;
				}

				visible = true;
				active = true;

				super.render(DrawContext, mouseX, mouseY, delta);
		}

		@Override
		public void renderButton(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
			RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
			RenderSystem.setShaderColor(1, 1, 1, 1f);
			super.renderButton(DrawContext, mouseX, mouseY, delta);
		}
	};

		downloadAllSButton.setTooltip(Tooltip.of(DOWNLOADALLSERV_T));

		this.descriptionListWidget = new DescriptionListWidget(this.client, paneWidth, this.height, RIGHT_PANE_Y + 60, this.height - 36, textRenderer.fontHeight + 1, this);
		this.descriptionListWidget.setLeftPos(rightPaneX);

		ButtonWidget downloadButton = new TexturedButtonWidget(width - 24, RIGHT_PANE_Y, 20, 20, 0, 0, 20, DOWNLOSD_BUTTON_LOCATION, 32, 64, button -> {
			if(selected == null) return;

			if(!ModMenu.buttonEntries.get(selected.serverName).active) return;
			if(!ModMenu.buttonEntries.get(selected.serverName).visible) return;
			final String id = Objects.requireNonNull(selected).getSMod().getId();
//			if(selected.renderSvnNO){
//				button.visible = false;
//				return;
//			}
			if(Networking.isModAlreadyPresent(id)){
//				button.visible = false;
				return;
			} else {
	//			button.visible = true;
				button.active = false;
				Thread orgw = new Thread(() -> {
					MainNetwork.requestNDownload(selected.serverName, id);
				});

				orgw.start();

				AtomicBoolean isNE = new AtomicBoolean(false);

				Thread bla = new Thread(() -> {
					while (true) {
						if (orgw.getState() != Thread.State.RUNNABLE) {
							if(Objects.equals(MainNetwork.networkErrors.get(selected.serverName), "ERR")){
							button.active = true;
							button.visible = true;
								isNE.set(true);
							} else {
								ModMenu.idsDLD.add(selected.getSMod().id);
								selected.smod.isDownloaded = true;
								button.active = true;
								if (!ModMenu.isAllDFB) {
									List<Boolean> isAllt = new ArrayList<>();
									ModMenu.buttonEntries.forEach((name, mButton) -> {
										if (!mButton.visible)
											isAllt.add(true);
									});

									if (isAllt.size() == ModMenu.buttonEntries.size())
										ModMenu.isAllDFB = true;
								}
							}
					break;
						}
					}
					return;
				});

				bla.start();

				do {
					if (bla.getState() != Thread.State.RUNNABLE) {
						if(!isNE.get()) switchToConfirm();
						break;
					}
				} while (true);

			}
		}) {
			@Override
			public void render(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
				if(selected == null) return;

				if(ModMenu.isAllDFB){
					visible = false;
					super.render(DrawContext, mouseX, mouseY, delta);
				}

				if(!ModMenu.buttonEntries.get(selected.serverName).visible){
					visible = false;
					super.render(DrawContext, mouseX, mouseY, delta);
					return;
				}
				// all mods downloaded if all mods are downloaded
				if(!ModMenu.buttonEntries.get(selected.serverName).active){
					active = false;
					super.render(DrawContext, mouseX, mouseY, delta);
					return;
				}

				if(selected.renderSvnNO){
					visible = false;
					super.render(DrawContext, mouseX, mouseY, delta);
					return;
				}
				if(Networking.isModAlreadyPresent(selected.getSMod().id)){
					visible = false;
					super.render(DrawContext, mouseX, mouseY, delta);
					return;
				} else {

						for (String s : ModMenu.idsDLD) {
							if (Objects.equals(s, selected.getSMod().getId())) {
								visible = false;
								return;
							}
						}

						visible = true;
						active = true;

						super.render(DrawContext, mouseX, mouseY, delta);
						return;


				}


				//
			}

			@Override
			public void renderButton(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
				RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
				RenderSystem.setShaderColor(1, 1, 1, 1f);
				super.renderButton(DrawContext, mouseX, mouseY, delta);
			}
		};
		int urlButtonWidths = paneWidth / 2 - 2;
		int cappedButtonWidth = Math.min(urlButtonWidths, 200);
		ButtonWidget websiteButton = new ButtonWidget(rightPaneX + (urlButtonWidths / 2) - (cappedButtonWidth / 2), RIGHT_PANE_Y + 36, Math.min(urlButtonWidths, 200), 20,
				Text.translatable("modmenu.website"), button -> {
			if(selected.useSMOD()) {
				final SMod mod = Objects.requireNonNull(selected).getSMod();
				this.client.setScreen(new ConfirmLinkScreen((bool) -> {
					if (bool) {
						Util.getOperatingSystem().open(mod.meta.contact.getHomepage().replaceAll("\"", ""));
					}
					this.client.setScreen(this);
				}, mod.meta.contact.getHomepage().replaceAll("\"", ""), false));
			} else {
				final Mod mod = Objects.requireNonNull(selected).getMod();
				this.client.setScreen(new ConfirmLinkScreen((bool) -> {
					if (bool) {
						Util.getOperatingSystem().open(mod.getWebsite());
					}
					this.client.setScreen(this);
				}, mod.getWebsite(), false));
			}
		}, Supplier::get) {
			@Override
			public void render(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
				visible = selected != null;
				if(selected != null) {
					if (selected.renderSvnNO) {
						visible = false;
					} else {
						if (selected.useSMOD())
							active = visible && selected.getSMod().meta.contact.getHomepage() != null;
						else active = visible && selected.getMod().getWebsite() != null;
					}
				}
				super.render(DrawContext, mouseX, mouseY, delta);
			}
		};
		ButtonWidget issuesButton = new ButtonWidget(rightPaneX + urlButtonWidths + 4 + (urlButtonWidths / 2) - (cappedButtonWidth / 2), RIGHT_PANE_Y + 36, Math.min(urlButtonWidths, 200), 20,
				Text.translatable("modmenu.issues"), button -> {
			if(selected.useSMOD()){
				if(selected.renderSvnNO){
					return;
				}
				final SMod mod = Objects.requireNonNull(selected).getSMod();
				this.client.setScreen(new ConfirmLinkScreen((bool) -> {
					if (bool) {
						Util.getOperatingSystem().open(mod.meta.contact.getIssues().replaceAll("\"", ""));
					}
					this.client.setScreen(this);
				}, mod.meta.contact.getIssues().replaceAll("\"", ""), false));
			} else {
				final Mod mod = Objects.requireNonNull(selected).getMod();
				this.client.setScreen(new ConfirmLinkScreen((bool) -> {
					if (bool) {
						Util.getOperatingSystem().open(mod.getIssueTracker());
					}
					this.client.setScreen(this);
				}, mod.getIssueTracker(), false));
			}
		}, Supplier::get) {
			@Override
			public void render(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
				visible = selected != null;
				if(selected != null) {
					if (selected.renderSvnNO) {
						visible = false;
					} else {
						if (selected.useSMOD()) active = visible && selected.getSMod().meta.contact.getIssues() != null;
						else active = visible && selected.getMod().getIssueTracker() != null;
					}
				}

				super.render(DrawContext, mouseX, mouseY, delta);
			}
		};
		this.addSelectableChild(this.searchBox);
		ButtonWidget filtersButton = new TexturedButtonWidget(paneWidth / 2 + searchBoxWidth / 2 - 20 / 2 + 2, 22, 20, 20, 0, 0, 20, FILTERS_BUTTON_LOCATION, 32, 64, button -> filterOptionsShown = !filterOptionsShown, TOGGLE_FILTER_OPTIONS);
		filtersButton.setTooltip(Tooltip.of(TOGGLE_FILTER_OPTIONS));
		ButtonWidget reloadSButton = new TexturedButtonWidget(paneWidth / 2 + searchBoxWidth / 2 - 20 / 2 + 22, 22, 20, 20, 0, 0, 20, RELOADS_BUTTON_LOCATION, 32, 64, button -> {
			serverList = new ServerList(client);
			serverList.loadFile();
			MainNetwork.reloadAllServers(serverList);
			button.active = false;
			new Thread(() -> {
				while(true) {
					if(MainNetwork.isNthreadsDone()){
						this.modList.reloadFilters();
						button.active = true;
						break;
					}
				}
				return;
			}).start();
			}, RELOAD_ALLSERV_T);
		reloadSButton.setTooltip(Tooltip.of(RELOAD_ALLSERV_T));
//		if (!ModMenuConfig.CONFIG_MODE.getValue()) {
//
//
//		}
		this.addDrawableChild(filtersButton);
		this.addDrawableChild(reloadSButton);
		Text showLibrariesText = ModMenuConfig.SHOW_LIBRARIES.getButtonText();
		Text sortingText = ModMenuConfig.SORTING.getButtonText();
		int showLibrariesWidth = textRenderer.getWidth(showLibrariesText) + 4;
		int sortingWidth = textRenderer.getWidth(sortingText);
		Text showHBT = ModMenuConfig.SHOWHIDDENSERVERS.getButtonText();
		filtersWidth = showLibrariesWidth + sortingWidth + 2;
		searchRowWidth = searchBoxX + searchBoxWidth + 22;
		updateFiltersX();
		this.addDrawableChild(new ButtonWidget(21, 45, 50, 20, sortingText, button -> {
			ModMenuConfig.SSORTING.cycleValue();
			ModMenuConfigManager.save();
			modList.reloadFilters();
		}, Supplier::get) {
			@Override
			public void render(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
				DrawContext.getMatrices().translate(0, 0, 1);
				visible = filterOptionsShown;
				this.setMessage(ModMenuConfig.SSORTING.getButtonText());
				super.render(DrawContext, mouseX, mouseY, delta);
			}
		});
		this.addDrawableChild(new ButtonWidget(77, 45, textRenderer.getWidth(showHBT)+6, 20, showHBT, button -> {
			ModMenuConfig.SHOWHIDDENSERVERS.toggleValue();
			ModMenuConfigManager.save();
			calcServersSize();
			modList.reloadFilters();
		}, Supplier::get) {
			@Override
			public void render(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
				DrawContext.getMatrices().translate(0, 0, 1);
				visible = filterOptionsShown;
				this.setMessage(ModMenuConfig.SHOWHIDDENSERVERS.getButtonText());
				super.render(DrawContext, mouseX, mouseY, delta);
			}
		});
		this.addSelectableChild(this.modList);
		this.addDrawableChild(downloadAllSButton);
		if (!ModMenuConfig.HIDE_CONFIG_BUTTONS.getValue()) {
			this.addDrawableChild(downloadButton);
		}
		this.addDrawableChild(websiteButton);
		this.addDrawableChild(issuesButton);
		this.addSelectableChild(this.descriptionListWidget);
		this.addDrawableChild(
				ButtonWidget.builder(ScreenTexts.DONE, button -> client.setScreen(previousScreen))
						.position(215, this.height - 28) //this.width / 2 + 4 - 14
						.size(150, 20)
						.narrationSupplier(Supplier::get)
						.build());
		this.searchBox.setFocused(true);

		init = true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return super.keyPressed(keyCode, scanCode, modifiers) || this.searchBox.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char chr, int keyCode) {
		return this.searchBox.charTyped(chr, keyCode);
	}

	@Override
	public void render(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
		//if(selected == null && ModsA.length > 0)updateSelectedEntry(new ModListEntry(ModsA[0], modList));

		this.renderBackgroundTexture(DrawContext);
		ModListEntry selectedEntry = selected;
		if (selectedEntry != null && !selectedEntry.renderSvnNO) {
			this.descriptionListWidget.render(DrawContext, mouseX, mouseY, delta);
		}
		this.modList.render(DrawContext, mouseX, mouseY, delta);
		this.searchBox.render(DrawContext, mouseX, mouseY, delta);
		RenderSystem.disableBlend();
		DrawContext.drawCenteredTextWithShadow(this.textRenderer, this.title, this.modList.getWidth() / 2, 8, 16777215);
//		if (!ModMenuConfig.DISABLE_DRAG_AND_DROP.getValue()) {
//			DrawContext.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("modmenu.dropInfo.line1").formatted(Formatting.GRAY), this.width - this.modList.getWidth() / 2, RIGHT_PANE_Y / 2 - client.textRenderer.fontHeight - 1, 16777215);
//			DrawContext.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("modmenu.dropInfo.line2").formatted(Formatting.GRAY), this.width - this.modList.getWidth() / 2, RIGHT_PANE_Y / 2 + 1, 16777215);
//		}
		if (!ModMenuConfig.CONFIG_MODE.getValue()) {



			Text fullModCount = Text.translatable("servermodmenu.showingMods.n", amountofvmods);
			if (!ModMenuConfig.CONFIG_MODE.getValue() && updateFiltersX()) {
				if (filterOptionsShown) {
					if (!ModMenuConfig.SHOW_LIBRARIES.getValue() || textRenderer.getWidth(fullModCount) <= filtersX - 5) {
						DrawContext.drawText(textRenderer, fullModCount.asOrderedText(), searchBoxX, 52, 0xFFFFFF, false);
					} else {
						if (selected == null) {
							DrawContext.drawText(textRenderer, Text.translatable("modmenu.adddamnservers"), searchBoxX, 46, 0xFF0000, true);
						} else {
							DrawContext.drawText(textRenderer, Text.translatable("servermodmenu.showingMods.n", amountofvmods).asOrderedText(), searchBoxX, 46, 0xFFFFFF, false);
							DrawContext.drawText(textRenderer, computeLibraryCountText().asOrderedText(), searchBoxX, 57, 0xFFFFFF, false);
						}
					}
				} else {
					if (!ModMenuConfig.SHOW_LIBRARIES.getValue() || textRenderer.getWidth(fullModCount) <= modList.getWidth() - 5) {
						DrawContext.drawText(textRenderer, fullModCount.asOrderedText(), searchBoxX, 52, 0xFFFFFF, false);
					} else {
						if (selected == null) {
							DrawContext.drawText(textRenderer, Text.translatable("modmenu.adddamnservers"), searchBoxX, 46, 0xFF0000, true);
						} else {
							DrawContext.drawText(textRenderer, Text.translatable("servermodmenu.showingMods.n", amountofvmods).asOrderedText(), searchBoxX, 46, 0xFFFFFF, false);
							DrawContext.drawText(textRenderer, Text.translatable("servermodmenu.showingMods.n", amountofvmods).asOrderedText(), searchBoxX, 57, 0xFFFFFF, false);
						}
					}
				}
			}
		}
		if (selectedEntry != null) {
			if (selectedEntry.useSMOD()) {
				SMod smod = selectedEntry.getSMod();
				if(!selectedEntry.renderSvnNO) {
					int x = rightPaneX;
					if ("java".equals(smod.getId())) {
						DrawingUtil.drawRandomVersionBackgroundS(smod, DrawContext, x, RIGHT_PANE_Y, 32, 32);
					}
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
					RenderSystem.enableBlend();
					DrawContext.drawTexture(this.selected.getIconTexture(), x, RIGHT_PANE_Y, 0.0F, 0.0F, 32, 32, 32, 32);
					RenderSystem.disableBlend();
					int lineSpacing = textRenderer.fontHeight + 1;
					int imageOffset = 36;
					Text name = Text.literal(smod.meta.name);
					StringVisitable trimmedName = name;
					int maxNameWidth = this.width - (x + imageOffset);
					if (textRenderer.getWidth(name) > maxNameWidth) {
						StringVisitable ellipsis = StringVisitable.plain("...");
						trimmedName = StringVisitable.concat(textRenderer.trimToWidth(name, maxNameWidth - textRenderer.getWidth(ellipsis)), ellipsis);
					}
					DrawContext.drawText(textRenderer, Language.getInstance().reorder(trimmedName), x + imageOffset, RIGHT_PANE_Y + 1, 0xFFFFFF, false);
					if (mouseX > x + imageOffset && mouseY > RIGHT_PANE_Y + 1 && mouseY < RIGHT_PANE_Y + 1 + textRenderer.fontHeight && mouseX < x + imageOffset + textRenderer.getWidth(trimmedName)) {
						this.setTooltip(Text.translatable("modmenu.modIdToolTip", smod.getId()));
					}
					if (init || modBadgeRenderer == null || modBadgeRenderer.getSMod() != smod) {
						modBadgeRenderer = new ModBadgeRenderer(x + imageOffset + this.client.textRenderer.getWidth(trimmedName) + 2, RIGHT_PANE_Y, width - 28, selectedEntry.smod, this);
						init = false;
					}
					if (!ModMenuConfig.HIDE_BADGES.getValue()) {
						if (!selected.useSMOD()) modBadgeRenderer.draw(DrawContext, mouseX, mouseY);
					}

					DrawContext.drawText(textRenderer, smod.getVersion(), x + imageOffset, RIGHT_PANE_Y + 2 + lineSpacing, 0x808080, false);
					if(smod.isOptional){
						DrawContext.drawText(textRenderer, OptModT, x + imageOffset, RIGHT_PANE_Y + 10 + lineSpacing, 0x808080, false);
					} else {
						DrawContext.drawText(textRenderer, ReqModT, x + imageOffset, RIGHT_PANE_Y + 10 + lineSpacing, 0x808080, false);
					}


					String authors;
					List<String> names = Arrays.asList(smod.meta.authors);

					if (!names.isEmpty()) {
						if (names.size() > 1) {
							authors = Joiner.on(", ").join(names);
						} else {
							authors = names.get(0);
						}
						DrawingUtil.drawWrappedString(DrawContext, I18n.translate("modmenu.authorPrefix", authors), x + imageOffset, RIGHT_PANE_Y + 2 + lineSpacing * 2, paneWidth - imageOffset - 4, 1, 0x808080);
					}
				}
			} else {
				Mod mod = selectedEntry.getMod();
				int x = rightPaneX;
				if ("java".equals(mod.getId())) {
					DrawingUtil.drawRandomVersionBackground(mod, DrawContext, x, RIGHT_PANE_Y, 32, 32);
				}
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				RenderSystem.enableBlend();
				DrawContext.drawTexture(this.selected.getIconTexture(), x, RIGHT_PANE_Y, 0.0F, 0.0F, 32, 32, 32, 32);
				RenderSystem.disableBlend();
				int lineSpacing = textRenderer.fontHeight + 1;
				int imageOffset = 36;
				Text name = Text.literal(mod.getTranslatedName());
				StringVisitable trimmedName = name;
				int maxNameWidth = this.width - (x + imageOffset);
				if (textRenderer.getWidth(name) > maxNameWidth) {
					StringVisitable ellipsis = StringVisitable.plain("...");
					trimmedName = StringVisitable.concat(textRenderer.trimToWidth(name, maxNameWidth - textRenderer.getWidth(ellipsis)), ellipsis);
				}
				DrawContext.drawText(textRenderer, Language.getInstance().reorder(trimmedName), x + imageOffset, RIGHT_PANE_Y + 1, 0xFFFFFF, false);
				if (mouseX > x + imageOffset && mouseY > RIGHT_PANE_Y + 1 && mouseY < RIGHT_PANE_Y + 1 + textRenderer.fontHeight && mouseX < x + imageOffset + textRenderer.getWidth(trimmedName)) {
					this.setTooltip(Text.translatable("modmenu.modIdToolTip", mod.getId()));
				}
				if (init || modBadgeRenderer == null || modBadgeRenderer.getMod() != mod) {
					modBadgeRenderer = new ModBadgeRenderer(x + imageOffset + this.client.textRenderer.getWidth(trimmedName) + 2, RIGHT_PANE_Y, width - 28, selectedEntry.mod, this);
					init = false;
				}
				if (!ModMenuConfig.HIDE_BADGES.getValue()) {
					modBadgeRenderer.draw(DrawContext, mouseX, mouseY);
				}
				if (mod.isReal()) {
					DrawContext.drawText(textRenderer, mod.getPrefixedVersion(), x + imageOffset, RIGHT_PANE_Y + 2 + lineSpacing, 0x808080, false);
				}
				String authors;
				List<String> names = mod.getAuthors();

				if (!names.isEmpty()) {
					if (names.size() > 1) {
						authors = Joiner.on(", ").join(names);
					} else {
						authors = names.get(0);
					}
					DrawingUtil.drawWrappedString(DrawContext, I18n.translate("modmenu.authorPrefix", authors), x + imageOffset, RIGHT_PANE_Y + 2 + lineSpacing * 2, paneWidth - imageOffset - 4, 1, 0x808080);
				}
			}
			super.render(DrawContext, mouseX, mouseY, delta);
		} else {
			super.render(DrawContext, mouseX, mouseY, delta);
		}
	}

//	private Text computeModCountText(boolean includeLibs) {
//		int davin = ModMenu.SMODS.values().size();
//
//		//int[] rootMods = formatModCount(davin.stream().map((mmod) -> mmod.).collect(Collectors.toSet()));
//
//		if(davin < 1){
//			return Text.translatable("modmenu.adddamnservers");
//		}
//
//		if (includeLibs && ModMenuConfig.SHOW_LIBRARIES.getValue()) {
//			//int[] rootLibs = formatModCount(ModMenu.ROOT_MODS.values().stream().filter(mod -> !mod.isHidden() && mod.getBadges().contains(Mod.Badge.LIBRARY)).map(Mod::getId).collect(Collectors.toSet()));
//			return TranslationUtil.
//		} else {
//			return TranslationUtil.translateNumeric("modmenu.showingMods", rootMods);
//		}
//	}

	private Text computeLibraryCountText() {
		if (ModMenuConfig.SHOW_LIBRARIES.getValue()) {
			int[] rootLibs = formatModCount(ModMenu.ROOT_MODS.values().stream().filter(mod -> !mod.isHidden() && mod.getBadges().contains(Mod.Badge.LIBRARY)).map(Mod::getId).collect(Collectors.toSet()));

			if(rootLibs.length < 1){
				return Text.translatable("modmenu.adddamnservers");
			}
			return TranslationUtil.translateNumeric("modmenu.showingLibraries", rootLibs);
		} else {
			return Text.literal(null);
		}
	}

	private int[] formatModCount(Set<String> set) {
		int visible = modList.getDisplayedCountFor(set);
		int total = set.size();
		if (visible == total) {
			return new int[]{total};
		}
		return new int[]{visible, total};
	}

	@Override
	public void close() {
		this.modList.close();
		this.client.setScreen(this.previousScreen);
	}

	public ModListEntry getSelectedEntry() {
		return selected;
	}

	public void updateSelectedEntry(ModListEntry entry) {
		if (entry != null) {
			this.selected = entry;
		}
	}

	public double getScrollPercent() {
		return scrollPercent;
	}

	public void updateScrollPercent(double scrollPercent) {
		this.scrollPercent = scrollPercent;
	}

	public String getSearchInput() {
		return searchBox.getText();
	}

	private boolean updateFiltersX() {
		if ((filtersWidth + textRenderer.getWidth(Text.translatable("servermodmenu.showingMods.n", ModMenu.SMODS.values().size())) + 20) >= searchRowWidth && ((filtersWidth + textRenderer.getWidth(Text.translatable("servermodmenu.showingMods.n", ModMenu.SMODS.values().size())) + 20) >= searchRowWidth || (filtersWidth + textRenderer.getWidth(computeLibraryCountText()) + 20) >= searchRowWidth)) {
			filtersX = paneWidth / 2 - filtersWidth / 2;
			return !filterOptionsShown;
		} else {
			filtersX = searchRowWidth - filtersWidth + 1;
			return true;
		}
	}

	@Override
	public void filesDragged(List<Path> paths) {
		Path modsDirectory = FabricLoader.getInstance().getGameDir().resolve("mods");

		// Filter out none mods
		List<Path> mods = paths.stream()
				.filter(ModsScreen::isFabricMod)
				.collect(Collectors.toList());

		if (mods.isEmpty()) {
			return;
		}

		String modList = mods.stream()
				.map(Path::getFileName)
				.map(Path::toString)
				.collect(Collectors.joining(", "));

		this.client.setScreen(new ConfirmScreen((value) -> {
			if (value) {
				boolean allSuccessful = true;

				for (Path path : mods) {
					try {
						Files.copy(path, modsDirectory.resolve(path.getFileName()));
					} catch (IOException e) {
						LOGGER.warn("Failed to copy mod from {} to {}", path, modsDirectory.resolve(path.getFileName()));
						SystemToast.addPackCopyFailure(client, path.toString());
						allSuccessful = false;
						break;
					}
				}

				if (allSuccessful) {
					SystemToast.add(client.getToastManager(), SystemToast.Type.TUTORIAL_HINT, Text.translatable("modmenu.dropSuccessful.line1"), Text.translatable("modmenu.dropSuccessful.line2"));
				}
			}
			this.client.setScreen(this);
		}, Text.translatable("modmenu.dropConfirm"), Text.literal(modList)));
	}

	private static boolean isFabricMod(Path mod) {
		try (JarFile jarFile = new JarFile(mod.toFile())) {
			return jarFile.getEntry("fabric.mod.json") != null;
		} catch (IOException e) {
			return false;
		}
	}

	public Map<String, Boolean> getModHasConfigScreen() {
		return modHasConfigScreen;
	}
}
