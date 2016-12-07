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
package at.tugraz.mutation_equiv.eval.experiments.tcp.bsd;
import java.util.ArrayList;
import java.util.List;

import at.tugraz.learning.suls.MealyDotFileSul;
import at.tugraz.mutation_equiv.TestCase;
import at.tugraz.mutation_equiv.configuration.RandomCovSelEquivConfiguration;
import at.tugraz.mutation_equiv.eval.Evaluator;
import at.tugraz.mutation_equiv.eval.experiments.MultipleModeConfig;
import at.tugraz.mutation_equiv.eval.experiments.RandomSeedSample;
import at.tugraz.mutation_equiv.trace_gen.MuDirectedIterativeGenerator;

public class EvaluateClient {
	
	static <T extends TestCase> List<RandomCovSelEquivConfiguration<T>> configs(MultipleModeConfig config){
		List<RandomCovSelEquivConfiguration<T>> configs = new ArrayList<>();

		// TODO add configs		

		return configs;
	}
	public static void main(String[] args){
		MealyDotFileSul sul = new MealyDotFileSul("src/main/resources/tcp/tcp_client_bsd_trans.dot");
		MultipleModeConfig config = new MultipleModeConfig(30, 60000, sul, 
				new MuDirectedIterativeGenerator(0.1, 0.9, sul.getAlphabet(), 40,2));
		Evaluator evaluator = 
				new Evaluator(RandomSeedSample.seeds,sul, "experiments", "tcp.bsd.client");
		evaluator.performMultMeasurementSeries(1000, 9000, 500, sul.getMealy().size(), configs(config));
	}

}
