package net.demilich.metastone.game.behaviour.mcts;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.behaviour.heuristic.WeightedHeuristic;

import java.util.Random;

/**
 * Created by bugdx123 on 2017/7/18.
 */
public class Utils {

    private final static int numSample = 3;
    private static Random rng = null;
    private static IGameStateHeuristic heuristic = null;
    public  final static int rolloutDepth = 5;

    public static Random getRng(){
        if (rng == null) rng = new Random();
        return rng;
    }

    public static double getScore(GameContext context, int player){
        double lb = -40;
        double ub = 40;
        double range = 80;
        if (heuristic == null){
            heuristic = new WeightedHeuristic();
        }
        double score = heuristic.getScore(context, player);
        if (score <= lb) return 0.0;
        if (score >= ub) return 1.0;
        return (score + ub)/range;
    }

}
