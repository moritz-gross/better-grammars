package compression.grammargenerator.localsearch;

import compression.grammar.CharTerminal;
import compression.grammar.Grammar;
import compression.grammar.NonTerminal;
import compression.grammar.SecondaryStructureGrammar;
import compression.grammargenerator.localsearch.dataclasses.RunResult;
import compression.grammargenerator.localsearch.dataclasses.RunStats;
import compression.grammargenerator.localsearch.dataclasses.SearchState;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class LocalSearchExplorerTest {

	/**
	 * Ensures bestResult selects the run with the lowest bits-per-base score.
	 */
	@Test
	public void testBestResultSelectsLowestScore() {
		SecondaryStructureGrammar grammar = minimalGrammar();
		SearchState first = new SearchState(new boolean[] { true }, grammar, 2.0);
		SearchState second = new SearchState(new boolean[] { true }, grammar, 1.25);
		RunResult run1 = new RunResult(first, new RunStats(1, 10L, 1, 5, 1, 2.0));
		RunResult run2 = new RunResult(second, new RunStats(2, 11L, 1, 5, 1, 1.25));

		RunResult best = LocalSearchExplorer.bestResult(Arrays.asList(run1, run2));
		assertSame(run2, best); // refers to the same object
	}

	/**
	 * Confirms bestResult returns null for an empty list.
	 */
	@Test
	public void testBestResultWithEmptyList() {
		List<RunResult> empty = List.of();

		assertNull(LocalSearchExplorer.bestResult(empty));
	}

	/**
	 * test helper
	 */
	private static SecondaryStructureGrammar minimalGrammar() {
		NonTerminal start = new NonTerminal("S");
		Grammar<Character> grammar = new Grammar.Builder<Character>("minimal", start)
				.addRule(start, new CharTerminal('.'))
				.build();
		return SecondaryStructureGrammar.from(grammar);
	}
}
