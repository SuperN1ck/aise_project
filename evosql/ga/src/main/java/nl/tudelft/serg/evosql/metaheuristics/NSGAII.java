package nl.tudelft.serg.evosql.metaheuristics;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import genetic.QueryLevelData;

import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.fixture.Fixture;
import nl.tudelft.serg.evosql.fixture.FixtureMOO;
import nl.tudelft.serg.evosql.fixture.FixtureRow;
import nl.tudelft.serg.evosql.fixture.FixtureRowFactory;
import nl.tudelft.serg.evosql.fixture.FixtureTable;
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureFitnessComparator;
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureFitness;
import nl.tudelft.serg.evosql.sql.TableSchema;
import nl.tudelft.serg.evosql.util.IntegerComparator;
import nl.tudelft.serg.evosql.util.random.Randomness;

public class NSGAII // extends MOOApproach TODO: Nive to have
{
    protected static Logger log = LogManager.getLogger(NSGAII.class);

    protected static Randomness random = new Randomness();

    int populationSize = EvoSQLConfiguration.POPULATION_SIZE;

    protected List<String> pathsToTest;
    protected int amountPaths;

    protected Map<String, TableSchema> tableSchemas;

    protected String exceptions;

    /** Row Factory **/
    private FixtureRowFactory rowFactory = new FixtureRowFactory();

    public NSGAII(Map<String, TableSchema> pTableSchemas, List<String> pPathsToBeTested) {
        this.tableSchemas = pTableSchemas;
        this.pathsToTest = pPathsToBeTested;
        this.amountPaths = pPathsToBeTested.size();
        this.exceptions = "";
    }

    public Fixture execute() {
        long startTime = System.currentTimeMillis();
        log.info("Hello from NSGA-II");
        // TODO: Init populations
        List<FixtureMOO> parent_population = new ArrayList<FixtureMOO>();
        log.debug("Generating random initial population...");

        while (parent_population.size() < populationSize) {
            List<FixtureTable> tables = new ArrayList<FixtureTable>();
            for (TableSchema tableSchema : tableSchemas.values()) {
                tables.add(createFixtureTable(tableSchema, tables));
            }

            FixtureMOO fixture = new FixtureMOO(tables);
            log.debug("Fixture created: {}", fixture);
            parent_population.add(fixture);
        }
        log.info("Generated random population with {} fixtures", parent_population.size());

        /* NSGA-II Mainloop */

        while (System.currentTimeMillis() - startTime < EvoSQLConfiguration.MS_EXECUTION_TIME) {
            for (FixtureMOO f : parent_population) {
                try {
                    f.calculate_fitness_moo(pathsToTest, tableSchemas);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            // TODO: Main Loop should go here
            // TODO: Find out how to deal with all the different populations --> When copy,
            // When referencing?
            HashMap<Integer, List<FixtureMOO>> rankedFronts = nonDominatedSort(parent_population);
            if (true)
                break;

            int current_front_idx = 0;
            List<FixtureMOO> next_population = new ArrayList<FixtureMOO>(populationSize);
            while (next_population.size() + rankedFronts.get(current_front_idx).size() < populationSize) {
                List<FixtureMOO> current_front = rankedFronts.get(current_front_idx);
                crowdingDistanceAssignement(current_front);
                next_population.addAll(current_front);

                if (++current_front_idx == rankedFronts.size())
                    break;
            }
            /* If we have already added everything we don't need to cut the last front */
            if (current_front_idx != rankedFronts.size()) {
                List<FixtureMOO> last_front = rankedFronts.get(current_front_idx);
                // last_front.sort((FixtureMOO f1, FixtureMOO f2) ->
                // Double.compare(f2.getCrowdingDistance(), f1.getCrowdingDistance()));
                last_front.sort(Comparator.comparing(FixtureMOO::getCrowdingDistance).reversed());

                /*
                 * As long as we have space in the population and are not at the end of the list
                 * add subsequently everything to th next population
                 */
                for (int last_front_idx = 0; next_population.size() < populationSize
                        && last_front_idx < last_front.size(); ++last_front_idx)
                    next_population.add(last_front.get(last_front_idx++));
            }

            // TODO use selection, crossover and mutation to create a new population
            parent_population = next_population;

            // TODO break earlier when all targets are covered
        }

        return parent_population.get(0);
    }

    HashMap<Integer, List<FixtureMOO>> nonDominatedSort(List<FixtureMOO> population) {
        FixtureFitnessComparator ffc = new FixtureFitnessComparator();

        HashMap<FixtureMOO, List<FixtureMOO>> fitnessMap = new HashMap<>(); // S
        HashMap<Integer, List<FixtureMOO>> paretoFront = new HashMap<>(); // F
        HashMap<FixtureMOO, Integer> n = new HashMap<>(); // n

        // int[] rank = new int[population.size()];
        HashMap<FixtureMOO, Integer> rank = new HashMap<>();

        // log.info("population size = {}", population.size());

        for (int i = 0; i < population.size(); i++) {
            for (FixtureMOO f : population) {
                fitnessMap.put(f, new ArrayList<FixtureMOO>());
                n.put(f, new Integer(0));
            }
        }

        /****
         * get the dominating individual and set the number of dominants for each
         * individual
         ****/

        boolean frontCheck = false;
        for (int p_idx = 0; p_idx < population.size(); p_idx++) {
            FixtureMOO p = population.get(p_idx);

            for (int q_idx = 0; q_idx < population.size(); q_idx++) {
                FixtureMOO q = population.get(q_idx);

                boolean p_is_dominating_q = true;
                boolean q_is_dominating_p = true;

                for (int k = 0; k < amountPaths; k++) {

                    // log.info("value of {}th objective's {}th table fitness : {}", k, i,
                    // population.get(i).getFitnessMOO().get(k));
                    // 1: p is worse than q
                    // 0: p and q are equal
                    // -1: p is better than q
                    // --> >= : p Is better than q (truly dominating)
                    // --> > : p Is better or equal q (partly dominating)
                    int comparison_result = ffc.compare(p.getFitnessMOO().get(k), q.getFitnessMOO().get(k));
                    if (comparison_result >= 0) {
                        // log.info("{} individual is not dominant", q_idx);
                        // log.info("First one (p) is dominated by the second one (q)");
                        // log.info(p.getFitnessMOO().get(k));
                        // log.info(q.getFitnessMOO().get(k));
                        p_is_dominating_q = false;
                        // break;
                    } else if (comparison_result <= 0) {
                        q_is_dominating_p = false;
                    }

                }

                if (p_is_dominating_q) {
                    if (!fitnessMap.get(p).contains(q)) {
                        fitnessMap.get(p).add(q);
                    }
                } else if (q_is_dominating_p) {
                    // n[i]++;
                    int temp = n.get(p).intValue() + 1;
                    n.put(p, new Integer(temp));
                }
            }

        
            // /**** adding dominant individuals to the first pareto front ****/
            // log.info(n.get(p));
            // if (n.get(p) == 0) {
            // log.info("{}th individual is dominating others", p_idx);
            // // rank[i] = 0;
            // rank.put(p, new Integer(0));
            // FixtureMOO dominantIndividual = population.get(p_idx);

            // if (paretoFront.get(new Integer(0)) == null)
            // paretoFront.put(new Integer(0), new ArrayList<FixtureMOO>());
            // if (!paretoFront.get(new Integer(0)).contains(dominantIndividual))
            // paretoFront.get(new Integer(0)).add(dominantIndividual);
            // }

        }

        /**** adding indiviauls to the pareto front sequentially ****/
        for (FixtureMOO fixture : population){
            if (fixture.getFitnessMOO().get(0).getDistance() > 5000)
                log.info("n: {}", n.get(fixture));
        }

        log.info(n.values());

        int start = 0;
        int r = 0;

        // Create first front
        // if (!frontCheck) {
        //     int mostDominant = Collections.min(n.values(), new IntegerComparator()).intValue();
        //     // log.info(mostDominant); // In the ideal case this should be 0

        //     List<FixtureMOO> front = new ArrayList<FixtureMOO>();

        //     for (int i = 0; i < population.size(); i++) {
        //         if (n.get(population.get(i)).intValue() != mostDominant)
        //             continue;

        //         /* Found an individual with == mostDominant */
        //         rank.put(population.get(i), new Integer(current_pareto_front_idx));
        //         FixtureMOO dominantIndividual = population.get(i);

        //         /* Add individual to the front */
        //         if (!front.contains(dominantIndividual)) {
        //             front.add(dominantIndividual);
        //             ++covered_individuals;
        //         }

        //         n.put(dominantIndividual, Integer.MAX_VALUE);
        //         /* Reduce number by one for dominated individuals */
        //         for (FixtureMOO dominatedIndividual : fitnessMap.get(dominantIndividual)) {
        //             n.put(dominatedIndividual, new Integer(n.get(dominatedIndividual).intValue() - 1));
        //         }
        //     }
        //     start = mostDominant;
        //     paretoFront.put(new Integer(current_pareto_front_idx), front);
        // }

        // log.info(n.values());
        // log.info(paretoFront.get(0).get(0).getFitnessMOO());
        // log.info(paretoFront.get(0).size());
        // log.info(fitnessMap.get(paretoFront.get(0).get(0)).size());

        // int mostDominant;
        int current_pareto_front_idx = 0;
        int covered_individuals = 0;
        while (covered_individuals < populationSize) {
            List<FixtureMOO> front = new ArrayList<FixtureMOO>();
            List<FixtureMOO> previous_front = null;

            int mostDominant;
            List<FixtureMOO> potential_next_front;
            if (previous_front == null) {
                mostDominant = Collections.min(n.values(), new IntegerComparator()).intValue();
                potential_next_front = population;
            }
            else
            {
                potential_next_front = new ArrayList<FixtureMOO>();
                // TODO Look only in the dominationg previous ones
                mostDominant = Integer.MAX_VALUE;
                for (FixtureMOO dominating_fixture : previous_front)
                    for (FixtureMOO dominated_fixture : fitnessMap.get(dominating_fixture)) {
                        mostDominant = Math.min(mostDominant, n.get(dominated_fixture));
                        potential_next_front.add(dominated_fixture);
                    }
            }
            log.info("Most Dominant: {}", mostDominant); // In the ideal case this should be 0

            for (int i = 0; i < potential_next_front.size(); i++) {
                if (n.get(potential_next_front.get(i)).intValue() != mostDominant)
                    continue;

                /* Found an individual with == mostDominant */
                rank.put(potential_next_front.get(i), new Integer(current_pareto_front_idx));
                FixtureMOO dominantIndividual = potential_next_front.get(i);

                /* Add individual to the front */
                if (!front.contains(dominantIndividual)) {
                    front.add(dominantIndividual);
                    ++covered_individuals;
                }

                n.put(dominantIndividual, Integer.MAX_VALUE);
                /* Reduce number by one for dominated individuals */
                for (FixtureMOO dominatedIndividual : fitnessMap.get(dominantIndividual)) {
                    n.put(dominatedIndividual, new Integer(n.get(dominatedIndividual).intValue() - 1));
                }
            }
            // log.info(covered_individuals);
            // log.info(front.size());
            paretoFront.put(new Integer(current_pareto_front_idx++), front);

            // log.info(n.values());
        }

        // while (paretoFront.get(new Integer(current_pareto_front_idx)) != null) {
        //     List<FixtureMOO> Q = new ArrayList<FixtureMOO>();
        //     for (FixtureMOO p : paretoFront.get(new Integer(start))) {
        //         for (FixtureMOO q : fitnessMap.get(p)) {
        //             n.put(p, new Integer(n.get(q).intValue() - 1));
        //             if (n.get(q).intValue() == start - 1) {
        //                 rank.put(q, new Integer(r + 1));
        //             }
        //         }
        //     }
        //     start++;
        //     r++;
        //     current_pareto_front_idx++;
        // }

        log.info("Total fronts: {}", paretoFront.size());
        for (int i = 0; i < paretoFront.size(); ++i)
        {
            List<FixtureMOO> front = paretoFront.get(i);
            log.info("Length of the {}th pareto front: {}", i, front.size());
            log.info("Fitness values");
            for (FixtureMOO fixture : front)
                log.info(fixture.getFitnessMOO());
        }
        
        return paretoFront;
    }

    /**
     * 
     * @param fixtures a pareto front (list of individuals)
     */
    void crowdingDistanceAssignement(List<FixtureMOO> fixtures) {
        // TODO: Fix Level calculation in numericaFitnessValue()?
        // 1. Level must be inverse?
        // 2. Level distance is not normalized? (As stated in the paper)
        FixtureFitnessComparator fc = new FixtureFitnessComparator();

        for (FixtureMOO fixture : fixtures)
            fixture.setCrowdingDistance(0.);

        for (int objective_index = 0; objective_index < amountPaths; ++objective_index) {
            final int idx = objective_index;

            fixtures.sort((FixtureMOO f1, FixtureMOO f2) -> fc.compare(f1.getFitnessMOO().get(idx),
                    f2.getFitnessMOO().get(idx)));

            fixtures.get(0).setCrowdingDistance(Double.MAX_VALUE);
            fixtures.get(fixtures.size() - 1).setCrowdingDistance(Double.MAX_VALUE);

            double f_min = fixtures.get(0).getFitnessMOO().get(objective_index).getNumericFitnessValue();
            double f_max = fixtures.get(fixtures.size() - 1).getFitnessMOO().get(objective_index)
                    .getNumericFitnessValue();
            double scaling = (f_max - f_min);

            for (int fixture_idx = 1; fixture_idx < fixtures.size() - 1; ++fixture_idx) {
                fixtures.get(fixture_idx).addCrowdingDistance(((fixtures.get(fixture_idx + 1).getFitnessMOO()
                        .get(objective_index).getNumericFitnessValue()
                        - fixtures.get(fixture_idx - 1).getFitnessMOO().get(objective_index).getNumericFitnessValue())
                        / scaling));
            }
        }
    }

    // TODO Refactor this; Almost same code as in StandardGA.java

    private FixtureTable createFixtureTable(TableSchema tableSchema, List<FixtureTable> tables) {
        List<FixtureRow> rows = new ArrayList<FixtureRow>();
        int numberOfRows = EvoSQLConfiguration.MIN_ROW_QTY;
        if (EvoSQLConfiguration.MAX_ROW_QTY > EvoSQLConfiguration.MIN_ROW_QTY)
            numberOfRows += random.nextInt(EvoSQLConfiguration.MAX_ROW_QTY - EvoSQLConfiguration.MIN_ROW_QTY);
        for (int j = 0; j < numberOfRows; j++) {
            FixtureRow row = rowFactory.create(tableSchema, tables, null); // Seeds: null
            rows.add(row);
            log.debug("Row created: {}", row);
        }
        return new FixtureTable(tableSchema, rows);
    }
}
