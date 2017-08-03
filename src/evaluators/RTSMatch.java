package evaluators;

import evolver.GRNGenome;
import grn.GRNModel;
import java.util.Random;

public class RTSMatch extends GRNGenomeEvaluator {

    Random r;

    public RTSMatch() {
        name = "RTSMatch";

        numGRNInputs = 19;
        numGRNOutputs = 9;
        r = new Random();
    }

    @Override
    public double evaluate(GRNGenome aGenome) {
        double fitness = r.nextDouble();
        aGenome.setNewFitness(fitness);
        GRNGenomeEvaluator.numEvaluations++;
        return fitness;
    }
}
