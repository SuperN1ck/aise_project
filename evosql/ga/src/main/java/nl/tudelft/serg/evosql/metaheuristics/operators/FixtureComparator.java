package nl.tudelft.serg.evosql.metaheuristics.operators;

import java.util.Comparator;

import nl.tudelft.serg.evosql.fixture.Fixture;

public class FixtureComparator implements Comparator<Fixture>{

	@Override
	/**
	 * Important to note that smaller is better.
	 */
	public int compare(Fixture o1, Fixture o2) {		
		FixtureFitness f1 = o1.getFitness();
		FixtureFitness f2 = o2.getFitness();

		FixtureFitnessComparator fc = new FixtureFitnessComparator();
		return fc.compare(f1, f2);
	}
}
