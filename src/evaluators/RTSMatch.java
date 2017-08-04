package evaluators;

import ai.*;
import ai.core.AI;
import ai.abstraction.LightRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.wilson.GRNAI;
import evolver.GRNGenome;
import grn.GRNModel;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.LinkedList;
import tournaments.CompetitionMatch;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class RTSMatch extends GRNGenomeEvaluator {

    Random r;
    List<PhysicalGameState> maps;
    UnitTypeTable utt;
    int[] gameLengths;
    int[] sides;
    CompetitionMatch c;
    GRNModel best;
    double bestfit = 0.0;
    int opponent = 0;

    public RTSMatch() {
        name = "RTSMatch";
        numGRNInputs = 16;
        numGRNOutputs = 9;
        r = new Random();
        utt = new UnitTypeTable();
        maps = new LinkedList<PhysicalGameState>();
        try {
            maps.add(PhysicalGameState.load("maps/8x8/basesWorkers8x8A.xml",utt));
            maps.add(PhysicalGameState.load("maps/16x16/basesWorkers16x16A.xml",utt));
            maps.add(PhysicalGameState.load("maps/BWDistantResources32x32.xml",utt));
            // maps.add(PhysicalGameState.load("maps/BroodWar/(4)BloodBath.scmB.xml",utt));
        } catch (Exception e) {
            e.printStackTrace();
        }
        gameLengths = new int[]{3000, 4000, 6000};
        sides = new int[]{0, 1, 0};
        c = new CompetitionMatch();
        c.visualize = false;
    }

    @Override
    public double evaluate(GRNGenome aGenome) {
        double fitness = 0.0;
        GRNModel grn = buildGRNFromGenome(aGenome);
        AI player = new GRNAI(utt, new AStarPathFinding(), grn);
        AI opp = new RandomAI();
        if (opponent == 1) {
            opp = new RandomBiasedAI();
        } else if (opponent == 2) {
            opp = new LightRush(utt, new BFSPathFinding());
        } else if (opponent == 3) {
            opp = new WorkerRush(utt, new BFSPathFinding());
        } else if (opponent == 4) {
            opp = new NaiveMCTS(100, -1, 100, 1, 1.00f, 0.0f, 0.25f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true);
        } else if (opponent > 4) {
            opp = new GRNAI(utt, new AStarPathFinding(), best);
        }
        try {
            fitness = CompetitionMatch.runMatches(player, opp, maps, gameLengths, sides,utt);
            // fitness = r.nextDouble();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("eval " + opponent + " " + Arrays.toString(grn.weights)
                           + " " + grn.size() + " " + fitness);
        if (opponent < 4) {
            if (fitness > 0.75) {
                opponent += 1;
            }
        } else if (opponent == 4) {
            if (fitness > 0.75) {
                opponent += 1;
                best = grn.copy();
            }
        } else {
            if (fitness > bestfit) {
                bestfit = fitness;
                best = grn.copy();
            }
        }
        aGenome.setNewFitness(fitness);
        GRNGenomeEvaluator.numEvaluations++;
        return fitness;
    }
}
