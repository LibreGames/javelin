package javelin.view.screen;

import java.awt.Image;
import java.util.ArrayList;
import java.util.HashSet;

import javelin.controller.Point;
import javelin.controller.exception.battle.StartBattle;
import javelin.controller.fight.Fight;
import javelin.controller.fight.RandomEncounter;
import javelin.controller.old.Game;
import javelin.model.BattleMap;
import javelin.model.unit.Squad;
import javelin.model.world.WorldActor;
import javelin.model.world.location.dungeon.Dungeon;
import javelin.model.world.location.dungeon.Feature;
import javelin.model.world.location.dungeon.Trap;
import javelin.view.Images;
import javelin.view.mappanel.MapPanel;
import javelin.view.mappanel.dungeon.DungeonPanel;
import tyrant.mikera.engine.Thing;

/**
 * Shows the inside of a {@link Dungeon}.
 * 
 * @author alex
 */
public class DungeonScreen extends WorldScreen {
	/** TODO hack */
	public static boolean dontenter = false;
	public static boolean stopmovesequence = false;

	/** Exhibits a dungeon. */
	public DungeonScreen(BattleMap map) {
		super(map);
		mappanel.settilesize(32);
		mappanel.setdiscovered(new HashSet<Point>() {
			@Override
			public boolean add(Point e) {
				return false;
			};

			@Override
			public boolean contains(Object o) {
				return false;
			};
		});
	}

	@Override
	public boolean explore(float hoursellapsed, boolean encounter) {
		try {
			if (encounter) {
				RandomEncounter.encounter(Dungeon.ENCOUNTERRATIO);
			}
		} catch (StartBattle e) {
			map.removeThing(Game.hero());
			throw e;
		}
		return !Dungeon.active.hazard();
	}

	@Override
	public boolean react(WorldActor actor, int x, int y) {
		int searchroll = Squad.active.search();
		for (Feature f : new ArrayList<Feature>(Dungeon.active.features)) {
			if (f.x == x && f.y == y) {
				boolean activated = f.activate();
				if (activated && f.remove) {
					f.remove();
					DungeonScreen.dontenter = !f.enter;
					DungeonScreen.stopmovesequence = f.stop;
				}
				// if (!activated) {
				// DungeonScreen.dontmove = true;
				// }
				return true;
			}
			if (x - 1 <= f.x && f.x <= x + 1 && y - 1 <= f.y && f.y <= y + 1) {
				Trap t = f instanceof Trap ? (Trap) f : null;
				if (t != null && !t.draw) {
					if (searchroll >= t.searchdc) {
						t.discover();
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean allowmove(int x, int y) {
		return !Dungeon.active.walls
				.contains(new javelin.controller.Point(x, y));
	}

	@Override
	public void updatelocation(int x, int y) {
		// don't
	}

	@Override
	public void view(Thing h) {
		for (int x = -1; x <= +1; x++) {
			for (int y = -1; y <= +1; y++) {
				try {
					Dungeon.active.setvisible(h.x + x, h.y + y);
				} catch (ArrayIndexOutOfBoundsException e) {
					continue;
				}
			}
		}
		map.makeAllInvisible();
		for (int x = 0; x < Dungeon.SIZE; x++) {
			for (int y = 0; y < Dungeon.SIZE; y++) {
				if (Dungeon.active.visible[x][y]) {
					map.setVisible(x, y);
				}
			}
		}
	}

	@Override
	public Thing gethero() {
		Squad.active.updateavatar();
		Game.hero().combatant = Squad.active.visual.combatant;
		return Game.hero();
	}

	@Override
	public boolean scale() {
		return false;
	}

	@Override
	public Image gettile(int x, int y) {
		return Images.getImage(Dungeon.active.walls.contains(new Point(x, y))
				? Dungeon.active.wall : Dungeon.active.floor);
	}

	@Override
	public Fight encounter() {
		return Dungeon.active.encounter();
	}

	@Override
	protected MapPanel getmappanel() {
		return new DungeonPanel();
	}

	@Override
	protected void humanTurn() {
		super.humanTurn();
		if (Dungeon.active != null) {
			Dungeon.active.herolocation =
					new Point(Game.hero().x, Game.hero().y);
			// mappanel.refresh();
		}
	}
}
