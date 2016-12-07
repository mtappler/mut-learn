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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import at.tugraz.mutation_equiv.MutationTestCase;
import at.tugraz.mutation_equiv.equiv_check.EquivalenceChecker;
import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Symbol;

/**
 * Abstract base class for mutation-based selectors (incl. the test-case generator 
 * <code>NonProbMutationSelector</code>). 
 * 
 * It implements basic functionality for mutation testing such as
 *  * evaluation of mutation coverage of mutants and
 *  * the generation of tests for killing alive mutants
 * 
 * The latter, however, has rarely been used in the evaluation. 
 * 
 * Mutation analysis, as one of the most computationally-intensive parts, has been optimised
 * taking the specifics of this project into account. Hence, changes should be made with care
 * to not break anything. 
 * 
 * @author Martin Tappler
 *
 */
public abstract class AbstractMutationTestSelector extends TestSelector<MutationTestCase> {


	protected Map<Integer,MealyMachine<Object, Symbol, Object, String>> mutantsWithIndexes = null;
	protected EquivalenceChecker equivChecker = null;
	protected Alphabet<Symbol> inputAlphabet = null;
	protected Map<Object,List<Integer>> transToDefinitelyKilled = null;
	protected Map<Object,List<Pair<Integer,Optional<List<Symbol>>>>> transToMaybeKilled = null;
	public AbstractMutationTestSelector(int testSuiteSize, Alphabet<Symbol> inputAlphabet) {
		super(testSuiteSize);
		this.equivChecker = new EquivalenceChecker(inputAlphabet);
		this.inputAlphabet = inputAlphabet;
	}
	@SuppressWarnings("unchecked")
	@Override
	public void setCurrentMachines(List<MutantProducer> mutants,
			MealyMachine<?, Symbol, ?, String> hypothesis, List<List<Symbol>> executedTests) {
		super.setCurrentMachines(mutants, hypothesis,executedTests);
		mutantsWithIndexes = new HashMap<>();

		transToDefinitelyKilled = new HashMap<>();
		transToMaybeKilled = new HashMap<>();
		
		for(MutantProducer newMutant : mutants){
			mutantsWithIndexes.put(newMutant.getId(), 
					(MealyMachine<Object,Symbol,Object,String>)newMutant.get());
			if(newMutant.getCritTrans().isDefinitelyKilled()){
				addToTransMap(transToDefinitelyKilled,newMutant.getCritTrans().getCriticalTrans(),newMutant.getId());
			} else{
				addToTransMap(transToMaybeKilled,
						newMutant.getCritTrans().getCriticalTrans(),
						new ImmutablePair<>(newMutant.getId(),newMutant.getCritTrans().getDefKillingSucc()));
			}
		}
		if(!executedTests.isEmpty())
			System.out.println("Before checking " + executedTests.size() 
							 + " executed tests we have " + mutantsWithIndexes.size() + " mutants.");
		for(List<Symbol> execTest : executedTests){
			Triple<Word<String>, Set<Pair<Integer,Object>>, Set<Integer>> killInfo = 
					computeOutputAndKillInfo((MealyMachine<Object, Symbol, Object, String>) hypothesis, execTest);
			for(Pair<Integer,Object> defKilled : killInfo.getMiddle()){
				mutantsWithIndexes.remove(defKilled.getLeft());
				transToDefinitelyKilled.get(defKilled.getRight()).remove(defKilled.getLeft());
			}
		}

		if(!executedTests.isEmpty())
			System.out.println("After checking executed tests we have " + mutantsWithIndexes.size() + " mutants.");
	}
	private <T>void addToTransMap(Map<Object, List<T>> transToKilledInfo, Object trans, T id) {
		
		if(transToKilledInfo.containsKey(trans)){
			transToKilledInfo.get(trans).add(id);
		}
		else {
			List<T> newSubList = new ArrayList<>();
			newSubList.add(id);
			transToKilledInfo.put(trans, newSubList);
		}
	}
	protected List<MutationTestCase> killMutants(
			List<Pair<Integer, MealyMachine<Object, Symbol, Object, String>>> aliveMutants,
			Set<Integer> aliveMutantIndexes) {
		List<MutationTestCase> killTcs = new ArrayList<>();
		while(!aliveMutants.isEmpty()){
			Pair<Integer, MealyMachine<Object, Symbol, Object, String>> mutant = aliveMutants.remove(0);
			Optional<List<Symbol>> traceToKill = equivChecker.killMutant(
					mutant.getRight(),
					hypothesis, new ArrayList<>());
			traceToKill.ifPresent(actualTrace -> {
				aliveMutantIndexes.remove(mutant.getLeft());
				Set<Integer> killedOtherMutantIndexes = 
						aliveMutants.stream().filter(mutPair -> 
				!mutPair.getRight().computeOutput(actualTrace).equals(hypothesis.computeOutput(actualTrace)))
						.map(mutPair -> mutPair.getLeft()).collect(Collectors.toSet());
				aliveMutantIndexes.removeAll(killedOtherMutantIndexes);
				aliveMutants.removeIf(mPair -> killedOtherMutantIndexes.contains(mPair.getLeft()));
				// I don't want to bother calculating a score
				killTcs.add( new MutationTestCase(0.0, actualTrace));
			});
		}
		return killTcs;
	}
	@Override
	public List<MutationTestCase> evaluate(List<List<Symbol>> tests) {
		Stream<MutationTestCase> mutationTests = tests.parallelStream().map(this::evaluateMutationTest);
		List<MutationTestCase> result = mutationTests.collect(Collectors.toList());
		return result;
	}
	private boolean doesTestKill(MealyMachine<Object, Symbol, Object, String> mutant, List<Symbol> test, Word<String> hypOutput) {
		Object currentState = mutant.getInitialState();
		for(int i = 0; i < test.size(); i ++){
			Symbol currInput = test.get(i);
			Object transition = mutant.getTransition(currentState, currInput);
			String mutOutput = mutant.getTransitionOutput(transition);// mutant.getOutput(currentState, currInput);
			String currHypOutput = hypOutput.getSymbol(i);
			currentState = mutant.getSuccessor(transition);
			if(!mutOutput.equals(currHypOutput)){
				return true;
			}
		}
		return false;
	}
	
	private MutationTestCase evaluateMutationTest(List<Symbol> test) {
		Triple<Word<String>,Set<Pair<Integer,Object>>,Set<Integer>> hypOutputAndKillInfo = 
				computeOutputAndKillInfo(hypothesis,test);
		int nrKilled = 0;
		ArrayList<Integer> killedMutants = new ArrayList<>();
		for(Pair<Integer,Object> defKilled : hypOutputAndKillInfo.getMiddle()){
			killedMutants.add(defKilled.getLeft());
		}
		// for debugging/"testing"
//		for(int defKilledIndex : hypOutputAndKillInfo.getMiddle()){
//			MealyMachine<Object, Symbol, Object, String> mut = mutantsWithIndexes.get(defKilledIndex);
//			if(!doesTestKill(mut,test,hypOutputAndKillInfo.getLeft())){
//				doesTestKill(mut,test,hypOutputAndKillInfo.getLeft());
//				if(!equivChecker.killMutant(mut, hypothesis, Collections.emptyList()).isPresent()){
//					System.out.println("There's an equivalent mutant");
//				}
//				else{
//					System.out.println("There's a bug");
//					System.out.println(equivChecker.killMutant(mut, hypothesis, Collections.emptyList()));
//				}
//			}
//		}
		for(int maybeKilledIndex : hypOutputAndKillInfo.getRight()){
			MealyMachine<Object, Symbol, Object, String> mut = mutantsWithIndexes.get(maybeKilledIndex);
			if(doesTestKill(mut,test,hypOutputAndKillInfo.getLeft())){
				killedMutants.add(maybeKilledIndex);
			}
		}
		return new MutationTestCase( (double) nrKilled / mutantsWithIndexes.size(),test,
				killedMutants);
	}
	private Triple<Word<String>, Set<Pair<Integer,Object>>, Set<Integer>> computeOutputAndKillInfo(
			MealyMachine<Object, Symbol, Object, String> hypothesis, List<Symbol> test) {
		Object currentState = hypothesis.getInitialState();
		WordBuilder<String> output = new WordBuilder<>();
		Set<Pair<Integer,Object>> defKilled = new LinkedHashSet<>(); // LinkedHashSet for fast iteration
		Set<Integer> defKilledIndexes = new HashSet<>(); 
		Set<Integer> maybeKilled = new LinkedHashSet<Integer>();
		HashMap<Integer,Pair<Object,LinkedList<Symbol>>> activeMaybeKilled = new HashMap<>();
		
		for(int i = 0; i < test.size(); i ++ ){
			Symbol s = test.get(i);
			Set<Integer> activeToRemove = new HashSet<>();
			for(Entry<Integer, Pair<Object,LinkedList<Symbol>>> entry : activeMaybeKilled.entrySet()){
				if(entry.getValue().getRight().peekFirst().equals(s)){
					entry.getValue().getRight().remove();
				} else {
					activeToRemove.add(entry.getKey());
				}
				if(entry.getValue().getRight().isEmpty()){
					activeToRemove.add(entry.getKey());
					defKilled.add(new ImmutablePair<>(entry.getKey(), entry.getValue().getLeft()));
					defKilledIndexes.add(entry.getKey());
				}
			}
			Object trans = hypothesis.getTransition(currentState, s);
			List<Integer> currDefKilled = transToDefinitelyKilled.get(trans);
			if(currDefKilled != null){
				currDefKilled.forEach(currDefSingle -> {
					defKilled.add(new ImmutablePair<>(currDefSingle, trans));
					defKilledIndexes.add(currDefSingle);
				});
			}
			List<Pair<Integer,Optional<List<Symbol>>>> currMaybeKilled = transToMaybeKilled.get(trans);
			if(currMaybeKilled != null){
				for(Pair<Integer, Optional<List<Symbol>>> mKilled : currMaybeKilled){
					if(defKilledIndexes.contains(mKilled.getLeft())){
						continue;
					}
					else if(mKilled.getRight().isPresent()){
						if(!activeMaybeKilled.containsKey(mKilled.getLeft())){
							activeMaybeKilled.put(mKilled.getLeft(), new ImmutablePair<>(trans, 
									new LinkedList<>(mKilled.getRight().get())));
						}
					} else {
						maybeKilled.add(mKilled.getLeft());
					}
				}
			}

			activeToRemove.forEach(activeMaybeKilled::remove);
			output.add(hypothesis.getTransitionOutput(trans));
			currentState = hypothesis.getSuccessor(trans);
		}
		return new ImmutableTriple<>(output.toWord(), defKilled, maybeKilled);
	}

}
