package javelin.view.mappanel.dungeon;

import java.awt.event.MouseEvent;

import javelin.controller.Point;
import javelin.controller.old.Game;
import javelin.model.world.location.dungeon.Dungeon;
import javelin.view.mappanel.MapPanel;
import javelin.view.mappanel.Mouse;
import javelin.view.mappanel.MoveOverlay;
import javelin.view.mappanel.Tile;
import javelin.view.mappanel.world.WorldMouse;

public class DungeonMouse extends Mouse {
	public DungeonMouse(MapPanel panel) {
		super(panel);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (overrideinput()) {
			return;
		}
		if (!Game.getUserinterface().waiting) {
			return;
		}
		final Tile t = (Tile) e.getSource();
		if (!t.discovered) {
			return;
		}
		if (e.getButton() == MouseEvent.BUTTON1 && WorldMouse.move()) {
			return;
		}
		super.mouseClicked(e);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (!Game.getUserinterface().waiting) {
			return;
		}
		if (MapPanel.overlay != null) {
			MapPanel.overlay.clear();
		}
		MoveOverlay.cancel();
		final Tile t = (Tile) e.getSource();
		if (!t.discovered) {
			return;
		}
		MoveOverlay
				.schedule(new MoveOverlay(new DungeonMover(
						new Point(Dungeon.active.herolocation.x,
								Dungeon.active.herolocation.y),
						new Point(t.x, t.y))));
	}
}