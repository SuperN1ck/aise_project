package nl.tudelft.serg.evosql.metaheuristics;

import java.sql.SQLException;
import java.util.ArrayList;
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
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureFitness;
import nl.tudelft.serg.evosql.sql.TableSchema;
import nl.tudelft.serg.evosql.util.random.Randomness;

public class NSGAII // extends MOOApproach TODO: Nive to have
{
    protected static Logger log = LogManager.getLogger(NSGAII.class);

    protected static Randomness random = new Randomness();

    int populationSize = EvoSQLConfiguration.POPULATION_SIZE;

    protected List<String> pathsToTest;

    protected Map<String, TableSchema> tableSchemas;

    protected String exceptions;

    /** Row Factory **/
    private FixtureRowFactory rowFactory = new FixtureRowFactory();

    public NSGAII(Map<String, TableSchema> pTableSchemas, List<String> pPathsToBeTested) {
        this.tableSchemas = pTableSchemas;
        this.pathsToTest = pPathsToBeTested;
        this.exceptions = "";
    }

    public Fixture execute() {
        
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

        // TODO: Main Loop should go here
        // TODO: while Every FixtureMOO's fitness doesn't reach 0

        for(FixtureMOO f : parent_population) {
            try {
                f.calculate_fitness_moo(pathsToTest, tableSchemas);
            } catch (SQLException e) {
                e.printStackTrace();
             }
        }

        //for(FixtureMOO f: parent_population) log.info(f.getFitnessMOO());

        nonDominatedSort(parent_population);

        return parent_population.get(0);
    }
    
    HashMap<Integer, List<FixtureMOO>> nonDominatedSort(List<FixtureMOO> population){
        
        HashMap<Integer, List<FixtureMOO>> fitnessMap = new HashMap<>();
        HashMap<Integer, List<FixtureMOO>> paretoFront = new HashMap<>();

        int[] n = new int[population.size()];
        int[] rank = new int[population.size()];

        // log.info("population size = {}", population.size());
        for(int i = 0 ;i<population.size();i++) {
            fitnessMap.put(new Integer(i),new ArrayList<FixtureMOO>());
        }

        for(int i = 0 ;i < population.size(); i++){

            for(int j = 0 ; j<population.size();j++){
               
                boolean check = true;
            
                for(int k = 0 ; k < population.get(0).getFitnessMOO().size(); k++){
                    //log.info("i = {}, j = {}, k = {}", i,j,k);
                    log.info("value of {}th objective's {}th table fitness : {}", k, i, population.get(i).getFitnessMOO().get(k));
                    
                    if(fitnessCompare(population.get(i).getFitnessMOO().get(k), population.get(j).getFitnessMOO().get(k))==1){
                        //log.info("{} individual is not dominant", j);
                        check = false;
                        break;
                    }
                }   
                //log.info("check : {}", check);  
                if(check){   
                    FixtureMOO p = population.get(j);
                    if(!fitnessMap.get(new Integer(i)).contains(p)) {
                        fitnessMap.get(new Integer(i)).add(p);
                    }
                }
                else{
                    n[i]++;
                }  
            }
               
            if(n[i] == 0){
                log.info("{}th individual is dominating others",i);
                rank[i] = 0;
                FixtureMOO dominantIndividual = population.get(i);

                if(paretoFront.get(new Integer(0)) == null) paretoFront.put(new Integer(0), new ArrayList<FixtureMOO>());
                if(!paretoFront.get(new Integer(0)).contains(dominantIndividual)) paretoFront.get(new Integer(0)).add(dominantIndividual);
            }
        }

        //log.info(fitnessMap.get(new Integer(0)));
        log.info(paretoFront);
        
        int i = 0;

        //TO DO : regard cases when we don't get the 1st front
        //To Do : how to map the individual in the front[i] and S[p]


        return paretoFront;

       
     }


    int fitnessCompare(FixtureFitness f1, FixtureFitness f2){
			// Check nulls
		if (f1 == null && f2 == null)
			return 0;
		else if (f1 == null)
			return 1;
		else if (f2 == null)
			return -1;
		
		// Compare max query levels, higher is better
		if (f1.getMaxQueryLevel() < f2.getMaxQueryLevel())
			return 1;
		else if (f1.getMaxQueryLevel() > f2.getMaxQueryLevel())
			return -1;
		
		// From max query level downwards check for differences
		for (int queryLevel = f1.getMaxQueryLevel(); queryLevel >= 0; queryLevel--) {
			QueryLevelData qld1 = f1.getQueryLevelData(queryLevel);
			QueryLevelData qld2 = f2.getQueryLevelData(queryLevel);

			int comp = qld1.compare(qld1, qld2);
			if (comp != 0)
				return comp;
		}
		
		return 0;
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