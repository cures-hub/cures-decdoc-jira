package de.uhd.ifi.se.decision.management.jira.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.codehaus.jackson.annotate.JsonAutoDetect;

import de.uhd.ifi.se.decision.management.jira.extraction.model.Sentence;
import de.uhd.ifi.se.decision.management.jira.persistence.GenericLinkManager;

/**
 * Model class for a graph of decision knowledge elements (no filter-criteria
 * given)
 */
@JsonAutoDetect
public class GraphImpl implements Graph {

	protected DecisionKnowledgeElement rootElement;
	protected DecisionKnowledgeProject project;
	protected List<Long> linkIds;
	protected List<Long> genericLinkIds;

	public GraphImpl() {
		linkIds = new ArrayList<Long>();
		genericLinkIds = new ArrayList<Long>();
		
		GenericLinkManager.migrateOldLinkIdColumn();
	}

	public GraphImpl(String projectKey) {
		this();
		this.project = new DecisionKnowledgeProjectImpl(projectKey);
	}

	public GraphImpl(DecisionKnowledgeElement rootElement) {
		this(rootElement.getProject().getProjectKey());
		this.rootElement = rootElement;
	}

	public GraphImpl(String projectKey, String rootElementKey) {
		this(projectKey);

		// for decision knowledge elements documented in comments or commit messages
		String issueKey = getJiraIssueKey(rootElementKey);
		this.rootElement = this.project.getPersistenceStrategy().getDecisionKnowledgeElement(issueKey);
	}

	private static String getJiraIssueKey(String rootElementKey) {
		if (rootElementKey.contains(":")) {
			return rootElementKey.substring(0, rootElementKey.indexOf(":"));
		}
		return rootElementKey;
	}

	@Override
	public List<DecisionKnowledgeElement> getLinkedElements(DecisionKnowledgeElement element) {
		Map<DecisionKnowledgeElement, Link> linkedElementsAndLinks = this.getLinkedElementsAndLinks(element);
		List<DecisionKnowledgeElement> linkedElements = new ArrayList<DecisionKnowledgeElement>(
				linkedElementsAndLinks.keySet());
		return linkedElements;
	}

	@Override
	public Map<DecisionKnowledgeElement, Link> getLinkedElementsAndLinks(DecisionKnowledgeElement element) {
		Map<DecisionKnowledgeElement, Link> linkedElementsAndLinks = new HashMap<DecisionKnowledgeElement, Link>();

		if (element == null) {
			return linkedElementsAndLinks;
		}

		linkedElementsAndLinks.putAll(this.getLinkedFirstClassElementsAndLinks(element));
		linkedElementsAndLinks.putAll(this.getLinkedSentencesAndLinks(element));
		return linkedElementsAndLinks;
	}

	protected Map<DecisionKnowledgeElement, Link> getLinkedFirstClassElementsAndLinks(
			DecisionKnowledgeElement element) {
		Map<DecisionKnowledgeElement, Link> linkedElementsAndLinks = new HashMap<DecisionKnowledgeElement, Link>();
		if(!element.getDocumentationLocation().equals(DocumentationLocation.JIRAISSUE)) {
			return linkedElementsAndLinks;
		}
		List<Link> links = this.project.getPersistenceStrategy().getLinks(element);
		for (Link link : links) {
			if (linkIds.contains(link.getId())) {
				continue;
			}
			DecisionKnowledgeElement oppositeElement = link.getOppositeElement(element);
			if (oppositeElement == null) {
				continue;
			}
			linkIds.add(link.getId());
			linkedElementsAndLinks.put(oppositeElement, link);
		}

		return linkedElementsAndLinks;
	}

	protected Map<DecisionKnowledgeElement, Link> getLinkedSentencesAndLinks(DecisionKnowledgeElement element) {
		Map<DecisionKnowledgeElement, Link> linkedElementsAndLinks = new HashMap<DecisionKnowledgeElement, Link>();

		String prefix = DocumentationLocation.getIdentifier(element);
		List<Link> links = GenericLinkManager.getLinksForElement(prefix + element.getId());

		for (Link link : links) {
			if (link.isInterProjectLink() || this.genericLinkIds.contains(link.getId())) {
				continue;
			}
			this.genericLinkIds.add(link.getId());
			linkedElementsAndLinks.put(link.getOppositeElement(element), link);
		}
		// remove irrelevant sentences from graph
		linkedElementsAndLinks.keySet().removeIf(e -> (e instanceof Sentence && !((Sentence) e).isRelevant()));

		return linkedElementsAndLinks;
	}

	@Override
	public List<DecisionKnowledgeElement> getAllElements() {
		List<DecisionKnowledgeElement> allElements = new ArrayList<DecisionKnowledgeElement>();
		allElements.add(this.getRootElement());
		allElements.addAll(this.getLinkedElements(this.getRootElement()));
		ListIterator<DecisionKnowledgeElement> iterator = allElements.listIterator();
		while (iterator.hasNext()) {
			List<DecisionKnowledgeElement> linkedElements = this.getLinkedElements(iterator.next());
			for (DecisionKnowledgeElement element : linkedElements) {
				if (!IteratorUtils.toList(iterator).contains(element)) {
					iterator.add(element);
					iterator.previous();
				}
			}
		}
		return allElements;
	}

	@Override
	public DecisionKnowledgeElement getRootElement() {
		return rootElement;
	}

	@Override
	public void setRootElement(DecisionKnowledgeElement rootElement) {
		this.rootElement = rootElement;
	}

	@Override
	public DecisionKnowledgeProject getProject() {
		return project;
	}

	@Override
	public void setProject(DecisionKnowledgeProject project) {
		this.project = project;
	}
}