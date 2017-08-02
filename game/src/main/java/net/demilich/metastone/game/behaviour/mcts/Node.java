package net.demilich.metastone.game.behaviour.mcts;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.StochasticOptimizeMove;
import net.demilich.metastone.game.behaviour.heuristic.WeightedHeuristic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class Node {

	private GameContext state;
	private List<GameAction> validTransitions;
	private final List<Node> children = new LinkedList<>();
	private final GameAction incomingAction;
	private int visits;
	private double score;
	private final int player;
	private int numSample = 3;

	public boolean isChanceNode() {
		return isChanceNode;
	}

	private boolean isChanceNode;

	public Node(GameAction incomingAction, int player) {
		this.incomingAction = incomingAction;
		this.player = player;
	}

	private boolean canFurtherExpanded() {
		return !validTransitions.isEmpty();
	}

	private Node expand() {
		GameAction action = validTransitions.remove(0);
		GameContext newState = state.clone();

		try {
			newState.getLogic().performGameAction(newState.getActivePlayer().getId(), action);
		} catch (Exception e) {
			System.err.println("Exception on action: " + action + " state decided: " + state.gameDecided());
			e.printStackTrace();
			throw e;
		}

		Node child = new Node(action, getPlayer());
		child.initState(newState, newState.getValidActions());
		children.add(child);
		return child;
	}

	private void sampleNextTurn()
	{
		List<GameContext> nextStates = new ArrayList<>();
		for (int i = 0; i < numSample; i++){
			GameContext newState = getState().clone();
			newState.endTurn();
			newState.startTurn();
			Node child = new Node(null, getPlayer());
			child.initState(newState, newState.getValidActions());
			this.children.add(child);
		}
	}

	public GameAction getBestAction() {
		GameAction best = null;
		double bestScore = Double.MIN_VALUE;
//		System.out.println("***************getBestAction***************");
		for (Node node : children) {
			if (node.getAvgScore() > bestScore) {
				best = node.incomingAction;
				bestScore = node.getAvgScore();
//				System.out.println("Action: " + node.incomingAction.toString());
//				System.out.println("Score: " +  node.getAvgScore());
			}
		}
		return best;
	}

	public List<Node> getChildren() {
		return children;
	}

	public int getPlayer() {
		return player;
	}

	public double getScore() {
		return score;
	}

	public double getAvgScore() {
		return score/visits;
	}

	public GameContext getState() {
		return state;
	}

	public int getVisits() {
		return visits;
	}

	public void initState(GameContext state, List<GameAction> validActions) {
		this.state = state.clone();
		this.validTransitions = new ArrayList<GameAction>(validActions);
		if (this.validTransitions.size() == 0 && !this.state.gameDecided()){
			this.isChanceNode = true;
			sampleNextTurn();
		}
	}

	private Node advanceChance()
	{
		int idx = Utils.getRng().nextInt(numSample);
		return children.get(idx);
	}

	public boolean isExpandable() {
		if (validTransitions.isEmpty()) {
			return false;
		}
		if (state.gameDecided()) {
			return false;
		}
		return getChildren().size() < validTransitions.size();
	}

	public boolean isLeaf() {
		return children == null || children.isEmpty();
	}

	private boolean isTerminal() {
		return state.gameDecided();
	}

	public void process(ITreePolicy treePolicy) {
		List<Node> visited = new LinkedList<Node>();
		Node current = this;
		visited.add(this);
		int treeDepth = 0;
		while (!current.isTerminal()) {
			if (current.isChanceNode()){
				current = current.advanceChance();
				visited.add(current);
			}else {
				if (current.canFurtherExpanded()) {
					current = current.expand();
					visited.add(current);
					break;
				} else {
					current = treePolicy.select(current);
					visited.add(current);
				}
			}
		}

		double value = rollOut(current);
		for (Node node : visited) {
			if (node.player == node.getState().getActivePlayerId()){
				node.updateStats(value);
			}else {
				node.updateStats(1.0 - value);
			}
		}
	}

	public double rollOut(Node node) {
		if (node.getState().gameDecided()) {
			GameContext state = node.getState();
			return state.getWinningPlayerId() == getPlayer() ? 1 : 0;
		}

		GameContext simulation = node.getState().clone();
		for (Player player : simulation.getPlayers()) {
			player.setBehaviour(new StochasticOptimizeMove(new WeightedHeuristic()));
		}

		int turnCount = 0;
		while (turnCount < Utils.rolloutDepth){
			simulation.playTurn();
			if (simulation.gameDecided()) return state.getWinningPlayerId() == getPlayer() ? 1 : 0;
			turnCount++;
		}
		double value = Utils.getScore(simulation, simulation.getActivePlayerId());
		if (simulation.getActivePlayerId() == player){
			return value;
		}else {
			return 1.0 - value;
		}
	}

	private void updateStats(double value) {
		visits++;
		score += value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("GameContext hashCode: " + state.hashCode() + "\n");
		builder.append("validTransitions:" + validTransitions.size());
		builder.append("children:" + children.size());
		return builder.toString();
	}

}
