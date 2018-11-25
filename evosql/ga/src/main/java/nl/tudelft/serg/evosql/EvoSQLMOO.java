package nl.tudelft.serg.evosql;

// Java internal imports
import java.util.List;
import java.util.Map;

// Java exteral imports
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Project imports
import nl.tudelft.serg.evosql.db.ISchemaExtractor;
import nl.tudelft.serg.evosql.db.Seeds;
import nl.tudelft.serg.evosql.fixture.Fixture;
import nl.tudelft.serg.evosql.metaheuristics.NSGAII;
import nl.tudelft.serg.evosql.sql.TableSchema;
import nl.tudelft.serg.evosql.sql.parser.SqlSecurer;



public class EvoSQLMOO extends EvoSQLSolver{

    private static Logger log = LogManager.getLogger(EvoSQLMOO.class);

    public EvoSQLMOO(String jdbcString, String dbDatabase, String dbUser, String dbPwd) {
        super(jdbcString, dbDatabase, dbUser, dbPwd);
    }

    public EvoSQLMOO(ISchemaExtractor se) {
		super(se);
	}

    public Result execute(String sqlToBeTested)
    {
        // genetic.Instrumenter.startDatabase()
		genetic.Instrumenter.startDatabase();

        log.info("Hello from EvoSQLMOO");
        // Check if query can be parsed
		try {
			// Make sql safe
			sqlToBeTested = new SqlSecurer(sqlToBeTested).getSecureSql();
		} catch (RuntimeException e) {
			log.error("Could not parse input query.");
			e.printStackTrace();
			return null;
		}
		
		log.info("SQL to be tested: " + sqlToBeTested);
		
		// A path is a SQL query that only passes a certain condition set.
		List<String> allPaths;
		try {
			pathExtractor.initialize();
			allPaths = pathExtractor.getPaths(sqlToBeTested);
		} catch (Exception e) {
			log.error("Could not extract the paths, ensure that you are connected to the internet. Message: " + e.getMessage(), e);
			return null;
		}
		log.info("Found " + allPaths.size() + " paths");
		allPaths.stream().forEach(path -> log.debug(path));
		
		Map<String, TableSchema> tableSchemas;
		Seeds seeds; // TODO Is this needed, maybe something like SeedsMOO?
		
        long start, end = -1;

        int     totalPaths = allPaths.size()
            ,   coveredPaths = 0;
        
        long max_execution_time = EvoSQLConfiguration.MS_EXECUTION_TIME;

        // TODO Verify that this works
        tableSchemas = schemaExtractor.getTablesFromQuery(sqlToBeTested);

        Result result = new Result(sqlToBeTested, System.currentTimeMillis());

        // TODO implement MOO
        /* [Nick] Some thougts I had:
         * - A "Fixture" is a individual
         * - In StandardGA.calculateFitness(Fixture fixture)
         * ---- fitness is calculated for one explicit solution
         * ---- As this methods is in the StandardGA-class it uses some class
         *      variables. Especially, in this line
         *          genetic.Instrumenter.execute(pathToTest);
         *      where the query plan is run and analyzed
         * - We therefore should refactor this function 
         * ---- Decouple it from StandardGA-class
         * ---- Make sure individualCount++ is still increased
         * ---- We should leave calculateFitness in StandardGA
         * ---- I already created a file named FixtureFitnessCalculator
         * - With this refactoring comes a decision how deep we should modify 
         *   EvoSQL. Mainly this question raises because this would require a
         *   deeper modification in FixtureFitness. Right now it is assumed that
         *   That there is only one objective.
         * - The simplest possibility would be to completly develop orthogonal 
         *   to the current implementation and mostly copy and paste things just 
         *   as in this file.
         * - In FixtureComparotr.compare(Fixture o1, Fixture o2) two fixtures
         *   are compared. If they have the same level, the result of the 
         *   comparison for this level is returned
         * - In QueryLevelData.compare(QueryLeveldData qld1, QueryLevelData qld2)
         *   two results of a level are getting compared against each other
         * - We can look at these implementation to see and understand how the 
         *   query level processing works 
         * - --> From that we can derive how and what to change to formulate as
         *   MOO.
        */

        NSGAII nsga_ii = new NSGAII(tableSchemas, allPaths);
        Fixture fixture = nsga_ii.execute();

        // TODO Refactor "Evaluation of fixture into result" into EvoSQLSolver ?

        genetic.Instrumenter.stopDatabase();

        return result;
    }

}
