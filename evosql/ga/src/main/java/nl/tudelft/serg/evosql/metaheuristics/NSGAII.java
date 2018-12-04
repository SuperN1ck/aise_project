package nl.tudelft.serg.evosql.metaheuristics;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.db.Seeds;
import nl.tudelft.serg.evosql.fixture.Fixture;
import nl.tudelft.serg.evosql.fixture.FixtureMOO;
import nl.tudelft.serg.evosql.fixture.FixtureRow;
import nl.tudelft.serg.evosql.fixture.FixtureRowFactory;
import nl.tudelft.serg.evosql.fixture.FixtureTable;
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureCombine;
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureCrossover;
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureFitnessComparator;
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureMutation;
import nl.tudelft.serg.evosql.metaheuristics.operators.TournamentSelection;
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

    /** Selection operator **/
	private TournamentSelection<FixtureMOO> selection = new TournamentSelection<FixtureMOO>();

	/** Row Factory **/
	private FixtureRowFactory rowFactory = new FixtureRowFactory();
	
	/** Mutation operator **/
	private FixtureMutation mutation;

    /** Crossover operator **/
    private FixtureCrossover<FixtureMOO> crossover = new FixtureCrossover<FixtureMOO>(new Randomness());
    
    /** Combine operator **/
    private FixtureCombine<FixtureMOO> combine;

	/** Seeds store **/
	private Seeds seeds;

    public NSGAII(Map<String, TableSchema> pTableSchemas, List<String> pPathsToBeTested, Seeds seeds) {
        this.tableSchemas = pTableSchemas;
        this.pathsToTest = pPathsToBeTested;
        this.amountPaths = pPathsToBeTested.size();
        this.exceptions = "";

        this.seeds = seeds;
        this.mutation = new FixtureMutation(rowFactory, seeds, EvoSQLConfiguration.MAX_ROW_QTY * amountPaths);
        this.combine = new FixtureCombine<FixtureMOO>(EvoSQLConfiguration.MAX_ROW_QTY * amountPaths, new Randomness());
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
            fixture.setChanged(true);
			try {
				fixture.calculate_fitness_moo(pathsToTest, tableSchemas);
			} catch (SQLException e) {
				e.printStackTrace();
			}
            // log.debug("Fixture created: {}", fixture);
            parent_population.add(fixture);
        }
        log.debug("Generated random population with {} fixtures", parent_population.size());

        /* NSGA-II Mainloop */

        while (System.currentTimeMillis() - startTime < EvoSQLConfiguration.MS_EXECUTION_TIME) {
            // genetic.Instrumenter.stopDatabase();
            // genetic.Instrumenter.startDatabase();
            // try {
            //     // Create schema on instrumenter
            //     for (TableSchema ts : tableSchemas.values()) {
            //         genetic.Instrumenter.execute(ts.getDropSQL());
            //         genetic.Instrumenter.execute(ts.getCreateSQL());
            //     }
            // } catch(Exception e) {
            //     e.printStackTrace();
            // }

            List<FixtureMOO> combined_population = new ArrayList<FixtureMOO>(2 * populationSize);

            // TODO use selection, crossover and mutation to create a new population
            List<FixtureMOO> offspring_population = new ArrayList<FixtureMOO>(populationSize);
			//while(offspring_population.size() < populationSize - 1) {
			for (int index = 0; index < populationSize; index += 1){
				// Get two parents through selection operator
				FixtureMOO parent1 = selection.getFixture(parent_population);
                FixtureMOO parent2 = selection.getFixture(parent_population);

                if (random.nextDouble() < EvoSQLConfiguration.P_COMBINE_PARENTS) {
                    FixtureMOO offspring = this.combine.combine(parent1, parent2);
                    // we add only changed solutions (to avoid clones in the new population)
                    try {
                        offspring.calculate_fitness_moo(pathsToTest, tableSchemas);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    offspring_population.add(offspring);
                } else {
                    FixtureMOO offspring1;
                    FixtureMOO offspring2;
                    
                    // Use the crossover operator
                    if (random.nextDouble() < EvoSQLConfiguration.P_CROSSOVER) {

                        List<FixtureMOO>generatedOffspring = this.crossover.crossover(parent1, parent2);
                        offspring1 = generatedOffspring.get(0);
                        offspring2 = generatedOffspring.get(1);
                    } else {
                        offspring1 = parent1.copy();
                        offspring2 = parent2.copy();
                    }
                    
                    // Mutate
                    mutation.mutate(offspring1);
                    mutation.mutate(offspring2);
                    
                    // Calculate fitness and add offspring to the offspring_population if needed
                    if (offspring1.isChanged()) {
                        // we add only changed solutions (to avoid clones in the new population)
                        try {
                            offspring1.calculate_fitness_moo(pathsToTest, tableSchemas);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        offspring_population.add(offspring1);
                    }
                    
                    if (offspring2.isChanged()){
                        // we add only changed solutions (to avoid clones in the new population)
                        try {
                            offspring2.calculate_fitness_moo(pathsToTest, tableSchemas);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        offspring_population.add(offspring2);
                    }
                }
			}

            combined_population.addAll(parent_population);
            combined_population.addAll(offspring_population);

            log.debug("Combined population size: {}", combined_population.size());
            log.debug("-- Parent:    {}", parent_population.size());
            log.debug("-- Offspr:    {}", offspring_population.size());
            /*
            for (FixtureMOO f : combined_population) {
                try {
                    if (f.isChanged())
                        f.calculate_fitness_moo(pathsToTest, tableSchemas);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            */
            HashMap<Integer, List<FixtureMOO>> rankedFronts = nonDominatedSort(combined_population);

            int current_front_idx = 0;
            parent_population.clear();

            // log.info("Current front fitness values");
            // for(FixtureMOO fixture : rankedFronts.get(0))
            //     log.info(fixture.getFitnessMOO());

            int most_covered_targets = 0;
            for(FixtureMOO fixtureMOO : rankedFronts.get(0))
            {
                if (fixtureMOO.getCoveredTargets() == amountPaths)
                {
                    log.info("Covering {} of {} targets", fixtureMOO.getCoveredTargets(), amountPaths);
                    return fixtureMOO;
                }
                // else
                //     log.info(fixtureMOO.getCoveredTargets());
                most_covered_targets = Math.max(most_covered_targets, fixtureMOO.getCoveredTargets());
            }
            log.info("Most covered targets: {} of {}", most_covered_targets, amountPaths);

            while (parent_population.size() + rankedFronts.get(current_front_idx).size() < populationSize) {
                List<FixtureMOO> current_front = rankedFronts.get(current_front_idx);
                parent_population.addAll(current_front);

                if (++current_front_idx == rankedFronts.size())
                    break;
            }
            log.info("Added all fronts, now cutting the last one, current parent_population size: {}", parent_population.size());
            /* If we have already added everything we don't need to cut the last front */
            if (current_front_idx != rankedFronts.size()) {
                List<FixtureMOO> last_front = rankedFronts.get(current_front_idx);
                crowdingDistanceAssignement(last_front);
                // last_front.sort((FixtureMOO f1, FixtureMOO f2) ->
                // Double.compare(f2.getCrowdingDistance(), f1.getCrowdingDistance()));
                // last_front.sort(Comparator.comparing(FixtureMOO::getCrowdingDistance).reversed());
                last_front.sort(Comparator.comparing(FixtureMOO::getCoveredTargets).reversed());

                // for (FixtureMOO fMoo: last_front)
                //     log.info("Crowding distance: {}", fMoo.getCrowdingDistance());

                /*
                 * As long as we have space in the population and are not at the end of the list
                 * add subsequently everything to th next population
                 */
                for (int last_front_idx = 0; parent_population.size() < populationSize
                        && last_front_idx < last_front.size(); ++last_front_idx)
                    parent_population.add(last_front.get(last_front_idx));
            }
        }
        
        FixtureMOO best_ffmoo = parent_population.get(0);
        int most_covered_targets = best_ffmoo.getCoveredTargets();
        for (FixtureMOO ffmoo : parent_population)
        {
            if (ffmoo.getCoveredTargets() < most_covered_targets)
                continue;
            
            best_ffmoo = ffmoo;
            most_covered_targets = best_ffmoo.getCoveredTargets();
        }

        log.info("Most covered targets: {}", most_covered_targets);
        return best_ffmoo;
    }

    HashMap<Integer, List<FixtureMOO>> nonDominatedSort(List<FixtureMOO> population) {
        FixtureFitnessComparator ffc = new FixtureFitnessComparator();

        HashMap<FixtureMOO, List<FixtureMOO>> dominationMap = new HashMap<>(); // S
        HashMap<Integer, List<FixtureMOO>> paretoFronts = new HashMap<>(); // F
        HashMap<FixtureMOO, Integer> n = new HashMap<>(); // n

        // int[] rank = new int[population.size()];
        HashMap<FixtureMOO, Integer> rank = new HashMap<>();

        for (FixtureMOO f : population) {
            dominationMap.put(f, new ArrayList<FixtureMOO>());
            n.put(f, new Integer(0));
        }

        log.info(n.size());

        /****
         * get the dominating individual and set the number of dominants for each
         * individual
         ****/

        for (int p_idx = 0; p_idx < population.size(); p_idx++) {
            FixtureMOO p = population.get(p_idx);

            for (int q_idx = 0; q_idx < population.size(); q_idx++) {
                FixtureMOO q = population.get(q_idx);

                boolean p_is_dominating_q = true;
                boolean q_is_dominating_p = true;

                for (int k = 0; k < amountPaths; k++) {

                    // log.debug("value of {}th objective's {}th table fitness : {}", k, i,
                    // population.get(i).getFitnessMOO().get(k));
                    // 1: p is worse than q
                    // 0: p and q are equal
                    // -1: p is better than q
                    // --> >= : p Is better than q (truly dominating)
                    // --> > : p Is better or equal q (partly dominating)
                    int comparison_result = ffc.compare(p.getFitnessMOO().get(k), q.getFitnessMOO().get(k));
                    // log.debug(p.getFitnessMOO().get(k));
                    // log.debug(q.getFitnessMOO().get(k));
                    if (comparison_result > 0) {
                        p_is_dominating_q = false;
                        // break;
                    } else if (comparison_result < 0) {
                        q_is_dominating_p = false;
                    }
                }

                if (p_is_dominating_q) {
                    if (!dominationMap.get(p).contains(q)) {
                        dominationMap.get(p).add(q);
                    }
                } else if (q_is_dominating_p) {
                    // n[i]++;
                    int temp = n.get(p).intValue() + 1;
                    n.put(p, new Integer(temp));
                }
            }
        }

        /**** adding indiviauls to the pareto front sequentially ****/
        log.info(n.size());
        log.debug(n.values());

        int current_pareto_front_idx = 0;
        int covered_individuals = 0;
        List<FixtureMOO> dominatedIndividuals = new ArrayList<FixtureMOO>();

        while (covered_individuals < n.size()) {
            log.debug("=-=-=-=-= Creating next front =-=-=-=-=");
            List<FixtureMOO> front = new ArrayList<FixtureMOO>();

            int minDominantion;
            List<FixtureMOO> potential_next_front = new ArrayList<FixtureMOO>();
            if (dominatedIndividuals.size() == 0) {
                minDominantion = Collections.min(n.values(), new IntegerComparator()).intValue();
                potential_next_front = population;
            } else {
                potential_next_front.clear();
                minDominantion = Integer.MAX_VALUE;
                for (FixtureMOO dominatedIndividual : dominatedIndividuals) {
                    minDominantion = Math.min(minDominantion, n.get(dominatedIndividual).intValue());
                    potential_next_front.add(dominatedIndividual);
                }
            }
            log.debug("Min Dominant: {}", minDominantion); // In the ideal case this should be 0
            dominatedIndividuals.clear();

            for (int i = 0; i < potential_next_front.size(); i++) {
                FixtureMOO dominantingIndividual = potential_next_front.get(i);
                if (n.get(dominantingIndividual).intValue() != minDominantion)
                    continue;

                /* Found an individual with == mostDominant */
                rank.put(potential_next_front.get(i), new Integer(current_pareto_front_idx));

                /* Add individual to the front */
                if (!front.contains(dominantingIndividual)) {
                    front.add(dominantingIndividual);
                    ++covered_individuals;
                    n.put(dominantingIndividual, Integer.MAX_VALUE);
                }                

                /* Reduce number by one for dominated individuals */
                for (FixtureMOO dominatedIndividual : dominationMap.get(dominantingIndividual)) {
                    if (!dominatedIndividuals.contains(dominatedIndividual)
                            && n.get(dominatedIndividual).intValue() < Integer.MAX_VALUE)
                        dominatedIndividuals.add(dominatedIndividual);
                }

            }
            log.debug("Dominated Individuals: {}", dominatedIndividuals.size());
            log.debug("Covered Indibiduals: {}", covered_individuals);
            log.debug("Added front size: {}", front.size());

            for (FixtureMOO dominatedIndividual : dominatedIndividuals)
                n.put(dominatedIndividual, new Integer(n.get(dominatedIndividual).intValue() - 1));

            paretoFronts.put(new Integer(current_pareto_front_idx++), front);
        }


        HashSet<FixtureMOO> input = new HashSet<FixtureMOO>(population);
        HashSet<FixtureMOO> output = new HashSet<FixtureMOO>(population.size());
        log.debug("Total fronts: {}", paretoFronts.size());
        for (int i = 0; i < paretoFronts.size(); ++i) {
            List<FixtureMOO> front = paretoFronts.get(i);
            log.debug("Length of the {}th pareto front: {}", i, front.size());
            log.debug("Fitness values");
            for (FixtureMOO fixture : front)
            {
                log.debug(fixture.getFitnessMOO());
                output.add(fixture);
            }
        }
        log.debug("Input/Output in Non Dominated Sorting equal {}", input.equals(output));

        return paretoFronts;
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

        if (fixtures.size() < 2)
            return;

        for (int objective_index = 0; objective_index < amountPaths; ++objective_index) {
            final int idx = objective_index;
            
            fixtures.sort((FixtureMOO f1, FixtureMOO f2) -> Double.compare(f1.getFitnessMOO().get(idx).getNumericFitnessValue(), f2.getFitnessMOO().get(idx).getNumericFitnessValue()));
            
            // List<FixtureMOO> sorted_1 = new ArrayList<FixtureMOO>(fixtures);
            // fixtures.sort((FixtureMOO f1, FixtureMOO f2) -> fc.compare(f1.getFitnessMOO().get(idx),
            //         f2.getFitnessMOO().get(idx)));
            
            // log.info("Equally sorted: {}", sorted_1.equals(fixtures));
            
            fixtures.get(0).setCrowdingDistance(Double.MAX_VALUE);
            fixtures.get(fixtures.size() - 1).setCrowdingDistance(Double.MAX_VALUE);
            
            double f_min = fixtures.get(0).getFitnessMOO().get(objective_index).getNumericFitnessValue();
            double f_max = fixtures.get(fixtures.size() - 1).getFitnessMOO().get(objective_index)
                    .getNumericFitnessValue();
            double scaling = (f_max - f_min);
            
            // log.debug("f_max: {}", f_max);
            // log.debug("f_min: {}", f_min);
            // log.debug("scaling: {}", scaling);

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
        if (amountPaths * EvoSQLConfiguration.MAX_ROW_QTY > EvoSQLConfiguration.MIN_ROW_QTY)
            numberOfRows += random.nextInt(amountPaths * EvoSQLConfiguration.MAX_ROW_QTY - EvoSQLConfiguration.MIN_ROW_QTY);
        for (int j = 0; j < numberOfRows; j++) {
            FixtureRow row = rowFactory.create(tableSchema, tables, seeds);
            rows.add(row);
            // log.debug("Row created: {}", row);
        }
        return new FixtureTable(tableSchema, rows);
    }
}
