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
package at.tugraz.mutation_equiv.trace_gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import at.tugraz.mutation_equiv.equiv_check.EquivalenceChecker;
import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Symbol;

/**
 * A test-case generator which aims killing multiple mutants combined with randomisation.
 * 
 * It starts with probability 0.5 with a random walk and then iterates two steps:
 * 1. choosing a mutant and generating trace killing it
 * 2. performing a random walk 
 * 
 * Stopping is controlled by an upper bound <code>maxTestLength</code>,
 * <code>retryProb</code> which defines the probability of repeating an unsuccessful first step,
 * and <code>stopProb</code> which defines the probability of stopping after the second step.
 * 
 * @author Martin Tappler
 *
 */
public class MuDirectedIterativeGenerator implements TraceGenerator {

	private Random randGen = new Random(1);
	private double stopProb = 0;
	private double retryProb = 0;
	private EquivalenceChecker equivChecker;
	private RandomWordGenerator randomWordGen;
	private int maxTestLength = 0;
	private RandomWordGenerator randomWordGenMaxLen;
	
	public MuDirectedIterativeGenerator(double stopProb, double retryProb, 
				Alphabet<Symbol> inputAlphabet, int maxTestLength, int maxRandWordLen) {
		super();
		this.stopProb = stopProb;
		this.retryProb = retryProb;
		this.equivChecker = new EquivalenceChecker(inputAlphabet,true);
		this.maxTestLength = maxTestLength;
		this.randomWordGen = new RandomWordGenerator(inputAlphabet, randGen, maxRandWordLen);
		this.randomWordGenMaxLen = new RandomWordGenerator(inputAlphabet, randGen, maxTestLength);
	}

	@Override
	public void updateRandomSeed(long seed) {
		this.randomWordGen.updateRandomSeed(seed);
		this.randomWordGenMaxLen.updateRandomSeed(seed);
		this.randGen = new Random(seed);
	}

	@Override
	public List<List<Symbol>> generateTraces(int nrTraces, MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<MutantProducer> mutants) {
		List<List<Symbol>> result = TraceGenerator.super.generateTraces(nrTraces, hypothesis, mutants);
		long lenSum = 0;
		for(List<Symbol> t : result){
			lenSum += t.size();
		}
		System.out.println("Mean len of traces: " + (double) lenSum / nrTraces);
		return result;
	}

	@Override
	public String description() {
		return String.format("mu-directed-iterative(max-len:%d, restart-prob:%f, stop-prob:%f, rand-word:%d)",
				maxTestLength, retryProb, stopProb, randomWordGen.getMaxTestLength());
	}

	@Override
	public List<Symbol> generateTrace(MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<MutantProducer> mutants) {
		if(!mutants.isEmpty()){
			List<Symbol> trace = new ArrayList<>();
			if(randGen.nextDouble() < 0.5) // 0.5 probability to start from initial state
				trace.addAll(randomWordGen.generateTrace(hypothesis, mutants));
			while(trace.size() < maxTestLength){
				MutantProducer chosenMutant = 
						mutants.get(randGen.nextInt(mutants.size()));
				Optional<List<Symbol>> traceExtension = equivChecker.killMutant(chosenMutant, hypothesis, trace);
				if(traceExtension.isPresent()){
					trace = traceExtension.get();
					trace.addAll(randomWordGen.generateTrace(hypothesis, mutants));
					if(randGen.nextDouble() <= stopProb){
						break;
					}
				}
				else {
					if(randGen.nextDouble() > retryProb){
						break;
					}
				}
			}
			return trace;
		} else 
			return randomWordGenMaxLen.generateTrace(hypothesis, mutants);
	}

}
