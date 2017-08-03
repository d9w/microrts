/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.wilson;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import ai.core.AIWithComputationBudget;
import ai.core.InterruptibleAI;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;
/**
 *
 * @author d9w
 */
public class GRNAI extends AbstractionLayerAI {

    Random r = new Random();
    UnitTypeTable utt;
    UnitType resourceType;
    UnitType baseType;
    UnitType barracksType;
    UnitType workerType;
    UnitType lightType, heavyType, rangedType;
    UnitType[] mobileTypes;
    double[] unitFactors;
    int resourceMax;

    public GRNAI(UnitTypeTable a_utt) {
        this(a_utt, new double[]{1.0, 3.0, 2.0, 0.5, 5.0}, new AStarPathFinding());
    }

    public GRNAI(UnitTypeTable a_utt, double[] units, PathFinding a_pf) {
        super(a_pf);
        utt = a_utt;
        resourceType = utt.getUnitType("Resource");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        workerType = utt.getUnitType("Worker");
        lightType = utt.getUnitType("Light");
        heavyType = utt.getUnitType("Heavy");
        rangedType = utt.getUnitType("Ranged");
        mobileTypes = new UnitType[]{workerType, lightType, heavyType, rangedType};
        unitFactors = units;
    }

    public void reset() {
        super.reset();
    }

    public AI clone() {
        return new GRNAI(utt);
    }

    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        Player p2 = gs.getPlayer(1-player);

        // get global inputs
        int selfResources = p.getResources();
        int enemyResources = p2.getResources();
        int selfUnits = 0;
        int enemyUnits = 0;
        int barracks = 0;
        int bases = 0;

        int[] unitCounts = {0, 0, 0, 0};
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() == p.getID()) {
                if (u.getType() == baseType) bases++;
                else if (u.getType() == barracksType) barracks++;
                else if (u.getType() == workerType) unitCounts[0]++;
                else if (u.getType() == lightType) unitCounts[1]++;
                else if (u.getType() == heavyType) unitCounts[2]++;
                else if (u.getType() == rangedType) unitCounts[3]++;
                selfUnits += 1;
            } else if (u.getPlayer() == p2.getID()) {
                enemyUnits += 1;
            }
        }

        // decide which units to make
        boolean[] makeUnits = new boolean[4];
        if (bases > 0) {
            double[] weightedUnitCounts = {0.0, 0.0, 0.0, 0.0, 0.0};
            for (int i=0; i<4; i++) {
                weightedUnitCounts[i] = (double)unitCounts[i]*unitFactors[i];
            }
            weightedUnitCounts[4] = unitFactors[4] * selfResources; // idle action

            if (barracks > 0) {
                int minType = -1;
                double minCount = weightedUnitCounts[0];
                for (int i=0; i<weightedUnitCounts.length; i++) {
                    if (weightedUnitCounts[i] < minCount) {
                        minType = i;
                        minCount = weightedUnitCounts[i];
                    }
                }

                if (minType == -1) {
                    makeUnits[0] = true;
                } else if (minType != 4) {
                    makeUnits[minType] = true;
                }
            } else {
                if (weightedUnitCounts[0] < weightedUnitCounts[4]) {
                    makeUnits[0] = true;
                }
            }
            // System.out.println("Unit weights: " + Arrays.toString(weightedUnitCounts));
        }

        boolean makeUnit = false;
        for (int i=0; i<makeUnits.length; i++) {
            if (makeUnits[i]) makeUnit = true;
        }

        // train units with a base or barracks and call worker and melee unit actions
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() == player && gs.getActionAssignment(u) == null) {
                if (u.getType() == baseType) {
                    if (makeUnit && makeUnits[0] && selfResources >= workerType.cost) {
                        train(u, workerType);
                        makeUnit = false;
                    }
                } else if (u.getType() == barracksType) {
                    if (makeUnit) {
                        for (int i=1; i<makeUnits.length; i++) {
                            if (makeUnits[i]) {
                                train(u, mobileTypes[i]);
                                makeUnit = false;
                            }
                        }
                    }
                } else {
                    meleeUnitBehavior(u, p, pgs, selfResources, enemyResources,
                                      selfUnits, enemyUnits);
                }
            }
        }

        return translateActions(player, gs);
    }

    // GRN goes here
    public double[] processGRN(double[] inputs) {
        double[] outputs = new double[9];
        for (int i=0; i<outputs.length; i++) {
            outputs[i] = r.nextDouble();
        }
        return outputs;
    }

    public void meleeUnitBehavior(Unit u, Player p, PhysicalGameState pgs,
                                  int selfResources, int enemyResources,
                                  int selfUnits, int enemyUnits) {

        // collect distance inputs
        int maxDist = pgs.getWidth() + pgs.getHeight();
        Unit closestResource = null; int closestResourceDist = maxDist;
        Unit selfUnit = null; int selfUnitDist = maxDist;
        Unit selfBase = null; int selfBaseDist = maxDist;
        Unit selfBarracks = null; int selfBarracksDist = maxDist;
        Unit enemyUnit = null; int enemyUnitDist = maxDist;
        Unit enemyBase = null; int enemyBaseDist = maxDist;
        Unit enemyBarracks = null; int enemyBarracksDist = maxDist;

        for (Unit u2 : pgs.getUnits()) {
            if (u2 == u) {
                continue;
            }
            int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
            if (u2.getType().isResource) {
                if (d < closestResourceDist) closestResource = u2; closestResourceDist = d;
            } else if (u2.getType() == baseType) {
                if (u2.getPlayer() == p.getID()) {
                    if (d < selfBaseDist) selfBase = u2; selfBaseDist = d;
                } else {
                    if (d < enemyBaseDist) enemyBase = u2; enemyBaseDist = d;
                }
            } else if (u2.getType() == barracksType) {
                if (u2.getPlayer() == p.getID()) {
                    if (d < selfBarracksDist) selfBarracks = u2; selfBarracksDist = d;
                } else {
                    if (d < enemyBarracksDist) enemyBarracks = u2; enemyBarracksDist = d;
                }
            } else {
                if (u2.getPlayer() == p.getID()) {
                    if (d < selfUnitDist) selfUnit = u2; selfUnitDist = d;
                } else {
                    if (d < enemyUnitDist) enemyUnit = u2; enemyUnitDist = d;
                }
            }
        }

        int[] distances = {closestResourceDist, selfUnitDist, selfBaseDist,
                           selfBarracksDist, enemyUnitDist, enemyBaseDist,
                           enemyBarracksDist};

        double[] inputs = new double[16];
        for (int i=0; i<distances.length; i++) {
            inputs[i] = 1.0 - (distances[i]/(double)maxDist);
        }
        inputs[7] = 1.0 - (u.getHitPoints()/(double)u.getMaxHitPoints());
        if (u.getType() == workerType) inputs[8] = 1.0;
        else if (u.getType() == lightType) inputs[9] = 1.0;
        else if (u.getType() == heavyType) inputs[10] = 1.0;
        else if (u.getType() == rangedType) inputs[11] = 1.0;
        inputs[12] = 1.0 - Math.min(1.0, selfResources/50.0);
        inputs[13] = 1.0 - Math.min(1.0, enemyResources/50.0);
        inputs[14] = 1.0 - Math.min(1.0, selfUnits/50.0);
        inputs[15] = 1.0 - Math.min(1.0, enemyUnits/50.0);

        // System.out.println("inputs: " + Arrays.toString(inputs));
        double[] outputs = processGRN(inputs);

        boolean[] validOutputs = new boolean[outputs.length];
        for (int i=0; i<outputs.length; i++) validOutputs[i] = true;
        if (closestResource == null) validOutputs[0] = false;
        if (selfBase == null) {
            validOutputs[1] = false;
            if (u.getType() == workerType) validOutputs[0] = false;
        }
        if (selfBarracks == null) validOutputs[2] = false;
        if (enemyUnit == null) validOutputs[3] = false;
        if (enemyBase == null) validOutputs[4] = false;
        if (enemyBarracks == null) validOutputs[5] = false;
        if (u.getType() == workerType) validOutputs[7] = false; validOutputs[8] = false;

        int maxact = -1;
        double maxout = 0.0;
        for (int i=0; i<outputs.length; i++) {
            if (validOutputs[i] && outputs[i] >= maxout) {
                maxout = outputs[i];
                maxact = i;
            }
        }

        if (maxact == 0) {
            if (u.getType() == workerType) {
                    harvest(u, closestResource, selfBase);
            } else {
                move(u, closestResource.getX(), closestResource.getY());
            }
        } else if (maxact == 1) move(u, selfBase.getX(), selfBase.getY());
        else if (maxact == 2) move(u, selfBarracks.getX(), selfBarracks.getY());
        else if (maxact == 3) attack(u, enemyUnit);
        else if (maxact == 4) attack(u, enemyBase);
        else if (maxact == 5) attack(u, enemyBarracks);
        else if (maxact == 7) train(u, baseType);
        else if (maxact == 8) train(u, barracksType);
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();

        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

        return parameters;
    }
}
