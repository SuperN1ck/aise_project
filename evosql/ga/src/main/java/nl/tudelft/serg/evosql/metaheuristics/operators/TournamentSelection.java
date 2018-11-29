package nl.tudelft.serg.evosql.metaheuristics.operators;

import java.util.List;
import java.util.Random;
import nl.tudelft.serg.evosql.fixture.Fixture;

public class TournamentSelection<T extends Fixture> {

	public static Random random = new Random();

	public static int ROUNDS = 4;

	/** 
	 * This method compute the tournament winner (tournament with n.rounds = ROUNDS)
	 * @param population from which to select the winner
	 * @return index of the winner in the current population
	 */
	public int getIndex(List<T> population) {
		int new_num = random.nextInt(population.size());
		int winner = new_num;
		
		FixtureComparator fc = new FixtureComparator();

		int round = 0;

		while (round < ROUNDS - 1) {
			new_num = random.nextInt(population.size());
			T selected = population.get(new_num);
			if (fc.compare(selected, population.get(winner)) == -1) {
				winner = new_num;
			}
			round++;
		}

		return winner;
	}
	
	public T getFixture(List<T> population) {
		return population.get(getIndex(population));
	}
}
