package javelin.view.screen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Panel;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javelin.Javelin;
import javelin.controller.Point;
import javelin.controller.action.Action;
import javelin.controller.action.ActionMapping;
import javelin.controller.action.Dig;
import javelin.controller.action.world.WorldAction;
import javelin.controller.ai.ChanceNode;
import javelin.controller.ai.ThreadManager;
import javelin.controller.exception.RepeatTurn;
import javelin.controller.exception.battle.EndBattle;
import javelin.controller.fight.Fight;
import javelin.controller.old.Game;
import javelin.controller.old.Game.Delay;
import javelin.controller.old.MessagePanel;
import javelin.controller.terrain.map.Map;
import javelin.model.state.BattleState;
import javelin.model.state.Square;
import javelin.model.unit.Combatant;
import javelin.view.StatusPanel;
import javelin.view.mappanel.MapPanel;
import javelin.view.mappanel.Mouse;
import javelin.view.mappanel.battle.BattlePanel;
import tyrant.mikera.tyrant.QuestApp;
import tyrant.mikera.tyrant.Screen;

/**
 * Screen context during battle.
 * 
 * TODO it has become a hierarchy that behaves how different types of
 * {@link Fight}s should behave. The ideal would be for all this type of
 * controller behavior to move to {@link Fight}. For example: {@link LairScreen}
 * .
 * 
 * TODO the 2.0 interface should absolutely not be redrawn every time, only when
 * an update is needed and even then the redraw should be on a tile-by-tile
 * basis, not the entire screen.
 * 
 * TODO many things this is actually handling should be moved to {@link Fight}
 * controllers instead.
 * 
 * TODO {@link BattleScreen} should not be the supertype for {@link WorldScreen}
 * and such. Extract a proper super type or move everything to a proper context
 * controller. Would probably be better to make this a unified ContextScreen and
 * delegate all differences to the context objects.
 * 
 * @author alex
 */
public class BattleScreen extends Screen {

	/** Blue team at the moment the {@link Fight} begins. */
	public static List<Combatant> originalblueteam;
	/** Red team at the moment the {@link Fight} begins. */
	public static List<Combatant> originalredteam;
	/**
	 * Active {@link BattleScreen} implementation.
	 * 
	 * @see WorldScreen
	 * @see DungeonScreen
	 */
	public static BattleScreen active;

	static Runnable callback = null;

	/** Visual representation of a {@link BattleState}. */
	public MapPanel mappanel;
	/** Text output component. */
	public MessagePanel messagepanel;
	/** Unit info component. */
	public StatusPanel statuspanel;
	/** Units that ran away from the fight. */
	public ArrayList<Combatant> fleeing = new ArrayList<Combatant>();
	/**
	 * Allows the human player to use up to .5AP of movement before ending turn.
	 */
	public float spentap = 0;

	boolean maprevealed = false;
	Combatant current = null;

	Combatant lastwascomputermove;
	boolean jointurns;

	/**
	 * @param texture
	 *            Background texture.
	 */
	public BattleScreen(Image texture) {
		super();
		Javelin.settexture(texture);
	}

	/**
	 * @param addsidebar
	 *            If <code>true</code> will add a {@link StatusPanel} to this
	 *            screen.
	 */
	public BattleScreen(boolean addsidebar) {
		BattleScreen.active = this;
		setForeground(Color.white);
		setBackground(Color.black);
		setLayout(new BorderLayout());
		messagepanel = new MessagePanel();
		add(messagepanel, "South");
		mappanel = getmappanel();
		mappanel.init();
		add(mappanel, "Center");
		final Panel cp = new Panel();
		cp.setLayout(new BorderLayout());
		add("East", cp);
		statuspanel = new StatusPanel();
		if (addsidebar) {
			cp.add("Center", statuspanel);
		}
		setFont(QuestApp.mainfont);
		Javelin.app.switchScreen(this);
		BattleScreen.active = this;
		Game.delayblock = false;
		mappanel.repaint();
	}

	/**
	 * @return Map panel implementation for this screen.
	 */
	protected MapPanel getmappanel() {
		return new BattlePanel(Fight.state);
	}

	/**
	 * this is the main game loop. it catches any exceptions for stability and
	 * lets the game continue <br>
	 * very important that endTurn() gets called after the player moves, this
	 * ensures that the rest of the map stays up to date <br>
	 */
	public void mainLoop() {
		mappanel.setVisible(false);
		Combatant hero = Fight.state.next;
		javelin.controller.Point t =
				new Point(hero.location[0], hero.location[1]);
		mappanel.setPosition(t.x, t.y);
		Game.redraw();
		mappanel.center(t.x, t.y, true);
		mappanel.setVisible(true);
		while (true) {
			turn();
		}
	}

	/** Routine for human interaction. */
	protected void turn() {
		try {
			for (Combatant c : Fight.state.getcombatants()) {
				c.refresh();
			}
			Fight.state.next();
			current = Fight.state.next;
			if (Fight.state.redTeam.contains(current) || current.automatic) {
				spentap = 0;
				lastwascomputermove = current;
				computermove();
			} else {
				humanmove();
				lastwascomputermove = null;
				jointurns = false;
			}
			updatescreen();
			checkblock();
		} catch (RepeatTurn e) {
			Game.messagepanel.clear();
			return;
		} finally {
			Javelin.app.fight.endturn();
			Javelin.app.fight.checkEndBattle();
		}
	}

	void humanmove() {
		// try {
		BattlePanel.current = current;
		centerscreen(current.location[0], current.location[1]);
		mappanel.refresh();
		Game.userinterface.waiting = true;
		final KeyEvent updatableUserAction = getUserInput();
		if (MapPanel.overlay != null) {
			MapPanel.overlay.clear();
		}
		if (updatableUserAction == null) {
			callback.run();
			callback = null;
		} else {
			perform(convertEventToAction(updatableUserAction),
					updatableUserAction.isShiftDown());
		}
		spendap(current, false);
	}

	void computermove() {
		if (jointurns) {
			jointurns = false;
		} else {
			BattlePanel.current = current;
			Game.messagepanel.clear();
			Game.message("Thinking...\n", Delay.NONE);
			messagepanel.repaint();
			updatescreen();
		}
		if (Javelin.DEBUG) {
			Action.outcome(ThreadManager.think(Fight.state), true);
		} else {
			try {
				Action.outcome(ThreadManager.think(Fight.state), true);
			} catch (final RuntimeException e) {
				Game.message("Fatal error: " + e.getMessage(), Delay.NONE);
				messagepanel.repaint();
				throw e;
			}
		}
	}

	/**
	 * Use this to break the input loop.
	 * 
	 * @param r
	 *            This will be run instead of an {@link Action} or
	 *            {@link WorldAction}.
	 * @see Mouse
	 */
	static public void perform(Runnable r) {
		callback = r;
		Game.userinterface.go(null);
	}

	/** Processes {@link Game#delayblock}. */
	public void checkblock() {
		if (Game.delayblock) {
			Game.delayblock = false;
			Game.getInput();
			messagepanel.clear();
		}
	}

	/** TODO */
	public void view(int x, int y) {
		if (Javelin.app.fight.period == Javelin.PERIODEVENING
				|| Javelin.app.fight.period == Javelin.PERIODNIGHT) {
			Fight.state.next.listen();
		} else if (!maprevealed) {
			for (javelin.view.mappanel.Tile[] ts : mappanel.tiles) {
				for (javelin.view.mappanel.Tile t : ts) {
					t.discovered = true;
				}
			}
			maprevealed = true;
		}
	}

	/** Like {@link #centerscreen(int, int, boolean)} but without forcing. */
	public void centerscreen(int x, int y) {
		mappanel.center(x, y, false);
	}

	/** Redraws screen. */
	protected void updatescreen() {
		Combatant current = Fight.state.clone(this.current);
		int x = current.location[0];
		int y = current.location[1];
		centerscreen(x, y);
		view(x, y);
		statuspanel.repaint();
		Game.redraw();
	}

	/**
	 * @param state
	 *            New state is {@link ChanceNode#n}.
	 * @param enableoverrun
	 *            If <code>true</code> may ignore {@link Delay#WAIT} and let the
	 *            next automaric unit think instead.
	 */
	public void setstate(final ChanceNode state, boolean enableoverrun) {
		BattlePanel.current = current;
		final BattleState s = (BattleState) state.n;
		Fight.state = s;
		if (lastwascomputermove == null) {
			Game.redraw();
		}
		Delay delay = state.delay;
		if (enableoverrun && delay == Delay.WAIT
				&& (s.redTeam.contains(s.next) || s.next.automatic)) {
			delay = Delay.NONE;
			jointurns = true;
		}
		messagepanel.clear();
		Game.message(state.action, delay);
		messagepanel.repaint();
		// updatescreen();
	}

	/**
	 * @return Gets action for this event
	 * @throws RepeatTurn
	 */
	public Action convertEventToAction(final KeyEvent keyEvent) {
		if (rejectEvent(keyEvent)) {
			throw new RepeatTurn();
		}
		return ActionMapping.SINGLETON.getaction(keyEvent);
	}

	/**
	 * @return User-input.
	 */
	public KeyEvent getUserInput() {
		// Game.instance().clearMessageList();
		return Game.getInput();
	}

	/**
	 * @param thing
	 *            Visual representation of current unit.
	 * @param action
	 *            What is being performed.
	 * @param isShiftDown
	 *            Ignored.
	 */
	void perform(final Action action, final boolean isShiftDown) {
		try {
			Combatant current = Fight.state.clone(this.current);
			if (current.burrowed && !action.allowwhileburrowed) {
				Dig.refuse();
			}
			if (!action.perform(current)) {
				throw new RepeatTurn();
			}
		} catch (EndBattle e) {
			throw e;
		} catch (Exception e) {
			// TODO throw on debug?
			if (!(e instanceof RepeatTurn)) {
				e.printStackTrace();
			}
			throw new RepeatTurn();
		}
	}

	/**
	 * Normalize {@link #spentap} with the canonical {@link Combatant} instance.
	 * 
	 * @param force
	 *            If <code>false</code> will check if an action has been
	 *            performed first by comparing actual {@link Combatant#ap}.
	 */
	public void spendap(Combatant combatant, boolean force) {
		for (Combatant c : Fight.state.getcombatants()) {
			if (c.id == combatant.id && (c.ap != combatant.ap || force)) {
				c.ap += spentap;
				break;
			}
		}
		spentap = 0;
	}

	/**
	 * TODO is needed?
	 * 
	 * BUG Fix for
	 * http://sourceforge.net/tracker/index.php?func=detail&aid=1088187
	 * &group_id=16696&atid=116696 Ignore Alt keypresses, we may need to add
	 * more of these for other platforms.
	 */
	protected boolean rejectEvent(final KeyEvent keyEvent) {
		return (keyEvent.getModifiers() | InputEvent.ALT_DOWN_MASK) > 0
				&& keyEvent.getKeyCode() == 18;
	}

	/**
	 * TODO with the {@link MapPanel} hierarchy now this is probably not needed
	 * anymore
	 */
	public Image gettile(int x, int y) {
		Map m = Javelin.app.fight.map;
		Square s = m.map[x][y];
		if (s.blocked) {
			return m.getblockedtile(x, y);
		}
		return m.floor;
	}

}