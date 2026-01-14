package compression.grammargenerator.localsearch;

import compression.grammar.CharTerminal;
import compression.grammar.Grammar;
import compression.grammar.NonTerminal;
import compression.grammar.SecondaryStructureGrammar;
import compression.grammar.Terminal;
import compression.parser.SRFParser;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilsTest {

	/**
	 * Verifies passesDataset returns true when all words are parsable by the grammar.
	 */
	@Test
	public void testPassesDatasetWhenAllWordsParsable() {
		SecondaryStructureGrammar grammar = minimalGrammar();
		SRFParser<Character> parser = new SRFParser<>(grammar);
		List<Terminal<Character>> word = List.of(new CharTerminal('.'));

		assertTrue(Utils.passesDataset(parser, List.of(word)));
	}

	/**
	 * Confirms passesDataset stops at the first unparsable word and returns false.
	 */
	@Test
	public void testPassesDatasetWhenAnyWordUnparsable() {
		SecondaryStructureGrammar grammar = minimalGrammar();
		SRFParser<Character> parser = new SRFParser<>(grammar);
		List<Terminal<Character>> parsable = List.of(new CharTerminal('.'));
		List<Terminal<Character>> unparsable = List.of(new CharTerminal('('));

		assertFalse(Utils.passesDataset(parser, List.of(parsable, unparsable)));
	}

	private static SecondaryStructureGrammar minimalGrammar() {
		NonTerminal start = new NonTerminal("S");
		Grammar<Character> grammar = new Grammar.Builder<Character>("minimal", start)
				.addRule(start, new CharTerminal('.'))
				.build();
		return SecondaryStructureGrammar.from(grammar);
	}
}
