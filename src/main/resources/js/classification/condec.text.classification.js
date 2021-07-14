/**
 * This module implements the link suggestion and duplicate detection.
 */
(function (global) {

	let ConDecTextClassification = function () {
		this.projectKey = conDecAPI.getProjectKey();
		this.currentNonValidatedElements = [];
	};

	ConDecTextClassification.prototype.init = function (isJiraIssueView = false) {
		if (isJiraIssueView) {
			this.viewIdentifier = "jira-issue-module";
		}
		else {
			this.viewIdentifier = "decision-knowledge-page";
		}

		this.issueId = JIRA.Issue.getIssueId();
		this.issueKey = conDecAPI.getIssueKey();

		this.nonValidatedTableElement = document.getElementById(`non-validated-table-${this.viewIdentifier}`);
		this.nonValidatedTableContentElement = document.getElementById(`non-validated-table-content-${this.viewIdentifier}`);
		this.loadingSpinnerElement = document.getElementById(`classification-loading-spinner-${this.viewIdentifier}`);
		this.validateAllButton = document.getElementById(`validate-all-elements-button-${this.viewIdentifier}`);

		conDecObservable.subscribe(this);
		this.loadData();
	}
	ConDecTextClassification.prototype.updateView = function () {
		this.loadData();
	}
	//-----------------------------------------
	//			Generate table of non-validated elements
	//-----------------------------------------
	ConDecTextClassification.prototype.displayNonValidatedElements = function (nonValidatedElementsList) {

		if (nonValidatedElementsList.length === 0) {
			//reset table content to empty
			this.nonValidatedTableContentElement.innerHTML = "<i>All elements have been validated!</i>";
			this.validateAllButton.style.display = "none";
		} else {
			//reset table content to empty
			this.nonValidatedTableContentElement.innerHTML = "";
			this.currentNonValidatedElements = nonValidatedElementsList;
			// append the elements
			for (let i = 0; i < nonValidatedElementsList.length; i++) {
				let row = generateTableRow(nonValidatedElementsList[i]);
				this.nonValidatedTableContentElement.appendChild(row);
			}
			this.validateAllButton.style.display = "inline";
			this.validateAllButton.onclick = () => {
				conDecTextClassificationAPI.validateAllElements(this.projectKey, conDecAPI.getIssueKey())
					.then(() => conDecObservable.notify());
			}
		}
		AJS.tabs.setup();
		conDecNudgingAPI.decideAmbientFeedbackForTab(nonValidatedElementsList.length, `menu-item-text-classification-${this.viewIdentifier}`);
	};

	let generateTableRow = function (nonValidatedElement) {
		let row = document.createElement("tr");
		row.appendChild(generateTableCell(nonValidatedElement.type, "th-type"));
		row.appendChild(generateTableCell(nonValidatedElement.summary, "th-name"));
		row.appendChild(generateTableCell(generateOptionButtons(nonValidatedElement.id), "th-options"));
		console.log("row", row)
		return row;
	};

	let generateTableCell = function (content, headersId, attributes) {
		let tableCell = document.createElement("td");
		tableCell.headers = headersId;
		tableCell.innerHTML = content;
		for (let key in attributes) {
			tableCell[key] = JSON.stringify(attributes[key]);
		}
		return tableCell
	};

	let generateOptionButtons = function (elementID) {
		return `<button class='aui-button aui-button-primary' onclick="conDecAPI.setValidated(${elementID}, () => conDecObservable.notify())"> <span class='aui-icon aui-icon-small aui-iconfont-link'>Validate</span> Validate </button>` +
			`<button class='aui-button aui-button-removed' onclick="conDecDialog.showEditDialog(${elementID}, 's')"> <span class="aui-icon aui-icon-small aui-iconfont-edit-filled">Edit</span> Edit </button>` +
			`<button class="aui-button aui-button-removed" onclick="conDecAPI.setSentenceIrrelevant(${elementID}, () => conDecObservable.notify())"> <span class="aui-icon aui-icon-small aui-iconfont-trash">Set Irrelevant</span> Set Irrelevant </button>`;
	};


	//-----------------------------------------
	// Load data and call display logic.
	//-----------------------------------------

	ConDecTextClassification.prototype.loadData = function () {
		startLoadingVisualization(this.nonValidatedTableElement, this.loadingSpinnerElement);
		conDecTextClassificationAPI.getNonValidatedElements(this.projectKey, this.issueKey)
			.then((result) => this.displayNonValidatedElements(result["nonValidatedElements"], this.viewIdentifier))
			.catch((error) => displayErrorMessage(error))
			.finally(() => stopLoadingVisualization(this.nonValidatedTableElement, this.loadingSpinnerElement)
			);
	}

	//-----------------------------------------
	//		General purpose functions
	//-----------------------------------------

	function displayErrorMessage(error) {
		conDecAPI.showFlag("error", "Something went wrong! <br/>" + error)
	}

	function startLoadingVisualization(table, spinner) {
		table.style.visibility = "hidden";
		spinner.style.display = "flex";
	}

	function stopLoadingVisualization(table, spinner) {
		spinner.style.display = "none";
		table.style.visibility = "visible";

	}

	global.conDecTextClassification = new ConDecTextClassification();
})(window);