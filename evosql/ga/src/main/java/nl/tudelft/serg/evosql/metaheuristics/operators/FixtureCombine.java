package nl.tudelft.serg.evosql.metaheuristics.operators;

import java.util.Random;

import nl.tudelft.serg.evosql.fixture.Fixture;
import nl.tudelft.serg.evosql.fixture.FixtureRow;
import nl.tudelft.serg.evosql.fixture.FixtureTable;
import nl.tudelft.serg.evosql.util.random.Randomness;

public class FixtureCombine<T extends Fixture> {

    private int MAX_ROW_QTY;
    private Randomness random;

    public FixtureCombine(int MAX_ROW_QTY, Randomness random) {
        this.MAX_ROW_QTY = MAX_ROW_QTY;
        this.random = random;
    }

    public T combine(T parent1, T parent2) {
        if (parent1.getTables().size() < 1 || parent2.getTables().size() < 1)
            throw new IllegalArgumentException("Each solution must have at least one Table");

        T offspring = (T) parent1.copy();

        for (int table_idx = 0; table_idx < offspring.getNumberOfTables(); ++table_idx) {
            /* Combine tables */
            FixtureTable offspring_table = offspring.getTable(table_idx);
            for (FixtureRow row : parent2.getTable(table_idx).getRows()) {
                offspring_table.addRow(row);
            }

            /* Adjust size rows */
            while (offspring_table.getRowCount() > MAX_ROW_QTY) {
                offspring_table.remove(random.nextInt(MAX_ROW_QTY));
            }
        }
        offspring.setChanged(true);
        return offspring;
    }
}