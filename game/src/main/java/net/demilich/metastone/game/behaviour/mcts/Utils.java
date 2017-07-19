package net.demilich.metastone.game.behaviour.mcts;

import net.demilich.metastone.game.GameContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by bugdx123 on 2017/7/18.
 */
public class Utils {

    private final static int numSample = 3;
    private static Random rng = null;
    public static Random getRng(){
        if (rng == null) rng = new Random();
        return rng;
    }
}
