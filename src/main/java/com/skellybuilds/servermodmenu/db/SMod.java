package com.skellybuilds.servermodmenu.db;


import net.fabricmc.loader.api.metadata.ModMetadata;

import java.util.HashMap;
import java.util.Map;

public class SMod {
	public String version;
	public String id;
	public String server;
	public ModMeta meta;
	public boolean isComponent = false;
	public SMod() {}

	public SMod(String version, String id, ModMeta meta, Boolean isComponent) {
		this.version = version;
		this.id = id;
		this.meta = meta;
		this.isComponent = isComponent;
	}

	public String getVersion() {
		return version;
	}

	public ModMeta getMeta() {
		return meta;
	}

	public boolean getIsComponent() {
		return isComponent;
	}

	public String getId() {
		return id;
	}

//	@Override
//	public String toString() {
//		return "Mod{version='" + version + "', age='" + id + "', meta='" + meta.toString() + "', isComponent='"+ isComponent +"'}";
//	}

	public static class ModMeta {
		public String icon;
		public String name;
		public String baseDesc;
		public String[] contributers;
		public String[] authors;
		public Contact contact;
		public Links links;
	}

	public static class Contact {
		private String homepage;
		private String sources;
		private String issues;

		public Contact(){

		}

		public Contact(String home, String source, String issue){
			homepage = home;
			sources = source;
			issues = issue;
		}

		// Getters and Setters
		public String getHomepage() {
			return homepage;
		}

		public void setHomepage(String homepage) {
			this.homepage = homepage;
		}

		public String getSources() {
			return sources;
		}

		public void setSources(String sources) {
			this.sources = sources;
		}

		public String getIssues() {
			return issues;
		}

		public void setIssues(String issues) {
			this.issues = issues;
		}



	}
	public static class Links {
		private Map<String, String> links = new HashMap<>();

		// Getter
		public Map<String, String> getLinks() {
			return links;
		}

		// Setter for the whole map
		public void setLinks(Map<String, String> links) {
			this.links = links;
		}

		// Method to set individual link
		public void setLink(String key, String value) {
			this.links.put("modmenu."+key, value);
		}

		// Method to get individual link
		public String getLink(String key) {
			return this.links.get(key);
		}
	}
}

