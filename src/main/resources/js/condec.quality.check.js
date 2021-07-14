/*
 *	This view provides a check  of the quality of the decision knowledge of the jira issue.
 */

(function(global) {
	const pluralize = (count, noun, suffix = 's') =>
		`${count} ${noun}${count !== 1 ? suffix : ''}`;

	var ConDecQualityCheck = function ConDecQualityCheck() {
	};

	ConDecQualityCheck.prototype.initView = function(viewIdentifier, node) {
		console.log("ConDecQualityCheck initView");

		var projectKey = conDecAPI.getProjectKey();
		var issueKey;
		if (node) {
			issueKey = node.key;
		} else {
			issueKey = conDecAPI.getIssueKey();
		}

		var filterSettings = {
			"projectKey": projectKey,
			"selectedElement": issueKey,
		};

		fillQualityCheckTab(filterSettings, viewIdentifier);
	};

	function fillQualityCheckTab(filterSettings, viewIdentifier) {
		fillIssueKey(filterSettings.selectedElement, viewIdentifier);

		conDecDoDCheckingAPI.getDefinitionOfDone(filterSettings.projectKey, (definitionOfDone) => {
			fillCoverageRequired(definitionOfDone, viewIdentifier);
		});

		conDecDoDCheckingAPI.getCoverageOfJiraIssue(filterSettings, (coverage) => {
			fillCoverageReached(coverage, viewIdentifier);
		});

		conDecDoDCheckingAPI.getQualityProblems(filterSettings, (qualityProblems) => {
			fillQualityProblems(qualityProblems, viewIdentifier);
			updateTabStatus(qualityProblems, viewIdentifier);
		});
	}

	function fillIssueKey(issueKey, viewIdentifier) {
		var knowledgeTypeLabel = document.getElementById("quality-check-issue-key-" + viewIdentifier)

		if (issueKey === null || issueKey === undefined) {
			knowledgeTypeLabel.innerText = "This element";
			return;
		}

		knowledgeTypeLabel.innerText = issueKey;
	}

	function fillCoverageRequired(definitionOfDone, viewIdentifier) {
		var minimumCoverageLabel = document.getElementById("quality-check-minimum-coverage-" + viewIdentifier);
		var linkDistanceLabel = document.getElementById("quality-check-link-distance-" + viewIdentifier);

		minimumCoverageLabel.innerText = pluralize(definitionOfDone.minimumDecisionsWithinLinkDistance, "decision");
		linkDistanceLabel.innerText = definitionOfDone.maximumLinkDistanceToDecisions;
	}

	function fillCoverageReached(coverage, viewIdentifier) {
		var coverageLabel = document.getElementById("quality-check-coverage-" + viewIdentifier);

		coverageLabel.innerText = pluralize(coverage, "decision");
	}

	function fillQualityProblems(qualityProblems, viewIdentifier) {
		var qualityProblemsTextField = document.getElementById("quality-check-problems-text-" + viewIdentifier);

		var text = "";

		qualityProblems.forEach(function(problem) {
			if (problem.name === "NO_DECISION_COVERAGE" || problem.name === "DECISION_COVERAGE_TOO_LOW" ||
				problem.name === "INCOMPLETE_KNOWLEDGE_LINKED") {
				text += problem.description;
				text += "\n\n";
			} else {
				text += problem.description;
				text += "\n";
			}
		})

		qualityProblemsTextField.innerText = text;
		conDecNudgingAPI.setAmbientFeedback(qualityProblemsTextField, "condec-error");
	}

	function updateTabStatus(qualityProblems, viewIdentifier) {
		var qualityCheckTab = document.getElementById("menu-item-quality-check-" + viewIdentifier);

		if (!qualityProblems || !qualityProblems.length) {
			conDecNudgingAPI.setAmbientFeedback(qualityCheckTab, "condec-fine");
		} else if (qualityProblems.some(problem => problem.name === "NO_DECISION_COVERAGE")) {
			conDecNudgingAPI.setAmbientFeedback(qualityCheckTab, "condec-error");
		} else {
			conDecNudgingAPI.setAmbientFeedback(qualityCheckTab, "condec-warning");
		}
	}

	global.conDecQualityCheck = new ConDecQualityCheck();
})(window);