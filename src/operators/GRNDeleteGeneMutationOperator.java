package operators;

import java.util.Random;

import evolver.GRNGenome;

public class GRNDeleteGeneMutationOperator extends GRNMutationOperator {
	public int grnMinSize=1;
	
	public GRNDeleteGeneMutationOperator() {
		grnMinSize=0;
		name="DeleteGeneMutation(minSize="+grnMinSize+", prob="+probability+")";
	}
	
	public GRNDeleteGeneMutationOperator(double prob) {
		grnMinSize=0;
		this.probability=prob;
		name="DeleteGeneMutation(minSize="+grnMinSize+", prob="+probability+")";
	}
	
	public GRNDeleteGeneMutationOperator(int nGRNMinSize) {
		grnMinSize=nGRNMinSize;
		name="DeleteGeneMutation(minSize="+grnMinSize+", prob="+probability+")";
	}
	
	public GRNDeleteGeneMutationOperator(int nGRNMinSize, double prob) {
		grnMinSize=nGRNMinSize;
		this.probability=prob;
		name="DeleteGeneMutation(minSize="+grnMinSize+", prob="+probability+")";
	}
	
	@Override
	public boolean mutateByModifying(GRNGenome aGenome, Random rng) {
		if (aGenome.size()>grnMinSize) {
			aGenome.parentBestFitness=aGenome.getLastFitness();
      for (int i=0; i<5; i++) {
          if (rng.nextDouble() > 0.5) {
              aGenome.setWeight(rng.nextDouble()*(aGenome.getWeightsMax()-aGenome.getWeightsMin())+aGenome.getWeightsMin(), i);
          }
      }
			aGenome.hasBeenModified();
			return aGenome.removeRandomlyRegulatoryGene(rng);
		} else {
			return false;
		}
	}

}
