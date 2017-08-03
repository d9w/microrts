package tournaments;

import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ContinuingAI;
import ai.core.InterruptibleAI;
import ai.RandomAI;
import ai.abstraction.LightRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.HeavyRush;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.wilson.GRNAI;
import gui.PhysicalGameStateJFrame;
import gui.PhysicalGameStatePanel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JFrame;
import rts.GameState;
import rts.PartiallyObservableGameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.Trace;
import rts.TraceEntry;
import rts.units.UnitTypeTable;
import util.XMLWriter;

/**
 *
 * @author d9w
 */
public class CompetitionMatch {

    public static boolean USE_CONTINUING_ON_INTERRUPTIBLE = true;
    public static boolean visualize = true;
    public static int TIMEOUT_CHECK_TOLERANCE = 20;

    public static int timeBudget = 100;
    public static int iterationsBudget = 100;
    public static boolean timeoutCheck = true;
    public static boolean runGC = true;

    public CompetitionMatch() {}

    public static double runMatches(AI player0, AI player1,
                                    List<PhysicalGameState> maps,
                                    int[] gameLengths,
                                    UnitTypeTable utt) throws Exception {
        double score = 0.0;
        for (int side = 0; side<2; side++) {
            for (int map_idx = 0; map_idx<maps.size(); map_idx++) {
                int gameLength = gameLengths[map_idx];
                PhysicalGameState pgs = maps.get(map_idx);
                AI ai1 = player0.clone();
                AI ai2 = player1.clone();
                if (side == 1) {
                    ai1 = player1.clone();
                    ai2 = player0.clone();
                }
                if (ai1 instanceof AIWithComputationBudget) {
                    ((AIWithComputationBudget) ai1).setTimeBudget(timeBudget);
                    ((AIWithComputationBudget) ai1).setIterationsBudget(iterationsBudget);
                }
                if (ai2 instanceof AIWithComputationBudget) {
                    ((AIWithComputationBudget) ai2).setTimeBudget(timeBudget);
                    ((AIWithComputationBudget) ai2).setIterationsBudget(iterationsBudget);
                }
                if (USE_CONTINUING_ON_INTERRUPTIBLE) {
                    if (ai1 instanceof InterruptibleAI) ai1 = new ContinuingAI(ai1);
                    if (ai2 instanceof InterruptibleAI) ai2 = new ContinuingAI(ai2);
                }

                ai1.reset();
                ai2.reset();

                GameState gs = new GameState(pgs.clone(),utt);
                PhysicalGameStateJFrame w = null;
                if (visualize) w = PhysicalGameStatePanel.newVisualizer(gs, 600, 600, false);
                boolean gameover = false;
                int crashed = -1;
                int timedout = -1;
                do {
                    PlayerAction pa1 = null;
                    PlayerAction pa2 = null;
                    long AI1start = 0, AI2start = 0, AI1end = 0, AI2end = 0;
                    if (runGC) System.gc();
                    try {
                        AI1start = System.currentTimeMillis();
                        pa1 = ai1.getAction(0, gs);
                        AI1end = System.currentTimeMillis();
                    }catch(Exception e) {
                        e.printStackTrace();
                        crashed = 0;
                        break;
                    }
                    if (runGC) System.gc();
                    try {
                        AI2start = System.currentTimeMillis();
                        pa2 = ai2.getAction(1, gs);
                        AI2end = System.currentTimeMillis();
                    }catch(Exception e) {
                        e.printStackTrace();
                        crashed = 1;
                        break;
                    }
                    if (timeoutCheck) {
                        long AI1time = AI1end - AI1start;
                        long AI2time = AI2end - AI2start;
                        if (AI1time>timeBudget + TIMEOUT_CHECK_TOLERANCE) {
                            timedout = 0;
                            break;
                        }
                        if (AI2time>timeBudget + TIMEOUT_CHECK_TOLERANCE) {
                            timedout = 1;
                            break;
                        }
                    }
                    gs.issueSafe(pa1);
                    gs.issueSafe(pa2);
                    gameover = gs.cycle();
                    if (w!=null) {
                        w.setStateCloning(gs);
                        w.repaint();
                        try {
                            Thread.sleep(1);    // give time to the window to repaint
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } while (!gameover &&
                            (gs.getTime() < gameLengths[map_idx]));

                if (w!=null) w.dispose();
                int winner = gs.winner();
                System.out.println("DEBUG " + map_idx + " " + ai1 + " " + ai2 + " " + winner);
                if (winner == -1) {
                    if (crashed != side && timedout != side) score += 0.5;
                } else if (winner == side) {
                    score += 1.0;
                }
            }
        }
        score /= (maps.size() * 2);
        return score;
    }

    public static void main(String args[]) throws Exception {
        UnitTypeTable utt = new UnitTypeTable();

        List<PhysicalGameState> maps = new LinkedList<PhysicalGameState>();
        maps.clear();
        maps.add(PhysicalGameState.load("maps/8x8/basesWorkers8x8A.xml",utt));
        maps.add(PhysicalGameState.load("maps/16x16/basesWorkers16x16A.xml",utt));
        maps.add(PhysicalGameState.load("maps/BWDistantResources32x32.xml",utt));
        maps.add(PhysicalGameState.load("maps/BroodWar/(4)BloodBath.scmB.xml",utt));

        int[] gameLengths = {3000, 4000, 5000, 6000, 8000};

        double score = runMatches(new HeavyRush(utt, new BFSPathFinding()),
                                  new HeavyRush(utt, new BFSPathFinding()),
                                  maps, gameLengths, utt);
        System.out.println(score);
    }
}
