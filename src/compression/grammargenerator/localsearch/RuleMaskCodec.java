package compression.grammargenerator.localsearch;

import compression.grammar.NonTerminal;
import compression.grammar.Rule;
import compression.grammar.SecondaryStructureGrammar;
import compression.util.MyMultimap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Encodes and decodes rule masks to and from grammars.
 */
final class RuleMaskCodec {
	private final Rule[] allPossibleRules;
	private final NonTerminal startSymbol;
	private final Map<Rule, Integer> ruleToIndex;

	RuleMaskCodec(final Rule[] allPossibleRules, final NonTerminal startSymbol) {
		this.allPossibleRules = allPossibleRules;
		this.startSymbol = startSymbol;
		this.ruleToIndex = new HashMap<>(allPossibleRules.length);
		for (int i = 0; i < allPossibleRules.length; i++) {
			ruleToIndex.put(allPossibleRules[i], i);
		}
	}

	boolean[] toMask(final SecondaryStructureGrammar grammar) {
		boolean[] mask = new boolean[allPossibleRules.length];
		for (Rule rule : grammar.getAllRules()) {
			Integer idx = ruleToIndex.get(rule);
			if (idx != null) {
				mask[idx] = true;
			}
		}
		return mask;
	}
    boolean[] toMask(final SecondaryStructureGrammar grammar, Random random) {
        boolean[] mask = new boolean[allPossibleRules.length];
        for (int i = 0; i < allPossibleRules.length; i++) {
            double probability = random.nextDouble(1);
            if(probability < 0.30) mask[i] = true;
            else mask[i] = false;
        }
        return mask;
    }

	SecondaryStructureGrammar buildGrammarIfValid(final boolean[] mask) {
		MyMultimap<NonTerminal, Rule> rules = new MyMultimap<>();
		for (int i = 0; i < mask.length; i++) {
			if (mask[i]) {
				Rule rule = allPossibleRules[i];
				rules.put(rule.left, rule);
			}
		}
		try {
			String name = "LocalSearch_" + Arrays.toString(mask);
			return new SecondaryStructureGrammar(name, startSymbol, rules);
		} catch (IllegalArgumentException e) {
			return null; // invalid grammar
		}
	}
}
