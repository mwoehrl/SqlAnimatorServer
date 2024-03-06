package de.mwoehrl.sqlanimatorserver.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import de.mwoehrl.sqlanimator.PreparedAnimation;
import de.mwoehrl.sqlanimator.execution.AbstractAction;
import de.mwoehrl.sqlanimator.execution.CellTransition;
import de.mwoehrl.sqlanimator.execution.MoveCellTransition;
import de.mwoehrl.sqlanimator.renderer.AbsoluteCellPosition;
import de.mwoehrl.sqlanimatorserver.ServerMain;
import de.mwoehrl.sqlanimatorserver.record.Animation;
import de.mwoehrl.sqlanimatorserver.record.CellPosition;
import de.mwoehrl.sqlanimatorserver.record.ExecutionAction;
import de.mwoehrl.sqlanimatorserver.record.ExecutionStep;
import de.mwoehrl.sqlanimatorserver.record.Transition;

@RestController
public class QueryCanvasController {
	@GetMapping(value = "/query", produces = MediaType.IMAGE_PNG_VALUE)
	public @ResponseBody byte[] getQuery() throws IOException {
		BufferedImage img = (BufferedImage) ServerMain.prep.queryCanvas.drawImage();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(img, "PNG", output);
		output.close();
		return output.toByteArray();
	}
	
	@GetMapping(value = "/canvas", produces = MediaType.IMAGE_PNG_VALUE)
	public @ResponseBody byte[] getCanvas(@RequestParam(value = "step", defaultValue = "0") String step, @RequestParam(value = "action", defaultValue = "0") String action) throws IOException {
		BufferedImage img = (BufferedImage) ServerMain.prep.steps[Integer.parseInt(step)].getActions()[Integer.parseInt(action)].getResultingCanvas().drawImage();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(img, "PNG", output);
		output.close();
		return output.toByteArray();
	}	
	
	@GetMapping(value = "/cell", produces = MediaType.IMAGE_PNG_VALUE)
	public @ResponseBody byte[] getCell(@RequestParam(value = "id", defaultValue = "0") String id) throws IOException {
		return ServerMain.cellCache.get(Integer.valueOf(id));
	}
	
	@GetMapping(value = "/animation")
	public @ResponseBody Animation getAnimation() {
		ExecutionStep[] executionSteps = new ExecutionStep[ServerMain.prep.steps.length]; 
		for (int i = 0; i < executionSteps.length; i++) {
			executionSteps[i] = getRecord(ServerMain.prep.steps[i], i);
		}
		return new Animation(1, executionSteps, ServerMain.prep.queryCanvas.getWidth());
	}

	private ExecutionStep getRecord(de.mwoehrl.sqlanimator.execution.ExecutionStep step, int stepIndex) {
		ExecutionAction[] actions = new ExecutionAction[step.getActions().length];
		for (int i = 0; i < actions.length; i++) {
			actions[i] = getRecord(step.getActions()[i],stepIndex, i);
		}
		return new ExecutionStep(step.getName(), actions);
	}

	private ExecutionAction getRecord(AbstractAction action, int stepIndex, int actionIndex) {
		Transition[] transitions = new Transition[action.getTransitions().length];
		for (int i = 0; i < transitions.length; i++) {
			transitions[i] = getRecord(action.getTransitions()[i]);
		}
		String prevCanvas = null;
		if(action.showPrevCanvas()) {
			int ps = stepIndex;
			int pa = actionIndex - 1;
			if (pa < 0) {
				ps--;
				pa = ServerMain.prep.steps[ps].getActions().length-1;
			}
			prevCanvas = "/canvas?step=" + ps + "&action=" + pa;
		}
		return new ExecutionAction(transitions, prevCanvas);
	}

	private Transition getRecord(CellTransition cellTransition) {
		CellPosition[] cellPositions = new CellPosition[cellTransition.getCellPositions().length];
		for (int i = 0; i < cellPositions.length; i++) {
			cellPositions[i] = getRecord(cellTransition.getCellPositions()[i]);
		}
		String transName = cellTransition.getClass().getSimpleName().replace("CellTransition", "");
		if (transName.equals("Move") && ((MoveCellTransition)cellTransition).sourceOnly()) {
			cellPositions[1] = new CellPosition(
					cellPositions[1].x(),
					cellPositions[1].y(),
					cellPositions[1].w(),
					cellPositions[1].h(),
					cellPositions[0].id());					
		}
		return new Transition(transName, cellPositions);
	}

	private CellPosition getRecord(AbsoluteCellPosition pos) {
		return new CellPosition((int)pos.getX(), (int)pos.getY(), (int)pos.getW(), (int)pos.getH(), ServerMain.cellMap.get(pos.getCellCanvas()).intValue());
	}
}
