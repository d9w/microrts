GRNFILE=example.grn

default:
	javac -cp src:lib/jdom.jar:lib/minimal-json-0.9.4.jar src/tournaments/CompetitionMatch.java
	javac -cp src:lib/jdom.jar:lib/minimal-json-0.9.4.jar src/evolver/Evolver.java

run:
	java -cp src:lib/jdom.jar:lib/minimal-json-0.9.4.jar evolver.Evolver maxNumGen 50 great true xover 4 speciationThreshold 0.15 saveAllPopulation false randomSeed 0 experienceName RTSMatch crossoverRate 0.25 mutationRate 0.75 duplicateInit 1 representativeMethod 1 speciesSizeAdjustingMethod 2 addMutationProbability 0.5 deleteMutationProbability 0.25 changeMutationProbability 0.25 addMutationMaxSize 250

single:
	java -cp src:lib/jdom.jar:lib/minimal-json-0.9.4.jar tournaments.CompetitionMatch $(GRNFILE)

clean:
	find src -type f -name "*.class" -delete
