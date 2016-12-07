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
package at.tugraz.mutation_equiv.equiv_check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Symbol;
/**
 * An equivalence checker between Mealy machines. Its most important function 
 * <code>killMutant</code> tries to find a trace showing non-equivalence between
 * two Mealy machines after executing some input sequence. If there exists such a 
 * trace, it is return, otherwise <code>Optional.empty()</code>, i.e.  is returned. 
 * 
 * Note that it is not a fully-fledged equivalence checker, but intended for checking 
 * equivalence between some model and derived model. The main reason for this restriction
 * is that both models must use the same alphabet.
 * 
 * It is possible to use precompute trace in the original model, to speed-up equivalence
 * checking in certain cases.
 * 
 * @author Martin Tappler
 *
 */
public class EquivalenceChecker {

	/**
	 * A simple sequence type providing Nil and Cons to save 
	 * memory in precomputation.
	 * 
	 * @author Martin Tappler
	 *
	 */
	public static interface Trace{
		List<Symbol> toList();
		List<Symbol> toList(List<Symbol> prefix);
		static Trace fromList(List<Symbol> trace){
			Trace result = new Nil();
			for(Symbol s : trace){
				result = new Cons(s,result);
			}
			return result;
		}
	}
	public static class Cons implements Trace{
		public Cons(Symbol v, Trace tail) {
			super();
			this.v = v;
			this.tail = tail;
		}
		Symbol v;
		Trace tail;
		@Override
		public List<Symbol> toList() {
			List<Symbol> prefix = new ArrayList<>();
			prefix.add(v);
			return tail.toList(prefix);
		}
		@Override
		public List<Symbol> toList(List<Symbol> prefix) {
			prefix.add(v);
			return tail.toList(prefix);
		}
	}
	public static class Nil implements Trace{

		@Override
		public List<Symbol> toList() {
			return new ArrayList<>();
		}

		@Override
		public List<Symbol> toList(List<Symbol> prefix) {
			Collections.reverse(prefix);
			return prefix;
		}
		
	}
	private Alphabet<Symbol> inputAlphabet = null;
	private boolean precompute = false;
	private Object hypoReference = null;
	private Map<Object,Map<Object,Trace>> precompTraces = new HashMap<>();

	public EquivalenceChecker(Alphabet<Symbol> inputAlphabet, boolean precompute) {
		super();
		this.inputAlphabet = inputAlphabet;
		this.precompute = precompute;
	}
	public EquivalenceChecker(Alphabet<Symbol> inputAlphabet) {
		this(inputAlphabet,false);
	}
	
	public void precomputeTraces(MealyMachine<Object,Symbol,?,String> machine){
		Map<Object,Map<Object,Trace>> precomputedTraces = new HashMap<>();
		for(Object s : machine.getStates())
			precomputedTraces.put(s, precomputeTraces(machine,s));
		precompTraces = precomputedTraces;
	}

	private Map<Object, Trace> precomputeTraces(MealyMachine<Object, Symbol, ?, String> machine, Object startState) {
		Set<Object> visited = new HashSet<>();
		Map<Object,Trace> tracesFromStart = new HashMap<>();
		LinkedList<Pair<Object,Trace>> schedule = new LinkedList<>();
		schedule.add(new ImmutablePair<Object, EquivalenceChecker.Trace>(startState, new Nil()));
		while(!schedule.isEmpty()){
			Pair<Object, Trace> current = schedule.remove();
			Object currentState = current.getLeft();
			Trace currentTrace = current.getRight();
			if(!tracesFromStart.containsKey(currentState))
				tracesFromStart.put(currentState, currentTrace);
			if(!visited.contains(currentState)){
				visited.add(currentState);
				for (Symbol input : inputAlphabet){				
					Object nextState = machine.getSuccessor(currentState, input);
					Trace traceToNext = new Cons(input,currentTrace);
					
					schedule.add(new ImmutablePair<>(nextState, traceToNext));
				}
			}
		}
		return tracesFromStart;
	}

	public Optional<List<Symbol>> killMutant(MutantProducer mutant,
			MealyMachine<Object, Symbol, ?, String> hypothesis, List<Symbol> initialTrace) {
		if(precompute && hypothesis != hypoReference){
			precomputeTraces(hypothesis);
			hypoReference = hypothesis;
		}
		// this if works in the current implementation, but causes large speed-up so it is used
		// change output are the only mutants with getCritTrans().isDefinitelyKilled() == true
		// for these it suffices to find a transition from the current state to the prestate (and if there is none
		// then the mutant can't be killed)
		// for split state mutants, however, this would not work, as they may be killed even if the
		// pre state of the critical transition is not reachable
		if(precompute && mutant.getCritTrans().isDefinitelyKilled()){
			Map<Object,Trace> traceFromInit = precompTraces.get(hypothesis.getState(initialTrace));
			Trace traceToPreCritical = traceFromInit.get(mutant.getCritTrans().getPreState());
			if(traceToPreCritical != null)
				initialTrace.addAll(traceToPreCritical.toList());
			else	
				return Optional.empty();
		}
		 if(mutant.getCritTrans().isDefinitelyKilled()){
			// if mutant is definitely killed by visiting the critical transition 
			// we may also just avoid creating the mutant altogether and find a path to the transition
			// actually this may also be an option for the latter case where the critical transition may be
			// visited without killing the mutant, i.e. the kill trace would be 
			// initialTrace ++ traceToTrans ++ trace to wrong output (keep in mind for future experiments)
//			int sizeBefore = initialTrace.size();
			Optional<List<Symbol>> result = 
					traceToTransition(hypothesis,initialTrace,mutant.getCritTrans().getCriticalTrans());	

			return result;
		 } else {
			 return killMutant(mutant.get(), hypothesis, initialTrace);
		 }
	}
	private Optional<List<Symbol>> traceToTransition(MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<Symbol> initialTrace, Object criticalTrans) {
		Object state = hypothesis.getSuccessor(hypothesis.getInitialState(), initialTrace);
		Set<Object> visited = new HashSet<>();
		LinkedList<Pair<Object,Trace>> schedule = new LinkedList<>();
		schedule.add(new ImmutablePair<Object, EquivalenceChecker.Trace>(state, Trace.fromList(initialTrace)));
		while(!schedule.isEmpty()){
			Pair<Object, Trace> current = schedule.remove();
			Object currentState = current.getLeft();
			Trace currentTrace = current.getRight();
			if(!visited.contains(currentState)){
				visited.add(currentState);
				for (Symbol input : inputAlphabet){
					Object currTrans = hypothesis.getTransition(currentState, input);
					if(currTrans.equals(criticalTrans)){
						Trace finalTrace = new Cons(input,currentTrace);
						return Optional.of(finalTrace.toList());
					}
					else {
						Object nextState = hypothesis.getSuccessor(currentState, input);
						Trace traceToNext = new Cons(input,currentTrace);
						schedule.add(new ImmutablePair<>(nextState, traceToNext));
					} 
				}
			}
		}
		return Optional.empty();
	}

	public Optional<List<Symbol>> killMutant(MealyMachine<Object, Symbol,?, String> mutant,
			MealyMachine<Object, Symbol, ?, String> hypothesis, List<Symbol> initialTrace) {
		Object mutantState = mutant.getSuccessor(mutant.getInitialState(), initialTrace);
		Object hypState = hypothesis.getSuccessor(hypothesis.getInitialState(), initialTrace);
		Pair<Object,Object> initState = new ImmutablePair<Object, Object>(mutantState,hypState);
		Set<Pair<Object,Object>> visitedStates = new HashSet<>();
		LinkedList<Pair<Pair<Object,Object>, Trace>> schedule = new LinkedList<>();
		schedule.add(new ImmutablePair<>(initState, Trace.fromList(initialTrace)));
		return killMutant(mutant,hypothesis,schedule,visitedStates);
	}
	private Optional<List<Symbol>> killMutant(MealyMachine<Object, Symbol, ?, String> mutant,
			MealyMachine<Object, Symbol, ?, String> hypothesis, LinkedList<Pair<Pair<Object,Object>, Trace>> schedule,
			Set<Pair<Object, Object>> visitedStates) {
		// a simple parallel breadth-first exploration of both Mealy machines
		while(!schedule.isEmpty()){
			Pair<Pair<Object, Object>, Trace> current = schedule.remove();
			Pair<Object, Object> currentState = current.getLeft();
			Trace currentTrace = current.getRight();
			if(!visitedStates.contains(currentState)){
				visitedStates.add(currentState);
				for (Symbol input : inputAlphabet){
					String outMut = mutant.getOutput(currentState.getLeft(),input);
					String outOrig = hypothesis.getOutput(currentState.getRight(),input);
					if(outMut.equals(outOrig)){
						Pair<Object,Object> nextState = new ImmutablePair<Object, Object>(
								mutant.getSuccessor(currentState.getLeft(), input), 
								hypothesis.getSuccessor(currentState.getRight(), input));
						Trace traceToNext = new Cons(input,currentTrace);
						
						schedule.add(new ImmutablePair<>(nextState, traceToNext));
					} else {
						Trace finalTrace = new Cons(input,currentTrace);
						return Optional.of(finalTrace.toList());
					}
				}
			}
		}
		return Optional.empty();
	}

}
