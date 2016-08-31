package javelin.view.mappanel.world;

import java.awt.event.MouseEvent;

import javelin.controller.Point;
import javelin.controller.action.world.WorldMove;
import javelin.controller.fight.RandomEncounter;
import javelin.controller.old.Game;
import javelin.controller.old.Game.Delay;
import javelin.model.unit.Squad;
import javelin.model.world.WorldActor;
import javelin.model.world.location.Location;
import javelin.view.mappanel.MapPanel;
import javelin.view.mappanel.Mouse;
import javelin.view.mappanel.MoveOverlay;
import javelin.view.mappanel.battle.BattlePanel;
import javelin.view.screen.BattleScreen;
import javelin.view.screen.DungeonScreen;
import javelin.view.screen.WorldScreen;

/**
 * Handles mouse events for {@link WorldScreen}.
 * 
 * @author alex
 */
public class WorldMouse extends Mouse {
	static class Movement implements Runnable {
		private final MoveOverlay overlay;

		public Movement(MoveOverlay overlay) {
			this.overlay = overlay;
		}

		@Override
		public void run() {
			int i = -1;
			boolean interrupted = false;
			for (Point p : overlay.affected) {
				i += 1;
				if (!WorldMove.move(p.x, p.y, false)) {
					interrupted = true;
					break;
				}
			}
			RandomEncounter.encounter(overlay.path.steps.get(i).apcost);
			BattleScreen.active.mappanel.refresh();
			if (interrupted) {
				Point p = overlay.path.resetlocation();
				if (p != null) {
					overlay.reset();
					overlay.path.sourcex = p.x;
					overlay.path.sourcey = p.y;
					overlay.walk();
					BattlePanel.overlay = overlay;
				}
			}
		}
	}

	class Interact implements Runnable {
		private final WorldActor target;

		public Interact(WorldActor target) {
			this.target = target;
		}

		@Override
		public void run() {
			Squad s = target instanceof Squad ? (Squad) target : null;
			if (s != null) {
				s.join(Squad.active);
				return;
			}
			Location l = target instanceof Location ? (Location) target : null;
			if (l != null && l.allowentry && l.discard
					&& l.garrison.isEmpty()) {
				WorldMove.place(l.x, l.y);
			}
			target.interact();
		}
	}

	static final Runnable TOOFAR = new Runnable() {
		@Override
		public void run() {
			Game.messagepanel.clear();
			Game.message("Too far...", Delay.WAIT);
		}
	};

	static boolean processing = false;
	static Object LOCK = new Object();

	boolean showingdescription = false;
	long lastcall = -Integer.MIN_VALUE;

	/** Constructor. */
	public WorldMouse(MapPanel panel) {
		super(panel);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (overrideinput() || !Game.userinterface.waiting) {
			return;
		}
		final WorldTile t = (WorldTile) e.getSource();
		if (!t.discovered) {
			return;
		}
		if (e.getButton() == MouseEvent.BUTTON1) {
			final WorldActor target = WorldActor.get(t.x, t.y);
			if (target == Squad.active) {
				return;
			}
			if (target == null) {
				if (move()) {
					return;
				}
			} else {
				BattleScreen.perform(target.isadjacent(Squad.active)
						? new Interact(target) : TOOFAR);
				return;
			}
		}
		super.mouseClicked(e);
	}

	/**
	 * Handles movement for {@link WorldScreen} and {@link DungeonScreen}.
	 * 
	 * @return <code>true</code> if moved the current {@link Squad}.
	 */
	public static boolean move() {
		final MoveOverlay overlay = (MoveOverlay) BattlePanel.overlay;
		if (BattlePanel.overlay == null || overlay.path.steps.isEmpty()) {
			return false;
		}
		BattleScreen.perform(new Movement(overlay));
		return true;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (!Game.userinterface.waiting) {
			return;
		}
		if (MapPanel.overlay != null) {
			MapPanel.overlay.clear();
		}
		MoveOverlay.cancel();
		final WorldTile t = (WorldTile) e.getSource();
		if (!t.discovered) {
			return;
		}
		final WorldActor target = WorldActor.get(t.x, t.y);
		if (target == null) {
			if (showingdescription) {
				showingdescription = false;
				Game.messagepanel.clear();
				((WorldScreen) WorldScreen.active).updateplayerinformation();
				Game.messagepanel.getPanel().repaint();
			}
			MoveOverlay.schedule(new MoveOverlay(
					new WorldMover(new Point(Squad.active.x, Squad.active.y),
							new Point(t.x, t.y))));
		} else {
			Game.messagepanel.clear();
			Game.message(target.describe(), Delay.NONE);
			Game.messagepanel.getPanel().repaint();
			showingdescription = true;
		}
	}
}
