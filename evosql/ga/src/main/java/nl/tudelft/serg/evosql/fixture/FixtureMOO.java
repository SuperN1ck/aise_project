package nl.tudelft.serg.evosql.fixture;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureFitness;
import nl.tudelft.serg.evosql.sql.TableSchema;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FixtureMOO extends Fixture {

    protected static Logger log = LogManager.getLogger(FixtureMOO.class);
    private List<FixtureFitness> fitness_moo = new ArrayList<FixtureFitness>();
    private double crowdingDistance;
    private int targetNum;
    private HashMap<Integer, FixtureMOO> testsPassed = new HashMap<>();

    public FixtureMOO(List<FixtureTable> tables) {
        super(tables);
    }

    public List<FixtureFitness> getFitnessMOO() {
        return fitness_moo;
    }

    public void setFitnessMOO(List<FixtureFitness> fitness_moo) {
        this.fitness_moo = fitness_moo;
    }

    public void unsetFitnessMOO() {
        this.fitness_moo.clear();
    }

    public void setCrowdingDistance(double crowdingDistance) {
        this.crowdingDistance = crowdingDistance;
    }

    public void addCrowdingDistance(double term) {
        crowdingDistance += term;
    }

    public double getCrowdingDistance() {
        return crowdingDistance;
    }

    // TODO: Test
    @Override
    public FixtureMOO copy() {
        List<FixtureTable> cloneList = new ArrayList<FixtureTable>();
        for (FixtureTable table : this.tables) {
            cloneList.add(table.copy());
        }
        FixtureMOO clone = new FixtureMOO(cloneList);
        clone.setFitnessMOO(new ArrayList<FixtureFitness>(fitness_moo));
        clone.setCrowdingDistance(crowdingDistance);
        return clone;
    }

    // TODO: Low priority
    // @Override
    // public String prettyPrint()

    public int calculate_fitness_moo(List<String> paths_to_test, Map<String, TableSchema> tableSchemas)
            throws SQLException {
        fitness_moo.clear();
        int evaluations = 0;
        setTargetNum(paths_to_test.size());

        // Truncate tables in Instrumented DB
        for (TableSchema tableSchema : tableSchemas.values()) {
            genetic.Instrumenter.execute(tableSchema.getTruncateSQL());
        }

        // Insert population
        for (String sqlStatement : getInsertStatements()) {
            genetic.Instrumenter.execute(sqlStatement);
        }

        for (String path_to_test : paths_to_test) {
            // Start instrumenter
            genetic.Instrumenter.startInstrumenting();

            // Execute the path
            genetic.Instrumenter.execute(path_to_test);
            FixtureFitness ff = new FixtureFitness(genetic.Instrumenter.getFitness());
            fitness_moo.add(ff);

            // Store exceptions
            // TODO Re-enable it?
            // if (!genetic.Instrumenter.getException().isEmpty())
            // log.error(genetic.Instrumenter.getException());

            // Stop instrumenter
            genetic.Instrumenter.stopInstrumenting();
        }

        // set the fixture as "not changed" to avoid future fitness function computation
        setChanged(false);

        return evaluations;
    }

    public int getCoveredTargets()
    {
        int coveredTargets = 0;
        for (FixtureFitness ff : fitness_moo)
            coveredTargets += ff.getDistance() == 0. ? 1 : 0;
        return coveredTargets;
    }

    public HashMap<Integer, FixtureMOO> getCoveredTargetsHash()
   {
       for (FixtureFitness ff : fitness_moo){
           for(int targetIdx = 0 ; targetIdx < getTargetNum(); targetIdx++){

               if(ff.getDistance() == 0) {
                   testsPassed.put(new Integer(targetIdx), this);
               }
           }
       }
       log.info(testsPassed.keySet());
       return testsPassed;
   }


    public int getTargetNum() {
        return targetNum;
    }
 
    
    public void setTargetNum(int targetNum) {
        this.targetNum = targetNum;
    }


}