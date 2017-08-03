package evaluators;

import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.PathFinding;
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
    CompetitionMatch c;

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
            maps.add(PhysicalGameState.load("maps/BroodWar/(4)BloodBath.scmB.xml",utt));
        } catch (Exception e) {
            e.printStackTrace();
        }
        gameLengths = new int[]{3000, 4000, 5000, 6000, 8000};
        c = new CompetitionMatch();
        c.visualize = false;
    }

    @Override
    public double evaluate(GRNGenome aGenome) {
        double fitness = 0.0;
        GRNModel grn = buildGRNFromGenome(aGenome);
        System.out.println("DEBUG Unit weights: " + Arrays.toString(aGenome.getWeights()));
        try {
            fitness = CompetitionMatch.runMatches(new GRNAI(utt, new AStarPathFinding(),
                                                            aGenome.getWeights(), grn),
                                                  new WorkerRush(utt, new BFSPathFinding()),
                                                  maps, gameLengths, utt);
            System.out.println("DEBUG " + fitness);
        } catch (Exception e) {
            e.printStackTrace();
        }
        aGenome.setNewFitness(fitness);
        GRNGenomeEvaluator.numEvaluations++;
        return fitness;
    }
}
