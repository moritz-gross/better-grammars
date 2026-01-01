package compression.grammargenerator;

import compression.data.CachedDataset;
import compression.data.Dataset;
import compression.data.FolderBasedDataset;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class LocalSearchExplorerTest {

	private LocalSearchExplorer newExplorer(int nNonterminals, long seed, Dataset objective, Dataset parsable, boolean withNCR, int objectiveLimit)
			throws Exception {
		Constructor<LocalSearchExplorer> ctor = LocalSearchExplorer.class.getDeclaredConstructor(
				int.class, long.class, Dataset.class, Dataset.class, boolean.class, int.class);
		ctor.setAccessible(true);
		return ctor.newInstance(nNonterminals, seed, objective, parsable, withNCR, objectiveLimit);
	}

	@Test
	public void runSingleSearchProducesFiniteScore() throws Exception {
		Dataset objective = new CachedDataset(new FolderBasedDataset("minimal-parsable"));
		LocalSearchExplorer explorer = newExplorer(3, 123456L, objective, objective, false, -1);

		Method runSingleSearch = LocalSearchExplorer.class.getDeclaredMethod(
				"runSingleSearch", int.class, int.class, int.class, int.class, int.class);
		runSingleSearch.setAccessible(true);
		Object state = runSingleSearch.invoke(explorer, 10, 500, 8, 20, 30);

		Method bitsPerBase = state.getClass().getDeclaredMethod("bitsPerBase");
		double bits = (double) bitsPerBase.invoke(state);
		Method grammarMethod = state.getClass().getDeclaredMethod("grammar");
		Object grammar = grammarMethod.invoke(state);
		Method sizeMethod = grammar.getClass().getMethod("size");
		int size = (int) sizeMethod.invoke(grammar);

		assertTrue("bits per base should be finite", Double.isFinite(bits));
		assertTrue("grammar size should be positive", size > 0);
	}

	@Test
	public void enumerateMovesIncludesAddRemoveSwap() throws Exception {
		Dataset objective = new CachedDataset(new FolderBasedDataset("minimal-parsable"));
		LocalSearchExplorer explorer = newExplorer(2, 9999L, objective, objective, false, -1);

		Field allRulesField = LocalSearchExplorer.class.getSuperclass().getDeclaredField("allPossibleRules");
		allRulesField.setAccessible(true);
		Object[] allRules = (Object[]) allRulesField.get(explorer);
		boolean[] mask = new boolean[allRules.length];
		// mark a couple present to force remove/swap
		if (mask.length > 0) mask[0] = true;
		if (mask.length > 1) mask[1] = true;

		Method enumerateMoves = LocalSearchExplorer.class.getDeclaredMethod("enumerateMoves", boolean[].class, int.class);
		enumerateMoves.setAccessible(true);
		@SuppressWarnings("unchecked")
		List<Object> moves = (List<Object>) enumerateMoves.invoke(explorer, mask, 5);

		boolean hasAdd = false, hasRemove = false, hasSwap = false;
		for (Object move : moves) {
			Object type = move.getClass().getDeclaredMethod("type").invoke(move);
			String t = type.toString();
			if (t.equals("ADD")) hasAdd = true;
			if (t.equals("REMOVE")) hasRemove = true;
			if (t.equals("SWAP")) hasSwap = true;
		}

		assertTrue("should have add move", hasAdd);
		assertTrue("should have remove move", hasRemove);
		assertTrue("should have swap move", hasSwap);
	}
}
