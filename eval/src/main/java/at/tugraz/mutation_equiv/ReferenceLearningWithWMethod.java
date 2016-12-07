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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import at.tugraz.learning.suls.MealyDotFileSul;
import at.tugraz.learning.suls.SULWithAlphabet;
import at.tugraz.mutation_equiv.eval.StepCounterSUL;
import de.learnlib.algorithms.lstargeneric.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstargeneric.closing.ClosingStrategies;
import de.learnlib.algorithms.lstargeneric.mealy.ExtensibleLStarMealy;
import de.learnlib.api.SUL;
import de.learnlib.cache.sul.SULCaches;
import de.learnlib.eqtests.basic.WpMethodEQOracle.MealyWpMethodEQOracle;
import de.learnlib.experiments.Experiment;
import de.learnlib.experiments.Experiment.MealyExperiment;
import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.ResetCounterSUL;
import de.learnlib.oracles.SULOracle;
import de.learnlib.statistics.SimpleProfiler;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.util.graphs.dot.GraphDOT;
import net.automatalib.words.Alphabet;
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

	
    public static void main(String[] args) throws NoSuchMethodException, IOException {

    	File targetDir = new File("src/main/resources/tcp/");
    	FileFilter filter = pathname -> 
    		pathname.getAbsolutePath().endsWith("trans.dot");
    	List<File> files = filterFilesRec(filter,targetDir);
    	
    	LearnLogger.getLogger(Experiment.class).setLevel(Level.WARNING);
    	for(File transFile : files){
    		System.out.println("Processing " + transFile.getAbsolutePath());
	    	MealyDotFileSul sul = new  MealyDotFileSul(transFile.getAbsolutePath());   	
	        learnWithWpMethod(sul,sul.getMealy().size(),1);
	
	    }
    }

    /**
     * Tries to learn a model of the SUL with the partial W-method with the specified depth 
     * and L* with Rivest & Schapire's counterexample-handling. Since learned models are minimal
     * are suffices to check whether the learned contains the same number of states of the true 
     * model. 
     * 
     * @param sul system under learning
     * @param expectedNrStates the number of states of the true system under learning
     * @param depth depth parameter for the partial W-method 
     * 		(difference in number of states of the hypothesis and an assumed upper bound)
     * @throws FileNotFoundException
     * @throws IOException
     */
	private static void learnWithWpMethod(SULWithAlphabet<Symbol, String> sul, int expectedNrStates, int depth) 
			throws FileNotFoundException, IOException {
		// oracle for counting queries wraps sul
		ResetCounterSUL<Symbol,String> statisticSUL = new ResetCounterSUL<>("membership queries", sul);
		StepCounterSUL mqStepCounter = new StepCounterSUL(statisticSUL);
		
		SUL<Symbol, String> effectiveSul = mqStepCounter;
		Alphabet<Symbol> alphabet = sul.getAlphabet();
		// caching not needed actually
		effectiveSul = SULCaches.createCache(alphabet, effectiveSul);
		
		SULOracle<Symbol, String> mqOracle = new SULOracle<>(effectiveSul);

		ExtensibleLStarMealy<Symbol,String> learner = new ExtensibleLStarMealy<>(alphabet, 
				mqOracle, Collections.emptyList(), 
				ObservationTableCEXHandlers.RIVEST_SCHAPIRE, 
				ClosingStrategies.CLOSE_SHORTEST);
		
		StepCounterSUL stepCounterEQ = new StepCounterSUL(sul);
		SULOracle<Symbol, String> mqOracleForEq = new SULOracle<>(stepCounterEQ);
		
		MealyWpMethodEQOracle<Symbol, String> eqOracle = 
				new MealyWpMethodEQOracle<Symbol,String>(depth, mqOracleForEq);
		MealyExperiment<Symbol, String> experiment =
		        new MealyExperiment<>(learner, eqOracle, alphabet);
		experiment.setProfile(true);
		experiment.setLogModels(true);
		experiment.run();

		// get learned model
		MealyMachine<?, Symbol, ?, String> result = 
		        experiment.getFinalHypothesis();
		
		// report results
		System.out.println("-------------------------------------------------------");
		
		// because of minimality of the result we know that we learned the correct model by checking the number of states //sanityCheckOracle.findCounterExample(result, alphabet) == null)
		if(result.size() == expectedNrStates)
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
		for(File f : currentDir.listFiles())
			if(f.isDirectory())
				result.addAll(filterFilesRec(filter, f));
		return result;
	}
}
