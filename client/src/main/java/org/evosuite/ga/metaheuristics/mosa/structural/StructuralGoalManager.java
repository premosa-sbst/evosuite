/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.ga.metaheuristics.mosa.structural;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.utils.Randomness;

/**
 * 
 * 
 * @author Annibale Panichella
 */
public abstract class StructuralGoalManager<T extends Chromosome> {

	/** Set of yet to cover goals **/
	protected Set<FitnessFunction<T>> uncoveredGoals;

	/** Set of goals currently used as objectives **/
	protected Set<FitnessFunction<T>> currentGoals;

	/** Map of covered goals **/
	protected Map<FitnessFunction<T>, T> coveredGoals;

	/** Map of test to archive and corresponding covered targets*/
	protected Map<T, List<FitnessFunction<T>>> archive;

	/** Map of fitness functions to archived tests **/
	private Map<String, Set<T>> tests;

	protected StructuralGoalManager(List<FitnessFunction<T>> fitnessFunctions){
		uncoveredGoals = new HashSet<FitnessFunction<T>>(fitnessFunctions.size());
		currentGoals = new HashSet<FitnessFunction<T>>(fitnessFunctions.size());
		coveredGoals = new HashMap<FitnessFunction<T>, T>(fitnessFunctions.size());
		archive = new HashMap<T, List<FitnessFunction<T>>>();
		tests = new HashMap<>(fitnessFunctions.size());
	}

	/**
	 * Update the set of covered goals and the set of current goals (actual objectives)
	 * @param c a TestChromosome
	 * @return covered goals along with the corresponding test case
	 */
	public abstract void calculateFitness(T c);

	public Set<FitnessFunction<T>> getUncoveredGoals() {
		return uncoveredGoals;
	}

	public Set<FitnessFunction<T>> getCurrentGoals() {
		return currentGoals;
	}

	public Map<FitnessFunction<T>, T> getCoveredGoals() {
		return coveredGoals;
	}

	protected void updateCoveredGoals(FitnessFunction<T> f, T tc) {
		// the next two lines are needed since that coverage information are used
		// during EvoSuite post-processing
		TestChromosome tch = (TestChromosome) tc;
		tch.getTestCase().getCoveredGoals().add((TestFitnessFunction) f);

		// update covered targets
		boolean toArchive = false;
		T best = coveredGoals.get(f);
		if (best == null){
			toArchive = true;
			coveredGoals.put(f, tc);
			uncoveredGoals.remove(f);
			if (Properties.REMOVE_COVERED_TARGETS) {
				currentGoals.remove(f);    // removing covered goals from currentGoals
			}
		} else {
			double bestSize = best.size();
			double size = tc.size();
			if (size < bestSize && size > 1){
				toArchive = true;
				coveredGoals.put(f, tc);
				if (!Properties.ARCHIVE_ALL) {
					archive.get(best).remove(f);
					if (archive.get(best).size() == 0) {
						archive.remove(best);
						removeFromTests(best);
					}
				}
			}
		}

		if (Properties.ARCHIVE_ALL) {
			toArchive = true;    // since we want to archive the test case anyway
		}

		// update archive
		if (toArchive){
			List<FitnessFunction<T>> coveredTargets = archive.get(tc);
			if (coveredTargets == null){
				List<FitnessFunction<T>> list = new ArrayList<FitnessFunction<T>>();
				list.add(f);
				archive.put(tc, list);
			} else {
				coveredTargets.add(f);
			}

			if (Properties.BALANCE_TEST_COV) {
				if (f instanceof BranchCoverageTestFitness) {
					addTestTo(f, tc);
				}
			}
		}
	}

	private void removeFromTests(T tc) {
		for (Set<T> testsForFf : tests.values()) {
			testsForFf.remove(tc);
		}
	}

	private void addTestTo(FitnessFunction<T> f, T tc) {
		String ffName = f.toString();

		if (tests.containsKey(ffName)) {
			tests.get(ffName).add(tc);
		} else {
			Set<T> testsForFitnessFunction = new HashSet<>();
			testsForFitnessFunction.add(tc);
			tests.put(ffName, testsForFitnessFunction);
		}
	}

	public int getNumTests(String ffName) {
		return this.tests.containsKey(ffName) ? this.tests.get(ffName).size() : 0;
	}

	public Set<T> getArchive(){
		return this.archive.keySet();
	}

	public int getNumberOfCoveredTargets(Class<?> targetClass) {
		return (int) this.coveredGoals.keySet().stream().filter(target -> target.getClass() == targetClass).count();
	}

	public int getNumberOfUncoveredTargets(Class<?> targetClass) {
		return (int) this.uncoveredGoals.stream().filter(target -> target.getClass() == targetClass).count();
	}

}
