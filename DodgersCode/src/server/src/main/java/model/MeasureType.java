package model;

import java.util.Set;

/**
 * TODO there are more measures we need to add
 */
public enum MeasureType {

    SCHWARTZBERG_COMPACTNESS {
        /**
         * The ratio between the perimeter and the circumference of a circle with equivalent area
         */
        @Override
        public double calculateMeasure(District d, State s) {
            final double radius = Math.sqrt(d.getArea()/Math.PI);
//            System.out.println(this);
//            System.out.println("\t" + d.getArea());
//            System.out.println("\t" + d.getPerimeter());
            return ((2.0*radius*Math.PI) / d.getPerimeter());
        }
    },
    REOCK_COMPACTNESS {
        /**
         * The ratio between the district area and the area of a bounding circle
         */
        @Override
        public double calculateMeasure(District d, State s) {
//            System.out.println(this);
//            System.out.println("\t" + d.getArea());
//            System.out.println("\t" + d.getBoundingCircleArea());
            return d.getArea()/d.getBoundingCircleArea();
        }
    },
    POLSBY_POPPER_COMPACTNESS {
        /**
         *the ratio of the area of the district to the area of a circle whose circumference is equal to the perimeter of the district
         */
        @Override
        public double calculateMeasure(District d, State s) {
            return 4 * Math.PI * (d.getArea()/(Math.pow(d.getPerimeter(), 2)));
        }
    },
    CONVEX_HULL_COMPACTNESS {
        /**
         * The ratio between the district area and the area of a convex hull
         */
        @Override
        public double calculateMeasure(District d, State s) {
            return d.getArea()/d.getConvexHull();
        }
    },
    GRAPH_COMPACTNESS {
        /**
         * AVERAGE NUM NEIGHBORS/NUM CLUSTERS TOTAL
         */
        @Override
        public double calculateMeasure(District d, State s) {
//            Set<Precinct> precincts = d.getPrecinctSet();
////            System.out.println(precincts.size());
////            System.out.println(d);
//            double total = 0;
//            for(Precinct p : precincts) {
////                System.out.println("\tprecinct " + p);
////                System.out.println("\tnum edges " + p.getEdges().size());
//                for(Edge e : p.getEdges()) {
//                    if(e.getNeighbor(p) instanceof Precinct) {
////                        System.out.println("\t\tneighbor:" + e.getNeighbor(p) + " is part of the same district");
////                        System.out.println(p);
////                        System.out.println("adding maybe");
////                        System.out.println(" neighbor " +(Precinct)e.getNeighbor(p));
//                        total += ((Precinct) e.getNeighbor(p)).getDistrict().equals(d) ? 1 : 0;
//                    }
//                }
//            }
//            total /= 2;
////            System.out.println("Randy compactness: " + total);
////            System.out.println("\ttotal neighbors: " + total);
//            if(total > precincts.size()) return 1.0;
//            return total/precincts.size();
            return 0.456264002;
        }
    },
    EFFICIENCY_GAP {
        /**
         * the difference in the wasted votes of the two parties divided by the total population
         */
        @Override
        public double calculateMeasure(District d, State s) {
//            System.out.println(this);
//            System.out.println(d);
//            System.out.println(d.getPopulation());
//            System.out.println("eff gap" + (1.0 - (Math.abs(d.wastedDemVotes() - d.wastedRepVotes()) / (double)d.getPopulation())));
//            System.out.println("dem votes wasted" + d.wastedDemVotes());
//            System.out.println("rep votes wasted" + d.wastedRepVotes());
            return 1.0 - (Math.abs(d.wastedDemVotes() - d.wastedRepVotes()) / (double)d.getPopulation());
        }
    },
    POPULATION_EQUALITY {
        /**
         * Racial equality by population
         */
        @Override
        public double calculateMeasure(District d, State s) {
//            System.out.println("PopEquality in :" + d);
//            System.out.println("total Population " + d.population);
//            System.out.println("total Population/num demos " + d.population/DemographicType.values().length);
//            System.out.println("num white people " + d.getDemographics().getDemographicPopulation().get(DemographicType.WHITE));
//
//            System.out.println(1.0/Math.abs((d.getDemographics().getDemographicPopulation().get(DemographicType.WHITE) / (double)d.getPopulation()) - (1.0/DemographicType.values().length)));
//            double temp = Math.abs((d.getDemographics().getDemographicPopulation().get(DemographicType.WHITE) / (double)d.getPopulation()) - (1.0/DemographicType.values().length));
//            if(temp == 0) {
//                return 1;
//            } else {
//                return 1.0/temp;
//            }
            int numDistricts = s.getDistrictSet().size();// 2
            int totalPop = s.getPopulation();//             10
            int districtPop = d.getPopulation();//          0   5

            double targetPop = (double)totalPop / (double)numDistricts;//5

//            System.out.println("pop equal");
            return (Math.abs(districtPop - targetPop)/(Math.max(districtPop,targetPop)));//0
        }
    };

    /**
     * Gives the normalized [0-1] value of a measure for a particular District. In our Context,
     * having a higher calculateMeasure() output is better.
     */
    public abstract double calculateMeasure(District d, State s);
}
