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
package at.tugraz.mutation_equiv.eval.experiments.tls.gnu_tls_3_3_8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import at.tugraz.learning.suls.MealyDotFileSul;
import at.tugraz.mutation_equiv.MutationTestCase;
import at.tugraz.mutation_equiv.SimpleTestCase;
import at.tugraz.mutation_equiv.TestCase;
import at.tugraz.mutation_equiv.configuration.RandomCovSelEquivConfiguration;
import at.tugraz.mutation_equiv.eval.Evaluator;
import at.tugraz.mutation_equiv.eval.experiments.MultipleModeConfig;
import at.tugraz.mutation_equiv.eval.experiments.RandomSeedSample;
import at.tugraz.mutation_equiv.mutation.ChangeOutputOperator;
import at.tugraz.mutation_equiv.mutation.sampling.impl.IdentitySampler;
import at.tugraz.mutation_equiv.test_selection.RandomSelector;
import at.tugraz.mutation_equiv.trace_gen.MuDirectedRandomWordGenerator;

public class EvaluateClientRegular {
	
	
	@SuppressWarnings("unchecked")
	static <T extends TestCase> List<RandomCovSelEquivConfiguration<T>> configs(MultipleModeConfig config){
		List<RandomCovSelEquivConfiguration<T>> configs = new ArrayList<>();

		// TODO use different configs (changeout generation + split-state selection should work)
		RandomCovSelEquivConfiguration<MutationTestCase> mutChangeOut = config.mutChangeOutputConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutChangeOut);
		
		RandomCovSelEquivConfiguration<MutationTestCase> mutChangeAndSplit1 = config.mutSplit1OutputConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutChangeAndSplit1);

		RandomCovSelEquivConfiguration<SimpleTestCase> randChangeOut = new RandomCovSelEquivConfiguration<SimpleTestCase>(
				Collections.singletonList(new ChangeOutputOperator(config.sul.getAlphabet())), 
				config.mutSplit0Config.sizeTestSelectionSuite, 
				false, new RandomSelector(0), 
				config.defaultTraceGen, new IdentitySampler());
		configs.add((RandomCovSelEquivConfiguration<T>) randChangeOut);
		
		RandomCovSelEquivConfiguration<MutationTestCase> mutChangeAndSplit = config.mutSplit0OutputConfig;
		mutChangeAndSplit.mutantSampler = new IdentitySampler();
		configs.add((RandomCovSelEquivConfiguration<T>) mutChangeAndSplit);

		return configs;
	}
	public static void main(String[] args){
		MealyDotFileSul sul = new MealyDotFileSul("src/main/resources/tls/GnuTLS_3.3.8_client_regular_trans.dot");
		Random randGen = new Random(1);
		int nrTests = 140000;
		MultipleModeConfig config = new MultipleModeConfig(30, nrTests, sul, 
				new MuDirectedRandomWordGenerator(10, 10, randGen, sul.getAlphabet()));
		Evaluator evaluator = 
				new Evaluator(RandomSeedSample.seeds,sul, "experiments", "tls.gnutls_3_3_8.client_regular");
		// needs wp depth 4
		evaluator.performMultMeasurementSeries(2000, 14000, 2000, sul.getMealy().size(), configs(config));
	}

}
