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
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.impl.Symbol;

/**
 * This generators combines multiple test-case generators in a weighted comparison, i.e.
 * it generates a number of tests with each test-case generators proportional to the 
 * corresponding weight. 
 * 
 * It implements a batch mode which should be used if a test-case generator requires 
 * precomputation for generating a single test. 
 * 
 * @author Martin Tappler
 *
 */
public class CompositeTraceGenerator implements TraceGenerator {

	private Pair<TraceGenerator,Integer>[] generators;
	private int overallWeight = 0;
	private Random weightChoice = null;
	private boolean batchMode = false;
	@SafeVarargs
	public CompositeTraceGenerator(Pair<TraceGenerator,Integer>... weightedGenerators){
		this(false, weightedGenerators);
	}
	@SafeVarargs
	public CompositeTraceGenerator(boolean batchMode, Pair<TraceGenerator,Integer>... weightedGenerators){
		this.generators = weightedGenerators;
		for(Pair<TraceGenerator, Integer> g : generators)
			overallWeight += g.getRight();
		weightChoice = new Random();
		this.batchMode = batchMode;
	}
	@Override
	public void updateRandomSeed(long seed) {
		for(Pair<TraceGenerator, Integer> g : generators)
			g.getLeft().updateRandomSeed(seed);
		
		weightChoice = new Random(seed);
	}

	@Override
	public String description() {
		List<String> genDescs = new ArrayList<>();
		for(Pair<TraceGenerator, Integer> g : generators)
			genDescs.add(g.getLeft().description() + " : " + g.getRight());
		return "composite-generator(" + String.join(",",genDescs)+ ")";
	}

	@Override
	public List<Symbol> generateTrace(MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<MutantProducer> mutants) {
		TraceGenerator chosenGen = chooseGenerator();
		return chosenGen.generateTrace(hypothesis, mutants);
	}
	private TraceGenerator chooseGenerator() {
		int chosenWeight = weightChoice.nextInt(overallWeight);
		TraceGenerator chosenGen = null;
		for(Pair<TraceGenerator, Integer> g : generators){
			if(chosenWeight < g.getRight()){
				chosenGen = g.getLeft();
				break;
			}
			chosenWeight -= g.getRight();
		}
		return chosenGen;
	}
	@Override
	public List<List<Symbol>> generateTraces(int nrTraces, MealyMachine<Object, Symbol, ?, String> hypothesis,
			List<MutantProducer> mutants) {
		if(batchMode){
			List<List<Symbol>> traces = new ArrayList<>();
			for(Pair<TraceGenerator, Integer> g : generators){
				traces.addAll(g.getLeft()
						.generateTraces((int) Math.ceil((((double)nrTraces)/overallWeight) * g.getRight()),
						hypothesis, mutants));
			}
			if(traces.size() > nrTraces) // as we use ceil
				traces.subList(nrTraces, traces.size()).clear();
			return traces;
		}
		else
			return TraceGenerator.super.generateTraces(nrTraces, hypothesis, mutants);
	}

}
