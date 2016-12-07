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
package at.tugraz.mutation_equiv.eval.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import at.tugraz.learning.suls.SULWithAlphabet;
import at.tugraz.mutation_equiv.MutationTestCase;
import at.tugraz.mutation_equiv.SimpleTestCase;
import at.tugraz.mutation_equiv.configuration.RandomCovSelEquivConfiguration;
import at.tugraz.mutation_equiv.mutation.ChangeOutputOperator;
import at.tugraz.mutation_equiv.mutation.ChangeTargetOperator;
import at.tugraz.mutation_equiv.mutation.MutationOperator;
import at.tugraz.mutation_equiv.mutation.SplitStateOperator;
import at.tugraz.mutation_equiv.mutation.sampling.MutantSamplingStrategy;
import at.tugraz.mutation_equiv.mutation.sampling.impl.CompositionSampler;
import at.tugraz.mutation_equiv.mutation.sampling.impl.IdentitySampler;
import at.tugraz.mutation_equiv.mutation.sampling.impl.OverallFractionSampler;
import at.tugraz.mutation_equiv.mutation.sampling.impl.ReduceToMeanSampler;
import at.tugraz.mutation_equiv.test_selection.LengthBasedSelector;
import at.tugraz.mutation_equiv.test_selection.MutationSuiteBasedSelector;
import at.tugraz.mutation_equiv.test_selection.RandomSelector;
import at.tugraz.mutation_equiv.trace_gen.MuDirectedRandomWordGenerator;
import at.tugraz.mutation_equiv.trace_gen.RandomWordGenerator;
import at.tugraz.mutation_equiv.trace_gen.TraceGenerator;
import net.automatalib.words.impl.Symbol;

/**
 * Some default configurations used during development. It turned out
 * that most of them do not work exceptionally well. It should also 
 * be noted that split-state with the depth parameter 0 does not make a lot
 * of sense from fault-based testing view (learning algorithms define transitions
 * between states using S * I where S are access sequence so sequence of length
 * 1 (split-state depth = 0) are covered for many access sequences). 
 * 
 * @author Martin Tappler
 *
 */
public class MultipleModeConfig {
	// "global" parameters 
	
	// note that the constant random is overwritten in the evaluation so this
	// is just a placeholder 
	private static final Random constantRandSource = new Random(1);
	public boolean reuseRemaining = false;
	
	public boolean defaultKillAliveMutants = false;
	public TraceGenerator defaultTraceGen = null;
	public MutantSamplingStrategy defaultSampler = null;
	public RandomCovSelEquivConfiguration<MutationTestCase> mutChangeTargetConfig = null;
	public RandomCovSelEquivConfiguration<MutationTestCase> mutSplit0OutputConfig = null;
	public RandomCovSelEquivConfiguration<MutationTestCase> mutSplit1OutputConfig = null;
	public RandomCovSelEquivConfiguration<MutationTestCase> mutSplit1Config = null;
	public  RandomCovSelEquivConfiguration<MutationTestCase> mutSplit1MuDirectedConfig = null;
	public  RandomCovSelEquivConfiguration<MutationTestCase> mutSplit0MuDirectedConfig = null;
	public  RandomCovSelEquivConfiguration<MutationTestCase> mutSplit0OutputMuDirectedConfig = null;
	public RandomCovSelEquivConfiguration<MutationTestCase> mutSplit0Config = null;
	public RandomCovSelEquivConfiguration<SimpleTestCase> randomConfig = null;
	public RandomCovSelEquivConfiguration<SimpleTestCase> lengthConfig = null;
	public  RandomCovSelEquivConfiguration<MutationTestCase> mutChangeOutputConfig = null;
	public SULWithAlphabet<Symbol, String> sul;
	public MultipleModeConfig(int maxTestLength, 
			int sizeTestSelectionSuite, SULWithAlphabet<Symbol, String> sul){
		this(maxTestLength,sizeTestSelectionSuite, sul,new RandomWordGenerator(sul.getAlphabet(), 
				constantRandSource, maxTestLength));
		
	}

	public MultipleModeConfig(int maxTestLength, int sizeTestSelectionSuite, SULWithAlphabet<Symbol, String> sul, 
			TraceGenerator defaultGen){
		this(maxTestLength,sizeTestSelectionSuite,sul,defaultGen,false);
	}
	public MultipleModeConfig(int maxTestLength, int sizeTestSelectionSuite, SULWithAlphabet<Symbol, String> sul, 
			TraceGenerator defaultGen, boolean defaultReuse){
		this.reuseRemaining = defaultReuse;
		defaultTraceGen = defaultGen;
		this.sul = sul;
		defaultSampler = new IdentitySampler();

		ArrayList<MutationOperator> split0OutputOp = new ArrayList<MutationOperator>();
		split0OutputOp.add(new SplitStateOperator(sul.getAlphabet(), 0));
		split0OutputOp.add(new ChangeOutputOperator(sul.getAlphabet()));

		mutSplit0OutputConfig = new RandomCovSelEquivConfiguration<>(split0OutputOp, 
				sizeTestSelectionSuite, reuseRemaining, 
				new MutationSuiteBasedSelector(0,sul.getAlphabet(),defaultKillAliveMutants ), 
				defaultTraceGen, new IdentitySampler());
		
		mutSplit0OutputMuDirectedConfig = new RandomCovSelEquivConfiguration<>(split0OutputOp, 
						sizeTestSelectionSuite, reuseRemaining, 
						new MutationSuiteBasedSelector(0,sul.getAlphabet(),defaultKillAliveMutants ), 
						new MuDirectedRandomWordGenerator(maxTestLength/2, 
								maxTestLength/2, new Random(1), sul.getAlphabet()), new IdentitySampler());

		ArrayList<MutationOperator> split1OutputOp = new ArrayList<MutationOperator>();
		split1OutputOp.add(new SplitStateOperator(sul.getAlphabet(), 1));
		split1OutputOp.add(new ChangeOutputOperator(sul.getAlphabet()));
		mutSplit1OutputConfig = new RandomCovSelEquivConfiguration<>(split1OutputOp, 
				sizeTestSelectionSuite, reuseRemaining, 
				new MutationSuiteBasedSelector(0,sul.getAlphabet(),defaultKillAliveMutants ), 
				defaultTraceGen, new CompositionSampler(
//						new ElementBasedFractionSampler(2, constantRandSource, "operator:change-output"),
						new ReduceToMeanSampler(constantRandSource, "operator"),
						new OverallFractionSampler(1, constantRandSource)));
		
		mutChangeTargetConfig = 
				new RandomCovSelEquivConfiguration<>(Collections.singletonList(new ChangeTargetOperator(sul.getAlphabet())), 
						sizeTestSelectionSuite, reuseRemaining, 
						new MutationSuiteBasedSelector(0,sul.getAlphabet(),defaultKillAliveMutants ), 
						defaultTraceGen, defaultSampler);
		mutSplit1Config = 
				new RandomCovSelEquivConfiguration<>(Collections.singletonList(new SplitStateOperator(sul.getAlphabet(), 1)), 
						sizeTestSelectionSuite, reuseRemaining, 
						new MutationSuiteBasedSelector(0,sul.getAlphabet(),defaultKillAliveMutants ), 
						defaultTraceGen, 
						defaultSampler);
		mutSplit1MuDirectedConfig = 
				new RandomCovSelEquivConfiguration<>(Collections.singletonList(new SplitStateOperator(sul.getAlphabet(), 1)), 
						sizeTestSelectionSuite, reuseRemaining, 
						new MutationSuiteBasedSelector(0,sul.getAlphabet(),defaultKillAliveMutants ), 
						new MuDirectedRandomWordGenerator(maxTestLength/2, maxTestLength/2, constantRandSource,
								sul.getAlphabet()), defaultSampler);
		mutSplit0MuDirectedConfig = 
				new RandomCovSelEquivConfiguration<>(Collections.singletonList(new SplitStateOperator(sul.getAlphabet(), 0)), 
						sizeTestSelectionSuite, reuseRemaining, 
						new MutationSuiteBasedSelector(0,sul.getAlphabet(),defaultKillAliveMutants ), 
						new MuDirectedRandomWordGenerator(maxTestLength/2, maxTestLength/2, constantRandSource,
								sul.getAlphabet()), defaultSampler);

		mutSplit0Config = 
				new RandomCovSelEquivConfiguration<>(Collections.singletonList(new SplitStateOperator(sul.getAlphabet(),0)), 
						sizeTestSelectionSuite, reuseRemaining, 
						new MutationSuiteBasedSelector(0,sul.getAlphabet(),defaultKillAliveMutants ), 
						defaultTraceGen, defaultSampler);
		randomConfig = 
				new RandomCovSelEquivConfiguration<>(Collections.emptyList(), 
						sizeTestSelectionSuite, reuseRemaining, 
						new RandomSelector(0), defaultTraceGen, defaultSampler);
		lengthConfig = 
				new RandomCovSelEquivConfiguration<>(Collections.emptyList(), 
						sizeTestSelectionSuite, reuseRemaining, 
						new LengthBasedSelector(0), defaultTraceGen, defaultSampler);
		mutChangeOutputConfig = 
				new RandomCovSelEquivConfiguration<>(Collections.singletonList(new ChangeOutputOperator(sul.getAlphabet())), 
						sizeTestSelectionSuite, reuseRemaining, 
						new MutationSuiteBasedSelector(0,sul.getAlphabet(),defaultKillAliveMutants ), 
						defaultTraceGen, new IdentitySampler());
		mutSplit1MuDirectedConfig.nameSuffix = "_mudirected";
		mutSplit0MuDirectedConfig.nameSuffix = "_mudirected";
		mutSplit0OutputMuDirectedConfig.nameSuffix = "_mudirected";
		
	}
}
