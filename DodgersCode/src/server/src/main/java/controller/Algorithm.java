package controller;

import model.*;

import java.util.*;

public class Algorithm {
    private Preference pref;
    private State state;
    private int annealingSteps = 0;
    private double lastObjFunVal = 0;
    private int lastTargetNumberOfClusters = -1;

    public Algorithm(Preference pref, State state) {
        this.pref = pref;
        this.state = state;
    }

    /**
     * This is just an example of how to run the algorithm
     */
    public Summary doJob() {
        state.reset();
        String gps = doGraphPartitioning();
        while(!"done".equals(gps)) {
            gps = doGraphPartitioning();
        }
        System.out.println("GRAPH PART DONE");
        System.out.println(state.getClusters());

        Summary s = doSimulatedAnnealing();
        while (s.getMove() != null) {
            s = doSimulatedAnnealing();
        }
        return s;
    }

    /**
     * Since the measure is relevant on the district level, the calculation will need
     * to get each measure value for each district
     *
     * @return the output of the objective function
     */
    private double calculateObjectiveFunction() {
        double objFunOutput = 0;
        double totalWeight = 0;
        for(District d : state.getDistrictSet()) {
            for(MeasureType m : MeasureType.values()) {
                objFunOutput += (((m.calculateMeasure(d, state) * pref.getWeight(m)))/ (state.getDistrictSet().size()));
                totalWeight += pref.getWeight(m);
            }
        }
        totalWeight /= state.getDistrictSet().size();
        objFunOutput /= totalWeight;
        return objFunOutput;
    }

    private double[] calculateTotalMeasuresScores() {
        double[] measureScores = new double[MeasureType.values().length];

        for(District d : state.getDistrictSet()) {
            for(int ii = 0; ii < MeasureType.values().length; ii++) {
                measureScores[ii] += MeasureType.values()[ii].calculateMeasure(d, state);
            }
        }
        for(int ii = 0; ii < measureScores.length; ii++) {
            measureScores[ii] = measureScores[ii]/(double)state.getDistrictSet().size();
        }
        return measureScores;
    }

    public String doGraphPartitioning() {
        String s;
        //you must reset the state so we dont have to make extra database calls
        System.out.println("\nclusters size: " + state.getClusters().size());
        System.out.println("target num dist: " + pref.getNumDistricts());
        if(state.getClusters().size() != pref.getNumDistricts()) {
            int targetNumClusters = (int)Math.ceil(state.getClusters().size() / 2.0);
            int targetPop = (int)Math.ceil(state.getPopulation() / targetNumClusters);
//            int minTargetPop = 0;   //TODO: load percentage to ignore from config file

            ((List<Cluster>) state.getClusters()).sort(Comparator.comparingInt(Cluster::getPopulation));

            if(targetNumClusters == lastTargetNumberOfClusters) {
                Cluster combineC = ((List<Cluster>) state.getClusters()).get(state.getClusters().size()-1);
                Iterator<Edge> eiter = combineC.getEdges().iterator();
                final Random r = new Random();
                int index = r.nextInt(combineC.getEdges().size());
                for(int i = 0; i < index; i++) {
                    eiter.next();
                }
                Edge toEdge = eiter.next();
                Cluster c = state.combinePair(combineC, (Cluster)toEdge.getNeighbor(combineC));
                state.getClusters().add(c);
                return state.getClusters().toString();
            } else {
                lastTargetNumberOfClusters = targetNumClusters;
            }


            Collection<Cluster> mergedClusters = new LinkedList<>();
            int steps = 0;
//            System.out.println("trying to get candidate again");
            while(!state.getClusters().isEmpty() && steps < state.getClusters().size()) {
//                System.out.println(state.getClusters());
//                System.out.println("GETTING CLUSTER PAIR");
                final ClusterPair clusterPair = state.findCandidateClusterPair(targetPop);
//                System.out.println(clusterPair);
                if(clusterPair == null){
                    System.out.println("NO VALID CLUSTER PAIR");
//                    System.out.println(state.getClusters());
                    break;
                }
//                System.out.println("found cluster pair: " + clusterPair);
                Cluster c = state.combinePair(clusterPair.getC1(), clusterPair.getC2());
                mergedClusters.add(c);
                if(mergedClusters.size() + state.getClusters().size() == pref.getNumDistricts()){
                    break;
                }
                steps++;
            }
//            System.out.println("steps:" + steps);
//            System.out.println("merged this turn: " + mergedClusters.size());
            (state.getClusters()).addAll(mergedClusters);
            System.out.println("total clusters: " + state.getClusters().size());
            s = state.getClusters().toString();
        } else {
            s = "done";
        }
        return s;
    }

    public Summary doSimulatedAnnealing() {
        System.out.println("\n\tSTARTED SIM ANNEALING STEP");
        if(state.getDistrictSet().size() == 0) {
            state.convertClustersToDistricts();
            lastObjFunVal = calculateObjectiveFunction();
            System.out.println("obj funct init value: " + lastObjFunVal);
            System.out.println("GENERATE DISTRICTS FROM SLUSTERS");
            System.out.println(state.getDistrictSet());
        }

        Move candidateMove = null;
        //anneal until the objective function output is acceptable or the max steps is reached
        System.out.println("Objective function value: " + lastObjFunVal);
        if(calculateObjectiveFunction() < Configuration.OBJECTIVE_FUNCTION_GOAL && annealingSteps < Configuration.MAX_ANNEALING_STEPS) {
            while(annealingSteps < Configuration.MAX_ANNEALING_STEPS && candidateMove == null) {

                candidateMove = state.findCandidateMove();
                if (candidateMove != null) {
//                System.out.println(candidateMove);

                    final int oldMajMinDistNum = state.numMaxMinDists();
                    state.doMove(candidateMove);
                    final int newMajMinDistNum = state.numMaxMinDists();

                    if (newMajMinDistNum < pref.getMinMajMinDistricts() && oldMajMinDistNum > newMajMinDistNum) {//if it is less than min and it went down
                        System.out.println("undoing move MIN DIST");
                        state.undoMove();
                        candidateMove = null;
                    } else if (newMajMinDistNum > pref.getMaxMajMinDistricts() && oldMajMinDistNum < newMajMinDistNum) {//if it is greater than max and it went up
                        System.out.println("undoing move MAX DIST");
                        state.undoMove();
                        candidateMove = null;
                    } else {
                        final double currObjFunVal = calculateObjectiveFunction();
//                        System.out.println("last:" + lastObjFunVal);
//                        System.out.println("curr:" + currObjFunVal);
                        if ((currObjFunVal - lastObjFunVal) > Configuration.OBJECTIVE_FUNCTION_MIN_CHANGE) {
                            lastObjFunVal = currObjFunVal;
                            System.out.println("VALID MOVE ACCEPTED");
                        } else {
//                            System.out.println("undoing move because OBJECTIVE FUNCTION" + annealingSteps);
                            state.undoMove();
                            candidateMove = null;
                        }
                    }

                } else {
//                    System.out.println("NO CANDIDATE MOVE FOUND... try again");
                }
                annealingSteps++;
                System.out.println(annealingSteps);
            }
        } else {
            System.out.println("SIM END CONDITION MET");
        }
//        System.out.println(annealingSteps);
        System.out.println("\tENDED SIM ANNEALING STEP");
        System.out.println(state.getDistrictSet());
        System.out.println(lastObjFunVal);
        int dist = state.numMaxMinDists();
        if(dist < pref.getMinMajMinDistricts()) dist = pref.getMinMajMinDistricts();
        else if(dist > pref.getMaxMajMinDistricts()) dist = pref.getMaxMajMinDistricts();

        return new Summary(lastObjFunVal,calculateTotalMeasuresScores(), candidateMove, dist);
    }
}
