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

                /*  As long as we have space in the population and are not at the end of the list 
                    add subsequently everything to th next population */
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
    
    HashMap<Integer, List<FixtureMOO>> nonDominatedSort(List<FixtureMOO> population){

        FixtureFitnessComparator ffc = new FixtureFitnessComparator();

        HashMap<FixtureMOO, List<FixtureMOO>> fitnessMap = new HashMap<>();
        HashMap<Integer, List<FixtureMOO>> paretoFront = new HashMap<>();
        HashMap<FixtureMOO, Integer> n = new HashMap<>();
        HashMap<FixtureMOO, Integer> rank = new HashMap<>();

        // log.info("population size = {}", population.size());
        
        for(int i = 0 ;i<population.size();i++) {
            for(FixtureMOO f : population) {
                fitnessMap.put(f,new ArrayList<FixtureMOO>());
                n.put(f, new Integer(0));
            }
        }

        /**** get the dominating individual and set the number of dominants for each individual ****/
        
        boolean frontCheck = false;

        for(int i = 0 ;i < population.size(); i++){

            for(int j = 0 ; j<population.size();j++){   

                boolean check = true;
            
                for(int k = 0 ; k < population.get(0).getFitnessMOO().size(); k++){
                    
                    //log.info("value of {}th objective's {}th table fitness : {}", k, i, population.get(i).getFitnessMOO().get(k));
                    
                    if(ffc.compare(population.get(i).getFitnessMOO().get(k), population.get(j).getFitnessMOO().get(k))==1){
                        //log.info("{} individual is not dominant", j);
                        check = false;
                        break;
                    }
                }   
                
                if(check){   
                    FixtureMOO p = population.get(j);
                    if(!fitnessMap.get(population.get(i)).contains(p)) {
                        fitnessMap.get(population.get(i)).add(p);
                    }
                }
                else{
                    //n[i]++;
                    int temp = n.get(population.get(i)).intValue() + 1;
                    n.put(population.get(i), new Integer(temp));
                }  
            }

            /**** adding dominant individuals to the first pareto front****/ 

            if(n.get(population.get(i))==0){
                frontCheck = true;
                //log.info("{}th individual is dominating others",i);
                //rank[i] = 0;
                rank.put(population.get(i), new Integer(0));
                FixtureMOO dominantIndividual = population.get(i);

                if(paretoFront.get(new Integer(0)) == null) paretoFront.put(new Integer(0), new ArrayList<FixtureMOO>());
                if(!paretoFront.get(new Integer(0)).contains(dominantIndividual)) paretoFront.get(new Integer(0)).add(dominantIndividual);
            }

        }

        /**** adding indiviauls to the pareto front sequentially****/ 
        
        log.info(n.values());

        int start=0;
        int r = 0;

        //if first front doesn't exist, find the individual with minimum dominants and make a front
        if (!frontCheck) {
            int mostDominant = Collections.min(n.values(), new IntegerComparator()).intValue();
            log.info(mostDominant);

            for(int i = 0 ; i<population.size();i++){
                if(n.get(population.get(i)).intValue()==mostDominant){

                    rank.put(population.get(i), new Integer(0));
                    FixtureMOO dominantIndividual = population.get(i);
    
                    if(paretoFront.get(new Integer(mostDominant)) == null){
                        paretoFront.put(new Integer(mostDominant), new ArrayList<FixtureMOO>());
                    }

                    if(!paretoFront.get(new Integer(mostDominant)).contains(dominantIndividual)){
                        paretoFront.get(new Integer(mostDominant)).add(dominantIndividual);
                    }
                }
            }
            start = mostDominant;
        }

        while(paretoFront.get(new Integer(start))!=null){
            List<FixtureMOO> Q = new ArrayList<FixtureMOO>();
            for(FixtureMOO p :paretoFront.get(new Integer(start))){
                for(FixtureMOO q : fitnessMap.get(p)){
                    n.put(p, new Integer(n.get(q).intValue()-1));
                    if(n.get(q).intValue()==start-1){
                        rank.put(q, new Integer(r+1));
                    }
                }
            }
            start++;
            r++;
        }
        
        log.info(paretoFront);

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

