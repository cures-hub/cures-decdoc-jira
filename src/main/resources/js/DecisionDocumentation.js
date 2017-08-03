var getJSON = function(url, callback) {
	var xhr = new XMLHttpRequest();
	xhr.open("GET", url, true);
	xhr.setRequestHeader("Content-type", "application/json", "charset=utf-8");
	xhr.responseType = "json";
	xhr.onload = function() {
		var status = xhr.status;
		if (status == 200) {
			callback(null, xhr.response);
		} else {
			callback(status);
		}
	};
	xhr.send();
};
var postJSON = function(url, data, callback) {
	var xhr = new XMLHttpRequest();
	xhr.open("POST", url, true);
	xhr.setRequestHeader("Content-type", "application/json", "charset=utf-8");
	xhr.setRequestHeader("Accept", "application/json");
	xhr.responseType = "json";
	xhr.onload = function() {
		var status = xhr.status;
		if (status == 200) {
			callback(null, xhr.response);
		} else {
			callback(status);
		}
	};
	xhr.send(JSON.stringify(data));
};
var putJSON = function(url, data, callback) {
	var xhr = new XMLHttpRequest();
	xhr.open("PUT", url, true);
	xhr.setRequestHeader("Content-type", "application/json", "charset=utf-8");
	xhr.setRequestHeader("Accept", "application/json");
	xhr.responseType = "json";
	xhr.onload = function() {
		var status = xhr.status;
		if (status == 200) {
			callback(null, xhr.response);
		} else {
			callback(status);
		}
	};
	xhr.send(JSON.stringify(data));
};

var createDecisionComponent = function(summary, issueType, callback){
	var pathname = window.location.pathname;
	var stringArray = pathname.split("/");
	var projectKey = stringArray[stringArray.length-1];
	if(summary != ""){
		var jsondata = {
			"projectKey": projectKey,
			"name": summary,
			"type": issueType
		};
		postJSON(AJS.contextPath() + "/rest/decisions/latest/decisions.json?actionType=create", jsondata, function(err, data) {
			if (err!=null){
				var errorFlag = AJS.flag({
					type: 'error',
					close: 'auto',
					title: 'Error',
					body: issueType + ' has not been created. Error Code: ' + err
				});
			} else {
				callback(data);
			}
		});
	} else {
		//summary is empty
	}
}
var editDecisionComponent = function(issueId, summary, description, callback){
	var pathname = window.location.pathname;
	var stringArray = pathname.split("/");
	var projectKey = stringArray[stringArray.length-1];
	var jsondata = {
		"id": issueId,
		"name": summary,
		"projectKey": projectKey,
		"description": description
	};
	postJSON(AJS.contextPath() + "/rest/decisions/latest/decisions.json?actionType=edit", jsondata, function(err, data) {
		if (err!=null){
			var errorFlag = AJS.flag({
				type: 'error',
				close: 'auto',
				title: 'Error',
				body: issueType + ' has not been updated. Error Code: ' + err
			});
		} else {
			callback(data);
		}
	});
}
var deleteDecisionComponent = function(issueId, callback){
	var pathname = window.location.pathname;
	var stringArray = pathname.split("/");
	var projectKey = stringArray[stringArray.length-1];
	var jsondata = {
		"id": issueId,
		"projectKey": projectKey
	};
	postJSON(AJS.contextPath() + "/rest/decisions/latest/decisions.json?actionType=delete", jsondata, function(err, data) {
		if (err!=null){
			var errorFlag = AJS.flag({
				type: 'error',
				close: 'auto',
				title: 'Error',
				body: 'Decision Component has not been deleted. Error Code: ' + err
			});
		} else {
			callback(data);
		}
	});
}
var createLink = function(parentId, childId, linkType, callback){
	var pathname = window.location.pathname;
	var stringArray = pathname.split("/");
	var projectKey = stringArray[stringArray.length-1];
	var jsondata = {
		"linkType": linkType,
		"ingoingId": childId,
		"outgoingId": parentId
	};
	putJSON(AJS.contextPath() + "/rest/decisions/latest/decisions.json?projectKey=" + projectKey + "&actionType=create", jsondata, function(err, data) {
		if (err!=null){
			var errorFlag = AJS.flag({
				type: 'error',
				close: 'auto',
				title: 'Error',
				body: 'IssueLink could not be created'
			});
		} else {
			callback(data);
		}
	});
}
var deleteLink = function(parentId, childId, linkType, callback){
	var pathname = window.location.pathname;
	var stringArray = pathname.split("/");
	var projectKey = stringArray[stringArray.length-1];
	var jsondata = {
		"linkType": linkType,
		"ingoingId": childId,
		"outgoingId": parentId
	};
	putJSON(AJS.contextPath() + "/rest/decisions/latest/decisions.json?projectKey=" + projectKey + "&actionType=delete", jsondata, function(err, data) {
		if (err!=null){
			var errorFlag = AJS.flag({
				type: 'error',
				close: 'auto',
				title: 'Error',
				body: 'IssueLink could not be deleted'
			});
		} else {
			callback(data);
		}
	});
}

var createContextMenuForTreeNodes = function(projectKey){
	$(function() {
		$.contextMenu({
			selector: '.rationale, .context, .problem, .solution',
			callback: function(key, options) {
				var m = "clicked: " + key;
				console.log(m);
			},
			//TODO icons
			items: {
				"add": {name: "Add Decision Component", 
					callback: function(key, options){
						//set header
						var closeX = document.getElementById('modal-close-x');
						closeX.insertAdjacentHTML('beforeBegin', 'Add Decision Component');
						
						//set content
						var content = document.getElementById('modal-content');
						content.insertAdjacentHTML('afterBegin',
							'<p><label for="form-input-name" style="display:block;width:45%;float:left;">Name</label><input id="form-input-name" type="text" name="name" placeholder="Name of decisioncomponent" style="width:50%;"/></p>' +
							'<p><label for="form-input-type" style="display:block;width:45%;float:left;">Componenttype</label><input id="form-input-type" type="text" name="type" placeholder="Select componenttype..." style="width:50%;"/></p>' +
							'<p><input id="form-input-submit" type="submit" value="Add Decision Component" style="float:right;"/></p>'
						);
						
						$("#form-input-type").on('change', function () {
							var typeInput = document.getElementById('form-input-type');
							var type = typeInput.value;
							if(type === 'Argument'){
								typeInput.insertAdjacentHTML('afterEnd', '<p id="type-of-argument-para"><label for="type-of-argument" style="display:block;width:45%;float:left;">Type of Argument</label><input type="radio" name="type-of-argument" value="pro" checked="checked">Pro<input type="radio" name="type-of-argument" value="contra">Contra<input type="radio" name="type-of-argument" value="comment">Comment</p>');
							} else {
								var para = document.getElementById("type-of-argument-para");
								if(para){
									clearInner(para);
									para.parentNode.removeChild(para);
								}
							}
						});
						
						var data = [
							{
								id: "Alternative",
								text: "Alternative"
							},
							{
								id: "Argument",
								text: "Argument"
							},
							{
								id: "Assessment",
								text: "Assessment"
							},
							{
								id: "Assumption",
								text: "Assumption"
							},
							{
								id: "Claim",
								text: "Claim"
							},
							{
								id: "Constraint",
								text: "Constraint"
							},
							{
								id: "Context",
								text: "Context"
							},
							{
								id: "Goal",
								text: "Goal"
							},
							{
								id: "Implication",
								text: "Implication"
							},
							{
								id: "Issue",
								text: "Issue"
							},
							{
								id: "Problem",
								text: "Problem"
							},
							{
								id: "Solution",
								text: "Solution"
							}
						];
						var singleSelect = $("#form-input-type").select2({
							data: data
						});
						
						var submitButton = document.getElementById('form-input-submit');
						submitButton.onclick = function (){
							var name = document.getElementById('form-input-name').value;
							var type = document.getElementById('form-input-type').value;
							if(type === "Argument"){
								var argumentCheckBoxGroup = document.getElementsByName("type-of-argument");
								for(var i = 0; i < argumentCheckBoxGroup.length; i++) {
								    if(argumentCheckBoxGroup[i].checked == true) {
									    var selectedNatureOfArgument = argumentCheckBoxGroup[i].value;
									    if (selectedNatureOfArgument == "pro"){
											createDecisionComponent(name, type, function(data){
												var successFlag = AJS.flag({
													type: 'success',
													close: 'auto',
													title: 'Success',
													body: type + ' has been created.'
												});
												var idOfNewObject = data.id;
												createLink(options.$trigger.context.id, idOfNewObject, "support", function(data){
													var successFlag = AJS.flag({
														type: 'success',
														close: 'auto',
														title: 'Success',
														body: 'IssueLink has been created.'
													});
													buildTreeViewer(projectKey, idOfNewObject);
												});
											});
									    } else if (selectedNatureOfArgument == "contra"){
											createDecisionComponent(name, type, function(data){
												var successFlag = AJS.flag({
													type: 'success',
													close: 'auto',
													title: 'Success',
													body: type + ' has been created.'
												});
												var idOfNewObject = data.id;
												createLink(options.$trigger.context.id, idOfNewObject, "attack", function(data){
													var successFlag = AJS.flag({
														type: 'success',
														close: 'auto',
														title: 'Success',
														body: 'IssueLink has been created.'
													});
													buildTreeViewer(projectKey, idOfNewObject);
												});
											});
									    } else if (selectedNatureOfArgument == "comment"){
											createDecisionComponent(name, type, function(data){
												var successFlag = AJS.flag({
													type: 'success',
													close: 'auto',
													title: 'Success',
													body: type + ' has been created.'
												});
												var idOfNewObject = data.id;
												createLink(options.$trigger.context.id, idOfNewObject, "comment", function(data){
													var successFlag = AJS.flag({
														type: 'success',
														close: 'auto',
														title: 'Success',
														body: 'IssueLink has been created.'
													});
													buildTreeViewer(projectKey, idOfNewObject);
												});
											});
									    }
									}
								}
							} else {
								createDecisionComponent(name, type, function(data){
									var successFlag = AJS.flag({
										type: 'success',
										close: 'auto',
										title: 'Success',
										body: type + ' has been created.'
									});
									var idOfNewObject = data.id;
									createLink(options.$trigger.context.id, idOfNewObject, "contain", function(data){
										var successFlag = AJS.flag({
											type: 'success',
											close: 'auto',
											title: 'Success',
											body: 'IssueLink has been created.'
										});
										buildTreeViewer(projectKey, idOfNewObject);
									});
								});
							}
							closeModal();
						};
						
						var modal = document.getElementById('ContextMenuModal');
						modal.style.display = "block";
					}
				},
				"edit": {name: "Edit Decision Component", 
					callback: function(key, options){
						//set header
						var closeX = document.getElementById('modal-close-x');
						closeX.insertAdjacentHTML('beforeBegin', 'Edit Decision Component');
						
						var context = options.$trigger.context;
						var content = document.getElementById('modal-content');
						content.insertAdjacentHTML('afterBegin',
							'<p><label for="form-input-name" style="display:block;width:45%;float:left;">Name</label><input id="form-input-name" type="text" name="name" value="' + context.textContent + '" style="width:50%;" readonly/></p>' +
							'<p><label for="form-input-description" style="display:block;width:45%;float:left;">Description</label><input id="form-input-description" type="text" name="type" placeholder="Type in description" style="width:50%;"/></p>' +
							'<p><input id="form-input-submit" type="submit" value="Edit Decision Component" style="float:right;"/></p>'
						);
						
						var children = context.childNodes;
						for(var index = 0; index < children.length; index++){
							if (children.hasOwnProperty(index)) { 
								if(index === 0){
									document.getElementById('form-input-name').value = children[index].innerText;
								} else if(index === 1){
									//title, not needed right now
								} else if(index === 2){
									//description, not needed right now
								} else {
									//not implemented, not needed right now
								}
							}
						}
						
						var submitButton = document.getElementById('form-input-submit');
						submitButton.onclick = function (){
							var name = document.getElementById('form-input-name').value;
							var description = document.getElementById('form-input-description').value;
							editDecisionComponent(context.id, name, description, function(data){
								var successFlag = AJS.flag({
									type: 'success',
									close: 'auto',
									title: 'Success',
									body: 'Decisioncomponent has been updated'
								});
								var nodeId = $.jstree.reference('#evts').get_selected()[0];
								buildTreeViewer(projectKey, nodeId);
							});
							closeModal();
						};
						
						// Get the modal window
						var modal = document.getElementById('ContextMenuModal');
						modal.style.display = "block";
					}
				},
				"delete": {name: "Delete Decision Component", 
					callback: function(key, options){
						//set header
						var closeX = document.getElementById('modal-close-x');
						closeX.insertAdjacentHTML('beforeBegin', 'Delete Decision Component');
						
						var context = options.$trigger.context;
						var content = document.getElementById('modal-content');
						content.insertAdjacentHTML('afterBegin',
							'<p><input id="abort-submit" type="submit" value="Abort Action" style="float:right;"/><input id="form-input-submit" type="submit" value="Delete Decision Component" style="float:right;"/></p>'
						);
						
						var abortButton = document.getElementById('abort-submit');
						abortButton.onclick = function(){
							closeModal();
						};
						var submitButton = document.getElementById('form-input-submit');
						submitButton.onclick = function (){
							deleteDecisionComponent(context.id, function(data){
								var successFlag = AJS.flag({
									type: 'success',
									close: 'auto',
									title: 'Success',
									body: 'Decisioncomponent has been deleted'
								});
								var nodeId = $.jstree.reference('#evts').get_selected()[0];
								buildTreeViewer(projectKey, nodeId);
							});
							closeModal();
						};
						
						// Get the modal window
						var modal = document.getElementById('ContextMenuModal');
						modal.style.display = "block";
					}
				}
			}
		});

		$('.context-menu-one').on('click', function(e){
			console.log('clicked', this);
		})
	});
};
var buildTreant = function(projectKey, node){
	var depthOfTree = document.getElementById("depthOfTreeInput").value;
	var treantUrl = AJS.contextPath() + "/rest/treantsrest/latest/treant.json?projectKey=" + projectKey + "&issueKey=" + node.key + "&depthOfTree=" + depthOfTree;
	getJSON(treantUrl, function(err, data) {
		if (err!=null){
			document.getElementById("treant-container").innerHTML = "Fehler beim Abfragen der Daten. Error-Code: " + err;
		} else {
			document.getElementById("treant-container").innerHTML="";
			var myTreant = new Treant(data);
			var modal = document.getElementById('ContextMenuModal');
			//add click-handler for elements in modal to close modal window
			var elementsWithCloseFunction = document.getElementsByClassName("modal-close");
			for (var counter = 0; counter < elementsWithCloseFunction.length; counter++){
				elementsWithCloseFunction[counter].onclick = function() {
					closeModal();
				}
			}
			//close modal window if user clicks anywhere outside of the modal
			window.onclick = function(event) {
				if (event.target == modal) {
					closeModal();
				}
			}
			createContextMenuForTreeNodes(projectKey);
		}
	});
}


var addOptionsToAllDecisionComponents = function(parentNode){
	var issueTypes = ["Problem", "Issue", "Goal", "Solution", "Alternative", "Claim", "Context", "Assumption", "Constraint", "Implication", "Assessment", "Argument"];
	for (counter = 0; counter < issueTypes.length; ++ counter){
		addOptionToDecisionComponent(issueTypes[counter],parentNode);
	}
};
var addOptionToDecisionComponent = function(type, parentNode){
	if (type === "Solution"){
		if(document.getElementById(type).innerHTML == ""){
			document.getElementById(type).insertAdjacentHTML('beforeend', '<p>Do you want to add an additional ' + type + '?<input type="text" id="inputField' + type + '" placeholder="Name of ' + type + '"><input type="button" name="CreateAndLinkDecisionComponent' + type+ '" id="CreateAndLinkDecisionComponent' + type+ '" value="Add ' + type + '"/></p>');
			var createDecisionComponentButton = document.getElementById("CreateAndLinkDecisionComponent" + type);
			var inputField = document.getElementById("inputField" + type);
			createDecisionComponentButton.addEventListener('click', function(event){
				var tempString = inputField.value;
				inputField.value = "";
				createDecisionComponent(tempString, type, function(data){
					var successFlag = AJS.flag({
						type: 'success',
						close: 'auto',
						title: 'Success',
						body: type + ' has been created.'
					});
					createLink(parentNode.id, data.id, "contain", function(data){
						var successFlag = AJS.flag({
							type: 'success',
							close: 'auto',
							title: 'Success',
							body: 'IssueLink has been created.'
						});
					});
					var tree = $('#evts').jstree(true);
					var nodeId = tree.create_node('' + parentNode.id, data, 'last', tree.redraw(true), true);
					tree.deselect_all();
					tree.select_node(nodeId);
				});
			});
		}
	} else if (type === "Argument"){
		document.getElementById(type).insertAdjacentHTML('beforeend', '<p>Do you want to add an additional ' + type + '? <input type="radio" name="natureOfArgument" value="pro" checked="checked">Pro<input type="radio" name="natureOfArgument" value="contra">Contra<input type="radio" name="natureOfArgument" value="comment">Comment<input type="text" id="inputField' + type + '" placeholder="Name of ' + type + '"><input type="button" name="CreateAndLinkDecisionComponent' + type+ '" id="CreateAndLinkDecisionComponent' + type+ '" value="Add ' + type + '"/></p>');
		var createDecisionComponentButton = document.getElementById("CreateAndLinkDecisionComponent" + type);
		var inputField = document.getElementById("inputField" + type);		
		createDecisionComponentButton.addEventListener('click', function(event){
			var tempString = inputField.value;
			inputField.value = "";
			var argumentCheckBoxGroup = document.getElementsByName("natureOfArgument");
			for(var i = 0; i < argumentCheckBoxGroup.length; i++) {
			   if(argumentCheckBoxGroup[i].checked == true) {
				   var selectedNatureOfArgument = argumentCheckBoxGroup[i].value;
				   if (selectedNatureOfArgument == "pro"){
						createDecisionComponent(tempString, type, function(data){
							var successFlag = AJS.flag({
								type: 'success',
								close: 'auto',
								title: 'Success',
								body: type + ' has been created.'
							});
							createLink(parentNode.id, data.id, "support", function(data){
								var successFlag = AJS.flag({
									type: 'success',
									close: 'auto',
									title: 'Success',
									body: 'IssueLink has been created.'
								});
							});
							var tree = $('#evts').jstree(true);
							var nodeId = tree.create_node('' + parentNode.id, data, 'last', tree.redraw(true), true);
							tree.deselect_all();
							tree.select_node(nodeId);
						});
				   } else if (selectedNatureOfArgument == "contra"){
						createDecisionComponent(tempString, type, function(data){
							var successFlag = AJS.flag({
								type: 'success',
								close: 'auto',
								title: 'Success',
								body: type + ' has been created.'
							});
							createLink(parentNode.id, data.id, "attack", function(data){
								var successFlag = AJS.flag({
									type: 'success',
									close: 'auto',
									title: 'Success',
									body: 'IssueLink has been created.'
								});
							});
							var tree = $('#evts').jstree(true);
							var nodeId = tree.create_node('' + parentNode.id, data, 'last', tree.redraw(true), true);
							tree.deselect_all();
							tree.select_node(nodeId);
						});
				   } else if (selectedNatureOfArgument == "comment"){
						createDecisionComponent(tempString, type, function(data){
							var successFlag = AJS.flag({
								type: 'success',
								close: 'auto',
								title: 'Success',
								body: type + ' has been created.'
							});
							createLink(parentNode.id, data.id, "comment", function(data){
								var successFlag = AJS.flag({
									type: 'success',
									close: 'auto',
									title: 'Success',
									body: 'IssueLink has been created.'
								});
							});
							var tree = $('#evts').jstree(true);
							var nodeId = tree.create_node('' + parentNode.id, data, 'last', tree.redraw(true), true);
							tree.deselect_all();
							tree.select_node(nodeId);
						});
				    }
			    }
			}
		});
	} else {
		document.getElementById(type).insertAdjacentHTML('beforeend', '<p>Do you want to add an additional ' + type + '?<input type="text" id="inputField' + type + '" placeholder="Name of ' + type + '"><input type="button" name="CreateAndLinkDecisionComponent' + type+ '" id="CreateAndLinkDecisionComponent' + type+ '" value="Add ' + type + '"/></p>');
		var createDecisionComponentButton = document.getElementById("CreateAndLinkDecisionComponent" + type);	
		createDecisionComponentButton.addEventListener('click', function(event){
			var inputField = document.getElementById("inputField" + type);
			var tempString = inputField.value;
			inputField.value = "";
			createDecisionComponent(tempString, type, function(data){
				var successFlag = AJS.flag({
					type: 'success',
					close: 'auto',
					title: 'Success',
					body: type + ' has been created.'
				});
				console.log(parentNode);
				console.log(data);
				createLink(parentNode.id, data.id, "contain", function(data){
					var successFlag = AJS.flag({
						type: 'success',
						close: 'auto',
						title: 'Success',
						body: 'IssueLink has been created.'
					});
				});
				var tree = $('#evts').jstree(true);
				var nodeId = tree.create_node('' + parentNode.id, data, 'last', tree.redraw(true), true);
				tree.deselect_all();
				tree.select_node(nodeId);
			});
		});
	}
};
var fillAccordion = function(data, projectKey, node){
	var detailsElement = document.getElementById("Details");
	detailsElement.insertAdjacentHTML('beforeend', '<p>' + node.key + ' / ' + node.summary + '<input type="button" name="updataIssue" id="updateIssue" value="Update Issue"/></p>' + 
		'<p><textarea id="IssueDescription" style="width:99%; height:auto;border: 1px solid rgba(204,204,204,1); ">' + 
		node.description + '</textarea></p>'
	);
	/*
	detailsElement.insertAdjacentHTML('beforeend', '<p><a href="' +
		AJS.contextPath() + '/browse/' + node.key + '">' + node.key + 
		' / ' + node.summary + '</a><input type="button" name="updataIssue" id="updateIssue" value="Update Issue"/></p>' + 
		'<p><textarea id="IssueDescription" style="width:99%; height:auto;border: 1px solid rgba(204,204,204,1); ">' + 
		node.description + '</textarea></p>'
	);
	*/
	detailsElement.style.display = "block";
	var updateButton = document.getElementById("updateIssue");
	updateButton.addEventListener('click', function(event){
		editDecisionComponent(node.id, node.summary, document.getElementById("IssueDescription").value, function(data){
			var successFlag = AJS.flag({
				type: 'success',
				close: 'auto',
				title: 'Success',
				body: 'Decisioncomponent has been updated'
			});
			buildTreeViewer(projectKey, node.id);
		});
	});
	
	getJSON(AJS.contextPath() + "/rest/decisions/latest/decisions.json?projectKey=" + projectKey + '&issueId=' + node.id, function(err, data) {
		if (err!=null){
			displayGetJsonError(err);
		} else {
			document.getElementById("Details").insertAdjacentHTML('beforeend', '<input type="text" id="linkExistingIssueSearchField" placeholder="Link to existing Issue..."/>' + 
				'<input type="button" name="linkExistingIssueButton" id="linkExistingIssueButton" value="Create Link"/>');
			var singleSelect = $("#linkExistingIssueSearchField").select2({
				data: data,
			});
			document.getElementById("linkExistingIssueSearchField").style.display = "block";
			var linkButton = document.getElementById("linkExistingIssueButton");
			linkButton.addEventListener('click', function(event){ 
				var arrayOfSelectedIssues = $('#linkExistingIssueSearchField').select2('data');
				var divider = arrayOfSelectedIssues.text.indexOf(" / ");
				createIssueLink("contain", node.key, arrayOfSelectedIssues.text.substring(0, divider), AJS.contextPath());
				singleSelect.select2('val', '');
				window.location.reload();
			});
		}
	});
	
	if(data.node.children.length > 0){
		for(counter = 0; counter < data.node.children.length; ++counter){
			var child = $('#evts').jstree(true).get_node(data.node.children[counter]);
			var issueType = child.data.issueType;
			var array= ["Problem", "Issue", "Goal", "Solution", "Alternative", "Claim", "Context", "Assumption", "Constraint", "Implication", "Assessment", "Argument"];
			if(array.indexOf(issueType)!=-1){
				document.getElementById(issueType).insertAdjacentHTML('beforeend', '<div class="issuelinkbox"><p>' + child.data.key +
					' / ' + child.data.summary + '</p>' + '<p>Description: ' + child.data.description + '</p></div>'
				);
				/*
				document.getElementById(issueType).insertAdjacentHTML('beforeend', '<div class="issuelinkbox"><p><a href="' +
					AJS.contextPath() + '/browse/' + child.data.key + '">' + child.data.key +
					' / ' + child.data.summary + '</a></p>' + '<p>Description: ' + child.data.description + '</p></div>'
				);
				*/
				document.getElementById(child.data.issueType).style.display = "block";
			}
		}
		addOptionsToAllDecisionComponents(data.node.data);
	} else {
		addOptionsToAllDecisionComponents(data.node.data);
	}
}
var buildTreeViewer = function(projectKey, nodeId){
	var treeJSONUrl = AJS.contextPath() + "/rest/treeviewerrest/latest/treeviewer.json?projectKey=" + projectKey;
	getJSON(treeJSONUrl, function(err, data) {
		if (err!=null){
			displayGetJsonError(err);
		} else {
			if($('#evts').jstree(true)){
				var tree = $('#evts').jstree(true);
				tree.destroy();
			}
			$('#evts')
				.on("select_node.jstree", function (e,data) {
					setBack();
					var node = data.node.data;
					fillAccordion(data, projectKey, node);
					buildTreant(projectKey, node);
				})
				.on('ready.jstree', function(e, data){
					if(nodeId){
						console.log($('#evts').jstree(true).select_node("" + nodeId));
					}
				})
				.jstree(data);
			document.getElementById("Details").style.display = "block";
			$(".search-input").keyup(function() {
				var searchString = $(this).val();
				$('#evts').jstree(true).search(searchString);
			});
		}
	});
}

/* Displays Error Message in Accoridon */
var displayGetJsonError = function(errorCode){
	document.getElementById("Details").innerHTML = "Error occured while retrieving data. Error-Code: " + errorCode;
	document.getElementById("Problem").innerHTML = "Error occured while retrieving data. Error-Code: " + errorCode;
	document.getElementById("Issue").innerHTML = "Error occured while retrieving data. Error-Code: " + errorCode;
	document.getElementById("Goal").innerHTML = "Error occured while retrieving data. Error-Code: " + errorCode;
	document.getElementById("Solution").innerHTML = "Error occured while retrieving data. Error-Code: " + errorCode;
	document.getElementById("Alternative").innerHTML = "Error occured while retrieving data. Error-Code: " + errorCode;
	document.getElementById("Claim").innerHTML = "Error occured while retrieving data. Error-Code: " + errorCode;
	document.getElementById("Context").innerHTML = "Error occured while retrieving data. Error-Code: " + errorCode;
	document.getElementById("Assumption").innerHTML = "Error occured while retrieving data. Error-Code: " + errorCode;
	document.getElementById("Constraint").innerHTML = "Error occured while retrieving data. Error-Code: " + errorCode;
	document.getElementById("Implication").innerHTML = "Error occured while retrieving data. Error-Code: " + errorCode;
	document.getElementById("Assessment").innerHTML = "Error occured while retrieving data. Error-Code: " + errorCode;
	document.getElementById("Argument").innerHTML = "Error occured while retrieving data. Error-Code: " + errorCode;
};
/* Deletes all content from Accordion */
var setBack = function(){
	document.getElementById("Details").innerHTML = "";
	document.getElementById("Problem").innerHTML = "";
	document.getElementById("Problem").style.display = "none";
	document.getElementById("Issue").innerHTML = "";
	document.getElementById("Issue").style.display = "none";
	document.getElementById("Goal").innerHTML = "";
	document.getElementById("Goal").style.display = "none";
	document.getElementById("Solution").innerHTML = "";
	document.getElementById("Solution").style.display = "none";
	document.getElementById("Alternative").innerHTML = "";
	document.getElementById("Alternative").style.display = "none";
	document.getElementById("Claim").innerHTML = "";
	document.getElementById("Claim").style.display = "none";
	document.getElementById("Context").innerHTML = "";
	document.getElementById("Context").style.display = "none";
	document.getElementById("Assumption").innerHTML = "";
	document.getElementById("Assumption").style.display = "none";
	document.getElementById("Constraint").innerHTML = "";
	document.getElementById("Constraint").style.display = "none";
	document.getElementById("Implication").innerHTML = "";
	document.getElementById("Implication").style.display = "none";
	document.getElementById("Assessment").innerHTML = "";
	document.getElementById("Assessment").style.display = "none";
	document.getElementById("Argument").innerHTML = "";
	document.getElementById("Argument").style.display = "none";
}
var initializeSite = function(){
	var pathname = window.location.pathname;
	var stringArray = pathname.split("/");
	var projectKey = stringArray[stringArray.length-1];
	buildTreeViewer(projectKey);
	
	/*ClickHandler for accordionelements*/
	$(document).ready(function(){
		$("dt").click(function(){
			$(this).next("dd").slideToggle("fast");
		});
	});
	/*ClickHandler for the creation of decisions*/
	var createDecisionButton = document.getElementById("CreateDecision");
	var DecisionInputField = document.getElementById("DecisionInputField");
	createDecisionButton.addEventListener('click', function(event){
		var tempDecString = DecisionInputField.value;
		DecisionInputField.value = "";
		createDecisionComponent(tempDecString, "Decision", function(data){
			/*TODO data verarbeiten*/
			var successFlag = AJS.flag({
				type: 'success',
				close: 'auto',
				title: 'Success',
				body: 'Decision has been created'
			});
			var tree = $('#evts').jstree(true);
			var nodeId = tree.create_node('#', data, 'last', tree.redraw(true), true);
			tree.deselect_all();
			tree.select_node(nodeId);
		});
	});
	/*ClickHandler for the Editor Button*/
	var viewEditorButton = document.getElementById("view-editor");
	viewEditorButton.addEventListener('click', function(event){
		var editorContainer = document.getElementById("container");
		var treantContainer = document.getElementById("treant-container");
		editorContainer.style.display="block";
		treantContainer.style.visibility = "hidden";
	});
	/*ClickHandler for the Tree Button*/
	var viewTreeButton = document.getElementById("view-tree");
	viewTreeButton.addEventListener('click', function(event){
		var editorContainer = document.getElementById("container");
		var treantContainer = document.getElementById("treant-container");
		treantContainer.style.visibility = "visible";
		editorContainer.style.display = "none";
	});
	var DepthOfTreeInput = document.getElementById("depthOfTreeInput");
	DepthOfTreeInput.addEventListener('input', function (evt) {
		var DepthOfTreeWarningLabel = document.getElementById("DepthOfTreeWarning");
		if(this.value > 0){
			DepthOfTreeWarningLabel.style.visibility = "hidden";
		} else {
			DepthOfTreeWarningLabel.style.visibility = "visible";
		}
	});
}

function closeModal(){
	// Get the modal window
	var modal = document.getElementById('ContextMenuModal');
	modal.style.display = "none";
	var modalHeader = document.getElementById('modal-header');
	if(modalHeader.hasChildNodes()){
		var childNodes = modalHeader.childNodes;
		for (var index = 0; index < childNodes.length; ++index){	
			var child = childNodes[index];
			if(child.nodeType === 3){
				child.parentNode.removeChild(child);
			}
		}
	}
	var modalContent = document.getElementById('modal-content');
	if(modalContent){
		clearInner(modalContent);
	}
}
/*
Source: https://stackoverflow.com/users/2234742/maximillian-laumeister
Maximillian Laumeister
Software Developer at Tanzle
*/
function clearInner(node) {
  while (node.hasChildNodes()) {
    clear(node.firstChild);
  }
}
function clear(node) {
  while (node.hasChildNodes()) {
    clear(node.firstChild);
  }
  node.parentNode.removeChild(node);
}