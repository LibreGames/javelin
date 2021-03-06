package javelin.model.world.location.dungeon.temple.features;

import javelin.Javelin;
import javelin.controller.Point;
import javelin.model.item.Item;
import javelin.model.item.relic.Relic;
import javelin.model.world.location.dungeon.Feature;
import javelin.model.world.location.dungeon.temple.Temple;

/**
 * Holds the {@link Relic} for this temple. If for any reason the {@link Relic}
 * is lost by the player it shall be available for pickup here again.
 * 
 * @author alex
 */
public class Altar extends Feature {
	Temple temple;

	/** Constructor. */
	public Altar(Point p, Temple temple) {
		super("altar", p.x, p.y, "dungeonaltar");
		this.temple = temple;
		remove = false;
	}

	@Override
	public boolean activate() {
		if (Item.getplayeritems().contains(temple.relic)) {
			Javelin.message("The " + temple.relic + " is not here anymore...",
					true);
		} else {
			Javelin.message(
					"This altar holds the epic " + temple.relic + "!\n"
							+ "If it is lost for any reason it shall be teleported back to safety here.",
					true);
			temple.relic.clone().grab();
		}
		return true;
	}
}
