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

/**
 * Utility class for creating plots of statistics using the LaTex-package pgfplots.
 * 
 * Currently, it implements plots of the estimated probability of learning the correct
 * with respect to test suite size and with respect to the mean number of steps executed 
 * during equivalence testing. It creates one plot line for each testing strategy.
 * 
 * @author Martin Tappler
 *
 */
public class StatPlotCreator {

	private static String fileContentsStart(String name){
		return String.format("\\begin{filecontents*}{%s.dat}",name);
	}
	private static final String fileContentsEnd = "\\end{filecontents*}";
	private static final String tikzStart = "\\begin{tikzpicture} \\begin{axis}";
	private static final String tikzEnd = "\\end{axis} \\end{tikzpicture} ";
	
	public static void meanEqStepsToProbCorrect(String measDataPath, int maxCorrect) 
			throws JAXBException, FileNotFoundException{
		File measDir = new File(measDataPath);
		File[] measFiles = measDir.listFiles(f -> f.getAbsolutePath().endsWith(".xml"));
		Map<String,List<EvalStatistics>> groupedByStrategy = new HashMap<>();
		JAXBContext statXMLContext = JAXBContext.newInstance(EvalStatistics.class);
		Unmarshaller um = statXMLContext.createUnmarshaller();		
		for(File measFile : measFiles){
			String strategy = getStrategy(measFile);
			if(!groupedByStrategy.containsKey(strategy)){
				groupedByStrategy.put(strategy, new ArrayList<>());
			}

	        EvalStatistics stats = (EvalStatistics) um.unmarshal(new FileReader(measFile));
			groupedByStrategy.get(strategy).add(stats);
		}

		System.out.println(tikzStart);
		System.out.println("[xlabel = mean \\# steps[equivalence], ylabel=prob. correct  ]");
		for(String strategy : groupedByStrategy.keySet()){
			List<EvalStatistics> stats = groupedByStrategy.get(strategy);
			System.out.println(String.format("\\addplot[color=red,mark=x] coordinates { %% %s",strategy));
			stats = stats.stream().sorted( (s1,s2) -> (int)(s1.getStepsEqSum() - s2.getStepsEqSum()))
										.collect(Collectors.toList());
			for(EvalStatistics stat : stats){
				double meanNrSteps = stat.getStepsEqSum() / (double)maxCorrect;
				double prob = stat.getNrCorrectRuns() / (double)maxCorrect;
				System.out.println(String.format("(%.2f, %.2f)", meanNrSteps,prob));
			}
			System.out.println("};");
		}
		
		System.out.println(tikzEnd);
	}
	
	public static void nrCorrect(String measDataPath, String experimentName, int maxCorrect) 
			throws JAXBException, FileNotFoundException{
		File measDir = new File(measDataPath);
		File[] measFiles = measDir.listFiles(f -> f.getAbsolutePath().endsWith(".xml"));
		Map<String,List<EvalStatistics>> groupedByStrategy = new HashMap<>();
		JAXBContext statXMLContext = JAXBContext.newInstance(EvalStatistics.class);
		Unmarshaller um = statXMLContext.createUnmarshaller();		
		Set<Long> testSuiteSizes = new HashSet<>();
		for(File measFile : measFiles){
			String strategy = getStrategy(measFile);
			if(!groupedByStrategy.containsKey(strategy)){
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
		for(Long suiteSize : testSuiteSizesSorted){
			List<String> lineContents = new ArrayList<>();
			for(String strategy : sortedStrategies){
				List<EvalStatistics> measData = groupedByStrategy.get(strategy);
				EvalStatistics measDataForSize = null;
				for(EvalStatistics data : measData)
					if(data.getTestSuiteSize() == suiteSize)
						measDataForSize = data;
				if(measDataForSize != null)
					lineContents.add(Double.toString(measDataForSize.getNrCorrectRuns()/(double) maxCorrect));
				else 
					lineContents.add(Double.toString(1.0));
			}
			System.out.print(Long.toString(suiteSize) + ",");
			System.out.println(String.join(",", lineContents));
		}
		System.out.println(fileContentsEnd);

		System.out.println(tikzStart);
		for(String strategy : sortedStrategies){
			System.out.println("\\addplot table[x=suite-size, y="+ strategy + ",col sep=comma] {"+fileName+".dat};");
		}
		System.out.println(tikzEnd);
	}
	private static String getStrategy(File measFile) {
		String strategy = measFile.getName().replaceAll("_\\d+\\.xml", "")
				.replaceAll("\\d+_mudirected\\.xml", "mudirected")
				.replaceAll("\\d+_wp_method\\.xml", "_wp_method")
				.replaceAll("\\d+_ly\\.xml", "_ly");
		return strategy;
	}
	public static void main(String[] main) throws FileNotFoundException, JAXBException{
		meanEqStepsToProbCorrect("rv_experiments/mqtt/emqtt/two_client_will_retain",50);
	}
}
