/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.wilson;

import ai.abstraction.*;
import ai.abstraction.pathfinding.PathFinding;
import rts.GameState;
import rts.PhysicalGameState;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.units.Unit;

/**
 *
 * @author santi
 */
public class MoveAdjacent extends AbstractAction {

    int x,y;
    PathFinding pf;
    Unit sunit;

    
    public MoveAdjacent(Unit u, int a_x, int a_y, PathFinding a_pf) {
        super(u);
        sunit = u;
        x = a_x;
        y = a_y;
        pf = a_pf;
    }
    
    public boolean completed(GameState gs) {
        if (sunit.getX()==x && sunit.getY()==y) return true;
        return false;
    }

    public UnitAction execute(GameState gs, ResourceUsage ru) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        UnitAction move = pf.findPathToPositionInRange(sunit, x+y*pgs.getWidth(), 2, gs, ru);
//        System.out.println("AStarAttak returns: " + move);
        if (move!=null && gs.isUnitActionAllowed(sunit, move)) return move;
        return null;
    }
}
