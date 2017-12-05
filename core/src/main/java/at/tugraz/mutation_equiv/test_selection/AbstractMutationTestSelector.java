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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
 * Abstract base class for mutation-based selectors (incl. the test-case
 * generator <code>NonProbMutationSelector</code>).
 * 
 * It implements basic functionality for mutation testing such as * evaluation
 * of mutation coverage of mutants and * the generation of tests for killing
 * alive mutants
 * 
 * The latter, however, has rarely been used in the evaluation.
 * 
 * Mutation analysis, as one of the most computationally-intensive parts, has
 * been optimised taking the specifics of this project into account. Hence,
 * changes should be made with care to not break anything.
 * 
 * @author Martin Tappler
 *
 */
public abstract class AbstractMutationTestSelector extends TestSelector<MutationTestCase> {

	private static class IdGen implements Iterator<Integer> {

		int id = 0;

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public Integer next() {
			return id++;
		}

	}

	private static class State {

		int hash = 1;

		@Override
		public String toString() {
			return "State [id=" + id + ", useSingleTon=" + useSingleTon + "]";
		}

		int id;
		Map<Integer, List<Transition>> transitions;

		private boolean useSingleTon;

		public State(int id) {
			this.id = id;
			transitions = new HashMap<>();
		}

		public State(int id, boolean useSingletonMap) {
			this.id = id;
			this.useSingleTon = true;
			transitions = Collections.emptyMap();
		}

		@Override
		public int hashCode() {
			if (hash != 1)
				return hash;
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			hash = result;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			State other = (State) obj;
			if (id != other.id)
				return false;
			return true;
		}

		public void addTransition(Symbol input, State target, List<Integer> killed) {
			addTransIntern(new Transition(this, input, target, killed));
		}

		private Transition addTransIntern(Transition trans) {
			if (useSingleTon) {
				if (transitions.equals(Collections.emptyMap())) {
					transitions = Collections.singletonMap(trans.symbol.getId(), Collections.singletonList(trans));
					return trans;
				} else
					throw new Error("Implementation error");
			}
			List<Transition> transList = null;
			if (!transitions.containsKey(trans.symbol.getId())) {
				transList = new ArrayList<>();
				transitions.put(trans.symbol.getId(), transList);
			} else
				transList = transitions.get(trans.symbol.getId());
			transList.add(trans);
			return trans;
		}

		public Transition addTransition(Symbol input, State state) {
			return addTransIntern(new Transition(this, input, state));
		}

		public Transition addTransition(Symbol input, State target, Integer killedMut) {
			List<Integer> killedMutants = Collections.singletonList(killedMut);
			// new ArrayList<>(1);
			// killedMutants.add(killedMut);
			return addTransIntern(new Transition(this, input, target, killedMutants));
		}

		public void addTransition(Symbol input, State state, boolean isMut) {
			Transition trans = addTransition(input, state);
			trans.mutant = isMut;
		}

		public void addTransition(Symbol input, State target, Integer killedMut, boolean isMut) {
			Transition trans = addTransition(input, target, killedMut);
			trans.mutant = isMut;
		}
	}

	private static class Transition {
		State source;
		Symbol symbol;
		@Override
		public String toString() {
			return "Transition [source=" + source + ", symbol=" + symbol + ", target=" + target + ", mutant=" + mutant
					+ ", active=" + active + ", killedMutants=" + killedMutants + "]";
		}

		int hash = 1;

		@Override
		public int hashCode() {
			if (hash != 1)
				return hash;
			final int prime = 31;
			int result = 1;
			result = prime * result + ((target == null) ? 0 : target.hashCode());
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
			result *= prime;
			hash = result;
			// System.out.println(hash);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Transition other = (Transition) obj;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			if (symbol == null) {
				if (other.symbol != null)
					return false;
			} else if (!symbol.equals(other.symbol))
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			return true;
		}

		State target;
		boolean mutant = false;
		boolean active = false;
		List<Integer> killedMutants; // TODO maybe change to single integer as
										// there may only be one currently

		public Transition(State source, Symbol symbol, State target, List<Integer> killed) {
			super();
			this.source = source;
			this.target = target;
			this.symbol = symbol;
			killedMutants = killed;
		}

		public Transition(State source, Symbol input, State target) {
			this.source = source;
			this.target = target;
			this.symbol = input;
			killedMutants = Collections.emptyList();
		}
	}

	private static class MutationNFA {
		State init;

		public MutationNFA(State init) {
			super();
			this.init = init;
		}
	}

	protected Map<Integer, MutantProducer> mutantsWithIndexes = null;
	protected EquivalenceChecker equivChecker = null;
	protected Alphabet<Symbol> inputAlphabet = null;
	protected Map<Object, List<Integer>> transToDefinitelyKilled = null;
	protected Map<Object, List<Pair<Integer, Optional<List<Symbol>>>>> transToMaybeKilled = null;
	protected MutationNFA mutNFA = null;
	protected boolean useNfaBasedOptimization = false;
	private boolean nfaBasedEvalPossible = false;

	public AbstractMutationTestSelector(int testSuiteSize, Alphabet<Symbol> inputAlphabet) {
		super(testSuiteSize);
		this.equivChecker = new EquivalenceChecker(inputAlphabet);
		this.inputAlphabet = inputAlphabet;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setCurrentMachines(List<MutantProducer> mutants, MealyMachine<?, Symbol, ?, String> hypothesis,
			List<List<Symbol>> executedTests) {
		super.setCurrentMachines(mutants, hypothesis, executedTests);
		mutantsWithIndexes = new HashMap<>();

		transToDefinitelyKilled = new HashMap<>();
		transToMaybeKilled = new HashMap<>();
		mutNFA = null;

		for (MutantProducer newMutant : mutants) {
			mutantsWithIndexes.put(newMutant.getId(), newMutant);
			if (newMutant.getCritTrans().isDefinitelyKilled()) {
				addToTransMap(transToDefinitelyKilled, newMutant.getCritTrans().getCriticalTrans(), newMutant.getId());
			} else {
				addToTransMap(transToMaybeKilled, newMutant.getCritTrans().getCriticalTrans(),
						new ImmutablePair<>(newMutant.getId(), newMutant.getCritTrans().getDefKillingSucc()));
			}
		}
		if (!executedTests.isEmpty())
			System.out.println("Before checking " + executedTests.size() + " executed tests we have "
					+ mutantsWithIndexes.size() + " mutants.");
		for (List<Symbol> execTest : executedTests) {
			Triple<Word<String>, Set<Pair<Integer, Object>>, Set<Integer>> killInfo = computeOutputAndKillInfo(
					(MealyMachine<Object, Symbol, Object, String>) hypothesis, execTest);
			for (Pair<Integer, Object> defKilled : killInfo.getMiddle()) {
				mutantsWithIndexes.remove(defKilled.getLeft());
				boolean removed = transToDefinitelyKilled.get(defKilled.getRight()).remove(defKilled.getLeft());
				if (!removed) {
					removed = transToMaybeKilled.get(defKilled.getRight())
							.removeIf(p -> p.getLeft() == defKilled.getLeft());
					if (!removed)
						System.out.println(defKilled.getLeft());
				}
			}

			// this will most certainly never be executed, because we mostly
			// use split-state and change-output mutation which produces mutants
			// that are either
			// found to be killed definitely by computeOutputAndKillInfo, or
			// which are not killed at all
			for (int maybeKilledIndex : killInfo.getRight()) {
				MutantProducer mutProd = mutantsWithIndexes.get(maybeKilledIndex);
				MealyMachine<Object, Symbol, Object, String> mut = mutProd.get();
				if (doesTestKill(mut, execTest, killInfo.getLeft())) {
					mutantsWithIndexes.remove(maybeKilledIndex);
					transToMaybeKilled.get(mutProd.getCritTrans())
							.removeIf(critPair -> critPair.getLeft() == maybeKilledIndex);
				}
			}
		}
		if (useNfaBasedOptimization) {
			nfaBasedEvalPossible = true;
			initMutationNFA();
		} else {
			nfaBasedEvalPossible = false;
		}

		if (!executedTests.isEmpty())
			System.out.println("After checking executed tests we have " + mutantsWithIndexes.size() + " mutants.");
	}

	private void initMutationNFA() {
		IdGen idGen = new IdGen();
		mutNFA = new MutationNFA(new State(idGen.next()));
		LinkedList<Pair<Object, State>> stateSchedule = new LinkedList<>();
		stateSchedule.add(new ImmutablePair<>(hypothesis.getInitialState(), mutNFA.init));
		Map<Object, State> mealyToNFAState = new HashMap<>();
		mealyToNFAState.put(hypothesis.getInitialState(), mutNFA.init);
		Set<Object> visited = new HashSet<>();
		while (!stateSchedule.isEmpty()) {
			Pair<Object, State> currentStatePair = stateSchedule.poll();
			Object currentMealyState = currentStatePair.getLeft();
			State currentNFAState = currentStatePair.getRight();
			visited.add(currentMealyState);
			for (Symbol input : inputAlphabet) {
				Object transition = hypothesis.getTransition(currentMealyState, input);
				List<Pair<Integer, Optional<List<Symbol>>>> maybeKilled = transToMaybeKilled.get(transition);

				if (maybeKilled != null && !maybeKilled.isEmpty()) {
					for (Pair<Integer, Optional<List<Symbol>>> m : maybeKilled) {
						if (!m.getRight().isPresent()) {
							nfaBasedEvalPossible = false;
							return;
						}
						Integer killedMut = m.getLeft();
						addSequence(killedMut, currentNFAState, input, new LinkedList<>(m.getRight().get()), idGen);
					}
				}
				State nextNFAState = null;
				Object mealyTarget = hypothesis.getSuccessor(transition);
				
				if (!mealyToNFAState.containsKey(mealyTarget)) {
					nextNFAState = new State(idGen.next());
					mealyToNFAState.put(mealyTarget, nextNFAState);
					stateSchedule.add(new ImmutablePair<>(mealyTarget, nextNFAState));
				} else {
					nextNFAState = mealyToNFAState.get(mealyTarget);
				}
				List<Integer> defKilled = transToDefinitelyKilled.get(transition);
				if (defKilled != null && !defKilled.isEmpty()) {
					currentNFAState.addTransition(input, nextNFAState, defKilled);
				} else {
					currentNFAState.addTransition(input, nextNFAState);
				}
			}
		}
	}

	private void addSequence(Integer killedMut, State nfaState, Symbol first, LinkedList<Symbol> rest, IdGen idGen) {
		State newNfaState = new State(idGen.next(), true);
		if (!rest.isEmpty()) {
			nfaState.addTransition(first, newNfaState, true);
			addSequence(killedMut, newNfaState, rest.remove(), rest, idGen);
		} else {
			nfaState.addTransition(first, newNfaState, killedMut, true);
		}

	}

	private <T> void addToTransMap(Map<Object, List<T>> transToKilledInfo, Object trans, T id) {

		if (transToKilledInfo.containsKey(trans)) {
			transToKilledInfo.get(trans).add(id);
		} else {
			List<T> newSubList = new ArrayList<>();
			newSubList.add(id);
			transToKilledInfo.put(trans, newSubList);
		}
	}

	protected List<MutationTestCase> killMutants(
			List<Pair<Integer, MealyMachine<Object, Symbol, Object, String>>> aliveMutants,
			Set<Integer> aliveMutantIndexes) {
		List<MutationTestCase> killTcs = new ArrayList<>();
		while (!aliveMutants.isEmpty()) {
			Pair<Integer, MealyMachine<Object, Symbol, Object, String>> mutant = aliveMutants.remove(0);
			Optional<List<Symbol>> traceToKill = equivChecker.killMutant(mutant.getRight(), hypothesis,
					new ArrayList<>());
			traceToKill.ifPresent(actualTrace -> {
				aliveMutantIndexes.remove(mutant.getLeft());
				Set<Integer> killedOtherMutantIndexes = aliveMutants.stream()
						.filter(mutPair -> !mutPair.getRight().computeOutput(actualTrace)
								.equals(hypothesis.computeOutput(actualTrace)))
						.map(mutPair -> mutPair.getLeft()).collect(Collectors.toSet());
				aliveMutantIndexes.removeAll(killedOtherMutantIndexes);
				aliveMutants.removeIf(mPair -> killedOtherMutantIndexes.contains(mPair.getLeft()));
				// I don't want to bother calculating a score
				killTcs.add(new MutationTestCase(0.0, actualTrace));
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

	private boolean doesTestKill(MealyMachine<Object, Symbol, Object, String> mutant, List<Symbol> test,
			Word<String> hypOutput) {
		Object currentState = mutant.getInitialState();
		for (int i = 0; i < test.size(); i++) {
			Symbol currInput = test.get(i);
			Object transition = mutant.getTransition(currentState, currInput);
			String mutOutput = mutant.getTransitionOutput(transition);// mutant.getOutput(currentState,
																		// currInput);
			String currHypOutput = hypOutput.getSymbol(i);
			currentState = mutant.getSuccessor(transition);
			if (!mutOutput.equals(currHypOutput)) {
				return true;
			}
		}
		return false;
	}

	protected MutationTestCase evaluateMutationTest(List<Symbol> test) {

		Set<Integer> killedMutants = new HashSet<>();

		if (nfaBasedEvalPossible)
			findKilledInNFA(test, killedMutants);
		else {
			Triple<Word<String>, Set<Pair<Integer, Object>>, Set<Integer>> hypOutputAndKillInfo = computeOutputAndKillInfo(
					hypothesis, test);
			for (Pair<Integer, Object> defKilled : hypOutputAndKillInfo.getMiddle()) {
				killedMutants.add(defKilled.getLeft());
			}
			for (int maybeKilledIndex : hypOutputAndKillInfo.getRight()) {
				MealyMachine<Object, Symbol, Object, String> mut = mutantsWithIndexes.get(maybeKilledIndex).get();
				if (doesTestKill(mut, test, hypOutputAndKillInfo.getLeft())) {
					killedMutants.add(maybeKilledIndex);
				}
			}
		}

		// for debugging/"testing"
		// for(int defKilledIndex : hypOutputAndKillInfo.getMiddle()){
		// MealyMachine<Object, Symbol, Object, String> mut =
		// mutantsWithIndexes.get(defKilledIndex);
		// if(!doesTestKill(mut,test,hypOutputAndKillInfo.getLeft())){
		// doesTestKill(mut,test,hypOutputAndKillInfo.getLeft());
		// if(!equivChecker.killMutant(mut, hypothesis,
		// Collections.emptyList()).isPresent()){
		// System.out.println("There's an equivalent mutant");
		// }
		// else{
		// System.out.println("There's a bug");
		// System.out.println(equivChecker.killMutant(mut, hypothesis,
		// Collections.emptyList()));
		// }
		// }
		// }

		int nrKilled = killedMutants.size();
		return new MutationTestCase((double) nrKilled / mutantsWithIndexes.size(), test,
				new ArrayList<>(killedMutants));
	}

	private void findKilledInNFA(List<Symbol> test, Set<Integer> killedMutants) {
		List<Pair<State, Transition>> state = new ArrayList<>();
		state.add(new ImmutablePair<>(this.mutNFA.init, null));
		ArrayList<Integer> killedMutantList = new ArrayList<Integer>();
		for (Symbol input : test) {
			state = makeStep(state, input, killedMutantList);
		}
		for(Pair<State,Transition> st : state){
			if(st.getRight() != null)
				st.getRight().active = false;
		}
		killedMutants.addAll(killedMutantList);
	}

	private List<Pair<State, Transition>> makeStep(List<Pair<State, Transition>> state, Symbol input,
			List<Integer> killedMutants) {
		List<Pair<State, Transition>> nextState = new ArrayList<>();
		List<Transition> removeFromActive = new ArrayList<Transition>();
		for (Pair<State, Transition> s : state) {
			makeSingleStep(nextState, s, input, killedMutants, removeFromActive);
		}
		for (Transition t : removeFromActive)
			t.active = false;
		return nextState;
	}

	private void makeSingleStep(List<Pair<State, Transition>> nextState, Pair<State, Transition> s, Symbol input,
			List<Integer> killedMutants, List<Transition> removeFromActive) {
		List<Transition> transitions = s.getLeft().transitions.get(input.getId());
		boolean changed = false;
		if (transitions != null) {
			for (Transition t : transitions) {
				if (t.active)
					continue;
				killedMutants.addAll(t.killedMutants);
				if (t.mutant) {
					if (s.getRight() == null) {
						t.active = true;
						nextState.add(new ImmutablePair<>(t.target, t));
					} else
						nextState.add(new ImmutablePair<>(t.target, s.getRight()));
				} else {
					nextState.add(new ImmutablePair<>(t.target, null));
				}
				changed = true;
			}
		}
		if (!changed) {
			if (s.getRight() != null)
				removeFromActive.add(s.getRight());
		}
	}

	private Triple<Word<String>, Set<Pair<Integer, Object>>, Set<Integer>> computeOutputAndKillInfo(
			MealyMachine<Object, Symbol, Object, String> hypothesis, List<Symbol> test) {
		Object currentState = hypothesis.getInitialState();
		WordBuilder<String> output = new WordBuilder<>();
		List<Pair<Integer, Object>> defKilled = new ArrayList<>(); // LinkedHashSet
																	// for
																	// fast
																	// iteration
		Set<Integer> defKilledIndexes = new HashSet<>();
		List<Integer> maybeKilled = new ArrayList<Integer>();
		HashMap<Integer, Pair<Object, LinkedList<Symbol>>> activeMaybeKilled = new HashMap<>();

		for (int i = 0; i < test.size(); i++) {
			Symbol s = test.get(i);
			LinkedList<Integer> activeToRemove = new LinkedList<>();
			for (Entry<Integer, Pair<Object, LinkedList<Symbol>>> entry : activeMaybeKilled.entrySet()) {
				if (entry.getValue().getRight().peekFirst().equals(s)) {
					entry.getValue().getRight().remove();
				} else {
					activeToRemove.add(entry.getKey());
				}
				if (entry.getValue().getRight().isEmpty()) {
					activeToRemove.add(entry.getKey());
					defKilled.add(new ImmutablePair<>(entry.getKey(), entry.getValue().getLeft()));
					defKilledIndexes.add(entry.getKey());
				}
			}
			Object trans = hypothesis.getTransition(currentState, s);
			List<Integer> currDefKilled = transToDefinitelyKilled.get(trans);
			if (currDefKilled != null) {
				currDefKilled.forEach(currDefSingle -> {
					defKilled.add(new ImmutablePair<>(currDefSingle, trans));
					defKilledIndexes.add(currDefSingle);
				});
			}
			List<Pair<Integer, Optional<List<Symbol>>>> currMaybeKilled = transToMaybeKilled.get(trans);
			if (currMaybeKilled != null) {
				for (Pair<Integer, Optional<List<Symbol>>> mKilled : currMaybeKilled) {
					if (defKilledIndexes.contains(mKilled.getLeft())) {
						continue;
					} else if (mKilled.getRight().isPresent()) {
						if (!activeMaybeKilled.containsKey(mKilled.getLeft())) {
							activeMaybeKilled.put(mKilled.getLeft(),
									new ImmutablePair<>(trans, new LinkedList<>(mKilled.getRight().get())));
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
		return new ImmutableTriple<>(output.toWord(), new LinkedHashSet<>(defKilled), new LinkedHashSet<>(maybeKilled));
	}

	public boolean isUseNfaBasedOptimization() {
		return useNfaBasedOptimization;
	}

	public void setUseNfaBasedOptimization(boolean useNfaBasedOptimization) {
		this.useNfaBasedOptimization = useNfaBasedOptimization;
	}

}
