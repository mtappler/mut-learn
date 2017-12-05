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
package at.tugraz.mutation_equiv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import at.tugraz.mutation_equiv.mutation.MutationOperator;
import at.tugraz.mutation_equiv.mutation.sampling.CompoundSample;
import at.tugraz.mutation_equiv.mutation.sampling.MutantProducer;
import at.tugraz.mutation_equiv.mutation.sampling.MutantSample;
import at.tugraz.mutation_equiv.mutation.sampling.MutantSamplingStrategy;
import at.tugraz.mutation_equiv.test_selection.TestSelector;
import at.tugraz.mutation_equiv.trace_gen.TraceGenerator;
import de.learnlib.api.EquivalenceOracle.MealyEquivalenceOracle;
import de.learnlib.api.SUL;
import de.learnlib.oracles.DefaultQuery;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.automata.transout.impl.FastMealy;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Symbol;

/**
 * The class that implements the equivalence oracle, i.e. the basic data flow,
 * creating test cases of which a subset is selected. It also implements basic
 * logging, can be configured flexibly, but delegates the actual test-case
 * generation and selection to other classes.
 * 
 * Although it can be used for different generation strategies, it is mainly
 * intended to be used with mutation so the mutation operators and sampling
 * strategies can configured in this class.
 * 
 * Furthermore, it can be configured to reuse tests (<code>reuseRemaining</code>
 * ) between rounds of learning. Hence, if generation and selection are
 * expensive, the execution of these can be avoided to some extent.
 * Additionally, tests executed on the SUL can be stored in order to account for
 * them in test-case selection in later rounds.
 * 
 * @author Martin Tappler
 *
 * @param <T>
 *            Type of test case which is used the test-case selector
 */
public class RandomCoverageSelectionEQOracle<T extends TestCase> implements MealyEquivalenceOracle<Symbol, String> {

	private TestTrackingSUL sul;
	private List<MutationOperator> mutationOperators = new ArrayList<>();
	private int sizeTestSelectionSuite = 0;
	private long nrActualTestSteps = 0;
	private Iterator<List<Symbol>> remainingTests = null;
	private boolean reuseRemaining = false;
	private TestSelector<T> selector = null;
	private TraceGenerator traceGen = null;
	private MutantSamplingStrategy mutantSampler = null;
	private MutantSamplingStrategy mutantGenerationSampler = null;
	private long mutationDuration = 0;
	private long evalAndSelDuration = 0;
	private long generationDuration = 0;
	private Alphabet<Symbol> alphabet = null;
	private int nrExecutedTests = 0;

	public int getNrExecutedTests() {
		return nrExecutedTests;
	}

	public void resetNrExecutedTests() {
		nrExecutedTests = 0;
	}

	private boolean keepExecutedTests = true;
	private List<List<Symbol>> executedTests = new ArrayList<>();
	private AccessSequenceProvider accSeqProvider = null;

	public RandomCoverageSelectionEQOracle(TestTrackingSUL sul, Alphabet<Symbol> alphabet,
			List<MutationOperator> mutationOperators, int sizeTestSelectionSuite, boolean reuseRemaining,
			TestSelector<T> selector, TraceGenerator traceGen, MutantSamplingStrategy mutantSampler) {
		super();
		this.sul = sul;
		this.mutationOperators = mutationOperators;
		this.alphabet = alphabet;
		this.sizeTestSelectionSuite = sizeTestSelectionSuite;
		this.reuseRemaining = reuseRemaining;
		this.selector = selector;
		this.traceGen = traceGen;
		this.mutantSampler = mutantSampler;
	}

	public boolean isReuseRemaining() {
		return reuseRemaining;
	}

	public void setReuseRemaining(boolean reuseRemaining) {
		this.reuseRemaining = reuseRemaining;
	}

	@Override
	public DefaultQuery<Symbol, Word<String>> findCounterExample(MealyMachine<?, Symbol, ?, String> hypothesis,
			Collection<? extends Symbol> inputs) {
		System.out.println("Starting mutation equiv check with model size: " + hypothesis.size());

		Iterator<List<Symbol>> bestTests = null;
		boolean reuseRound = false;
		if (keepExecutedTests) {
			executedTests.addAll(sul.getExecutedTests());
			sul.clear();
			sul.setTrack(false);
		}

		if (reuseRemaining && remainingTests != null) {
			bestTests = remainingTests;
			reuseRound = true;
		} else {
			FastMealy<Symbol, String> hypoCasted = MutationOperator
					.copyMealyMachine(hypothesis, alphabet, Collections.emptyList()).getLeft();
			long startMutation = System.currentTimeMillis();
			MutantSample allMutants = createMutants(hypoCasted);
			MutantSample sampledMutants = mutantSampler.sample(allMutants);

			// currently one mutants may be in both the set of mutants for
			// generation and in
			// the set for selection which not be a problem as mutants
			// (MutantProducer) are effectively immutable
			// but this may change depending on the implementation
			MutantSample sampledGenerationMutants = null;
			if (mutantGenerationSampler == null) {
				sampledGenerationMutants = sampledMutants;
			} else {
				sampledGenerationMutants = mutantGenerationSampler.sample(allMutants);
			}
			if ("true".equals(System.getProperty("ranked_random.debug"))) {
				System.out.println("Have " + sampledMutants.getMutants().size() + " after sampling.");
			}
			List<MutantProducer> mutants = sampledMutants.getMutants();

			mutationDuration += (System.currentTimeMillis() - startMutation);
			long startGeneration = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			List<List<Symbol>> tests = createTests(
					(MealyMachine<Object, Symbol, ?, String>) ((MealyMachine<?, Symbol, ?, String>) hypoCasted),
					sampledGenerationMutants.getMutants());
			generationDuration += (System.currentTimeMillis() - startGeneration);
			long startEvalAndSel = System.currentTimeMillis();
			selector.setCurrentMachines(mutants, hypoCasted, executedTests);
			bestTests = selector.select(selector.evaluate(tests));
			evalAndSelDuration += (System.currentTimeMillis() - startEvalAndSel);
		}

		while (bestTests.hasNext()) {
			List<Symbol> test = bestTests.next();
			Optional<Word<String>> differingSULOutput = runTest(test, hypothesis.computeOutput(test));
			nrExecutedTests++;
			Optional<DefaultQuery<Symbol, Word<String>>> differingQuery = differingSULOutput.map(actualSULOutput -> {
				WordBuilder<Symbol> wbInput = new WordBuilder<>();
				test.subList(0, actualSULOutput.size()).forEach(wbInput::append);
				return new DefaultQuery<>(wbInput.toWord(), actualSULOutput);
			});
			if (keepExecutedTests) {
				if (differingQuery.isPresent()) {
					List<Symbol> partlyExecutedTest = new ArrayList<>();
					differingQuery.get().getInput().forEach(partlyExecutedTest::add);
					executedTests.add(partlyExecutedTest);
				} else
					executedTests.add(test);
			}
			if (differingQuery.isPresent()) {
				if (reuseRemaining) {
					remainingTests = bestTests;
				}
				sul.setTrack(true);
				return differingQuery.get();
			}
		}
		remainingTests = null;
		if (reuseRound)
			return findCounterExample(hypothesis, inputs);
		else {
			sul.setTrack(true);
			return null;
		}
	}

	private Optional<Word<String>> runTest(List<Symbol> test, Word<String> hypOutput) {
		sul.pre();
		WordBuilder<String> sulOutBuilder = new WordBuilder<>();
		int i = 0;
		for (Symbol input : test) {
			nrActualTestSteps++;
			String sulOutputStep = sul.step(input);
			sulOutBuilder.add(sulOutputStep);
			String hypOutputStep = hypOutput.getSymbol(i++);
			if (!sulOutputStep.equals(hypOutputStep)) {
				sul.post();
				return Optional.of(sulOutBuilder.toWord());
			}
		}
		sul.post();
		return Optional.empty();
	}

	private List<List<Symbol>> createTests(MealyMachine<Object, Symbol, ?, String> hypoCasted,
			List<MutantProducer> mutants) {
		System.out.println("Have " + mutants.size() + " mutants for generation.");
		return traceGen.generateTraces(sizeTestSelectionSuite, hypoCasted, mutants);
	}

	private MutantSample createMutants(FastMealy<Symbol, String> hypothesis) {
		CompoundSample result = new CompoundSample("union-operators");
		for (MutationOperator op : mutationOperators){
			if(accSeqProvider != null)
				op.setAccSequences(accSeqProvider.accessSeqForStates(hypothesis));
			result.addChild(op.createMutants(hypothesis));
		}
		return result;
	}

	public void resetMutationOperators() {
		mutationOperators.clear();
	}

	public void addMutationOperator(MutationOperator op) {
		mutationOperators.add(op);
	}

	public long getNrActualTestSteps() {
		return this.nrActualTestSteps;
	}

	public void resetNrActualTestSteps() {
		this.nrActualTestSteps = 0;
	}

	public long getEvalAndSelDuration() {
		return evalAndSelDuration;
	}

	public void resetEvalAndSelDuration() {
		this.evalAndSelDuration = 0;
	}

	public long getMutationDuration() {
		return mutationDuration;
	}

	public void resetMutationDuration() {
		this.mutationDuration = 0;
	}

	public long getGenerationDuration() {
		return generationDuration;
	}

	public void resetGenerationDuration() {
		this.generationDuration = 0;
	}

	public MutantSamplingStrategy getMutantGenerationSampler() {
		return mutantGenerationSampler;
	}

	public void setMutantGenerationSampler(MutantSamplingStrategy mutantGenerationSampler) {
		this.mutantGenerationSampler = mutantGenerationSampler;
	}

	public boolean isKeepExecutedTests() {
		return keepExecutedTests;
	}

	public void setKeepExecutedTests(boolean keepExecutedTests) {
		this.keepExecutedTests = keepExecutedTests;
	}

	public void setAccSeqProvider(AccessSequenceProvider accSeqProvider) {
		this.accSeqProvider = accSeqProvider;
	}

}
