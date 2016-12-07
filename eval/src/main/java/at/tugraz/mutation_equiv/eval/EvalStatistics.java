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

import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class defines statistics to be logged during learning experiments.
 * It is intended to be serialised as XML-file. 
 * 
 * @author Martin Tappler
 *
 */
@XmlRootElement(name = "eval-statistics")
public class EvalStatistics {
	private String selectionConfig;
	private String mutOpConfig;
	private String shortMutOpConfig;
	private String sampleConfig;
	private String traceGenConfig; 
	private long testSelectionSuite;
	private long testSuiteSize;
	private long stepsEqSum;
	private String stepsEq;
	private long stepsMqSum;
	private String stepsMq;
	private long resetsEqSum;
	public long getResetsEqSum() {
		return resetsEqSum;
	}

	public void setResetsEqSum(long resetsEqSum) {
		this.resetsEqSum = resetsEqSum;
	}


	public long getResetsMqSum() {
		return resetsMqSum;
	}

	public void setResetsMqSum(long resetsMqSum) {
		this.resetsMqSum = resetsMqSum;
	}


	private String resetsEq;
	private long resetsMqSum;
	private String resetsMq; 
	private long nrRuns;
	private long nrCorrectRuns;
	private long mutationDuration;
	private String mutationDurations;
	private long selectionDuration;
	private String selectionDurations;
	private String experimentName;
	private long generationDuration;
	private String generationDurations;
	public EvalStatistics(
			String experimentName,
			String selectionConfig, 
			String mutOpConfig, 
			String shortMutOpConfig, 
			String sampleConfig,
			String traceGenConfig, 
			long testSelectionSuite, 
			long testSuiteSize, 
			long stepsEqSum, 
			List<Long> stepsEq,
			long stepsMqSum, 
			List<Long> stepsMq, 	
			long resetEqSum, 
			List<Long> resetsEq,
			long resetsMqSum, 
			List<Long> resetsMq, 
			long nrRuns, 
			long nrCorrectRuns, 
			long mutationDuration,
			List<Long> mutationDurations, 
			long selectionDuration, 
			List<Long> selectionDurations, 
			long generationDuration, 
			List<Long> generationDurations) {
		super();
		this.selectionConfig = selectionConfig;
		this.mutOpConfig = mutOpConfig;
		this.shortMutOpConfig = shortMutOpConfig;
		this.sampleConfig = sampleConfig;
		this.traceGenConfig = traceGenConfig;
		this.testSelectionSuite = testSelectionSuite;
		this.testSuiteSize = testSuiteSize;
		this.stepsEqSum = stepsEqSum;
		this.stepsEq = csv(stepsEq);
		this.stepsMqSum = stepsMqSum;
		this.stepsMq = csv(stepsMq);
		this.resetsEqSum = resetEqSum;
		this.setResetsEq(csv(resetsEq));
		this.resetsMqSum = resetsMqSum;
		this.setResetsMq(csv(resetsMq));
		this.nrRuns = nrRuns;
		this.nrCorrectRuns = nrCorrectRuns;
		this.mutationDuration = mutationDuration;
		this.mutationDurations = csv(mutationDurations);
		this.selectionDuration = selectionDuration;
		this.selectionDurations = csv(selectionDurations);
		this.experimentName = experimentName;
		this.generationDuration = generationDuration;
		this.generationDurations = csv(generationDurations);
	}

	private String csv(List<Long> longs){
		return String.join(",", longs.stream().map(Object::toString).collect(Collectors.toList()));
	}
	

	public EvalStatistics(){
		
	}


	public String getSelectionConfig() {
		return selectionConfig;
	}
	public void setSelectionConfig(String selectionConfig) {
		this.selectionConfig = selectionConfig;
	}
	public String getMutOpConfig() {
		return mutOpConfig;
	}
	public void setMutOpConfig(String mutOpConfig) {
		this.mutOpConfig = mutOpConfig;
	}
	public String getShortMutOpConfig() {
		return shortMutOpConfig;
	}
	public void setShortMutOpConfig(String shortMutOpConfig) {
		this.shortMutOpConfig = shortMutOpConfig;
	}
	public String getSampleConfig() {
		return sampleConfig;
	}
	public void setSampleConfig(String sampleConfig) {
		this.sampleConfig = sampleConfig;
	}
	public String getTraceGenConfig() {
		return traceGenConfig;
	}
	public void setTraceGenConfig(String traceGenConfig) {
		this.traceGenConfig = traceGenConfig;
	}
	public long getTestSelectionSuite() {
		return testSelectionSuite;
	}
	public void setTestSelectionSuite(long testSelectionSuite) {
		this.testSelectionSuite = testSelectionSuite;
	}
	public long getTestSuiteSize() {
		return testSuiteSize;
	}
	public void setTestSuiteSize(long testSuiteSize) {
		this.testSuiteSize = testSuiteSize;
	}
	public String getStepsEq() {
		return stepsEq;
	}
	public void setStepsEq(String stepsEq) {
		this.stepsEq = stepsEq;
	}
	public String getStepsMq() {
		return stepsMq;
	}
	public void setStepsMq(String stepsMq) {
		this.stepsMq = stepsMq;
	}
	public long getNrRuns() {
		return nrRuns;
	}
	public void setNrRuns(long nrRuns) {
		this.nrRuns = nrRuns;
	}
	public long getNrCorrectRuns() {
		return nrCorrectRuns;
	}
	public void setNrCorrectRuns(long nrCorrectRuns) {
		this.nrCorrectRuns = nrCorrectRuns;
	}
	public long getMutationDuration() {
		return mutationDuration;
	}
	public void setMutationDuration(long mutationDuration) {
		this.mutationDuration = mutationDuration;
	}
	public long getSelectionDuration() {
		return selectionDuration;
	}
	public void setSelectionDuration(long selectionDuration) {
		this.selectionDuration = selectionDuration;
	}
	public String getExperimentName() {
		return experimentName;
	}
	public void setExperimentName(String experimentName) {
		this.experimentName = experimentName;
	}
	public long getGenerationDuration() {
		return generationDuration;
	}
	public void setGenerationDuration(long generationDuration) {
		this.generationDuration = generationDuration;
	}


	public long getStepsEqSum() {
		return stepsEqSum;
	}


	public void setStepsEqSum(long stepsEqSum) {
		this.stepsEqSum = stepsEqSum;
	}


	public long getStepsMqSum() {
		return stepsMqSum;
	}


	public void setStepsMqSum(long stepsMqSum) {
		this.stepsMqSum = stepsMqSum;
	}


	public String getMutationDurations() {
		return mutationDurations;
	}


	public void setMutationDurations(String mutationDurations) {
		this.mutationDurations = mutationDurations;
	}


	public String getSelectionDurations() {
		return selectionDurations;
	}


	public void setSelectionDurations(String selectionDurations) {
		this.selectionDurations = selectionDurations;
	}


	public String getGenerationDurations() {
		return generationDurations;
	}


	public void setGenerationDurations(String generationDurations) {
		this.generationDurations = generationDurations;
	}

	public String getResetsMq() {
		return resetsMq;
	}

	public void setResetsMq(String resetsMq) {
		this.resetsMq = resetsMq;
	}

	public String getResetsEq() {
		return resetsEq;
	}

	public void setResetsEq(String resetsEq) {
		this.resetsEq = resetsEq;
	}
}
