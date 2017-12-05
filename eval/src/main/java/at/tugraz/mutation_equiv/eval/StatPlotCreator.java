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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Optional;

/**
 * Utility class for creating plots of statistics using the LaTex-package
 * pgfplots.
 * 
 * Currently, it implements plots of the estimated probability of learning the
 * correct with respect to test suite size and with respect to the mean number
 * of steps executed during equivalence testing. It creates one plot line for
 * each testing strategy.
 * 
 * @author Martin Tappler
 *
 */
public class StatPlotCreator {

	private static String fileContentsStart(String name) {
		return String.format("\\begin{filecontents*}{%s.dat}", name);
	}

	private static final String fileContentsEnd = "\\end{filecontents*}";
	private static final String tikzStart = "\\begin{tikzpicture} \\begin{axis}";
	private static final String tikzEnd = "\\end{axis} \\end{tikzpicture} ";
	private static final String equivalence = "eq.";
	private static final String membership = "mem.";

	public static void meanEqStepsToProbCorrect(String measDataPath, int maxCorrect)
			throws JAXBException, FileNotFoundException {
		Map<String, List<EvalStatistics>> groupedByStrategy = groupStrategies(measDataPath);

		System.out.println(tikzStart);
		System.out.println("[xlabel = mean \\# steps[equivalence], ylabel=prob. correct  ]");
		for (String strategy : groupedByStrategy.keySet()) {
			List<EvalStatistics> stats = groupedByStrategy.get(strategy);
			System.out.println(String.format("\\addplot[color=red,mark=x] coordinates { %% %s", strategy));
			stats = stats.stream().sorted((s1, s2) -> (int) (s1.getStepsEqSum() - s2.getStepsEqSum()))
					.collect(Collectors.toList());
			for (EvalStatistics stat : stats) {
				double meanNrSteps = stat.getStepsEqSum() / (double) maxCorrect;
				double prob = stat.getNrCorrectRuns() / (double) maxCorrect;
				System.out.println(String.format("(%.2f, %.2f)", meanNrSteps, prob));
			}
			System.out.println("};");
		}

		System.out.println(tikzEnd);
	}

	private static Map<String, List<EvalStatistics>> groupStrategies(String measDataPath)
			throws JAXBException, FileNotFoundException {
		File measDir = new File(measDataPath);
		File[] measFiles = measDir.listFiles(f -> f.getAbsolutePath().endsWith(".xml"));
		Map<String, List<EvalStatistics>> groupedByStrategy = new HashMap<>();
		JAXBContext statXMLContext = JAXBContext.newInstance(EvalStatistics.class);
		Unmarshaller um = statXMLContext.createUnmarshaller();
		for (File measFile : measFiles) {
			String strategy = getStrategy(measFile);
			if (!groupedByStrategy.containsKey(strategy)) {
				groupedByStrategy.put(strategy, new ArrayList<>());
			}

			EvalStatistics stats = (EvalStatistics) um.unmarshal(new FileReader(measFile));
			groupedByStrategy.get(strategy).add(stats);
		}
		return groupedByStrategy;
	}

	public static void nrCorrect(String measDataPath, String experimentName, int maxCorrect)
			throws JAXBException, FileNotFoundException {
		File measDir = new File(measDataPath);
		File[] measFiles = measDir.listFiles(f -> f.getAbsolutePath().endsWith(".xml"));
		Map<String, List<EvalStatistics>> groupedByStrategy = new HashMap<>();
		JAXBContext statXMLContext = JAXBContext.newInstance(EvalStatistics.class);
		Unmarshaller um = statXMLContext.createUnmarshaller();
		Set<Long> testSuiteSizes = new HashSet<>();
		for (File measFile : measFiles) {
			String strategy = getStrategy(measFile);
			if (!groupedByStrategy.containsKey(strategy)) {
				groupedByStrategy.put(strategy, new ArrayList<>());
			}

			EvalStatistics stats = (EvalStatistics) um.unmarshal(new FileReader(measFile));
			groupedByStrategy.get(strategy).add(stats);
			testSuiteSizes.add(stats.getTestSuiteSize());
		}
		String fileName = "nr_correct_" + experimentName;
		System.out.println(fileContentsStart(fileName));
		System.out.print("suite-size,");
		List<String> sortedStrategies = new ArrayList<>();
		sortedStrategies.addAll(groupedByStrategy.keySet());
		Collections.sort(sortedStrategies);
		System.out.println(String.join(",", sortedStrategies));
		List<Long> testSuiteSizesSorted = new ArrayList<>();
		testSuiteSizesSorted.addAll(testSuiteSizes);
		Collections.sort(testSuiteSizesSorted);
		for (Long suiteSize : testSuiteSizesSorted) {
			List<String> lineContents = new ArrayList<>();
			for (String strategy : sortedStrategies) {
				List<EvalStatistics> measData = groupedByStrategy.get(strategy);
				EvalStatistics measDataForSize = null;
				for (EvalStatistics data : measData)
					if (data.getTestSuiteSize() == suiteSize)
						measDataForSize = data;
				if (measDataForSize != null)
					lineContents.add(Double.toString(measDataForSize.getNrCorrectRuns() / (double) maxCorrect));
				else
					lineContents.add(Double.toString(1.0));
			}
			System.out.print(Long.toString(suiteSize) + ",");
			System.out.println(String.join(",", lineContents));
		}
		System.out.println(fileContentsEnd);

		System.out.println(tikzStart);
		for (String strategy : sortedStrategies) {
			System.out
					.println("\\addplot table[x=suite-size, y=" + strategy + ",col sep=comma] {" + fileName + ".dat};");
		}
		System.out.println(tikzEnd);
	}

	private static String getStrategy(File measFile) {
		String strategy = measFile.getName().replaceAll("_\\d+\\.xml", "")
				.replaceAll("\\d+_mudirected\\.xml", "mudirected").replaceAll("\\d+_wp_method\\.xml", "_wp_method")
				.replaceAll("\\d+_ly\\.xml", "_ly");
		return strategy;
	}

	private static <T extends Number & Comparable<? super T>> double quantile(List<T> elems, double p) {
		if (elems.size() == 1)
			return elems.get(0).doubleValue();
		List<T> copy = new ArrayList<>(elems);
		Collections.sort(copy);
		double g = (copy.size() - 1) * p - Math.floor((copy.size() - 1) * p);
		int index = (int) Math.floor((copy.size() - 1) * p);
		double result = (1 - g) * copy.get(index + 1).doubleValue() + g * copy.get(index + 2).doubleValue();
		return result;
	}

	public static void statsTable(String systemName, String experimentPath, int nrExperiments, boolean detailedEqSteps)
			throws FileNotFoundException, JAXBException {
		Map<String, List<EvalStatistics>> groupedStrategies = groupStrategies(experimentPath);
		List<Pair<String, EvalStatistics>> finalStrategies = new ArrayList<>();
		finalStrategies.add(null);
		finalStrategies.add(null);
		finalStrategies.add(null);

		for (String strategyName : groupedStrategies.keySet()) {
			List<EvalStatistics> statistics = groupedStrategies.get(strategyName);
			if ("random".equals(strategyName))
				continue;
			java.util.Optional<EvalStatistics> finalStatisticsOpt = statistics.stream().filter(s -> s.getNrCorrectRuns() == nrExperiments)
					.min((s1, s2) -> new Long(s1.getTestSuiteSize()).compareTo(new Long(s2.getTestSuiteSize())));
			if(!finalStatisticsOpt.isPresent())
				continue;
			EvalStatistics finalStatistics = finalStatisticsOpt.get();
			if (strategyName.contains("split")) {
				finalStrategies.set(0, ImmutablePair.of("split \\\\ state", finalStatistics));
				finalStrategies.set(1, ImmutablePair.of("split \\\\ state", finalStatistics));
			} else if (strategyName.contains("mutation"))
				finalStrategies.set(1, ImmutablePair.of("transition \\\\ coverage", finalStatistics));
			else if (strategyName.contains("ly"))
				finalStrategies.set(2, ImmutablePair.of("random \\\\ L \\& Y", finalStatistics));
		}
		if (finalStrategies.size() != 3)
			throw new RuntimeException();
		StringBuilder sb = new StringBuilder();
		appendHLine(sb);
		appendLine(sb, String.format("\\multicolumn{5}{|c|}{%s}\\\\", systemName));
		appendHLine(sb);
		appendHLine(sb);
		appendLine(sb,
				String.format(
						"& \\specialcell{%s} & \\specialcell{%s} & \\specialcell{partial \\ W-method} & "
								+ "\\specialcell{%s}  \\\\",
						finalStrategies.get(0).getLeft(), finalStrategies.get(1).getLeft(),
						finalStrategies.get(2).getLeft()));
		appendHLine(sb);
		appendLine(sb, "\\begin{tabular}[c]{@{}c@{}}bound on \\# " + equivalence
				+ " tests/ \\\\ depth parameter \\end{tabular}");
		appendLine(sb,
				String.format("& %s  & %s   &  & %s \\\\\\hline",
						number(finalStrategies.get(0).getRight().getTestSuiteSize()),
						number(finalStrategies.get(1).getRight().getTestSuiteSize()),
						number(finalStrategies.get(2).getRight().getTestSuiteSize())));
		appendLine(sb,
				String.format("mean \\# tests [%s] & %s  & %s   &  & %s \\\\\\hline", equivalence,
						fnumber(mean(finalStrategies.get(0).getRight().getResetsEq())),
						fnumber(mean(finalStrategies.get(1).getRight().getResetsEq())),
						fnumber(mean(finalStrategies.get(2).getRight().getResetsEq()))));
		if (detailedEqSteps) {
			appendLine(sb, "\\begin{tabular}[c]{@{}c@{}} mean \\# steps [" + equivalence
					+ "] \\\\ median \\\\ Q1 \\\\ Q3" + " \\\\min. \\\\ max.  \\\\ \\end{tabular} & ");
			appendLine(sb, "\\begin{tabular}[c]{@{}c@{}}"
					+ detailedStats(finalStrategies.get(0).getRight().getStepsEq()) + "\\end{tabular} &");
			appendLine(sb, "\\begin{tabular}[c]{@{}c@{}}"
					+ detailedStats(finalStrategies.get(1).getRight().getStepsEq()) + "\\end{tabular} & &");
			appendLine(sb, "\\begin{tabular}[c]{@{}c@{}}"
					+ detailedStats(finalStrategies.get(2).getRight().getStepsEq()) + "\\end{tabular} \\\\\\hline");
		} else {
			appendLine(sb,
					String.format("mean \\# steps [%s] & %s  & %s   &  & %s \\\\\\hline", equivalence,
							fnumber(mean(finalStrategies.get(0).getRight().getStepsEq())),
							fnumber(mean(finalStrategies.get(1).getRight().getStepsEq())),
							fnumber(mean(finalStrategies.get(2).getRight().getStepsEq()))));
		}
		appendLine(sb,
				String.format("mean \\# tests [%s] & %s  & %s   &  & %s \\\\\\hline", membership,
						fnumber(mean(finalStrategies.get(0).getRight().getResetsMq())),
						fnumber(mean(finalStrategies.get(1).getRight().getResetsMq())),
						fnumber(mean(finalStrategies.get(2).getRight().getResetsMq()))));
		appendLine(sb,
				String.format("mean \\# steps [%s] & %s  & %s   &  & %s \\\\\\hline", membership,
						fnumber(mean(finalStrategies.get(0).getRight().getStepsMq())),
						fnumber(mean(finalStrategies.get(1).getRight().getStepsMq())),
						fnumber(mean(finalStrategies.get(2).getRight().getStepsMq()))));

		System.out.print(sb.toString());
	}

	private static String detailedStats(String stepsEq) {
		List<Long> vals = parseCommaSepLong(stepsEq);
		String mean = fnumber(meanFromLongs(vals));
		String q1 = fnumber(quantile(vals, 0.25));
		String median = fnumber(quantile(vals, 0.5));
		String q3 = fnumber(quantile(vals, 0.75));
		String mini = number(vals.stream().mapToLong(l -> l).min().getAsLong());
		String maxi = number(vals.stream().mapToLong(l -> l).max().getAsLong());
		return String.format("%s \\\\ %s  \\\\  %s  \\\\ %s \\\\ %s  \\\\  %s", mean, median, q1, q3, mini, maxi);
	}

	private static double mean(String commaSepLongs) {
		return meanFromLongs(parseCommaSepLong(commaSepLongs));
	}

	private static String fnumber(double d) {
		return String.format("$\\n{%.2f}$", d);
	}

	private static String number(long n) {
		return "$\\n{" + n + "}$";
	}

	private static void appendHLine(StringBuilder sb) {
		appendLine(sb, "\\hline");
	}

	private static void appendLine(StringBuilder sb, String line) {
		sb.append(line);
		sb.append(System.lineSeparator());
	}

	public static void statsFromCSV(String commaSepVals) {
		List<Long> vals = parseCommaSepLong(commaSepVals);

		double mean = meanFromLongs(vals);
		Long max = vals.stream().mapToLong(l -> l).max().getAsLong();
		Long min = vals.stream().mapToLong(l -> l).min().getAsLong();
		Double firstQuartile = quantile(vals, 0.25);
		Double median = quantile(vals, 0.5);
		Double thirdQuartile = quantile(vals, 0.75);
		List<String> stats = new ArrayList<>();
		stats.add(ml(min));
		stats.add(md(firstQuartile));
		stats.add(md(mean));
		stats.add(md(median));
		stats.add(md(thirdQuartile));
		stats.add(ml(max));

		System.out.println(stats.stream().collect(Collectors.joining(",")));
	}

	private static double meanFromLongs(List<Long> vals) {
		double mean = vals.stream().mapToLong(l -> l).sum() / (double) vals.size();
		return mean;
	}

	private static List<Long> parseCommaSepLong(String commaSepVals) {
		List<Long> vals = new ArrayList<>();
		Arrays.stream(commaSepVals.split(",")).mapToLong(Long::parseLong).forEach(vals::add);
		return vals;
	}

	public static String ml(Long l) {
		return "$\\n{" + l + "}$";
	}

	public static String md(Double d) {
		return String.format("$\\n{%.2f}$", d);
	}

	public static void main(String[] main) throws FileNotFoundException, JAXBException {
		 meanEqStepsToProbCorrect("journal_experiments/mqtt/no_early_stop/emqtt/two_client_will_retain",50);
//		statsTable("GnuTLS_3.3.12_server_full", "journal_experiments/tls/server/GnuTLS_3_3_12_server_full", 50,
//				false);
		// for(String mqttName : new
		// String[]{"emqtt","ActiveMQ","hbmqtt","mosquitto","VerneMQ"}){
		// statsTable(mqttName, "journal_experiments/mqtt/no_early_stop/" +
		// mqttName + "/two_client_will_retain", 50, false);
		// }
		// statsFromCSV(
		// "2250,3067,2885,2286,1586,2123,2335,1826,1941,2929,2755,2197,1626,1655,2043,2203,3449,2116,1626,1998,1735,2263,1816,1678,1604,3131,1745,1792,1653,1759,2840,2074,1687,3025,1697,2095,1697,1687,2169,2241,1574,1837,3065,1565,2071,2071,2929,2724,1632,2573");
	}

}
