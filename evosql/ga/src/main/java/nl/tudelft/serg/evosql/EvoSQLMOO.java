package nl.tudelft.serg.evosql;

// Java internal imports
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
// Java exteral imports
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
// Project imports
import nl.tudelft.serg.evosql.db.ISchemaExtractor;
import nl.tudelft.serg.evosql.Result;
import nl.tudelft.serg.evosql.sql.parser.SqlSecurer;
import nl.tudelft.serg.evosql.sql.TableSchema;
import nl.tudelft.serg.evosql.db.Seeds;
import nl.tudelft.serg.evosql.fixture.Fixture;


public class EvoSQLMOO extends EvoSQLSolver{

    private static Logger log = LogManager.getLogger(EvoSQL.class);

    public EvoSQLMOO(String jdbcString, String dbDatabase, String dbUser, String dbPwd) {
        super(jdbcString, dbDatabase, dbUser, dbPwd);
    }

    public EvoSQLMOO(ISchemaExtractor se) {
		super(se);
	}

    public Result execute(String sqlToBeTested)
    {
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
		allPaths.stream().forEach(path -> log.info(path));
		
		Map<String, TableSchema> tableSchemas;
		Seeds seeds; // TODO Is this needed, maybe something like SeedsMOO?
		
        long start, end = -1;

        int     totalPaths = allPaths.size()
            ,   coveredPaths = 0;
        
        long max_execution_time = EvoSQLConfiguration.MS_EXECUTION_TIME;

        Result result = new Result(sqlToBeTested, System.currentTimeMillis());

        // TODO Create something like FixtureMOO?
        List<Fixture> population = new ArrayList<Fixture>();

        // TODO implement MOO

        return result;
    }
}