(function(global) {

	const ConDecDoDCheckingAPI = function() {
		this.restPrefix = AJS.contextPath() + "/rest/condec/latest/dodChecking";
	};

	/*
	 * external references: settings/definitionofdone/...
	 */
	ConDecDoDCheckingAPI.prototype.setDefinitionOfDone = function(projectKey, definitionOfDone) {
		generalApi.postJSON(this.restPrefix + "/setDefinitionOfDone.json?projectKey=" + projectKey, definitionOfDone, function(
			error, response) {
			if (error === null) {
				conDecAPI.showFlag("success", "The definition of done is updated.");
			}
		});
	};
	
	/*
	 * external references: settings/definitionofdone/...
	 * nudging/condec.prompts.js, condec.quality.check.js
	 */
	ConDecDoDCheckingAPI.prototype.getDefinitionOfDone = function(projectKey, callback) {
		conDecAPI.getFilterSettings(conDecAPI.getProjectKey(), "", (filterSettings) => {
			callback(filterSettings["definitionOfDone"]);
		});
	};

	/*
	 * external references: nudging/condec.prompts.js,
	 * condec.quality.check.js
	 */
	ConDecDoDCheckingAPI.prototype.getQualityProblems = function(filterSettings, callback) {
		generalApi.postJSON(this.restPrefix + '/getQualityProblems.json', filterSettings, function(
			error, result) {
			if (error === null) {
				callback(result);
			}
		});
	};

	/*
	 * external references: condec.quality.check.js,
	 * nudging/condec.prompts.js,
	 */
	ConDecDoDCheckingAPI.prototype.getCoverageOfJiraIssue = function(filterSettings, callback) {
		generalApi.postJSON(this.restPrefix + '/getCoverageOfJiraIssue.json', filterSettings, function(
			error, result) {
			if (error === null) {
				callback(result);
			}
		});
	};

	global.conDecDoDCheckingAPI = new ConDecDoDCheckingAPI();
})(window);