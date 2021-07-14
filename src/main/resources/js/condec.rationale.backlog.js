/*
 *	This view provides a backlog for decision knowledge.
 */
(function (global) {

	var ConDecRationaleBacklog = function() {
	};

	ConDecRationaleBacklog.prototype.initView = function () {
			
		// Fill filter elements
		conDecFiltering.fillFilterElements("rationale-backlog", ["Alternative", "Decision", "Issue", "Argument"]);
		conDecFiltering.initDropdown("status-dropdown-rationale-backlog", conDecAPI.rationaleBacklogItemStatus);
		conDecFiltering.fillDatePickers("rationale-backlog", 30);
		
		// Add on click listeners to filter button
     	conDecFiltering.addOnClickEventToFilterButton("rationale-backlog", conDecRationaleBacklog.updateView);
		conDecDecisionTable.addOnClickEventToDecisionTableButtons("rationale-backlog");	

		// Register/subscribe this view as an observer
		conDecObservable.subscribe(this);
		
		// Fill view
		this.updateView();
	};

	ConDecRationaleBacklog.prototype.updateView = function () {
		updateView(null);
	};

	function updateView(nodeId) {		
		var filterSettings = conDecFiltering.getFilterSettings("rationale-backlog");
		
		filterSettings["isOnlyDecisionKnowledgeShown"] = false; // since this only applies on right side
		
		conDecTreeViewer.buildTreeViewer(filterSettings, "#rationale-backlog-tree", "#search-input-rationale-backlog", "rationale-backlog-tree");
		if (nodeId === undefined) {
			var rootElement = treant.getCurrentRootElement();
			if (rootElement) {
				conDecTreeViewer.selectNodeInTreeViewer(rootElement.id, "#rationale-backlog-tree");
			}
		} else {
			conDecTreeViewer.selectNodeInTreeViewer(nodeId, "#rationale-backlog-tree");
		}
		
		addSelectNodeEventListenerToTreeViewer();
	}
	
	function addSelectNodeEventListenerToTreeViewer() {
		jQuery("#rationale-backlog-tree").on("select_node.jstree", function(error, tree) {
			var filterSettings = conDecFiltering.getFilterSettings("rationale-backlog");
			var node = tree.node.data;
			filterSettings["status"] = null;
			document.getElementById("selected-element-rationale-backlog").innerText = node.key;
			filterSettings["selectedElement"] = node.key;
			conDecTreant.buildTreant(filterSettings, true, "treant-rationale-backlog");
			conDecTreeViewer.buildTreeViewer(filterSettings, "#jstree-rationale-backlog", "#search-input-rationale-backlog", "jstree-rationale-backlog");
			jQuery("#jstree-rationale-backlog").on("loaded.jstree", function() {
				jQuery("#jstree-rationale-backlog").jstree("open_all");
			});
			conDecVis.buildVis(filterSettings, "graph-rationale-backlog");
			conDecMatrix.buildMatrix(filterSettings, "rationale-backlog");
			conDecDecisionTable.build(filterSettings, "rationale-backlog", node);
			conDecQualityCheck.initView("rationale-backlog", node);
		});
	}
	
	global.conDecRationaleBacklog = new ConDecRationaleBacklog();
})(window);