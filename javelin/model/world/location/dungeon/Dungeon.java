package javelin.model.world.location.dungeon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javelin.JavelinApp;
import javelin.controller.Point;
import javelin.controller.challenge.RewardCalculator;
import javelin.controller.exception.GaveUpException;
import javelin.controller.terrain.Terrain;
import javelin.controller.terrain.map.Maps;
import javelin.controller.terrain.map.tyrant.Caves;
import javelin.model.BattleMap;
import javelin.model.item.ItemSelection;
import javelin.model.item.Key;
import javelin.model.unit.Squad;
import javelin.model.world.Incursion;
import javelin.model.world.location.Location;
import javelin.view.screen.BattleScreen;
import javelin.view.screen.DungeonScreen;
import javelin.view.screen.WorldScreen;
import tyrant.mikera.engine.RPG;
import tyrant.mikera.engine.Thing;
import tyrant.mikera.tyrant.Game;
import tyrant.mikera.tyrant.Tile;

/**
 * A dungeon is an underground area of the world where the combats are harder
 * but have extra treasure laying around.
 * 
 * The in-game logic for dungeons is that they are a hideout of bandits or
 * similar, which is why they are sacrificeable by {@link Incursion}s and are
 * removed from the game after a {@link Squad} leaves one (in this case it's
 * assumed the bandits packed their stuff and left).
 * 
 * @author alex
 */
public class Dungeon extends Location {
	final class VerticalCorridor extends Crawler {
		int step;

		VerticalCorridor(Point start, Set<Point> used, int step) {
			super(start, used);
			this.step = step;
		}

		@Override
		public void step() {
			walker.y += step;
		}
	}

	final class HorizontalCorridor extends Crawler {
		int step;

		HorizontalCorridor(Point start, Set<Point> used, int step) {
			super(start, used);
			this.step = step;
		}

		@Override
		public void step() {
			walker.x += step;
		}
	}

	final class Room extends Crawler {
		final LinkedList<Point> room = new LinkedList<Point>();

		Room(Point start, Set<Point> used) {
			super(start, used);
		}

		@Override
		public void fill(int length, Set<Point> free) {
			for (int x = walker.x - length; x <= walker.x + length; x++) {
				for (int y = walker.y - length; y <= walker.y + length; y++) {
					room.add(new Point(x, y));
				}
			}
			super.fill(length, free);
		}

		@Override
		public void step() {
			Point p = room.pop();
			walker.x = p.x;
			walker.y = p.y;
		}
	}

	/** Screen dimensions. */
	public final static int SIZE = 7 + 1 + 7;
	final static float WALLRATIO = 1 / 4f;
	final static int WALKABLEAREA = Math.round(SIZE * SIZE * WALLRATIO);
	/**
	 * Assumes you will explore all the dungeon, being able to rest at the
	 * fountain mid-way for a total of 8 moderate encounters. This ignores the
	 * returning path towards the exit and getting to and from the dungeon
	 * itself, which means squads are supposed to be well equipped before diving
	 * in.
	 */
	public static final double ENCOUNTERRATIO = 8.0 / WALKABLEAREA;

	static final int MAXTRIES = 1000;
	final static int[] DELTAS = { -1, 0, 1 };

	/** Current {@link Dungeon} or <code>null</code> if not in one. */
	public static Dungeon active = null;
	/** All of this dungeon's {@link Feature}s. */
	public List<Feature> features = new ArrayList<Feature>();
	/** Set of points that are occupied by walls. */
	public HashSet<Point> walls = new HashSet<Point>();
	/** Explored squares in this dungeon. */
	public boolean[][] visible;
	/**
	 * Current {@link Squad} location.
	 * 
	 * TODO is this needed?
	 */
	public Point herolocation;

	transient BattleMap map = null;
	/** TODO remove from 2.0+ */
	transient public Thing hero;
	transient boolean generated = false;

	/** Constructor. */
	public Dungeon() {
		super("A dungeon");
		sacrificeable = true;
	}

	@Override
	public boolean interact() {
		super.interact();
		return true;
	}

	/** Create or recreate dungeon. */
	public void activate() {
		while (map == null) {
			map = new BattleMap(SIZE, SIZE);
			if (features.isEmpty()) {
				/* not loading a game */
				try {
					map();
				} catch (GaveUpException e) {
					map = null;
					features.clear();
					walls.clear();
				}
			}
		}
		regenerate();
		Game.instance().setHero(hero);
		JavelinApp.context = new DungeonScreen(map);
		active = this;
		BattleScreen.active = JavelinApp.context;
		Squad.active.updateavatar();
	}

	void map() throws GaveUpException {
		final Set<Point> free = new HashSet<Point>();
		final Set<Point> used = new HashSet<Point>();
		for (int i = 0; i < SIZE; i++) {
			used.add(new Point(i, 0));
			used.add(new Point(i, SIZE - 1));
			used.add(new Point(0, i));
			used.add(new Point(SIZE - 1, i));
		}
		Point root = new Point(RPG.r(2, SIZE - 3), RPG.r(2, SIZE - 3));
		free.add(root);
		used.add(root);
		build(new StairsUp("stairs up", root), used);
		for (int x = root.x - 1; x <= root.x + 1; x++) {
			for (int y = root.y - 1; y <= root.y + 1; y++) {
				Point p = new Point(x, y);
				free.add(p);
				used.add(p);
			}
		}
		herolocation =
				new Point(root.x + RPG.pick(DELTAS), root.y + RPG.pick(DELTAS));
		// while (herop == null || used.contains(herop) || !valid(herop.x)
		// || !valid(herop.y)) {
		// herop = ;
		// }
		// used.add(herop);
		// free.add(herop);
		carve(free, used);

		Point fountain = findspot(new ArrayList<Point>(free), used);
		build(new Fountain("fountain", fountain.x, fountain.y), used);
		free.add(fountain);
		createchests(free, used);
		visible = new boolean[SIZE][SIZE];
		for (int x = 0; x < Dungeon.SIZE; x++) {
			for (int y = 0; y < Dungeon.SIZE; y++) {
				visible[x][y] = false;
				Point p = new Point(x, y);
				if (!free.contains(p)) {
					walls.add(p);
				}
			}
		}
	}

	void carve(final Set<Point> free, Set<Point> used) {
		while (free.size() < WALKABLEAREA) {
			Point start = new Point(RPG.pick(new ArrayList<Point>(free)));
			int length = RPG.r(5, 7);
			int step = RPG.r(1, 2) == 1 ? -1 : +1;
			if (RPG.r(1, 2) == 1) {
				new HorizontalCorridor(start, used, step).fill(length, free);
			} else {
				new VerticalCorridor(start, used, step).fill(length, free);
			}
		}
	}

	static boolean valid(int coordinate) {
		return 0 <= coordinate && coordinate <= SIZE;
	}

	void createchests(Set<Point> free, final Set<Point> used) {
		int chests = RPG.r(3, 5);
		int pool = RewardCalculator.receivegold(Squad.active.members);
		for (int i = 0; i < chests; i++) {// equal trap/treasure find chance
			Trap t = new Trap(findspot(new ArrayList<Point>(free), used));
			build(t, used);
			pool += RewardCalculator.getgold(t.cr);
		}
		for (int i = chests; i > 0; i--) {
			int gold = i == 1 ? pool : pool / RPG.r(2, i);
			pool -= gold;
			Point p = findspot(new ArrayList<Point>(free), used);
			used.add(p);
			Chest t;
			if (i == chests) {
				t = new Chest("chest", p.x, p.y, 0, new ItemSelection());
				t.key = Key.generate();
			} else {
				t = RewardCalculator.createchest(gold, p.x, p.y);
			}
			build(t, used);
		}
	}

	void build(Feature feature, Set<Point> all) {
		features.add(feature);
		all.add(new Point(feature.x, feature.y));
	}

	Point findspot(ArrayList<Point> free, Set<Point> used) {
		Point p = null;
		while (p == null || used.contains(p)) {
			p = RPG.pick(free);
		}
		return p;
	}

	/** Exit and destroy this dungeon. */
	public void leave() {
		Dungeon.active = null;
		JavelinApp.context = new WorldScreen(JavelinApp.overviewmap);
		BattleScreen.active = JavelinApp.context;
		hero.remove();
	}

	void regenerate() {
		for (int x = 0; x < SIZE; x++) {
			for (int y = 0; y < SIZE; y++) {
				map.setTile(x, y, Tile.METALFLOOR);
			}
		}
		for (Point wall : walls) {
			map.setTile(wall.x, wall.y, Tile.STONEWALL);
		}
		hero = Squad.active.createvisual();
		hero.x = herolocation.x;
		hero.y = herolocation.y;
		map.addThing(hero, hero.x, hero.y);
		if (!generated) {
			for (Feature f : features) {
				f.generate(map);
			}
			generated = true;
		}
	}

	@Override
	protected Integer getel(int attackel) {
		return attackel - 3;
	}

	/**
	 * @return Possible battle maps.
	 * @see Terrain#getmaps()
	 */
	public static Maps getmaps() {
		Maps m = new Maps();
		m.add(new Caves());
		return m;
	}
}