package com.skellybuilds.servermodmenu.db;

import com.google.gson.*;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.lang.reflect.Type;

public class ModAdapter implements JsonDeserializer<SMod> {
	@Override
	public SMod deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
		//Gson gson = new Gson();
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(SMod.Links.class, new LinksAdapter());
		gsonBuilder.registerTypeAdapter(SMod.Contact.class, new ContactsAdapter());
		Gson gson2 = gsonBuilder.create();
		JsonObject jsonObject = json.getAsJsonObject();
		String version = jsonObject.get("version").getAsString();
		String id = jsonObject.get("id").getAsString();
		JsonObject metaJ = jsonObject.get("meta").getAsJsonObject();
		SMod.ModMeta meta = gson2.fromJson(metaJ, SMod.ModMeta.class);
		boolean isComponent = jsonObject.get("isComponent").getAsBoolean();
		return new SMod(version, id, meta, isComponent);
	}
}
