package de.uhd.ifi.se.decision.management.jira.view.matrix;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlElement;

import de.uhd.ifi.se.decision.management.jira.filtering.FilterSettings;
import de.uhd.ifi.se.decision.management.jira.filtering.FilteringManager;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeProject;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.model.LinkType;

/**
 * Creates an adjacency matrix of the {@link KnowledgeGraph}. The matrix can
 * either be created on the entire graph or on a subgraph matching the giving
 * {@link FilterSettings}. The subgraph is provided by the
 * {@link FilteringManager}.
 * 
 * Each matrix cell contains a {@link Link} object or null if no link exists.
 * 
 * If you want to change the shown (sub-)graph, do not change this class but
 * change the {@link FilteringManager} and/or the {@link KnowledgeGraph}.
 */
public class Matrix {
	@XmlElement
	private Set<KnowledgeElement> headerElements;

	@XmlElement
	private Link[][] links;
	private int size;

	public Matrix(Set<KnowledgeElement> elements) {
		headerElements = elements;
		size = headerElements.size();
		links = getLinks();
	}

	public Matrix(FilterSettings filterSettings) {
		this(new FilteringManager(filterSettings).getSubgraphMatchingFilterSettings().vertexSet());
	}

	public Set<KnowledgeElement> getHeaderElements() {
		return headerElements;
	}

	/**
	 * Matrix of links for each cell.
	 */
	public Link[][] getLinks() {
		Link[][] links = new Link[size][size];
		Iterator<KnowledgeElement> iterator = headerElements.iterator();
		for (int positionY = 0; positionY < size; positionY++) {
			KnowledgeElement sourceElement = iterator.next();
			links[positionY] = getRowWithLinks(sourceElement);
		}
		return links;
	}

	public Link[] getRowWithLinks(KnowledgeElement sourceElement) {
		Link[] row = new Link[headerElements.size()];
		Iterator<KnowledgeElement> iterator = headerElements.iterator();
		for (int positionX = 0; positionX < size; positionX++) {
			KnowledgeElement targetElement = iterator.next();
			if (targetElement.getId() == sourceElement.getId()) {
				row[positionX] = null;
			} else {
				Link linkToTargetElement = sourceElement.getOutgoingLink(targetElement);
				row[positionX] = linkToTargetElement;
			}
		}
		return row;
	}

	/**
	 * Used to plot the legend for relationship types in the frontend.
	 * 
	 * @return map of link type names and colors. Also contains Jira issue link
	 *         types.
	 */
	@XmlElement(name = "linkTypesWithColor")
	public Map<String, String> getLinkTypesWithColor() {
		Map<String, String> linkTypesWithColor = new TreeMap<>();
		for (String linkTypeName : DecisionKnowledgeProject.getNamesOfLinkTypes()) {
			String color = LinkType.getLinkTypeColor(linkTypeName);
			linkTypesWithColor.put(linkTypeName, color);
		}
		return linkTypesWithColor;
	}
}