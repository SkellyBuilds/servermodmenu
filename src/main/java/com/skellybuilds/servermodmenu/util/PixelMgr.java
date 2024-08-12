package com.skellybuilds.servermodmenu.util;

import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

import java.util.List;

public class PixelMgr {

	public static class Pixel {
		int x; int y; int r; int g; int b;
	}

	public static void  addPixel(BufferBuilder bufferBuilder, Matrix4f matrix, int x, int y, int r, int g, int b) {
		bufferBuilder.vertex(matrix, x, y, 0).color(r, g, b, 255).next();
		bufferBuilder.vertex(matrix, x + 1, y, 0).color(r, g, b, 255).next();
		bufferBuilder.vertex(matrix, x + 1, y + 1, 0).color(r, g, b, 255).next();
		bufferBuilder.vertex(matrix, x, y + 1, 0).color(r, g, b, 255).next();
	}

	public static void addPixels(BufferBuilder bufferBuilder, Matrix4f matrix, List<Pixel> pixels){
		for (Pixel pixel : pixels) {
			bufferBuilder.vertex(matrix, pixel.x, pixel.y, 0).color(pixel.r, pixel.g, pixel.b, 255).next();
			bufferBuilder.vertex(matrix, pixel.x + 1, pixel.y, 0).color(pixel.r, pixel.g, pixel.b, 255).next();
			bufferBuilder.vertex(matrix, pixel.x + 1, pixel.y + 1, 0).color(pixel.r, pixel.g, pixel.b, 255).next();
			bufferBuilder.vertex(matrix, pixel.x, pixel.y + 1, 0).color(pixel.r, pixel.g, pixel.b, 255).next();
		}
	}
}
