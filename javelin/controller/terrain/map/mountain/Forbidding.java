package javelin.controller.terrain.map.mountain;

import javelin.controller.terrain.map.DndMap;
import javelin.view.Images;

/**
 * TODO should be .7 walls but then we need to make sure there are paths between
 * different areas of the map
 * 
 * @see DndMap
 */
public class Forbidding extends DndMap {
	public Forbidding() {
		super(.4, 0, 0);
		floor = Images.getImage("terrainplains");
	}
}
