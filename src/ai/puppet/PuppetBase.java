package ai.puppet;

import java.util.ArrayList;
import java.util.stream.Collectors;

import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import rts.GameState;
import rts.PlayerAction;
import util.Pair;
class MoveGenerator{
	ArrayList<ArrayList<Pair<Integer,Integer>>> choices;
	int current=0;
	int player;
	MoveGenerator(ArrayList<ArrayList<Pair<Integer,Integer>>> choices, int player){
		this.choices=choices;
		this.player=player;
	}
	boolean hasNext(){
		return current<choices.size();
	}
	void swapFront(Move bestMove){
		for(int i=0;i<choices.size();i++){
			if(choices.get(i).equals(bestMove.choices)){
				if(i==0){
					break;
				}
				choices.set(i, choices.get(0));
				choices.set(0, bestMove.choices);
				break;
			}
		}
	}
	Move next(){
		return new Move(choices.get(current++),player);
	}
	Move last(){
		return new Move(choices.get(current-1),player);
	}
	void ABcut(){
		current=choices.size();
	}
}
class Move{
	ArrayList<Pair<Integer,Integer>> choices;
	int player;

	public Move(ArrayList<Pair<Integer,Integer>> choices, int player){
		this.choices=choices;
		this.player=player;
	}
	public String toString(ConfigurableScript<?> script){
		return "choices: "+choices.stream().map(
				(Pair<Integer,Integer>  p)-> 
				new Pair<String,Integer>(script.choicePointValues[p.m_a].name(),p.m_b))
				.collect(Collectors.toList())+", player: "+player;
	}
	
}


public abstract class PuppetBase extends AI {


   int MAX_TIME = 100;//ms
   int MAX_ITERATIONS = -1;
   int PLAN_TIME;
   int PLAN_PLAYOUTS;
	int STEP_PLAYOUT_TIME;
	boolean PLAN;

	EvaluationFunction eval;
	ConfigurableScript<?> script;
	int lastSearchFrame;
	long lastSearchTime;
	int frameLeaves = 0, totalLeaves = 0;
	long frameStartTime=0,frameTime=0, totalTime = 0;
	
	PuppetBase(int max_time_per_frame, int max_playouts_per_frame, 
			int max_plan_time, int max_plan_playouts,int step_playout_time,
			ConfigurableScript<?> script, EvaluationFunction evaluation) {
		super();
		assert(max_time_per_frame>=0||max_playouts_per_frame>=0);
		MAX_TIME=max_time_per_frame;
		MAX_ITERATIONS=max_playouts_per_frame;
		PLAN_TIME=max_plan_time;
		PLAN_PLAYOUTS=max_plan_playouts;
		STEP_PLAYOUT_TIME=step_playout_time;

		if(max_plan_time>=0||max_plan_playouts>=0){
			PLAN=true;
		}else{
			PLAN=false;
		}
		this.script=script;
		eval=evaluation;
		lastSearchFrame=-1;
		lastSearchTime=-1;
	}

	@Override
	public void reset() {
		lastSearchFrame=-1;
		lastSearchTime=-1;
		script.reset();
		frameLeaves = 0; totalLeaves = 0;
		frameTime=0; totalTime = 0;
	}

	boolean planBudgetExpired(){
		return (PLAN_PLAYOUTS>=0 && totalLeaves>=PLAN_PLAYOUTS) 
				|| (PLAN_TIME>=0 && totalTime>PLAN_TIME);
	}
	boolean frameBudgetExpired(){
		return (MAX_ITERATIONS>=0 && frameLeaves>=MAX_ITERATIONS) 
				|| (MAX_TIME>=0 && frameTime>MAX_TIME);
	}
	abstract void restartSearch(GameState gs, int player);
	abstract void computeDuringOneGameFrame() throws Exception;
	abstract PlayerAction getBestActionSoFar() throws Exception;
	
	
	
	static void simulate(GameState gs, AI ai1, AI ai2, int player1, int player2, int time)
			throws Exception {
		assert(player1!=player2);
		int timeOut = gs.getTime() + time;
		boolean gameover = gs.gameover();
		while(!gameover && gs.getTime()<timeOut) {
			if (gs.isComplete()) {
				gameover = gs.cycle();
			} else {
				gs.issue(ai1.getAction(player1, gs));
				gs.issue(ai2.getAction(player2, gs));
			}
		}    
	}

}