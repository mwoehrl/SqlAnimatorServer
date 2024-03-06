package de.mwoehrl.sqlanimatorserver;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import de.mwoehrl.sqlanimator.MainClass;
import de.mwoehrl.sqlanimator.PreparedAnimation;
import de.mwoehrl.sqlanimator.execution.AbstractAction;
import de.mwoehrl.sqlanimator.execution.CellTransition;
import de.mwoehrl.sqlanimator.execution.ExecutionStep;
import de.mwoehrl.sqlanimator.renderer.AbsoluteCellPosition;
import de.mwoehrl.sqlanimator.renderer.RenderCanvas;

@SpringBootApplication
public class ServerMain {
	public static PreparedAnimation prep;
	public static HashMap<RenderCanvas, Integer> cellMap;
	public static HashMap<Integer, byte[]> cellCache;
	
	public static void main(String[] args) throws IOException {
		prepareAnimation();
		SpringApplication.run(ServerMain.class, args);
	}

	private static void prepareAnimation() throws IOException {
		prep = MainClass.prepareAnimation(1280,720,400);
		cellMap = new HashMap<RenderCanvas, Integer>();
		cellCache = new HashMap<Integer, byte[]>();
		int cellID=0;
		int size = 0;
		for (ExecutionStep step : prep.steps) {
			for (AbstractAction action : step.getActions()) {
				for (CellTransition transition : action.getTransitions()) {
					for (AbsoluteCellPosition position : transition.getCellPositions()) {
						if (!cellMap.containsKey(position.getCellCanvas())) {
							cellID++;
							cellMap.put(position.getCellCanvas(), Integer.valueOf(cellID));

							BufferedImage img = (BufferedImage)position.getCellCanvas().drawImage();
							ByteArrayOutputStream output = new ByteArrayOutputStream();
							ImageIO.write(img, "PNG", output);
							output.close();
							byte[] byteArray = output.toByteArray();
							size += byteArray.length;
							cellCache.put(Integer.valueOf(cellID), byteArray);
						}						
					}					
				}
			}
		}
		System.out.println("Count: " + cellID + " Size: " + size);
	}
}
