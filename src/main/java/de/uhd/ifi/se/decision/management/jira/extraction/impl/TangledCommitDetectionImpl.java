package de.uhd.ifi.se.decision.management.jira.extraction.impl;

import java.util.List;

import de.uhd.ifi.se.decision.management.jira.extraction.ChangedFile;
import de.uhd.ifi.se.decision.management.jira.extraction.Diff;
import de.uhd.ifi.se.decision.management.jira.extraction.TangledCommitDetection;

public class TangledCommitDetectionImpl implements TangledCommitDetection {

	@Override
	public void calculatePredication(Diff diff) {
		this.calculatePackageDistances(diff);
		this.standardization(diff);
	}

	@Override
	public void calculatePackageDistances(Diff diff) {
		List<ChangedFile> changedFiles = diff.getChangedFiles();

		if (changedFiles.isEmpty()) {
			return;
		}

		int numberOfFiles = changedFiles.size();
		if (numberOfFiles == 1) {
			changedFiles.get(0).setPackageDistance(100);
			return;
		}

		int[][] matrix = new int[numberOfFiles][numberOfFiles];
		for (int i = 0; i < numberOfFiles; i++) {
			matrix[i] = calculatePackageDistance(changedFiles.get(i), changedFiles);
		}
	}

	private int[] calculatePackageDistance(ChangedFile file, List<ChangedFile> changedFiles) {
		int[] packageDistances = new int[changedFiles.size()];		
		for (int j = 0; j < changedFiles.size(); j++) {
			packageDistances[j] = calculatePackageDistance(file, changedFiles.get(j));
		}
		return packageDistances;
	}

	private int calculatePackageDistance(ChangedFile fileA, ChangedFile fileB) {		
		List<String> leftPackageDeclaration = fileA.getPackageName();
		List<String> rightPackageDeclaration = fileB.getPackageName();
		if (fileA.equals(fileB)) {
			return 0;
		}
		
		int packageDistance = 0;

		if (leftPackageDeclaration.size() >= rightPackageDeclaration.size()) {
			for (int k = 0; k < rightPackageDeclaration.size(); k++) {
				if (!leftPackageDeclaration.get(k).equals(rightPackageDeclaration.get(k))) {
					fileA.setPackageDistance(fileA.getPackageDistance() + (leftPackageDeclaration.size() - k));
					packageDistance = leftPackageDeclaration.size() - k;
					break;
				} else if ((rightPackageDeclaration.size() - 1) == k
						&& (leftPackageDeclaration.get(k).equals(rightPackageDeclaration.get(k)))) {
					fileA.setPackageDistance(fileA.getPackageDistance()
							+ (leftPackageDeclaration.size() - rightPackageDeclaration.size()));
					packageDistance = leftPackageDeclaration.size() - rightPackageDeclaration.size();
				}
			}
		} else {
			for (int k = 0; k < leftPackageDeclaration.size(); k++) {
				if (!leftPackageDeclaration.get(k).equals(rightPackageDeclaration.get(k))) {
					fileA.setPackageDistance(fileA.getPackageDistance() + (rightPackageDeclaration.size() - k));
					packageDistance = rightPackageDeclaration.size() - k;
					break;
				} else if (leftPackageDeclaration.get(k).equals(rightPackageDeclaration.get(k))
						&& (k == leftPackageDeclaration.size() - 1)) {
					fileA.setPackageDistance(fileA.getPackageDistance()
							+ (rightPackageDeclaration.size() - leftPackageDeclaration.size()));
					packageDistance = rightPackageDeclaration.size() - leftPackageDeclaration.size();
					break;
				}
			}
		}
		return packageDistance;
	}

	@Override
	public void standardization(Diff diff) {
		diff.getChangedFiles()
				.sort((ChangedFile c1, ChangedFile c2) -> c1.getPackageDistance() - c2.getPackageDistance());
		if (diff.getChangedFiles().size() > 1) {
			float max = diff.getChangedFiles().get(diff.getChangedFiles().size() - 1).getPackageDistance();
			float min = diff.getChangedFiles().get(0).getPackageDistance();
			for (ChangedFile changedFile : diff.getChangedFiles()) {
				changedFile.setProbabilityOfCorrectness(((max - changedFile.getPackageDistance()) / (max - min)) * 100);
			}
		} else {
			diff.getChangedFiles().get(0).setProbabilityOfCorrectness(100);
		}
	}

}
