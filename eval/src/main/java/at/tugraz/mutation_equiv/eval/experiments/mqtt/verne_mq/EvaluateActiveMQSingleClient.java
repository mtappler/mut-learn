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
package at.tugraz.mutation_equiv.eval.experiments.mqtt.verne_mq;

import java.util.ArrayList;
import java.util.List;

import at.tugraz.learning.suls.MealyDotFileSul;
import at.tugraz.mutation_equiv.MutationTestCase;
import at.tugraz.mutation_equiv.SimpleTestCase;
import at.tugraz.mutation_equiv.TestCase;
import at.tugraz.mutation_equiv.configuration.RandomCovSelEquivConfiguration;
import at.tugraz.mutation_equiv.eval.Evaluator;
import at.tugraz.mutation_equiv.eval.experiments.MultipleModeConfig;
import at.tugraz.mutation_equiv.eval.experiments.RandomSeedSample;

public class EvaluateActiveMQSingleClient {
	
	@SuppressWarnings("unchecked")
	static <T extends TestCase> List<RandomCovSelEquivConfiguration<T>> configs(MultipleModeConfig config){
		List<RandomCovSelEquivConfiguration<T>> configs = new ArrayList<>();
		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit0Config = config.mutSplit0Config;
		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit0Config);
		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit0OutputConfig = config.mutSplit0OutputConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit0OutputConfig);
		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit1Config = config.mutSplit1Config;
		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit1Config);	

		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit0OutputMuDirectedConfig = 
				config.mutSplit0OutputMuDirectedConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit0OutputMuDirectedConfig);
		
		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit1MuDirectedConfig = 
				config.mutSplit1MuDirectedConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit1MuDirectedConfig);
		
		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit0MuDirectedConfig = config.mutSplit0MuDirectedConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit0MuDirectedConfig);

		RandomCovSelEquivConfiguration<SimpleTestCase> randomConfig = config.randomConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) randomConfig);
		RandomCovSelEquivConfiguration<SimpleTestCase> lengthConfig = config.lengthConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) lengthConfig);
		RandomCovSelEquivConfiguration<MutationTestCase> mutChangeOutputConfig = config.mutChangeOutputConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutChangeOutputConfig);
		RandomCovSelEquivConfiguration<MutationTestCase> mutChangeTargetConfig = config.mutChangeTargetConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutChangeTargetConfig);

		return configs;
	}
	public static void main(String[] args){
		MealyDotFileSul sul = new MealyDotFileSul("src/main/resources/mqtt/VerneMQ/single_client.dot");
		MultipleModeConfig config = new MultipleModeConfig(40, 8000, sul);
		Evaluator evaluator = 
				new Evaluator(RandomSeedSample.seeds,sul, "experiments", "mqtt.verne_mq.single_client");
		evaluator.performMultMeasurementSeries(50, 2000, 50, sul.getMealy().size(), configs(config));
	}

}
