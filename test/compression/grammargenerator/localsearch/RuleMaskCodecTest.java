package compression.grammargenerator.localsearch;

import compression.grammar.CharTerminal;
import compression.grammar.Grammar;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;
import compression.grammar.SecondaryStructureGrammar;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RuleMaskCodecTest {

	/**
	 * Confirms toMask flags only rules present in the grammar and ignores unknown rules.
	 */
	@Test
	public void testToMaskIncludesOnlyKnownRules() {
		NonTerminal start = new NonTerminal("S");
		Rule rDot = new Rule(start, new CharTerminal('.'));
		Rule rOpen = new Rule(start, new CharTerminal('('));
		Rule rClose = new Rule(start, new CharTerminal(')')); // not tracked in RuleMaskCodec

		RuleMaskCodec codec = new RuleMaskCodec(new Rule[] { rDot, rOpen }, start);
		SecondaryStructureGrammar grammar = SecondaryStructureGrammar.from(
				new Grammar.Builder<Character>("my-grammar-1", start)
						.addRule(rDot)
						.addRule(rClose)
						.build());

		boolean[] mask = codec.toMask(grammar);
		assertArrayEquals(new boolean[] { true, false }, mask);
	}

	/**
	 * Ensures buildGrammarIfValid returns null when the start symbol has no rules.
	 */
	@Test
	public void testBuildGrammarIfValidRejectsMissingStartRules() {
		NonTerminal start = new NonTerminal("S");
		NonTerminal other = new NonTerminal("A");
		Rule otherRule = new Rule(other, new CharTerminal('.'));
		RuleMaskCodec codec = new RuleMaskCodec(new Rule[] { otherRule }, start);

		SecondaryStructureGrammar grammar = codec.buildGrammarIfValid(new boolean[] { true });
		assertNull(grammar);
	}

	/**
	 * Verifies buildGrammarIfValid creates a grammar when the mask includes the start rule.
	 */
	@Test
	public void testBuildGrammarIfValidCreatesGrammar() {
		NonTerminal start = new NonTerminal("S");
		Rule rDot = new Rule(start, new CharTerminal('.'));
		Rule rOpen = new Rule(start, new CharTerminal('('));
		RuleMaskCodec codec = new RuleMaskCodec(new Rule[] { rDot, rOpen }, start);

		SecondaryStructureGrammar grammar = codec.buildGrammarIfValid(new boolean[] { true, false });
		assertNotNull(grammar);
		assertEquals(1, grammar.size());
	}
}
