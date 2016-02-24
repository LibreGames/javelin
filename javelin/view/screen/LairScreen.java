package javelin.view.screen;

import java.awt.Image;

import javelin.Javelin;
import javelin.controller.action.world.WorldMove;
import javelin.controller.exception.battle.EndBattle;
import javelin.model.BattleMap;
import javelin.model.unit.Combatant;
import javelin.model.world.Lair;
import tyrant.mikera.tyrant.Game.Delay;
import tyrant.mikera.tyrant.QuestApp;

/**
 * @see Lair
 * @author alex
 */
public class LairScreen extends BattleScreen {
	public static final Image DUNGEONTEXTURE =
			QuestApp.getImage("/images/texture1.png");

	public LairScreen(final QuestApp q, final BattleMap mapp) {
		super(q, mapp, true);
		Javelin.settexture(DUNGEONTEXTURE);
	}

	/*
	 * TODO this type of thing should be on DungeonFight
	 */
	@Override
	public String dealreward() {
		Combatant capture = findmonster();
		if (capture == null) {
			return "You have killed the monster, cannot capture it!";
		}
		Javelin.captured = Javelin.recruit(capture.source.clone());
		Javelin.captured.hp = capture.hp;
		return "You have captured the " + capture + "!";
	}

	private Combatant findmonster() {
		return BattleMap.redTeam.size() == 1 ? BattleMap.redTeam.get(0) : null;
	}

	@Override
	public void afterend() {
		WorldMove.isleavingplace = true;
	}

	@Override
	public void checkEndBattle() {
		super.checkEndBattle();
		if (BattleMap.redTeam.size() >= 2) {
			return;
		}
		Combatant target = findmonster();
		if (target == null || target.hp <= target.maxhp / 2) {
			throw new EndBattle();
		}
	}

	@Override
	public void onEnd() {
		if (!BattleMap.blueTeam.isEmpty()) {
			messagepanel.clear();
			singleMessage(dealreward(), Delay.BLOCK);
			getUserInput();
			return;
		}
		super.onEnd();
	}
}