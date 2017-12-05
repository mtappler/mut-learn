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
package at.tugraz.mutation_equiv.mutation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import at.tugraz.mutation_equiv.mutation.sampling.CompoundSample;
import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import at.tugraz.mutation_equiv.mutation.sampling.MutantSample;
import at.tugraz.mutation_equiv.util.Cons;
import at.tugraz.mutation_equiv.util.Nil;
import at.tugraz.mutation_equiv.util.Trace;
import net.automatalib.automata.transout.impl.FastMealy;
import net.automatalib.automata.transout.impl.FastMealyState;
import net.automatalib.automata.transout.impl.MealyTransition;
import net.automatalib.commons.util.collections.CollectionsUtil;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Symbol;

/**
 * The split state operator.
 * 
 * Basically splits a state (as you can tell from the name) creating a new state
 * and adds transitions such that the new state behaves exactly like the
 * existing except for one sequence of inputs. This should mimic the effect of
 * counterexample processing in learning.
 * 
 * More detailed: The operator searches for each state q up to
 * <code>accSeqBound</code> sequences leading to q and creates mutants for those
 * state. The state q is then split for each sequence u, by creating a new state
 * q' and redirecting u to q'. A distinguishing sequence v of
 * <code>mutationDepth + 1</code> is then introduced such q and q' behave the
 * same for all sequences except v. To accomplished that
 * <code>mutationDepth</code> additional states introduced (essentially
 * splitting successors of q) and the output of one transition leaving the last
 * newly introduced state is mutated.
 * 
 * In some cases pre-state of q have to be split as well to maintain
 * determinism. The flag <code>allowEqualPreStateAndSymbol</code> controls
 * whether this is done or the corresponding access sequence is ignored.
 * 
 * @author Martin Tappler
 *
 */
public class SplitStateOperator extends MutationOperator {

	private boolean useGlobalVisited = true;

	// a simple but necessary form of sampling, as there are simply too many
	// (loop-free) access sequences
	private int accSeqBound = 10;

	public int getAccSeqBound() {
		return accSeqBound;
	}

	public void setAccSeqBound(int accSeqBound) {
		this.accSeqBound = accSeqBound;
	}

	private int mutationDepth = 1;
	private boolean allowDiffInLastAccSymbol = true;

	private boolean allowEqualPreStateAndSymbol = true;

	public boolean isAllowDiffInLastAccSymbol() {
		return allowDiffInLastAccSymbol;
	}

	public void setAllowDiffInLastAccSymbol(boolean allowDiffInLastAccSymbol) {
		this.allowDiffInLastAccSymbol = allowDiffInLastAccSymbol;
	}

	public boolean isAllowEqualPreStateAndSymbol() {
		return allowEqualPreStateAndSymbol;
	}

	public void setAllowEqualPreStateAndSymbol(boolean allowEqualPreStateAndSymbol) {
		this.allowEqualPreStateAndSymbol = allowEqualPreStateAndSymbol;
	}

	// if this is true and one of the acc. sequences u in the pair (u,u') is a
	// prefix of the other,
	// then u will be mutated, otherwise it won't
	// the distinction makes sense as strictly speaking u should not mutated, as
	// it would affect u' as well
	// and thereby it would not create the intended split-state mutant, but
	// still it could create valuable mutants
	// so we could also set it to true ( setting it to false might lead to
	// imbalanced
	// coverage of the model, if some states,
	// e.g., contain a lot of deadlocks)
	private boolean mutateAlsoPrefix = false;

	private Map<FastMealyState<String>, List<Word<Symbol>>> accSeqForStates = null;

	public SplitStateOperator(Alphabet<Symbol> inputAlphabet) {
		super(inputAlphabet);
	}

	public SplitStateOperator(Alphabet<Symbol> inputAlphabet, int mutationDepth) {
		super(inputAlphabet);
		this.mutationDepth = mutationDepth;
	}

	@Override
	public void setAccSequences(Map<FastMealyState<String>, List<Word<Symbol>>> accessSeqForStates) {
		this.accSeqForStates = accessSeqForStates;
	}

	@Override
	protected MutantSample createMutantsAbstr(FastMealy<Symbol, String> machine) {
		Collection<FastMealyState<String>> allStates = machine.getStates();
		CompoundSample fullSample = new CompoundSample("operator:split-state-" + mutationDepth);
		for (FastMealyState<String> s : allStates) {
			List<Word<Symbol>> accSeqs = null;
			if (accSeqForStates != null)
				accSeqs = accSeqForStates.get(s);
			else
				accSeqs = findAccSequences(machine, s);
			Set<Pair<Word<Symbol>, Word<Symbol>>> seqPairs = allUnEqualPairs(accSeqs);
			if (!allowEqualPreStateAndSymbol)
				seqPairs = seqPairs.stream().filter(aPair -> checkIfDiffInPreState(aPair, machine))
						.collect(Collectors.toSet());

			MutantSample mutantsForCurrState = splitAndAddTransitions(seqPairs, machine);
			fullSample.addChild(mutantsForCurrState);
		}

		if ("true".equals(System.getProperty("ranked_random.debug")))
			System.out.println("Split state created " + fullSample.getMutants().size() + " mutants in total");
		return fullSample;
	}

	private MutantSample splitAndAddTransitions(Set<Pair<Word<Symbol>, Word<Symbol>>> seqPairs,
			FastMealy<Symbol, String> machine) {
		Set<Triple<FastMealyState<String>, FastMealyState<String>, Symbol>> processedReachedPreSymbolTriple = new HashSet<>();
		CompoundSample mutantsForState = new CompoundSample("split-state-state");
		seqPairs.stream().map(pair -> splitAndAddTransitionsPair(pair, machine, processedReachedPreSymbolTriple))
				.forEach(mutantsForState::addChild);
		return mutantsForState;
	}

	private MutantSample splitAndAddTransitionsPair(Pair<Word<Symbol>, Word<Symbol>> pair,
			FastMealy<Symbol, String> machine,
			Set<Triple<FastMealyState<String>, FastMealyState<String>, Symbol>> processedReachedPreSymbolTriple) {

		Word<Symbol> leftAccSeq = pair.getLeft();
		Word<Symbol> rightAccSeq = pair.getRight();
		int equalSuffixLength = -1;

		FastMealyState<String> pre1 = null;
		FastMealyState<String> pre2 = null;
		int possUnequalSuffLen = 0;
		do {
			equalSuffixLength++;
			possUnequalSuffLen = equalSuffixLength + 1;
			pre1 = machine.getState(leftAccSeq.prefix(Math.max(0, leftAccSeq.size() - possUnequalSuffLen)));
			pre2 = machine.getState(rightAccSeq.prefix(Math.max(0, rightAccSeq.size() - possUnequalSuffLen)));
		} while (pre1 == pre2
				&& /*
					 * if the next two conditions fail, the last one would
					 * through an exception and strictly speaking fail as well
					 */
				possUnequalSuffLen <= leftAccSeq.size() && possUnequalSuffLen <= rightAccSeq.size()
				&& leftAccSeq.suffix(possUnequalSuffLen).equals(rightAccSeq.suffix(possUnequalSuffLen)));

		FastMealyState<String> reachedState1 = machine
				.getState(leftAccSeq.prefix(leftAccSeq.size() - equalSuffixLength));
		FastMealyState<String> reachedState2 = machine
				.getState(rightAccSeq.prefix(rightAccSeq.size() - equalSuffixLength));
		pair = ImmutablePair.of(leftAccSeq.prefix(leftAccSeq.size() - equalSuffixLength),
				rightAccSeq.prefix(rightAccSeq.size() - equalSuffixLength));

		Word<Symbol> equalSuffix = leftAccSeq.suffix(equalSuffixLength);
		if (!equalSuffix.equals(rightAccSeq.suffix(equalSuffixLength)) || reachedState1 != reachedState2)
			throw new RuntimeException("this should not happpen, there is some bug");
		CompoundSample pairSample = new CompoundSample("pair");

		if (mutateAlsoPrefix || !pair.getLeft().isPrefixOf(pair.getRight())) {
			final Triple<FastMealyState<String>, FastMealyState<String>, Symbol> nextTriple = new ImmutableTriple<>(
					reachedState1, pre1, pair.getLeft().lastSymbol());

			if (!processedReachedPreSymbolTriple.contains(nextTriple)) {
				CompoundSample seqSample = new CompoundSample("sequence");
				for (List<Symbol> wordToDifference : CollectionsUtil.allTuples(inputAlphabet, mutationDepth,
						mutationDepth)) {
					List<Symbol> actualWordToDiff = new ArrayList<Symbol>();
					equalSuffix.stream().forEach(actualWordToDiff::add);
					actualWordToDiff.addAll(wordToDifference);
					splitAndAddTransitionsSingleSeq(machine, reachedState1, pre1, pair.getLeft().lastSymbol(),
							seqSample, actualWordToDiff);
					// copy here, otherwise the iterator will change internally,
					// and
					// the closures created
					// inside the function will change as well
					processedReachedPreSymbolTriple.add(nextTriple);
				}
				pairSample.addChild(seqSample);
			}
		}
		if ((mutateAlsoPrefix || !pair.getRight().isPrefixOf(pair.getLeft()))) {
			final Triple<FastMealyState<String>, FastMealyState<String>, Symbol> otherNextTriple = new ImmutableTriple<>(
					reachedState2, pre2, pair.getRight().lastSymbol());
			if (!processedReachedPreSymbolTriple.contains(otherNextTriple)) {
				CompoundSample seqSample = new CompoundSample("sequence");
				for (List<Symbol> wordToDifference : CollectionsUtil.allTuples(inputAlphabet, mutationDepth,
						mutationDepth)) {
					List<Symbol> actualWordToDiff = new ArrayList<Symbol>();
					equalSuffix.stream().forEach(actualWordToDiff::add);
					actualWordToDiff.addAll(wordToDifference);
					splitAndAddTransitionsSingleSeq(machine, reachedState2, pre2, pair.getRight().lastSymbol(),
							seqSample, actualWordToDiff);
					processedReachedPreSymbolTriple.add(otherNextTriple);
				}
				pairSample.addChild(seqSample);
			}
		}
		return pairSample;
	}

	private void splitAndAddTransitionsSingleSeq(FastMealy<Symbol, String> machine,
			FastMealyState<String> reachedFirstState, FastMealyState<String> preState, Symbol lastInput,
			CompoundSample mutantSample, List<Symbol> inputsUntilDifference) {

		MealyTransition<FastMealyState<String>, String> ignoreTrans = machine.getTransition(preState, lastInput);
		CriticalTransition critTrans = new CriticalTransition(ignoreTrans, false, preState);

		for (Symbol mutateInput : inputAlphabet) {
			CriticalTransition critTransCopy = critTrans.copy();
			List<Symbol> succToDefKill = new ArrayList<>(inputsUntilDifference);
			succToDefKill.add(mutateInput);
			critTransCopy.setDefKillingSucc(Optional.of(succToDefKill));
			MutantProducer aMutant = new MutantProducer(idGen++, critTransCopy, () -> {
				Pair<FastMealy<Symbol, String>, Map<Object, FastMealyState<String>>> copyAndMapping = copyMealyMachine(
						machine, inputAlphabet, Collections.singleton(ignoreTrans));
				FastMealy<Symbol, String> copiedMachine = copyAndMapping.getLeft();
				Map<Object, FastMealyState<String>> mapping = copyAndMapping.getRight();
				FastMealyState<String> newFirstState = copiedMachine.addState();
				// mapping.put(reachedFirstState, newFirstState);
				String lastOutput = machine.getOutput(preState, lastInput);
				copiedMachine.addTransition(mapping.get(preState), lastInput,
						new MealyTransition<>(newFirstState, lastOutput));

				FastMealyState<String> reachedEndState = machine.getSuccessor(reachedFirstState, inputsUntilDifference);
				FastMealyState<String> newEndState = copyStates(machine, reachedFirstState, copiedMachine,
						newFirstState, mapping, inputsUntilDifference);
				// TODO does largely the same as copyStates
				for (Symbol input : inputAlphabet) {
					String originalOutput = machine.getOutput(reachedEndState, input);
					String chosenOutput = null;
					if (input.equals(mutateInput)) {
						chosenOutput = originalOutput + "Mutated";
					} else {
						chosenOutput = originalOutput;
					}

					MealyTransition<FastMealyState<String>, String> newTransition = new MealyTransition<>(
							mapping.get(machine.getSuccessor(reachedEndState, input)), chosenOutput);
					copiedMachine.addTransition(newEndState, input, newTransition);
				}
				return cast(copiedMachine);
			});
			mutantSample.addChild(MutantSample.create(aMutant));
		}
	}

	private FastMealyState<String> copyStates(FastMealy<Symbol, String> machine,
			FastMealyState<String> reachedFirstState, FastMealy<Symbol, String> copiedMachine,
			FastMealyState<String> newFirstState, Map<Object, FastMealyState<String>> stateMapping,
			List<Symbol> inputsUntilDifference) {
		FastMealyState<String> stateInOriginal = reachedFirstState;
		FastMealyState<String> preInOriginal = reachedFirstState;
		FastMealyState<String> preInCopy = newFirstState;

		for (Symbol input : inputsUntilDifference) {
			stateInOriginal = machine.getSuccessor(stateInOriginal, input);
			FastMealyState<String> stateInCopy = copiedMachine.addState();
			for (Symbol otherInput : inputAlphabet) {
				String originalOutput = machine.getOutput(preInOriginal, otherInput);
				FastMealyState<String> mappedOriginalSucc = stateMapping
						.get(machine.getSuccessor(preInOriginal, otherInput));
				if (!otherInput.equals(input)) {
					copiedMachine.addTransition(preInCopy, otherInput,
							new MealyTransition<>(mappedOriginalSucc, originalOutput));
				} else {
					copiedMachine.addTransition(preInCopy, input, new MealyTransition<>(stateInCopy, originalOutput));
				}
			}
			preInOriginal = stateInOriginal;
			preInCopy = stateInCopy;
		}
		return preInCopy;
	}

	private boolean checkIfDiffInPreState(Pair<Word<Symbol>, Word<Symbol>> accSeqPair,
			FastMealy<Symbol, String> machine) {
		Word<Symbol> seq1 = accSeqPair.getLeft();
		Word<Symbol> seq2 = accSeqPair.getRight();
		FastMealyState<String> secondToLastState1 = machine.getState(seq1.prefix(seq1.length() - 1));
		FastMealyState<String> secondToLastState2 = machine.getState(seq2.prefix(seq2.length() - 1));
		return secondToLastState1 != secondToLastState2
				|| (allowDiffInLastAccSymbol && !seq1.lastSymbol().equals(seq2.lastSymbol()));
	}

	private Set<Pair<Word<Symbol>, Word<Symbol>>> allUnEqualPairs(List<Word<Symbol>> accSeqs) {
		return accSeqs.stream()
				.flatMap(a1 -> accSeqs.stream().map(a2 -> new UnorderedPair<>(a1, a2))
						.filter(aPair -> aPair.isActualPair()))
				.map(unordered -> unordered.toOrderedPair()).collect(Collectors.toSet());
	}

	public List<Word<Symbol>> findAccSequences(FastMealy<Symbol, String> machine, FastMealyState<String> target) {
		LinkedList<Triple<FastMealyState<String>, Trace<Symbol>, Set<FastMealyState<String>>>> schedule = new LinkedList<>();

		schedule.add(new ImmutableTriple<>(machine.getInitialState(), new Nil<>(),
				useGlobalVisited ? null : new HashSet<>()));
		List<Trace<Symbol>> result = new ArrayList<>();
		HashSet<FastMealyState<String>> globalVisited = new HashSet<>();
		HashSet<FastMealyState<String>> globalVisitedTwice = new HashSet<>();

		while (!schedule.isEmpty() && result.size() < accSeqBound) {
			Triple<FastMealyState<String>, Trace<Symbol>, Set<FastMealyState<String>>> current = schedule.removeFirst();

			boolean wasInSet = !globalVisited.add(current.getLeft());
			if (wasInSet)
				globalVisitedTwice.add(current.getLeft());

			// exclude the empty sequence as it is not very helpful in splitting
			// later on and we do not lose
			// a lot: for hypotheses of size one there will be self loops for
			// all inputs and after that
			// we can almost be certain that the initial gets explored
			// sufficiently
			if (current.getLeft() == target && (current.getMiddle().size() > 0)) {
				result.add(current.getMiddle());
			}

			for (Symbol input : inputAlphabet) {
				FastMealyState<String> nextState = machine.getSuccessor(current.getLeft(), input);
				if (((!useGlobalVisited && !current.getRight().contains(nextState))
						|| (useGlobalVisited && !globalVisitedTwice.contains(
								nextState))) /*
												 * && current.getLeft() !=
												 * nextState allow for loops
												 */) {
					Trace<Symbol> nextAccSeq = new Cons<>(input, current.getMiddle());
					Set<FastMealyState<String>> newVisited = null;
					if (!useGlobalVisited) {
						newVisited = new HashSet<>(current.getRight());
						newVisited.add(current.getLeft());
					}
					schedule.add(new ImmutableTriple<>(nextState, nextAccSeq, newVisited));
				}
			}
		}
		return result.stream().map(Trace::toWord).collect(Collectors.toList());
	}

	@Override
	public String description() {
		return String.format("split-state(acc-seq=%d, depth=%d, diff-last=%b, allow-equal-pre=%b)", accSeqBound,
				mutationDepth, allowDiffInLastAccSymbol, allowEqualPreStateAndSymbol);
	}

	public String shortDescription() {
		return String.format("split-state_%d", mutationDepth);
	}

	public boolean isMutateAlsoPrefix() {
		return mutateAlsoPrefix;
	}

	public void setMutateAlsoPrefix(boolean mutateAlsoPrefix) {
		this.mutateAlsoPrefix = mutateAlsoPrefix;
	}

}
