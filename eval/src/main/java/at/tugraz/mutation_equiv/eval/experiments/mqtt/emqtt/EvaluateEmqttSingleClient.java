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
package at.tugraz.mutation_equiv.eval.experiments.mqtt.emqtt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import at.tugraz.learning.suls.MealyDotFileSul;
import at.tugraz.learning.suls.SULWithAlphabet;
import at.tugraz.mutation_equiv.MutationTestCase;
import at.tugraz.mutation_equiv.SimpleTestCase;
import at.tugraz.mutation_equiv.TestCase;
import at.tugraz.mutation_equiv.configuration.RandomCovSelEquivConfiguration;
import at.tugraz.mutation_equiv.eval.Evaluator;
import at.tugraz.mutation_equiv.eval.experiments.MultipleModeConfig;
import at.tugraz.mutation_equiv.eval.experiments.RandomSeedSample;
import at.tugraz.mutation_equiv.mutation.ChangeOutputOperator;
import at.tugraz.mutation_equiv.mutation.sampling.impl.IdentitySampler;
import at.tugraz.mutation_equiv.test_selection.MutationSuiteBasedSelector;
import at.tugraz.mutation_equiv.test_selection.RandomSelector;
import at.tugraz.mutation_equiv.trace_gen.AdaptedLeeYannakakisGenerator;
import net.automatalib.words.impl.Symbol;

public class EvaluateEmqttSingleClient {
	
	
	@SuppressWarnings("unchecked")
	static <T extends TestCase> List<RandomCovSelEquivConfiguration<T>> configs(MultipleModeConfig config){
		List<RandomCovSelEquivConfiguration<T>> configs = new ArrayList<>();
		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit0Config = config.mutSplit0Config;
		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit0Config);
		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit0OutputConfig = config.mutSplit0OutputConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit0OutputConfig);
		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit1Config = config.mutSplit1Config;
		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit1Config);
		
		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit1MuDirectedConfig = 
				config.mutSplit1MuDirectedConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit1MuDirectedConfig);

		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit0MuDirectedConfig = config.mutSplit0MuDirectedConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit0MuDirectedConfig);
		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit0OutputMuDirectedConfig = 
				config.mutSplit0OutputMuDirectedConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit0OutputMuDirectedConfig);
		RandomCovSelEquivConfiguration<SimpleTestCase> randomConfig = config.randomConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) randomConfig);
		RandomCovSelEquivConfiguration<SimpleTestCase> lengthConfig = config.lengthConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) lengthConfig);
		RandomCovSelEquivConfiguration<MutationTestCase> mutChangeOutputConfig = config.mutChangeOutputConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutChangeOutputConfig);
		RandomCovSelEquivConfiguration<MutationTestCase> mutChangeTargetConfig = config.mutChangeTargetConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutChangeTargetConfig);

		RandomCovSelEquivConfiguration<SimpleTestCase> ly = new RandomCovSelEquivConfiguration<SimpleTestCase>(
				Collections.emptyList(), 
				upperNrTests, 
				false, 
				new RandomSelector(0),
				new AdaptedLeeYannakakisGenerator("src/main/resources/lee_yannakakis", 
						"minimal", 2, config.sul.getAlphabet()),
				new IdentitySampler());
		configs.add((RandomCovSelEquivConfiguration<T>) ly);
		ly.nameSuffix = "_ly";
		
		RandomCovSelEquivConfiguration<MutationTestCase> mutChangeOutputConfigMuDirected = 
				new RandomCovSelEquivConfiguration<>(Collections.singletonList(
				new ChangeOutputOperator(config.sul.getAlphabet())), 
				config.mutChangeOutputConfig.sizeTestSelectionSuite, false, 
				new MutationSuiteBasedSelector(0,config.sul.getAlphabet(),false), 
				config.mutSplit0MuDirectedConfig.traceGen, new IdentitySampler());
		mutChangeOutputConfigMuDirected.nameSuffix = "_mu_directed";
		
		configs.add((RandomCovSelEquivConfiguration<T>) mutChangeOutputConfigMuDirected);
		
		return configs;
	}
	private static int upperNrTests = 2000;
	public static void main(String[] args){
		SULWithAlphabet<Symbol, String> sul = new MealyDotFileSul("src/main/resources/mqtt/emqtt/single_client.dot");
		MultipleModeConfig config = new MultipleModeConfig(40, 8000, sul);
		Evaluator evaluator = 
				new Evaluator(RandomSeedSample.seeds,sul, "experiments", "mqtt.emqtt.single_client");
		evaluator.performMultMeasurementSeries(50, upperNrTests, 50, 10, configs(config));
	}

}
