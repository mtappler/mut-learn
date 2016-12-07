/*******************************************************************************
 * mut-learn
 * Copyright (C) 2016 TU Graz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package at.tugraz.mutation_equiv.test_selection;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import at.tugraz.mutation_equiv.MutationTestCase;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Symbol;

/**
 * Mutation-based test-case selection is discussed in the paper.
 * 
 * Consists of three steps:
 *  * Greedily select tests such that all mutant are covered
 *  * optionally generate tests killing mutants which have not yet been covered
 *  * selects tests which individually achieve high coverage
 * 
 * @author Martin Tappler
 *
 */
public class MutationSuiteBasedSelector extends AbstractMutationTestSelector {

	public static class LazyMutationBasedSelector implements Iterator<List<Symbol>> {

		private int testSuiteSize = 0;
		private int providedTests = 0;
		private Set<Integer> mutantIndexes;
		private List<MutationTestCase> tests;
		private boolean noMoreKillsPossible = false;
		private Map<Integer, MealyMachine<Object, Symbol, Object, String>> mutantsWithIndexes;
		private boolean killAliveMutants;
		private Iterator<MutationTestCase> remainingIterator = null;
		private MutationSuiteBasedSelector sel = null;
		private List<MutationTestCase> additionalTestsForAlive = null;

		// don't instantiate from "outside"
		private LazyMutationBasedSelector(int testSuiteSize, 
				Map<Integer, MealyMachine<Object, Symbol, Object, String>> mutantsWithIndexes, 
				List<MutationTestCase> tests, boolean killAliveMutants, MutationSuiteBasedSelector sel){
			this.testSuiteSize = testSuiteSize;
			this.mutantIndexes = new HashSet<>(mutantsWithIndexes.keySet());
			this.mutantsWithIndexes = mutantsWithIndexes;
			this.tests = tests;
			// TODO test kill alive mutants (old version was tested)
			this.killAliveMutants = killAliveMutants;
			this.sel  = sel;
		}
		@Override
		public boolean hasNext() {
			return providedTests < testSuiteSize;
		}

		@Override
		public List<Symbol> next() {
			if(!noMoreKillsPossible && !mutantIndexes.isEmpty() && providedTests < testSuiteSize){
				MutationTestCase bestTest = findTestWithMostKills(mutantIndexes,tests);
				if(bestTest == null){
					noMoreKillsPossible = true;
					System.out.println("Combined mutation score of " + providedTests + 
							": " + (double)(mutantsWithIndexes.size() - mutantIndexes.size()) 
							/ mutantsWithIndexes.size());
				}
				else {
					providedTests ++;
					tests.remove(bestTest);
					return bestTest.getTrace();
				}
			}
			if(!mutantIndexes.isEmpty() && killAliveMutants){
				if(additionalTestsForAlive == null){
						List<Pair<Integer, MealyMachine<Object, Symbol, Object, String>>> aliveMutants = 
								mutantsWithIndexes.entrySet().stream()
						.filter(mwi -> mutantIndexes.contains(mwi.getKey()))
						.map(entry -> new ImmutablePair<>(entry.getKey(), entry.getValue()))
						.collect(Collectors.toList());

						additionalTestsForAlive = sel.killMutants(aliveMutants,mutantIndexes);
						System.out.println("Have " + additionalTestsForAlive.size() + " additional tests");
				}

				providedTests ++;
				return additionalTestsForAlive.remove(0).getTrace();
			}
			if(providedTests < testSuiteSize){
				if(remainingIterator == null){
					remainingIterator = tests.stream()
					.sorted((o1, o2) -> o1.score < o2.score ? 1 : o1.score > o2.score ? -1 : 0)
					.limit(testSuiteSize - providedTests).iterator();
				}
				providedTests ++;
				return remainingIterator.next().getTrace();
			}
			
			throw new NoSuchElementException();
		}
		

		private MutationTestCase findTestWithMostKills(Set<Integer> mutantIndexes, List<MutationTestCase> mutationTests) {
			
			int highestKillCount = 0;
			MutationTestCase bestTest = null;
			for(MutationTestCase t : mutationTests){
				int nrKilled = 0;
				for(int killed : t.killedMutants){
					if(mutantIndexes.contains(killed))
						nrKilled ++;
				}
				if(nrKilled > highestKillCount){
					highestKillCount = nrKilled;
					bestTest = t;
				}
			}
			if(bestTest != null){
				for(int i : bestTest.killedMutants)
					mutantIndexes.remove(i);
			}
			return bestTest;
		}
		
	}

	public MutationSuiteBasedSelector(int testSuiteSize, Alphabet<Symbol> inputAlphabet, boolean killAliveMutants) {
		super(testSuiteSize,inputAlphabet);
		this.setKillAliveMutants(killAliveMutants);
	}

	private boolean killAliveMutants = false;
	
	@Override
	public Iterator<List<Symbol>> select(List<MutationTestCase> tests) {
		int effectiveTestSuiteSize = testSuiteSize;
		if(!initialSizes.isEmpty()){
			effectiveTestSuiteSize = initialSizes.remove(0);
		}
		if(!mutantsWithIndexes.isEmpty()){
			LazyMutationBasedSelector actualSelector = 
					new LazyMutationBasedSelector(effectiveTestSuiteSize,mutantsWithIndexes,tests, 
							killAliveMutants, this);		
			return actualSelector;
		} else {
			return tests.stream().map(MutationTestCase::getTrace).limit(effectiveTestSuiteSize).iterator();
		}
	}
	





	@Override
	public String description() {
		return "mutation-suite-based" + (killAliveMutants ? "-ka" : "");
	}



	public boolean isKillAliveMutants() {
		return killAliveMutants;
	}



	public void setKillAliveMutants(boolean killAliveMutants) {
		this.killAliveMutants = killAliveMutants;
	}

}
