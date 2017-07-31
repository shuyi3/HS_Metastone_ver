package net.demilich.metastone.game.behaviour;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.behaviour.mcts.Utils;
import net.demilich.metastone.game.cards.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StochasticOptimizeMove extends Behaviour {

	private final static Logger logger = LoggerFactory.getLogger(StochasticOptimizeMove.class);

	private final IGameStateHeuristic heuristic;

	public StochasticOptimizeMove(IGameStateHeuristic heuristic) {
		this.heuristic = heuristic;
	}

	@Override
	public String getName() {
		return "Stochastic Best Move";
	}

	@Override
	public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
		List<Card> discardedCards = new ArrayList<Card>();
		for (Card card : cards) {
			if (card.getBaseManaCost() >= 4) {
				discardedCards.add(card);
			}
		}
		return discardedCards;
	}

	@Override
	public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
		if (validActions.size() == 1) {
			return validActions.get(0);
		}
//		GameAction bestAction = validActions.get(0);
//		double bestScore = Double.NEGATIVE_INFINITY;
		GameAction selectedAction = null;
//		logger.debug("Current game state has a score of {}", bestScore, hashCode());
//		System.out.println("Enumerate actions ===========================");
		double[] scoreArr = new double[validActions.size()];
		for (int i = 0; i < validActions.size(); i++) {
			GameAction gameAction = validActions.get(i);
			GameContext simulationResult = simulateAction(context.clone(), player, gameAction);
			double gameStateScore = heuristic.getScaledScore(simulationResult, player.getId());
			if (gameAction.getActionType() == ActionType.END_TURN){
				scoreArr[i] = 0.05;
			}else {
				scoreArr[i] = gameStateScore;
			}
			simulationResult.dispose();
		}
//		logger.debug("Performing best action: {}", bestAction);
		selectedAction = selectAction(validActions, scoreArr);
		return selectedAction;
	}

	private GameContext simulateAction(GameContext simulation, Player player, GameAction action) {
//		GameLogic.logger.debug("");
//		GameLogic.logger.debug("********SIMULATION starts********** " + simulation.getLogic().hashCode());
		simulation.getLogic().performGameAction(player.getId(), action);
//		GameLogic.logger.debug("********SIMULATION ends**********");
//		GameLogic.logger.debug("");
		return simulation;
	}

	public GameAction selectAction(List<GameAction> actionList, double[] valueArr){
		GameAction selectedAction = null;

		softmax(valueArr, 0.1);

//		for (int i = 0; i < actionList.size(); i++) {
//			GameAction gameAction = actionList.get(i);
//			System.out.println("Action: " + gameAction.toString());
//			System.out.println("Score: " + valueArr[i]);
//		}

		//binary search
		double randomNum = Utils.getRng().nextDouble();
		int idx = Arrays.binarySearch(valueArr, randomNum);
		if (idx < 0)
		{
			idx = ~idx;
		}

		selectedAction = actionList.get(idx);
//		System.out.println("Selected: " + selectedAction.toString());
//		System.out.println("Done actions ===========================");
		return selectedAction;
	}

	public static void softmax(double[] probArr, double temp)
	{
		if (probArr.length == 0) return;

		double cMax = Double.MIN_VALUE;
		for (double v : probArr)
		{
			cMax = Math.max(v, cMax);
		}

		for (int i = 0; i < probArr.length; i++)
		{
			probArr[i] = Math.exp((probArr[i] - cMax)/temp);
		}

		double expSum = 0.0;
		for (double v : probArr)
		{
			expSum += v;
		}

		for (int i = 0; i < probArr.length; i++)
		{
			if (i == probArr.length - 1)
			{
				probArr[i] = 1.0;
			}
			else if (i == 0)
			{
				probArr[i] /= expSum;
			}
			else
			{
				probArr[i] = probArr[i] / expSum + probArr[i - 1];
			}
		}
	}
}
