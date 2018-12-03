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
import nl.tudelft.serg.evosql.db.SeedExtractor;
import nl.tudelft.serg.evosql.db.Seeds;
import nl.tudelft.serg.evosql.fixture.Fixture;
import nl.tudelft.serg.evosql.fixture.FixtureMOO;
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
		// allPaths.stream().forEach(path -> log.debug(path));
		
		Map<String, TableSchema> tableSchemas;
        /*
        Seeds seeds; 
        if (EvoSQLConfiguration.USE_LITERAL_SEEDING) {
            // Get the seeds for the current path
            seeds = new SeedExtractor(sqlToBeTested).extract();
        } else {
            // Use no seeds
            seeds = Seeds.emptySeed();
        }
		*/
		
		List<Seeds> seedList = new ArrayList<Seeds>();
		
		for(String p: allPaths){
			if (EvoSQLConfiguration.USE_LITERAL_SEEDING) {
            // Get the seeds for the current path
            seedList.add(new SeedExtractor(p).extract());
			} else {
				// Use no seeds
				//seeds = Seeds.emptySeed();
			}
		}
		
		Seeds seedMOO = new Seeds();
		for(Seeds s: seedList){
			seedMOO.addLongList(s.getLongList());
			seedMOO.addDoubleList(s.getDoubleList());
			seedMOO.addStringList(s.getStringList());
			seedMOO.addTempList(s.getTempList());
		}
		
        long start, end = -1;

        int     totalPaths = allPaths.size()
            ,   coveredPaths = 0;
        
        long max_execution_time = EvoSQLConfiguration.MS_EXECUTION_TIME;

        // TODO Verify that this works
		
        tableSchemas = schemaExtractor.getTablesFromQuery(sqlToBeTested);

        start = System.currentTimeMillis();
        Result result = new Result(sqlToBeTested, start);
       
        try {
            // Create schema on instrumenter
            for (TableSchema ts : tableSchemas.values()) {
                genetic.Instrumenter.execute(ts.getDropSQL());
                genetic.Instrumenter.execute(ts.getCreateSQL());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        NSGAII nsga_ii = new NSGAII(tableSchemas, allPaths, seedMOO);
        FixtureMOO fixture = (FixtureMOO) nsga_ii.execute();

        end = System.currentTimeMillis();

        result.addCoveragePercentage((double) fixture.getCoveredTargets() / allPaths.size());
        for (int pathNo = 0; pathNo < allPaths.size(); ++pathNo)
        {
            if (fixture.getFitnessMOO().get(pathNo).getDistance() == 0)
            {
                // Solved this target
                // TODO Get correct generation / indiviudal
               result.addPathSuccess(pathNo + 1, allPaths.get(pathNo), end - start , fixture, null, 0, 0, "");
            }
            else
            {
                String message = "Remaining distance: " + fixture.getFitnessMOO().get(0).getDistance();
                result.addPathFailure(pathNo + 1, allPaths.get(pathNo), end - start, message, 0, 0, "");
            }
        }

        genetic.Instrumenter.stopDatabase();

        return result;
    }

}
