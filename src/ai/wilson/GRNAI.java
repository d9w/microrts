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
    double RESOURCE_CAP = 100.0;

    public GRNAI(UnitTypeTable a_utt) {
        this(a_utt, new double[]{1.0, 1.0, 1.0, 1.0, 5.0}, new AStarPathFinding());
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
        double selfResources = p.getResources();
        double enemyResources = p2.getResources();
        double selfUnits = 0.0;
        double enemyUnits = 0.0;
        int barracks = 0;
        int bases = 0;

        System.out.println("Resources: " + selfResources + " " + enemyResources);

        int[] unitCounts = {0, 0, 0, 0};
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() == p.getID()) {
                if (u.getType() == baseType) bases++;
                else if (u.getType() == barracksType) barracks++;
                else if (u.getType() == workerType) unitCounts[0]++;
                else if (u.getType() == lightType) unitCounts[1]++;
                else if (u.getType() == heavyType) unitCounts[2]++;
                else if (u.getType() == rangedType) unitCounts[3]++;
                selfUnits += 1.0;
            } else if (u.getPlayer() == p2.getID()) {
                enemyUnits += 1.0;
            }
        }

        // decide which units to make
        boolean[] makeUnits = {false, false, false, false};
        if (bases > 0) {
            if (barracks > 0) {
                System.out.println("Unit counts: " + Arrays.toString(unitCounts));

                double[] weightedUnitCounts = {0.0, 0.0, 0.0, 0.0, 0.0};

                for (int i=0; i<4; i++) {
                    weightedUnitCounts[i] = (double)unitCounts[i]*unitFactors[i];
                }

                weightedUnitCounts[4] = unitFactors[4]; // idle action

                int minType = -1;
                double minCount = weightedUnitCounts[0];
                for (int i=0; i<4; i++) {
                    if (weightedUnitCounts[i] < minCount) {
                        minType = i;
                        minCount = weightedUnitCounts[i];
                    }
                }

                System.out.println("Weighted unit counts: " + Arrays.toString(weightedUnitCounts));

                if (minType == -1) {
                    if (selfResources >= workerType.cost) {
                        makeUnits[0] = true;
                    }
                } else if (minType < 4) {
                    if (selfResources >= mobileTypes[minType].cost) {
                        makeUnits[minType] = true;
                    }
                }
            } else {
                // if (unitCounts[0] * unitFactors[0] < unitFactors[4]) {
                if (selfResources >= workerType.cost) {
                    makeUnits[0] = true;
                }
                // }
            }
        }

        System.out.println("Make units: " + Arrays.toString(makeUnits));

        // train units with a base or barracks and call worker and melee unit actions
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() == player && gs.getActionAssignment(u) == null) {
                if (u.getType() == baseType) {
                    if (makeUnits[0]) {
                        train(u, workerType);
                        makeUnits[0] = false;
                    }
                } else if (u.getType() == barracksType) {
                    for (int i=1; i<4; i++) {
                        if (makeUnits[i]) {
                            train(u, mobileTypes[i]);
                            makeUnits[i] = false;
                        }
                    }
                } else if (u.getType() == workerType) {
                    workerBehavior(u, p, pgs);
                } else {
                    meleeUnitBehavior(u, p, pgs);
                }
            }
        }

        return translateActions(player, gs);
    }

    public void meleeUnitBehavior(Unit u, Player p, PhysicalGameState pgs) {
        Unit closestEnemy = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy != null) {
            attack(u, closestEnemy);
        }
    }

    public void workerBehavior(Unit u, Player p, PhysicalGameState pgs) {
        Unit closestBase = null;
        Unit closestResource = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isResource) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestResource == null || d < closestDistance) {
                    closestResource = u2;
                    closestDistance = d;
                }
            }
        }
        closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestBase == null || d < closestDistance) {
                    closestBase = u2;
                    closestDistance = d;
                }
            }
        }
        Unit closestEnemy = null;
        int enemyDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < enemyDistance) {
                    closestEnemy = u2;
                    enemyDistance = d;
                }
            }
        }
        if (closestResource != null && closestBase != null) {
            if (closestEnemy != null) {
                if (closestDistance < enemyDistance) {
                    harvest(u, closestResource, closestBase);
                } else {
                    attack(u, closestEnemy);
                }
            } else {
                harvest(u, closestResource, closestBase);
            }
        } else if (closestEnemy != null) {
            attack(u, closestEnemy);
        }
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();

        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

        return parameters;
    }
}
