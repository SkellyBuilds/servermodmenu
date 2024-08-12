package com.skellybuilds.servermodmenu.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skellybuilds.servermodmenu.config.ModMenuConfig;
import com.skellybuilds.servermodmenu.db.SMod;
import com.skellybuilds.servermodmenu.gui.widget.entries.ChildEntry;
import com.skellybuilds.servermodmenu.gui.widget.entries.IndependentEntry;
import com.skellybuilds.servermodmenu.gui.widget.entries.ModListEntry;
import com.skellybuilds.servermodmenu.gui.widget.entries.ParentEntry;
import com.skellybuilds.servermodmenu.ModMenu;
import com.skellybuilds.servermodmenu.gui.ModsScreen;
import com.skellybuilds.servermodmenu.util.mod.Mod;
import com.skellybuilds.servermodmenu.util.mod.fabric.FabricIconHandler;
import com.skellybuilds.servermodmenu.util.mod.ModSearch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ModListWidget extends AlwaysSelectedEntryListWidget<ModListEntry> implements AutoCloseable {
	public static final boolean DEBUG = Boolean.getBoolean("modmenu.debug");
	private final ModsScreen parent;
	public List<Mod> mods = null;
	public List<SMod> smods = null;
	public boolean useSMod = false;
	private final Set<Mod> addedMods = new HashSet<>();
	// <Server Name, ServerMod> - for the highlighted text string to top
	private final Map<String, SMod> SaddedMods = new HashMap<>();
	private String selectedModId = null;
	private boolean scrolling;
	private final FabricIconHandler iconHandler = new FabricIconHandler();
	private boolean isInit = false;

	public ModListWidget(MinecraftClient client, int width, int height, int y1, int y2, int entryHeight, String searchTerm, ModListWidget list, ModsScreen parent) {
		super(client, width, height, y1, y2, entryHeight);
		this.parent = parent;
		if(list != null) {
			if (list.useSMod) {
			this.useSMod = true;
			this.smods = list.smods;
			}
				else this.mods = list.mods;


		}
		this.filter(searchTerm, false);
		setScrollAmount(parent.getScrollPercent() * Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4)));
	}

	@Override
	public void setScrollAmount(double amount) {
		super.setScrollAmount(amount);
		int denominator = Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4));
		if (denominator <= 0) {
			parent.updateScrollPercent(0);
		} else {
			parent.updateScrollPercent(getScrollAmount() / Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4)));
		}
	}

	@Override
	public boolean isFocused() {
		return parent.getFocused() == this;
	}

	public void select(String id, String server) {
		children().forEach((entryM) -> {
			if(Objects.equals(entryM.smod.getId(), id)){
				if(entryM.useSMOD()){
					if(Objects.equals(entryM.serverName, server)){
						this.setSelected(entryM);
						if(entryM.renderSvnNO) return;
						this.client.getNarratorManager().narrate(Text.translatable("narrator.select", entryM.smod.meta.name).getString());
					}
				} else {
					if(Objects.equals(entryM.serverName, server)){
					this.setSelected(entryM);
					this.client.getNarratorManager().narrate(Text.translatable("narrator.select", entryM.mod.getTranslatedName()).getString());
				}

				}
			}
		});

	}

	@Override
	public void setSelected(ModListEntry entry) {
		if(entry == null)return;
		super.setSelected(entry);
		if(entry.useSMOD()){
			selectedModId = entry.getSMod().getId();
			parent.updateSelectedEntry(getSelectedOrNull());
		} else {
			selectedModId = entry.getMod().getId();
			parent.updateSelectedEntry(getSelectedOrNull());
		}
	}

	@Override
	protected boolean isSelectedEntry(int index) {
		ModListEntry selected = getSelectedOrNull();
		//assert selected != null; why the fuck am i asserting?
		if(selected != null) {
			if (selected.useSMOD()) {
				if(!Objects.equals(selected.getSMod().server, getEntry(index).getSMod().server))return false;
				return selected.getSMod().getId().equals(getEntry(index).getSMod().getId());
			} else {
				if(!Objects.equals(selected.serverName, getEntry(index).serverName)) return false;
				return selected.getMod().getId().equals(getEntry(index).getMod().getId());
			}
		} else return false;

//		return false;
		}

	@Override
	public int addEntry(ModListEntry entry) {
		if(entry.useSMOD()){
			if (SaddedMods.get(entry.serverName) == entry.smod) {
				return 0;
			}
			SaddedMods.put(entry.serverName, entry.smod);
			int i = super.addEntry(entry);
			if (entry.getSMod().getId().equals(selectedModId)) {
				setSelected(entry);
			}
			return i;
		} else {
			if (addedMods.contains(entry.mod)) {
				return 0;
			}
			addedMods.add(entry.mod);
			int i = super.addEntry(entry);
			if (entry.getMod().getId().equals(selectedModId)) {
				setSelected(entry);
			}
			return i;
		}
	}

	@Override
	protected boolean removeEntry(ModListEntry entry) {
		if(entry.useSMOD()){
			SaddedMods.remove(entry.serverName, entry.smod);
		} else {
			addedMods.remove(entry.mod);
		}
		return super.removeEntry(entry);
	}

	@Override
	protected ModListEntry remove(int index) {
		if(getEntry(index).useSMOD()) addedMods.remove(getEntry(index).smod);
		else addedMods.remove(getEntry(index).mod);

		return super.remove(index);
	}

	public void reloadFilters() {
		isInit = false;
		filter(parent.getSearchInput(), true, false);
	}


	public void filter(String searchTerm, boolean refresh) {
		isInit = false;
		filter(searchTerm, refresh, true);
	}

	private boolean hasVisibleChildMods(Mod parent) {
		List<Mod> children = ModMenu.PARENT_MAP.get(parent);
		boolean hideLibraries = !ModMenuConfig.SHOW_LIBRARIES.getValue();

		return !children.stream().allMatch(child -> child.isHidden() || hideLibraries && child.getBadges().contains(Mod.Badge.LIBRARY));
	}

	private void filter(String searchTerm, boolean refresh, boolean search) {
		this.clearEntries();
		SaddedMods.clear();
		addedMods.clear();
		if (useSMod) {


			Map<String, SMod> modsMA;

			if(ModMenu.SMODSA.isEmpty()) {
				modsMA = new HashMap<>();

				ModMenu.SMODS.forEach((svn, mod) -> {
					for (Map.Entry<String, SMod> entry : mod.entrySet()) {
						String id = entry.getKey();
						SMod mods2 = entry.getValue();
						modsMA.put(id, mods2);
					}
				});
			} else modsMA = ModMenu.SMODSA;

			AtomicBoolean addMoreY = new AtomicBoolean(false);
			ModMenu.SMODS.forEach((serverName, modsM) -> {
				Collection<SMod> mods = new HashSet<>(modsM.values());
				this.smods = null;

				if (this.smods == null || refresh) {
					this.smods = new ArrayList<>();
					this.smods.addAll(mods);
					this.smods.sort(ModMenuConfig.SSORTING.getValue().getComparator());
				}

				List<SMod> matched = ModSearch.searchS(parent, searchTerm, this.smods);

				AtomicBoolean isHidden = new AtomicBoolean(false);
				boolean isF = true;
				for (SMod mod : matched) {
					if(isF){
						ModMenuConfig.HIDDEN_SERVERS.getValue().forEach((name) -> {
							if(Objects.equals(name, serverName)){
								if(!ModMenuConfig.SHOWHIDDENSERVERS.getValue()){
									isHidden.set(true);
								}
							}
						});
					}

				if(matched.isEmpty()){
					if(isHidden.get() && ModMenuConfig.SHOWHIDDENSERVERS.getValue() || !isHidden.get()) {
						this.addEntry(new IndependentEntry(new SMod("d", "d", null, false), this, serverName, true, true, addMoreY.get()));
					}
				}




					if(isHidden.get() && ModMenuConfig.SHOWHIDDENSERVERS.getValue() || !isHidden.get()) {
						this.addEntry(new IndependentEntry(mod, this, serverName, isF, false, addMoreY.get()));
					}
					isF = false;
				}

				addMoreY.set(true);

				if(children().size() > 1) {
					setSelected(getEntry(0));
				} else {
					setSelected(null);
				}

				if (getScrollAmount() > Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4))) {
					setScrollAmount(Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4)));
				}
			});

			isInit = true;

		} else {
			Collection<Mod> mods = ModMenu.MODS.values().stream().filter(mod -> {
				if (ModMenuConfig.CONFIG_MODE.getValue()) {
					Map<String, Boolean> modHasConfigScreen = parent.getModHasConfigScreen();
					var hasConfig = modHasConfigScreen.get(mod.getId());
					if (!hasConfig) {
						return false;
					}
				}

				return !mod.isHidden();
			}).collect(Collectors.toSet());

			if (DEBUG) {
				mods = new ArrayList<>(mods);
//			mods.addAll(TestModContainer.getTestModContainers());
			}

			if (this.mods == null || refresh) {
				this.mods = new ArrayList<>();
				this.mods.addAll(mods);
				this.mods.sort(ModMenuConfig.SORTING.getValue().getComparator());
			}

			List<Mod> matched = ModSearch.search(parent, searchTerm, this.mods);

			for (Mod mod : matched) {
				String modId = mod.getId();

				//Hide parent lib mods when the config is set to hide
				if (mod.getBadges().contains(Mod.Badge.LIBRARY) && !ModMenuConfig.SHOW_LIBRARIES.getValue()) {
					continue;
				}

				if (!ModMenu.PARENT_MAP.values().contains(mod)) {
					if (ModMenu.PARENT_MAP.keySet().contains(mod) && hasVisibleChildMods(mod)) {
						//Add parent mods when not searching
						List<Mod> children = ModMenu.PARENT_MAP.get(mod);
						children.sort(ModMenuConfig.SORTING.getValue().getComparator());
						ParentEntry parent = new ParentEntry(mod, children, this);
						this.addEntry(parent);
						//Add children if they are meant to be shown
						if (this.parent.showModChildren.contains(modId)) {
							List<Mod> validChildren = ModSearch.search(this.parent, searchTerm, children);
							for (Mod child : validChildren) {
								this.addEntry(new ChildEntry(child, parent, this, validChildren.indexOf(child) == validChildren.size() - 1));
							}
						}
					} else {
						//A mod with no children
						this.addEntry(new IndependentEntry(mod, this));
					}
				}
			}

			if (parent.getSelectedEntry() != null && !children().isEmpty() || this.getSelectedOrNull() != null && getSelectedOrNull().getMod() != parent.getSelectedEntry().getMod()) {
				for (ModListEntry entry : children()) {
					if (entry.getMod().equals(parent.getSelectedEntry().getMod())) {
						setSelected(entry);
					}
				}
			} else {
				if (getSelectedOrNull() == null && !children().isEmpty() && getEntry(0) != null) {
					setSelected(getEntry(0));
				}
			}

			if (getScrollAmount() > Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4))) {
				setScrollAmount(Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4)));
			}
		}
	}


	@Override
	protected void renderList(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
		int entryCount = this.getEntryCount();

			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder buffer = tessellator.getBuffer();


if(isInit) {
	for (int index = 0; index < entryCount; ++index) {

		if(this.children().isEmpty() || index > this.children().size()){
			return; // isInit and entry count can be unreliable when reloading servers.
			// This will reduce the likely chances of crashing on reload render
		}

		ModListEntry entry = this.getEntry(index);


		int entryTop = entry.isFirst ? this.getRowTop(index) + 12 : this.getRowTop(index) + 15;
		//int entryBottom = entry.isFirst ? this.getRowTop(index) + this.itemHeight + 8 : this.getRowTop(index) + this.itemHeight ;

		if(entry.moreY) entryTop = entryTop + 17;


		int entryHeight = this.itemHeight - 4;

//		if(entry.moreY){
//			this.bottom = this.bottom + 7;
//		}

		int rowWidth = this.getRowWidth();
		int entryLeft;
		if (this.isSelectedEntry(index) && !entry.renderSvnNO) {
			entryLeft = getRowLeft() - 2 + entry.getXOffset();
			int selectionRight = this.getRowLeft() + rowWidth + 2;
			float float_2 = this.isFocused() ? 1.0F : 0.5F;
			RenderSystem.setShader(GameRenderer::getPositionProgram);
			RenderSystem.setShaderColor(float_2, float_2, float_2, 1.0F);
			Matrix4f matrix = DrawContext.getMatrices().peek().getPositionMatrix();
			buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
			buffer.vertex(matrix, entryLeft, entryTop + entryHeight + 2, 0.0F).next();
			buffer.vertex(matrix, selectionRight, entryTop + entryHeight + 2, 0.0F).next();
			buffer.vertex(matrix, selectionRight, entryTop - 2, 0.0F).next();
			buffer.vertex(matrix, entryLeft, entryTop - 2, 0.0F).next();
			tessellator.draw();
			RenderSystem.setShader(GameRenderer::getPositionProgram);
			RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
			buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
			buffer.vertex(matrix, entryLeft + 1, entryTop + entryHeight + 1, 0.0F).next();
			buffer.vertex(matrix, selectionRight - 1, entryTop + entryHeight + 1, 0.0F).next();
			buffer.vertex(matrix, selectionRight - 1, entryTop - 1, 0.0F).next();
			buffer.vertex(matrix, entryLeft + 1, entryTop - 1, 0.0F).next();
			tessellator.draw();
		}

		entryLeft = this.getRowLeft();
		entry.render(DrawContext, index, entryTop, entryLeft, rowWidth, entryHeight, mouseX, mouseY, this.isMouseOver(mouseX, mouseY) && Objects.equals(this.getEntryAtPos(mouseX, mouseY), entry), delta);

	}
}
	}


	public void ensureVisible(ModListEntry entry) {
		super.ensureVisible(entry);
	}

	@Override
	protected void updateScrollingState(double double_1, double double_2, int int_1) {
		super.updateScrollingState(double_1, double_2, int_1);
		this.scrolling = int_1 == 0 && double_1 >= (double) this.getScrollbarPositionX() && double_1 < (double) (this.getScrollbarPositionX() + 6);
	}

	@Override
	public boolean mouseClicked(double double_1, double double_2, int int_1) {
		this.updateScrollingState(double_1, double_2, int_1);
		if (!this.isMouseOver(double_1, double_2)) {
			return false;
		} else {
			ModListEntry entry = this.getEntryAtPos(double_1, double_2);
			if (entry != null) {
				if (entry.mouseClicked(double_1, double_2, int_1)) {
					this.setFocused(entry);
					this.setDragging(true);
					return true;
				}
			} else if (int_1 == 0) {
				this.clickedHeader((int) (double_1 - (double) (this.left + this.width / 2 - this.getRowWidth() / 2)), (int) (double_2 - (double) this.top) + (int) this.getScrollAmount() - 4);
				return true;
			}

			return this.scrolling;
		}
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		if (getSelectedOrNull() != null) {
			return getSelectedOrNull().keyPressed(keyCode, scanCode, modifiers);
		}
		return false;
	}

	public final ModListEntry getEntryAtPos(double x, double y) {
		int int_5 = MathHelper.floor(y - (double) this.top) - this.headerHeight + (int) this.getScrollAmount() - 4;
		int index = int_5 / this.itemHeight;
		return x < (double) this.getScrollbarPositionX() && x >= (double) getRowLeft() && x <= (double) (getRowLeft() + getRowWidth()) && index >= 0 && int_5 >= 0 && index < this.getEntryCount() ? this.children().get(index) : null;
	}

	@Override
	protected int getScrollbarPositionX() {
		return this.width - 6;
	}

	@Override
	public int getRowWidth() {
		return this.width - (Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4)) > 0 ? 18 : 12);
	}

	@Override
	public int getRowLeft() {
		return left + 6;
	}

	public int getWidth() {
		return width;
	}

	public int getTop() {
		return this.top;
	}

	public ModsScreen getParent() {
		return parent;
	}

	@Override
	protected int getMaxPosition() {
		return super.getMaxPosition() + 4;
	}

	public int getDisplayedCountFor(Set<String> set) {
		int count = 0;
		for (ModListEntry c : children()) {
			if(c.useSMOD()){
				if (set.contains(c.getSMod().getId())) {
					count++;
				}
			} else {
			 	if (set.contains(c.getMod().getId())) {
					count++;
				}
			}
		}
		return count;
	}

	@Override
	public void close() {
		iconHandler.close();
	}

	public FabricIconHandler getFabricIconHandler() {
		return iconHandler;
	}
}
