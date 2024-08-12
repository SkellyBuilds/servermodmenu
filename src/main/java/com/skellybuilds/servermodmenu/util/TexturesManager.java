package com.skellybuilds.servermodmenu.util;

import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.skellybuilds.servermodmenu.ModMenu;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;

public class TexturesManager {

	public static Identifier lFromBase64(String base64, String texturePath) {
		// Decode the Base64 string into a byte array
		byte[] imageBytes = Base64.getDecoder().decode(base64);

		try {
			// Create a NativeImage from the byte array
			NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(imageBytes));

			// Create a NativeImageBackedTexture from the NativeImage
			NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);

			// Create an Identifier for the texture
			Identifier textureIdentifier = new Identifier(ModMenu.MOD_ID, texturePath);

			// Register the texture with Minecraft's texture manager
			MinecraftClient.getInstance().getTextureManager().registerTexture(textureIdentifier, texture);

			// Return the texture Identifier
			return textureIdentifier;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
