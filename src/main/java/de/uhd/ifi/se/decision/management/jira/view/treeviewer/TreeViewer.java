package de.uhd.ifi.se.decision.management.jira.view.treeviewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

import org.jgrapht.traverse.BreadthFirstIterator;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.model.impl.KnowledgeElementImpl;
import de.uhd.ifi.se.decision.management.jira.model.text.PartOfJiraIssueText;
import de.uhd.ifi.se.decision.management.jira.persistence.impl.GenericLinkManager;

/**
 * Creates tree viewer content
 */
public class TreeViewer {

	@XmlElement
	private boolean multiple;

	@XmlElement(name = "check_callback")
	private boolean checkCallback;

	@XmlElement
	private Map<String, Boolean> themes;

	@XmlElement
	protected Set<Data> data;

	protected KnowledgeGraph graph;
	private List<String> ids;
	private long index;

	private List<Long> alreadyVisitedEdges;

	/** If true irrelevant sentences are added to the tree */
	public static boolean isCalledFromIssueTabPanel = false;

	public TreeViewer() {
		this.multiple = false;
		this.checkCallback = true;
		this.themes = ImmutableMap.of("icons", true);

		this.alreadyVisitedEdges = new ArrayList<Long>();
		this.ids = new ArrayList<String>();
	}

	public TreeViewer(String projectKey, KnowledgeType rootElementType) {
		this();
		if (rootElementType == KnowledgeType.OTHER) {
			return;
		}
		graph = KnowledgeGraph.getOrCreate(projectKey);
		List<KnowledgeElement> elements = graph.getElements(rootElementType);

		Set<Data> dataSet = new HashSet<Data>();
		for (KnowledgeElement element : elements) {
			dataSet.add(this.makeIdUnique(new Data(element)));
		}

		this.data = dataSet;
	}

	public TreeViewer(String projectKey) {
		this(projectKey, KnowledgeType.DECISION);
	}

	/**
	 * Constructor for DecXtract TreeViewer in IssueTabPanel.
	 *
	 * @param issueKey
	 *            the issue id
	 * @param showKnowledgeTypes
	 *            the show relevant (deprecated) currently used to distinguish
	 *            between Constructors
	 */
	public TreeViewer(String issueKey, Boolean[] showKnowledgeTypes) {
		this();
		// TODO Replace isCalledFromIssueTabPanel by a more clear variable or remove it.
		TreeViewer.isCalledFromIssueTabPanel = true;
		if (issueKey == null) {
			return;
		}
		Issue issue = ComponentAccessor.getIssueManager().getIssueObject(issueKey);
		if (issue == null) {
			return;
		}
		graph = KnowledgeGraph.getOrCreate(issue.getProjectObject().getKey());
		Data issueNode = this.getDataStructure(new KnowledgeElementImpl(issue));
		// Match irrelevant sentences back to list
		if (showKnowledgeTypes[4]) {
			for (Link link : GenericLinkManager.getLinksForElement(issue.getId(), DocumentationLocation.JIRAISSUE)) {
				KnowledgeElement opposite = link.getOppositeElement(issue.getId());
				if (opposite instanceof PartOfJiraIssueText && isSentenceShown(opposite)) {
					issueNode.getChildren().add(new Data(opposite));
				}
			}
		}
		KnowledgeType[] knowledgeType = { KnowledgeType.ISSUE, KnowledgeType.DECISION, KnowledgeType.ALTERNATIVE,
				KnowledgeType.ARGUMENT };
		this.data = new HashSet<Data>(Arrays.asList(issueNode));
		for (int i = 0; i <= 3; i++) {
			for (Data node : this.data) {
				Iterator<Data> it = node.getChildren().iterator();
				while (it.hasNext()) {
					removeChildrenWithType(it, knowledgeType[i], showKnowledgeTypes[i]);
				}
			}
		}
	}

	private void removeChildrenWithType(Iterator<Data> node, KnowledgeType knowledgeType, Boolean showKnowledgeType) {
		Data currentNode = node.next();
		if (!showKnowledgeType && currentNode.getElement().getType().equals(knowledgeType)) {
			node.remove();
			return;
		} else {
			Iterator<Data> it = currentNode.getChildren().iterator();
			while (it.hasNext()) {
				removeChildrenWithType(it, knowledgeType, showKnowledgeType);

			}
		}
	}

	private boolean isSentenceShown(KnowledgeElement element) {
		return !((PartOfJiraIssueText) element).isRelevant()
				&& ((PartOfJiraIssueText) element).getDescription().length() > 0;
	}

	public Data getDataStructure(KnowledgeElement decisionKnowledgeElement) {
		if (decisionKnowledgeElement == null) {
			return new Data();
		}
		if (graph == null) {
			graph = KnowledgeGraph.getOrCreate(decisionKnowledgeElement.getKey());
		}
		Data data = new Data(decisionKnowledgeElement);
		data = this.makeIdUnique(data);
		List<Data> children = this.getChildren(decisionKnowledgeElement);
		data.setChildren(children);
		return data;
	}

	protected List<Data> getChildren(KnowledgeElement decisionKnowledgeElement) {
		List<Data> children = new ArrayList<Data>();
		BreadthFirstIterator<KnowledgeElement, Link> iterator;
		try {
			iterator = new BreadthFirstIterator<>(graph, decisionKnowledgeElement);
		} catch (IllegalArgumentException e) {
			graph.addVertex(decisionKnowledgeElement);
			iterator = new BreadthFirstIterator<>(graph, decisionKnowledgeElement);
		}
		while (iterator.hasNext()) {
			KnowledgeElement iterNode = iterator.next();
			KnowledgeElement parentNode = iterator.getParent(iterNode);
			if (parentNode == null || parentNode.getId() != decisionKnowledgeElement.getId()) {
				continue;
			}
			if (!(iterNode instanceof KnowledgeElement)) {
				continue;
			}
			KnowledgeElement nodeElement = iterNode;
			Link edge = this.graph.getEdge(parentNode, nodeElement);
			if (this.alreadyVisitedEdges.contains(edge.getId())) {
				continue;
			}
			this.alreadyVisitedEdges.add(edge.getId());
			Data dataChild = new Data(nodeElement, edge);
			if (dataChild.getElement().getProject() == null || !dataChild.getElement().getProject().getProjectKey()
					.equals(decisionKnowledgeElement.getProject().getProjectKey())) {
				continue;
			}
			dataChild = this.makeIdUnique(dataChild);
			List<Data> childrenOfElement = this.getChildren(nodeElement);
			dataChild.setChildren(childrenOfElement);
			children.add(dataChild);
		}
		return children;
	}

	protected Data makeIdUnique(Data data) {
		if (!ids.contains(data.getId())) {
			ids.add(data.getId());
		} else {
			data.setId(index + data.getId());
			index++;
		}
		return data;
	}

	public boolean isMultiple() {
		return multiple;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}

	public boolean isCheckCallback() {
		return checkCallback;
	}

	public void setCheckCallback(boolean checkCallback) {
		this.checkCallback = checkCallback;
	}

	public Map<String, Boolean> getThemes() {
		return themes;
	}

	public void setThemes(Map<String, Boolean> themes) {
		this.themes = themes;
	}

	public Set<Data> getData() {
		return data;
	}

	public void setData(Set<Data> data) {
		this.data = data;
	}

	public List<String> getIds() {
		return ids;
	}

	public void setIds(List<String> ids) {
		this.ids = ids;
	}
}
