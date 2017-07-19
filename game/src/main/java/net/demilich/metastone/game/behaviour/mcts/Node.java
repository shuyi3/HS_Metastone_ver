package net.demilich.metastone.game.behaviour.mcts;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import net.demilich.metastone.game.entities.Actor;

class Node {

	private GameContext state;
	private List<GameAction> validTransitions;
	private final List<Node> children = new LinkedList<>();
	private final GameAction incomingAction;
	private int visits;
	private int score;
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
		int bestScore = Integer.MIN_VALUE;
		for (Node node : children) {
			if (node.getScore() > bestScore) {
				best = node.incomingAction;
				bestScore = node.getScore();
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

	public int getScore() {
		return score;
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
		if (this.validTransitions.size() == 0){
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

		int value = rollOut(current);
		for (Node node : visited) {
			node.updateStats(value);
		}
	}

	public int rollOut(Node node) {
		if (node.getState().gameDecided()) {
			GameContext state = node.getState();
			return state.getWinningPlayerId() == getPlayer() ? 1 : 0;
		}

		GameContext simulation = node.getState().clone();
		for (Player player : simulation.getPlayers()) {
			player.setBehaviour(new PlayRandomBehaviour());
		}

		int turnCount = 0;
		simulation.playTurn();

		return simulation.getWinningPlayerId() == getPlayer() ? 1 : 0;
	}

	private void updateStats(int value) {
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
