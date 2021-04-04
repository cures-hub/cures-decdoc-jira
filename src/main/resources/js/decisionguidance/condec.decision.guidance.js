/**
 * This module implements the solution option recommendation for decision problems.
 */
(function(global) {

	let ConDecDecisionGuidance = function() {
	};

	ConDecDecisionGuidance.prototype.initView = function() {
		conDecObservable.subscribe(this);
	};

	ConDecDecisionGuidance.prototype.updateView = function() {
		// nothing is done here because this view is updated already with the decision table view
	};

	ConDecDecisionGuidance.prototype.issueSelected = function(currentIssue) {
		const keyword = $("#recommendation-keyword");
		conDecDecisionGuidanceAPI.getRecommendations(conDecAPI.getProjectKey(), keyword.val(), currentIssue.id, currentIssue.documentationLocation,
			function(recommendations, error) {
				if (error === null && recommendations.length > 0) {
					buildQuickRecommendationTable(recommendations, currentIssue);
				}
			});
	};

	/*
	 * external usage: condec.decision.table
	 */
	ConDecDecisionGuidance.prototype.addOnClickListenerForRecommendations = function() {
		$("#recommendation-button").click(function(event) {
			event.preventDefault();
			const currentIssue = conDecDecisionTable.getCurrentIssue();
			$(this).prop("disabled", true);
			$("#recommendation-container tbody tr").remove();
			const keyword = $("#recommendation-keyword");
			const spinner = $("#loading-spinner-recommendation");
			spinner.show();
			conDecDecisionGuidanceAPI.getRecommendations(conDecAPI.getProjectKey(), keyword.val(), currentIssue.id,
				currentIssue.documentationLocation, function(recommendations, error) {
					if (error === null) {
						buildRecommendationTable(recommendations, currentIssue);
					}
					$("#recommendation-button").prop("disabled", false);
					spinner.hide();
				});
		});
	};

	function buildRecommendationTable(recommendations, currentIssue) {
		const table = $("#recommendation-container tbody");

		let counter = 0;
		var sortedByScore = recommendations.slice(0);
		sortedByScore.sort(function(a, b) {
			return b.score.totalScore - a.score.totalScore;
		});

		sortedByScore.forEach(recommendation => {
			counter += 1;
			let tableRow = "";

			tableRow += "<tr>";
			tableRow += "<td><a class='alternative-summary' href='" + recommendation.url + "'>" + recommendation.recommendation + "</a></td>";
			tableRow += "<td><div style='display:flex;gap:3px;align-items:center;'>" + recommendation.knowledgeSourceName + "<span class='aui-icon aui-icon-small " + recommendation.icon + "'>Knowledge Source Type</span></div></td>";
			tableRow += "<td>" + buildScore(recommendation.score, "score_" + counter) + "</td>";
			tableRow += "<td><button title='Adds the recommendation to the knowledge graph' id='row_" + counter + "' class='aui-button-primary aui-button accept-solution-button'>" + "Accept" + "</button></td>";
			tableRow += "<td><ul>";
			recommendation.arguments.forEach((argument) => {
				if (argument) {
					tableRow += "<li><img src='" + argument.image + "'/>";
					tableRow += argument.summary + "</li>";
				}
			});
			tableRow += "</ul></td>";
			tableRow += "</tr>";
			table.append(tableRow);

			$("#row_" + counter).click(function() {
				conDecDialog.showCreateDialog(currentIssue.id, currentIssue.documentationLocation, "Alternative", recommendation.recommendation, "", function(id, documentationLocation) {
					recommendation.arguments.forEach(argument => {
						conDecAPI.createDecisionKnowledgeElement(argument.summary, "", argument.type, argument.documentationLocation, id, documentationLocation, function() {
							conDecAPI.showFlag("success", "Recommendation was added successfully!");
						});
					});
				});
			});
		});
		conDecAPI.showFlag("success", "#Recommendations: " + counter);
	}

	function buildQuickRecommendationTable(recommendations, currentIssue) {
		document.getElementById("decision-problem-summary").innerText = currentIssue.summary;

		let counter = 0;
		var sortedByScore = recommendations.slice(0);
		sortedByScore.sort(function(a, b) {
			return b.score.totalScore - a.score.totalScore;
		});

		var columns = "";

		var topResults = sortedByScore.slice(0, 4);
		topResults.forEach(recommendation => {
			counter += 1;
			let tableRow = "<tr>";
			tableRow += "<td><div style='display:flex;gap:3px;align-items:center;'><span class='aui-icon aui-icon-small " + recommendation.icon + "'>Knowledge Source Type</span><a class='alternative-summary' href='" + recommendation.url + "'>" + recommendation.recommendation + "</a></div></td>";
			tableRow += "<td>" + buildScore(recommendation.score, "score_quick" + counter) + "</td>";
			tableRow += "<td><button title='Adds the recommendation to the knowledge graph' id='row_quick_" + counter + "' class='aui-button-link'>Accept</button></td>";
			tableRow += "</tr>";
			columns += tableRow;
		});

		document.getElementById("quick-recommendations-table-body").innerHTML = columns;

		AJS.flag({
			type: "info",
			body: document.getElementById("quick-recommendations").outerHTML,
			title: "Quick Recommendation"
		});

		var i = 1;
		topResults.forEach(recommendation => {
			$("#row_quick_" + i).click(function() {
				conDecDialog.showCreateDialog(currentIssue.id, currentIssue.documentationLocation, "Alternative", recommendation.recommendation, "", function(id, documentationLocation) {
					recommendation.arguments.forEach(argument => {
						conDecAPI.createDecisionKnowledgeElement(argument.summary, "", argument.type, argument.documentationLocation, id, documentationLocation, function() {
							conDecAPI.showFlag("success", "Recommendation was added successfully!");
						});
					});
				});
			});

			i = i + 1;
		});

		$("#more-recommendations").click(function(event) {
			$(this).prop("disabled", true);
			$("#recommendation-container tbody tr").remove();
			buildRecommendationTable(recommendations, currentIssue);				
		});
	}

	function buildScore(scoreObject, ID) {
		var scoreControl = document.getElementById("control-score-explanation");
		scoreControl.innerText = scoreObject.totalScore.toFixed(2) + "%";

		var columns = "";
		scoreObject.partScores.forEach(partScore => {
			columns += "<tr><td>" + partScore.explanation + "</td><td>" + partScore.totalScore.toFixed(2) + "</td></tr>";
		})
		document.getElementById("score-explanation-table-body").innerHTML = columns;

		var scoreExplanation = scoreControl.outerHTML + document.getElementById("score-explanation").outerHTML;
		return scoreExplanation.replace(/score-explanation/g, "score-explanation-" + ID);
	}

	global.conDecDecisionGuidance = new ConDecDecisionGuidance();
})(window);