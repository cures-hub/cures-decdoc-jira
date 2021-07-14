/*
 This view provides trees of knowledge elements.

 Requires
 * conDecAPI
 * conDecObservable
 * conDecContextMenu
 * conDecTreant
 * conDecTreeViewer

 Is referenced in HTML by
 * overview.vm
 */
(function(global) {

	var ConDecKnowledgePage = function() {
	};

	ConDecKnowledgePage.prototype.initView = function() {
		console.log("conDecKnowledgePage initView");

		// Register/subscribe this view as an observer
		conDecObservable.subscribe(this);

		// Fill view
		this.fetchAndRender();
	};

	ConDecKnowledgePage.prototype.fetchAndRender = function() {
		conDecFiltering.fillFilterElements("overview");
		
		var knowledgeTypes = conDecAPI.getKnowledgeTypes();
		for (var index = 0; index < knowledgeTypes.length; index++) {
			var isSelected = "";
			const urlParams = new URLSearchParams(window.location.href);
			if (urlParams.has("codeFileName")) {
				if (knowledgeTypes[index] === "Code") {
					isSelected = "selected ";
				}
			} else {
				if (knowledgeTypes[index] === "Issue") {
					isSelected = "selected ";
				}
			}
			jQuery("select[name='knowledge-type-dropdown-overview']")[0].insertAdjacentHTML("beforeend", "<option "
				+ isSelected + " value='" + knowledgeTypes[index] + "'>" + knowledgeTypes[index] + "</option>");
		}

		var createElementButton = document.getElementById("create-element-button");
		var elementInputField = document.getElementById("element-input-field");
		conDecAPI.isJiraIssueDocumentationLocationActivated(function(isEnabled) {
			if (isEnabled) {
				createElementButton.addEventListener("click", function() {
					var summary = elementInputField.value;
					var type = jQuery("select[name='knowledge-type-dropdown-overview']").val();
					elementInputField.value = "";
					conDecAPI.createDecisionKnowledgeElement(summary, "", type, "i", 0, null, function(id) {
						updateView(id, treant, treeViewer);
					});
				});
			} else {
				createElementButton.style.display = "none";
				elementInputField.style.display = "none";
			}
		});		
		
		// Add on click listeners to filter button
     	conDecFiltering.addOnClickEventToFilterButton("overview", conDecKnowledgePage.updateView);     	
		conDecDecisionTable.addOnClickEventToDecisionTableButtons("overview");

		this.updateView();
	};

	ConDecKnowledgePage.prototype.updateView = function() {
		updateView(null);
	};

	function updateView(nodeId) {
		var filterSettings = conDecFiltering.getFilterSettings("overview");
		var knowledgeType = jQuery("select[name='knowledge-type-dropdown-overview']").val();
		filterSettings["knowledgeTypes"] = [ knowledgeType ];
		filterSettings["linkDistance"] = 0; // to speed-up loading
		filterSettings["isOnlyDecisionKnowledgeShown"] = false; // since this only applies on right side
		conDecTreeViewer.buildTreeViewer(filterSettings, "#jstree", "#search-input-overview", "jstree");
		if (nodeId === undefined) {
			var rootElement = treant.getCurrentRootElement();
			if (rootElement) {
				conDecTreeViewer.selectNodeInTreeViewer(rootElement.id, "#jstree");
			}
		} else {
			conDecTreeViewer.selectNodeInTreeViewer(nodeId, "#jstree");
		}

		addSelectNodeEventListenerToTreeViewer();
	}

	function addSelectNodeEventListenerToTreeViewer() {
		jQuery("#jstree").on("select_node.jstree", function(error, tree) {
			var filterSettings = conDecFiltering.getFilterSettings("overview");
			var node = tree.node.data;
			filterSettings["knowledgeTypes"] = null;
			filterSettings["status"] = null;
			document.getElementById("selected-element-overview").innerText = node.key;
			filterSettings["selectedElement"] = node.key;
			conDecTreant.buildTreant(filterSettings, true, "treant-overview");
			conDecTreeViewer.buildTreeViewer(filterSettings, "#jstree-overview", "#search-input-overview", "jstree-overview");
			jQuery("#jstree-overview").on("loaded.jstree", function () {
				jQuery("#jstree-overview").jstree("open_all");
			});
			conDecVis.buildVis(filterSettings, "graph-overview");
			conDecMatrix.buildMatrix(filterSettings, "overview");
			conDecDecisionTable.build(filterSettings, "overview", node);
			conDecQualityCheck.initView("overview", node);
		});
	}

	global.conDecKnowledgePage = new ConDecKnowledgePage();
})(window);