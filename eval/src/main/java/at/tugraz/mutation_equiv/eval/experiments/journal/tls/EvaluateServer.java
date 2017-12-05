package at.tugraz.mutation_equiv.eval.experiments.journal.tls;

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
import at.tugraz.mutation_equiv.mutation.MutationOperator;
import at.tugraz.mutation_equiv.mutation.SplitStateOperator;
import at.tugraz.mutation_equiv.mutation.sampling.impl.CompositionSampler;
import at.tugraz.mutation_equiv.mutation.sampling.impl.ElementBasedBoundSampler;
import at.tugraz.mutation_equiv.mutation.sampling.impl.ElementBasedFractionSampler;
import at.tugraz.mutation_equiv.mutation.sampling.impl.IdentitySampler;
import at.tugraz.mutation_equiv.mutation.sampling.impl.ReduceToMeanSampler;
import at.tugraz.mutation_equiv.mutation.sampling.impl.ReduceToMedianSampler;
import at.tugraz.mutation_equiv.mutation.sampling.impl.ReduceToMinSampler;
import at.tugraz.mutation_equiv.test_selection.MutationSuiteBasedSelector;
import at.tugraz.mutation_equiv.test_selection.RandomSelector;
import at.tugraz.mutation_equiv.trace_gen.AdaptedLeeYannakakisGenerator;
import at.tugraz.mutation_equiv.trace_gen.MuDirectedIterativeGenerator;
import at.tugraz.mutation_equiv.trace_gen.RandomWordGenerator;

public class EvaluateServer {
	@SuppressWarnings("unchecked")
	static <T extends TestCase> List<RandomCovSelEquivConfiguration<T>> configs(MultipleModeConfig config) {
		List<RandomCovSelEquivConfiguration<T>> configs = new ArrayList<>();
//		List<MutationOperator> ops = new ArrayList<>();
//		SplitStateOperator splitOp = new SplitStateOperator(config.sul.getAlphabet(), 1);
//		// probably too high to be restriction, but this is intended as we do
//		// not want to restrict
//		splitOp.setAccSeqBound(300);
//		splitOp.setAllowDiffInLastAccSymbol(true);
//		splitOp.setAllowEqualPreStateAndSymbol(true);
//
//		ops.add(splitOp);
//		ops.add(new ChangeOutputOperator(config.sul.getAlphabet()));
//
//		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit1OutGenMuDirectedConfig = new RandomCovSelEquivConfiguration<MutationTestCase>(
//				ops, config.mutChangeOutputConfig.sizeTestSelectionSuite, false,
//				new MutationSuiteBasedSelector(0, config.sul.getAlphabet(), false, true), config.defaultTraceGen,
//				new IdentitySampler());
//		mutSplit1OutGenMuDirectedConfig.mutantSampler = new CompositionSampler(
//				new ReduceToMeanSampler(new Random(), "split-state-state"),
//				new ElementBasedFractionSampler(1, new Random(), "operator:split-state-1") // ,
		// new ElementBasedBoundSampler(40000, new
		// Random(),"operator:split-state-2")
		// to reduce mutants for first steps
//		);

//		mutSplit1OutGenMuDirectedConfig.mutantGenerationSampler = new CompositionSampler(
//				new ElementBasedBoundSampler(0, new Random(), "operator:split-state-1"),
//				new ElementBasedBoundSampler(0, new Random(), "operator:split-state-2"),
//				new ElementBasedBoundSampler(0, new Random(), "operator:split-state-3"));
//		mutSplit1OutGenMuDirectedConfig.keepExecutedTests = true;
//		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit1OutGenMuDirectedConfig);

//		config.randomConfig.traceGen = new RandomWordGenerator(config.sul.getAlphabet(), new Random(), 40);
//		configs.add((RandomCovSelEquivConfiguration<T>) config.randomConfig);

//		RandomCovSelEquivConfiguration<MutationTestCase> mutChangeOut = config.mutChangeOutputConfig;
//		configs.add((RandomCovSelEquivConfiguration<T>) mutChangeOut);
//		mutChangeOut.reuseRemaining = false;

		RandomCovSelEquivConfiguration<SimpleTestCase> ly = new RandomCovSelEquivConfiguration<SimpleTestCase>(
				Collections.emptyList(), upperNrTests, false, new RandomSelector(0),
				new AdaptedLeeYannakakisGenerator("src/main/resources/lee_yannakakis", "minimal", 3,
						config.sul.getAlphabet()),
				new IdentitySampler());
		configs.add((RandomCovSelEquivConfiguration<T>) ly);
		ly.nameSuffix = "_ly";

		return configs;
	}

	private static int upperNrTests = 16000;

	public static void main(String[] args) {
		for (String sulName : new String[] { //"GnuTLS_3.3.8_server_full",
//												 "GnuTLS_3.3.12_server_full",
//				"miTLS_0.1.3_server_regular", 
				//"NSS_3.17.4_server_regular",
//				"OpenSSL_1.0.1j_server_regular",
				"OpenSSL_1.0.1l_server_regular" //
//				"OpenSSL_1.0.2_server_regular"
				//"RSA_BSAFE_C_4.0.4_server_regular",
				//"RSA_BSAFE_Java_6.1.1_server_regular" 
		}) {

			MealyDotFileSul sul = new MealyDotFileSul("src/main/resources/tls/" + sulName + "_trans.dot");
			MultipleModeConfig config = new MultipleModeConfig(20, 300000, sul,
					new MuDirectedIterativeGenerator(0.05, 0.95, sul.getAlphabet(), 20, 3));

			Evaluator evaluator = new Evaluator(RandomSeedSample.seeds, sul, "journal_experiments",
					"tls.server." + sulName.replace('.', '_'));
			evaluator.setUseRV();
			evaluator.performMultMeasurementSeries(15000, upperNrTests, 100, sul.getMealy().size(), configs(config));
		}
	}
}
