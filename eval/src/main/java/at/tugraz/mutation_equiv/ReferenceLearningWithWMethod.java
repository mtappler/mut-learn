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

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

import at.tugraz.learning.suls.MealyDotFileSul;
import at.tugraz.learning.suls.SULWithAlphabet;
import at.tugraz.mutation_equiv.eval.StepCounterSUL;
import de.learnlib.algorithms.lstargeneric.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstargeneric.closing.ClosingStrategies;
import de.learnlib.algorithms.lstargeneric.mealy.ExtensibleLStarMealy;
import de.learnlib.api.EquivalenceOracle.MealyEquivalenceOracle;
import de.learnlib.api.Query;
import de.learnlib.api.EquivalenceOracle;
import de.learnlib.api.SUL;
import de.learnlib.cache.sul.SULCaches;
import de.learnlib.eqtests.basic.WMethodEQOracle.MealyWMethodEQOracle;
import de.learnlib.eqtests.basic.WpMethodEQOracle.MealyWpMethodEQOracle;
import de.learnlib.experiments.Experiment;
import de.learnlib.experiments.Experiment.MealyExperiment;
import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.oracles.ResetCounterSUL;
import de.learnlib.oracles.SULOracle;
import de.learnlib.statistics.SimpleProfiler;
import net.automatalib.automata.UniversalDeterministicAutomaton;
import net.automatalib.automata.concepts.Output;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.commons.util.collections.CollectionsUtil;
import net.automatalib.commons.util.mappings.MutableMapping;
import net.automatalib.util.automata.Automata;
import net.automatalib.util.graphs.dot.GraphDOT;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Symbol;

/**
 * Class providing a static method for learning a model with partial W-method.
 * 
 * The main-method contains calls to this method.
 * 
 * 
 * @author Martin Tappler
 *
 */
public class ReferenceLearningWithWMethod {

	private static class TestStepLimitWpMealyEqOracle<A extends MealyMachine<?, I, ?, O>, I, O>
			implements EquivalenceOracle<A, I, Word<O>> {

		private long testStepLimit = -1;
		private int maxDepth;
		private SULOracle<I, O> sulOracle;

		public TestStepLimitWpMealyEqOracle(SULOracle<I, O> mqOracle, long testSteps, int maxDepth) {
			this.sulOracle = mqOracle;
			this.testStepLimit = testSteps;
			this.maxDepth = maxDepth;
		}

		// copied from LearnLib implementation of Wp-method
		private <S> List<Word<I>> doFindTraces(UniversalDeterministicAutomaton<S, I, ?, ?, ?> hypothesis,
				Output<I, Word<O>> output, Collection<? extends I> inputs) {

			List<Word<I>> stateCover = new ArrayList<Word<I>>(hypothesis.size());
			List<Word<I>> transitions = new ArrayList<Word<I>>(hypothesis.size() * (inputs.size() - 1));

			Automata.cover(hypothesis, inputs, stateCover, transitions);

			List<Word<I>> globalSuffixes = Automata.characterizingSet(hypothesis, inputs);
			if (globalSuffixes.isEmpty())
				globalSuffixes = Collections.singletonList(Word.<I> epsilon());

			WordBuilder<I> wb = new WordBuilder<>();
			List<Word<I>> resultTraces = new ArrayList<>();

			// Phase 1: state cover * middle part * global suffixes
			for (List<? extends I> middle : CollectionsUtil.allTuples(inputs, 0, maxDepth)) {
				for (Word<I> as : stateCover) {
					for (Word<I> suffix : globalSuffixes) {
						wb.append(as).append(middle).append(suffix);
						Word<I> queryWord = wb.toWord();
						resultTraces.add(queryWord);
						wb.clear();
					}
				}
			}

			// Phase 2: transitions (not in state cover) * middle part * local
			// suffixes
			MutableMapping<S, List<Word<I>>> localSuffixSets = hypothesis.createStaticStateMapping();

			for (List<? extends I> middle : CollectionsUtil.allTuples(inputs, 0, maxDepth)) {
				for (Word<I> trans : transitions) {
					S state = hypothesis.getState(trans);
					List<Word<I>> localSuffixes = localSuffixSets.get(state);
					if (localSuffixes == null) {
						localSuffixes = Automata.stateCharacterizingSet(hypothesis, inputs, state);
						if (localSuffixes.isEmpty())
							localSuffixes = Collections.singletonList(Word.<I> epsilon());
						localSuffixSets.put(state, localSuffixes);
					}

					for (Word<I> suffix : localSuffixes) {
						wb.append(trans).append(middle).append(suffix);
						Word<I> queryWord = wb.toWord();
						resultTraces.add(queryWord);
						wb.clear();
					}
				}
			}

			return resultTraces;
		}

		@Override
		public DefaultQuery<I, Word<O>> findCounterExample(A hypothesis, Collection<? extends I> inputs) {
			UniversalDeterministicAutomaton<?, I, ?, ?, ?> aut = hypothesis;
			Output<I, Word<O>> out = hypothesis;
			List<Word<I>> traces = doFindTraces(aut, out, inputs);
			for (Word<I> trace : traces) {
//				System.out.println(testStepLimit + " " + trace.length());
//				System.out.println(testStepLimit + " " + trace.length());
				if (testStepLimit == 0)
					return null;
				DefaultQuery<I, Word<O>> query = null;
				if (trace.length() < testStepLimit) {
					testStepLimit -= trace.length();
					query = new DefaultQuery<>(trace);

				} else {
					query = new DefaultQuery<>(trace.prefix((int)testStepLimit));
					testStepLimit = 0;
				}
				sulOracle.processQuery(query);
				Word<O> outputHyp = hypothesis.computeOutput(trace);
				if (!outputHyp.equals(query.getOutput())){					
					return query;	
				}
			}
			return null;
		}

	}

	public static void main(String[] args) throws NoSuchMethodException, IOException {

		File targetDir =
//		 new File("src/main/resources/mqtt/emqtt");
//				 new File("src/main/resources/mqtt/");
		 new File("src/main/resources/tcp/");
		FileFilter filter = 
				pathname -> pathname.getAbsolutePath().contains("server") && 
				pathname.getAbsolutePath().endsWith("trans.dot") &&
				pathname.getAbsolutePath().contains("ubuntu"); 
//		pathname -> //pathname.getAbsolutePath().contains("emqtt") && 
//				pathname.getAbsolutePath().contains("_trans.dot");
		// pathname.getAbsolutePath().endsWith("trans.dot");
		List<File> files = filterFilesRec(filter, targetDir);

		LearnLogger.getLogger(Experiment.class).setLevel(Level.WARNING);
		for (File transFile : files) {
			System.out.println("Processing " + transFile.getAbsolutePath());
			MealyDotFileSul sul = new MealyDotFileSul(transFile.getAbsolutePath());
//			learnWithWpMethod(sul, sul.getMealy().size(),5, -1);
			learnWithWpMethod(sul, sul.getMealy().size(),2, 226119);
		}
	}

	/**
	 * Tries to learn a model of the SUL with the partial W-method with the
	 * specified depth and L* with Rivest & Schapire's counterexample-handling.
	 * Since learned models are minimal are suffices to check whether the
	 * learned contains the same number of states of the true model.
	 * 
	 * @param sul
	 *            system under learning
	 * @param expectedNrStates
	 *            the number of states of the true system under learning
	 * @param depth
	 *            depth parameter for the partial W-method (difference in number
	 *            of states of the hypothesis and an assumed upper bound)
	 * @param i
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void learnWithWpMethod(SULWithAlphabet<Symbol, String> sul, int expectedNrStates, int depth,
			long testStepLimit) throws FileNotFoundException, IOException {
		// oracle for counting queries wraps sul
		ResetCounterSUL<Symbol, String> statisticSUL = new ResetCounterSUL<>("membership queries", sul);
		StepCounterSUL mqStepCounter = new StepCounterSUL(statisticSUL);

		SUL<Symbol, String> effectiveSul = mqStepCounter;
		Alphabet<Symbol> alphabet = sul.getAlphabet();
		// caching not needed actually
		effectiveSul = SULCaches.createCache(alphabet, effectiveSul);

		SULOracle<Symbol, String> mqOracle = new SULOracle<>(effectiveSul);

		ExtensibleLStarMealy<Symbol, String> learner = new ExtensibleLStarMealy<>(alphabet, mqOracle,
				Collections.emptyList(), ObservationTableCEXHandlers.RIVEST_SCHAPIRE, ClosingStrategies.CLOSE_SHORTEST);

		StepCounterSUL stepCounterEQ = new StepCounterSUL(sul);
		SULOracle<Symbol, String> mqOracleForEq = new SULOracle<>(stepCounterEQ);

		MealyWpMethodEQOracle<Symbol, String> eqOracle = new MealyWpMethodEQOracle<Symbol, String>(depth,
				mqOracleForEq);

		MealyExperiment<Symbol, String> experiment = null;
		if (testStepLimit > -1) {
			EquivalenceOracle<MealyMachine<?, Symbol, ?, String>, Symbol, Word<String>> limitOracle = new TestStepLimitWpMealyEqOracle<MealyMachine<?, Symbol, ?, String>, Symbol, String>(
					mqOracleForEq, testStepLimit, depth);

			experiment = new MealyExperiment<>(learner, limitOracle, alphabet);
		} else {
			experiment = new MealyExperiment<>(learner, eqOracle, alphabet);
		}
		experiment.setProfile(true);
		experiment.setLogModels(true);
		experiment.run();

		// get learned model
		MealyMachine<?, Symbol, ?, String> result = experiment.getFinalHypothesis();

		// report results
		System.out.println("-------------------------------------------------------");

		// because of minimality of the result we know that we learned the
		// correct model by checking the number of states
		// //sanityCheckOracle.findCounterExample(result, alphabet) == null)
		if (result.size() == expectedNrStates)
			System.out.println("We learned the correct model.");
		else
			System.out.println("We did not learn the correct model.");

		System.out.println("Actual steps executed for eq: " + stepCounterEQ.getStepCount());
		System.out.println("Actual tests executed for eq: " + stepCounterEQ.getResets());
		System.out.println("Actual steps executed for mq: " + mqStepCounter.getStepCount());
		System.out.println("Actual tests executed for mq: " + mqStepCounter.getResets());
		// profiling
		System.out.println(SimpleProfiler.getResults());

		// learning statistics
		System.out.println(experiment.getRounds().getSummary());
		System.out.println(statisticSUL.getStatisticalData().getSummary());

		// model statistics
		System.out.println("States: " + result.size());
		System.out.println("Sigma: " + alphabet.size());

		// show model
		System.out.println();
		System.out.println("Model: ");

		File outputFile = new File("SUL_mm.dot");
		PrintStream psDotFile = new PrintStream(outputFile);
		GraphDOT.write(result, alphabet, psDotFile); // may throw IOException!

		System.out.println("-------------------------------------------------------");
	}

	private static List<File> filterFilesRec(FileFilter filter, File currentDir) {
		List<File> result = new ArrayList<>();
		File[] inCurrentDir = currentDir.listFiles(filter);
		Arrays.stream(inCurrentDir).forEach(result::add);
		for (File f : currentDir.listFiles())
			if (f.isDirectory())
				result.addAll(filterFilesRec(filter, f));
		return result;
	}
}
