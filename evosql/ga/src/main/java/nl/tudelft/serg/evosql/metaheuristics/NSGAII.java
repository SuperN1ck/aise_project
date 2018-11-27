package nl.tudelft.serg.evosql.metaheuristics;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.fixture.Fixture;
import nl.tudelft.serg.evosql.fixture.FixtureMOO;
import nl.tudelft.serg.evosql.fixture.FixtureRow;
import nl.tudelft.serg.evosql.fixture.FixtureRowFactory;
import nl.tudelft.serg.evosql.fixture.FixtureTable;
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureFitnessComparator;
import nl.tudelft.serg.evosql.sql.TableSchema;
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

        for (FixtureMOO f : parent_population) {
            try {
                f.calculate_fitness_moo(pathsToTest, tableSchemas);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        while (System.currentTimeMillis() - startTime < EvoSQLConfiguration.MS_EXECUTION_TIME) {
            // TODO: Main Loop should go here
            // TODO: I think we need something like FixtureMOOComparator as already
            // given in FixtureComparator.java for the normal one
            HashMap<Integer, List<FixtureMOO>> rankedFronts = nonDominatedSort(parent_population);

        }

        return parent_population.get(0);
    }

    HashMap<Integer, List<FixtureMOO>> nonDominatedSort(List<FixtureMOO> fixture) {
        HashMap<Integer, List<FixtureMOO>> rankedFronts = new HashMap();

        return rankedFronts;
    }

    /**
     * 
     * @param fixtures a pareto front (list of individuals)
     */
    void crowdingDistanceAssignement(List<FixtureMOO> fixtures) {
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