package com.skellybuilds.servermodmenu.gui.widget.entries;

import com.skellybuilds.servermodmenu.db.SMod;
import com.skellybuilds.servermodmenu.gui.widget.ModListWidget;
import com.skellybuilds.servermodmenu.util.mod.Mod;

public class IndependentEntry extends ModListEntry {

	public IndependentEntry(Mod mod, ModListWidget list) {
		super(mod, list);
	}

	public IndependentEntry(SMod mod, ModListWidget list) {
		super(mod, list);
	}

	public IndependentEntry(SMod mod, ModListWidget list, String name) {
		super(mod, list, name);
	}

	public IndependentEntry(SMod mod, ModListWidget list, String name, boolean isF, boolean ren) {
		super(mod, list, name, isF, ren);
	}
	public IndependentEntry(SMod mod, ModListWidget list, String name, boolean isF, boolean ren, boolean moreY) {
		super(mod, list, name, isF, ren, moreY);
	}
}
