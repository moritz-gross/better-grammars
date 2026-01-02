package compression.grammargenerator.localsearch;

import compression.grammar.Terminal;
import compression.parser.SRFParser;

import java.util.List;

/**
 * Shared parsability check for datasets.
 */
final class Utils {
	private Utils() {
		// utility
	}

	static boolean passesDataset(final SRFParser<Character> parser, final List<List<Terminal<Character>>> words) {
		for (List<Terminal<Character>> word : words) {
			if (!parser.parsable(word)) {
				return false;
			}
		}
		return true;
	}
}
