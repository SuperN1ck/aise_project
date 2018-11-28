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

        // TODO : nonDominatedSort will be inserted
        //nonDominatedSort(parent_population);

        return parent_population.get(0);
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