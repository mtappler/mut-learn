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
package at.tugraz.mutation_equiv.eval;


import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import at.tugraz.learning.suls.SULWithAlphabet;
import at.tugraz.mutation_equiv.RandomCoverageSelectionEQOracle;
import at.tugraz.mutation_equiv.TestCase;
import at.tugraz.mutation_equiv.configuration.RandomCovSelEquivConfiguration;
import de.learnlib.acex.analyzers.AcexAnalyzers;
import de.learnlib.algorithms.kv.mealy.KearnsVaziraniMealyBuilder;
import de.learnlib.algorithms.lstargeneric.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstargeneric.closing.ClosingStrategies;
import de.learnlib.algorithms.lstargeneric.mealy.ExtensibleLStarMealy;
import de.learnlib.algorithms.lstargeneric.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealyBuilder;
import de.learnlib.api.LearningAlgorithm.MealyLearner;
import de.learnlib.experiments.Experiment;
import de.learnlib.experiments.Experiment.MealyExperiment;
import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.ResetCounterSUL;
import de.learnlib.oracles.SULOracle;
import de.learnlib.statistics.SimpleProfiler;
import de.learnlib.statistics.StatisticSUL;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.util.graphs.dot.GraphDOT;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Symbol;

/**
 * Class for evaluating randomised conformance testing technique. 
 * 
 * It implements iteration over different learning-set-ups
 * and executes them. To do that it runs a learning experiment
 * for each set up. Additionally, it logs result and checks whether
 * logs already exist for some set up. If a log exists it skips 
 * the experiment.
 * 
 * It has some several configuration options such as which learning
 * algorithm should be used. Most of them should should be self-explanatory.
 * 
 * @author Martin Tappler
 *
 */
public class Evaluator {
	private long[] seeds = null;
	private boolean verbose = false;
	private String experimentName;
	private String path = "";
	private boolean skipIfExists = true;
	private boolean useLstar = false;
	private boolean useKV = false;
	private boolean useRV = false;
	private boolean shrink = false;

	public void setUseLstar() {
		this.useLstar = true;
		this.useKV = false;
		this.useRV = false;
	}

	public void setUseKV() {
		this.useKV = true;
		this.useLstar = false;
		this.useRV = false;
	}

	public Evaluator(long[] seeds, SULWithAlphabet<Symbol, String> sul, String path, String experimentName) {
		super();
		this.seeds = seeds;
		this.sul = sul;
		this.experimentName = experimentName;
		this.path  = path;
		System.setProperty("ranked_random.debug", "true");
	}

	public Evaluator(long[] seeds, SULWithAlphabet<Symbol, String> sul, String path, 
			String experimentName, boolean verbose, boolean skipIfExists) {
		this(seeds,sul,path, experimentName);
		this.verbose = verbose;
		this.skipIfExists = skipIfExists;
	}

	private SULWithAlphabet<Symbol, String> sul = null;
	private JAXBContext statXMLContext;

	public <T extends TestCase> void performMultMeasurementSeries(int startSuiteSize, int endSuiteSize, int stepSize,
			int expectedStates, List<RandomCovSelEquivConfiguration<T>> configs){
		for(RandomCovSelEquivConfiguration<T> c : configs){
			 performMeasurementSeries(startSuiteSize, endSuiteSize, stepSize,expectedStates, c);
		}
	}
	public <T extends TestCase> void performMeasurementSeries(int startSuiteSize, int endSuiteSize, int stepSize,
			int expectedStates, RandomCovSelEquivConfiguration<T> config){
		performMeasurementSeries(startSuiteSize, endSuiteSize, stepSize,config,expectedStates, true);
	}
	public <T extends TestCase> void performMeasurementSeries(int startSuiteSize, int endSuiteSize, int stepSize,
				RandomCovSelEquivConfiguration<T> config, int expectedStates, boolean earlyStop){
		for(int suiteSize = startSuiteSize; suiteSize <= endSuiteSize; suiteSize += stepSize){
			System.out.println("Measurements for suite size = " + suiteSize);
			config.selector.updateSuiteSize(suiteSize);
			String fullFileName = deriveFileName(config.nameSuffix, config.selector.description(),
					config.shortMutationDescription(), suiteSize);
			if(skipIfExists && new File(fullFileName).exists()){
				if(perfectRun(fullFileName))
					break;
				else
					continue;
			}
			
			EvalStatistics statistics = performSingleMeasurement(config, expectedStates);
			try {
				persist(statistics, fullFileName);
			} catch (JAXBException e) {
				System.out.println("Error writing: " + fullFileName);
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
			if(earlyStop && statistics.getNrCorrectRuns() == statistics.getNrRuns())
				break;
		}
		
	}

	private boolean perfectRun(String fullFileName) {
		try{
			statXMLContext = JAXBContext.newInstance(EvalStatistics.class);
			Unmarshaller um = statXMLContext.createUnmarshaller();
	        EvalStatistics stats = (EvalStatistics) um.unmarshal(new FileReader(fullFileName));
	        return stats.getNrRuns() == stats.getNrCorrectRuns();
		} catch (Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	public void persist(EvalStatistics stat, String fullFileName) throws JAXBException, PropertyException {
		statXMLContext = JAXBContext.newInstance(EvalStatistics.class);
		Marshaller m = statXMLContext.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		 // Write to File
		File target = new File(fullFileName);
		if(!target.getParentFile().exists())
			target.getParentFile().mkdirs();
		m.marshal(stat, target);
	}

	public String deriveFileName(String nameSuffix, String selectionDescription, 
			String shortMutOpDesc, int testSuiteSize) {
		String pathExtension = nameToPath(experimentName);
		String fullPath = path + "/" + pathExtension;
		String baseFileName = selectionDescription;
		if(!shortMutOpDesc.equals("none"))
			baseFileName += ("_" + shortMutOpDesc);
		baseFileName += ("_" + testSuiteSize + nameSuffix + ".xml");
		String fullFileName = fullPath + "/" + baseFileName;
		return fullFileName;
	}
	
	private String nameToPath(String experimentName) {
		return experimentName.replace('.', '/');
	}
	
	public <T extends TestCase> EvalStatistics 
		performSingleMeasurement(RandomCovSelEquivConfiguration<T> config, int expectedStates){
		int nrCorrect = 0;
		List<Long> stepsEq = new ArrayList<Long>(seeds.length);
		List<Long> stepsMq = new ArrayList<Long>(seeds.length);
		List<Long> resetsEq = new ArrayList<Long>(seeds.length);
		List<Long> resetsMq = new ArrayList<Long>(seeds.length);
		List<Long> mutationDurations = new ArrayList<Long>(seeds.length);
		List<Long> selectionDurations = new ArrayList<Long>(seeds.length);
		List<Long> generationDurations = new ArrayList<Long>(seeds.length);
		
	    StatisticSUL<Symbol, String> statisticSul = 
	            new ResetCounterSUL<>("membership queries", sul);
	    Alphabet<Symbol> alphabet = sul.getAlphabet();
	    // we specifically do not use a cache, as SULs are simulated caches
	    // tend to really if learning is repeated fifty times with random testing with long
	    // individual tests
	    // SUL<Symbol,String> cacheSul = SULCaches.createCache(alphabet, statisticSul);
	    StepCounterSUL stepCounterSUL = new StepCounterSUL(statisticSul);
	    
	    SULOracle<Symbol, String> mqOracle = new SULOracle<>(stepCounterSUL);
		    // create random walks equivalence test
	    RandomCoverageSelectionEQOracle<T> eqOracle = null;
	    MealyExperiment<Symbol, String> experiment = null;
	    MealyMachine<?, Symbol, ?, String> result = null;
		MealyLearner<Symbol, String> learner = null;
		
		for(long i : seeds){
	    	long currentSeed = (long)i;
	    	config.updateRandomSeed(currentSeed);
		    while(true){
		    	try {
		    		eqOracle = new RandomCoverageSelectionEQOracle<T>(
		    				stepCounterSUL,
		    				sul.getAlphabet(),
		    				config.mutationOperators,
		    				config.sizeTestSelectionSuite,
		    				config.reuseRemaining,
		    				config.selector,
		    				config.traceGen,
		    				config.mutantSampler);
		    		config.selector.updateInitialSuiteSizes(new LinkedList<>(config.initialSuiteSizes));
		    	    eqOracle.setMutantGenerationSampler(config.mutantGenerationSampler);
		    	   	eqOracle.setKeepExecutedTests(config.keepExecutedTests);
		    		if(useLstar)
		    			learner = new ExtensibleLStarMealyBuilder<Symbol,String>()
		    			.withAlphabet(alphabet)
		    			.withOracle(mqOracle)
		    			.create();
		    		else if(useKV)
		    			learner = new KearnsVaziraniMealyBuilder<Symbol,String>()
		    			.withAlphabet(alphabet)
		    			.withOracle(mqOracle)
		    			.withCounterexampleAnalyzer(AcexAnalyzers.BINARY_SEARCH)
		    			.create();
		    		else if(useRV)
		    			learner = new ExtensibleLStarMealy<>(alphabet, 
		    					mqOracle, Collections.emptyList(), 
		    					ObservationTableCEXHandlers.RIVEST_SCHAPIRE, 
		    					ClosingStrategies.CLOSE_SHORTEST); // to match setup of Smeenk et al.
		    		else
				    	learner = new TTTLearnerMealyBuilder<Symbol,String>()
				    		.withAlphabet(alphabet) // input alphabet
				    		.withOracle(mqOracle)			  // membership oracle
				    		.create();

	    			experiment = new MealyExperiment<>(learner, eqOracle, alphabet);
				    if(!verbose)
				    	LearnLogger.getLogger(Experiment.class).setLevel(Level.WARNING);
				    experiment.run();
				    // get learned model
				    result = experiment.getFinalHypothesis();
				    stepsEq.add(eqOracle.getNrActualTestSteps());
				    resetsEq.add((long) eqOracle.getNrExecutedTests());
				    stepsMq.add(stepCounterSUL.getStepCount() - eqOracle.getNrActualTestSteps());
				    resetsMq.add((long) (stepCounterSUL.getResets() - eqOracle.getNrExecutedTests()));
				    mutationDurations.add(eqOracle.getMutationDuration());
				    selectionDurations.add(eqOracle.getEvalAndSelDuration());
				    generationDurations.add(eqOracle.getGenerationDuration());
				    System.out.println("Executed " + eqOracle.getNrActualTestSteps() + 
				    		" eq steps.");
				    resetCounts(eqOracle,stepCounterSUL);
				    break;
				    // Exceptions are thrown by TTT in latest Maven-release of LearnLib
				    // probably fixed on github, but I rather ignore such an exception and work with Maven
				    // TTT is not used for the experiments anyway
		    	} catch (Exception e){
		    		int stackI = -1;
		    		while(e.getStackTrace()[++stackI].getClassName().startsWith("java"));
		    		if(e.getStackTrace()[stackI].getClassName().startsWith("de.learnlib")){
			    		resetCounts(eqOracle,stepCounterSUL);
			    		System.out.println("Caught exception with seed " + currentSeed);
			    		System.out.println(e.getMessage());
			    		if(e.getCause() != null){
			    			System.out.println(e.getCause().getMessage());
			    			e.getCause().printStackTrace();
			    		}
			    		e.printStackTrace();
			    		currentSeed ++;
		    		}
		    		else throw e;
		    	}
		    }
		    // report results
		    System.out.println("-------------------------------------------------------");
			// because of minimality of the result we know that we learned the correct model by checking the number of states //sanityCheckOracle.findCounterExample(result, alphabet) == null)
			if( result.size() == expectedStates) {
				nrCorrect ++;
				System.out.println("We learned the correct model for " + i);
				System.out.println();
			}
			else 
				System.out.println("We did not learn the correct model for " + i);
		    if(verbose){
				System.out.println("Actual steps executed: " + eqOracle.getNrActualTestSteps());
			    // profiling
			    System.out.println(SimpleProfiler.getResults());
			
			    // learning statistics
			    System.out.println(experiment.getRounds().getSummary());
			    System.out.println(statisticSul.getStatisticalData().getSummary());    
			    // model statistics
			    System.out.println("States: " + result.size());
		    }
		    try{
		        File outputFile = new File("SUL_mm.dot");
		        PrintStream psDotFile = new PrintStream(outputFile);
		        GraphDOT.write(result, alphabet, psDotFile); // may throw IOException!
		    }catch(Exception e){
		    }
		}
	    System.out.println("We had "+ nrCorrect + " correct and executed " + stepsEq.get(stepsEq.size()-1) + 
				" equiv steps in total and " + stepsMq.get(stepsEq.size()-1) + " steps for membership queries");
		
		return new EvalStatistics(
				experimentName,
				config.selector.description(),
				config.mutationDescription(),
				config.shortMutationDescription(),
				config.mutantSampler.description(),
				config.traceGen.description(),
				config.sizeTestSelectionSuite,
				config.selector.getSuiteSize(),
				sum(stepsEq), stepsEq,
				sum(stepsMq), stepsMq,
				sum(resetsEq), resetsEq,
				sum(resetsMq), resetsMq,
				seeds.length,nrCorrect,
				sum(mutationDurations), mutationDurations,
				sum(selectionDurations), selectionDurations,
				sum(generationDurations),generationDurations
				);
	}

	private long sum(List<Long> longs){
		return longs.stream().reduce(0l, (x,y) -> x+y);
	}
	private <T extends TestCase> void 
	resetCounts(RandomCoverageSelectionEQOracle<T> eqOracle, StepCounterSUL stepCounterSUL) {
		eqOracle.resetEvalAndSelDuration();
		eqOracle.resetGenerationDuration();
		eqOracle.resetMutationDuration();
		eqOracle.resetNrActualTestSteps();
		eqOracle.resetNrExecutedTests();
		stepCounterSUL.resetStepCount();
		stepCounterSUL.setResets(0);
	}

	public void setUseRV() {
		useLstar = false;
		useKV = false;
		useRV = true;
	}

	public boolean isShrink() {
		return shrink;
	}

	public void setShrink(boolean shrink) {
		this.shrink = shrink;
	}
}
