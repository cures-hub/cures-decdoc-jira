package de.uhd.ifi.se.decision.management.jira.view.treant;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeProject;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Link;

/**
 * Model class for Treant node
 */
public class Node {
	@XmlElement(name = "text")
	private Map<String, String> nodeContent;

	@XmlElement
	private Map<String, Map<String, String>> connectors;

	@XmlElement
	private Map<String, String> link;

	@XmlElement(name = "HTMLclass")
	private String htmlClass;

	@XmlElement(name = "HTMLid")
	private long htmlId;

	@XmlElement(name = "innerHTML")
	private String innerHTML;

	@XmlElement
	private List<Node> children;

	@XmlElement
	private Map<String, Boolean> collapsed;

	public Node() {
		this.connectors = ImmutableMap.of("style", ImmutableMap.of("stroke", "#000000"));
		// this.connectors = new ConcurrentHashMap<String, Map<String, String>>();
		// Map<String, String> connectorStyle = new ConcurrentHashMap<String, String>();
		// this.connectorStyle.put("stroke", "#000000");
		// this.connectorStyle.put("stroke-width", "2");
		// this.connectorStyle.put("arrow-start", "block-wide-long");
		// this.connectors.put("style", connectorStyle);
	}

	public Node(DecisionKnowledgeElement decisionKnowledgeElement, boolean isCollapsed) {
		this();
		KnowledgeType type = decisionKnowledgeElement.getType();
		if (type == KnowledgeType.OTHER) {
			this.nodeContent = ImmutableMap.of("title", decisionKnowledgeElement.getSummary(), "desc",
					decisionKnowledgeElement.getKey());
		} else {
			this.nodeContent = ImmutableMap.of("name", type.toString(), "title", decisionKnowledgeElement.getSummary(),
					"desc", decisionKnowledgeElement.getKey());
		}
		this.htmlClass = decisionKnowledgeElement.getType().getSuperType().toString().toLowerCase(Locale.ENGLISH);
		this.htmlId = decisionKnowledgeElement.getId();
		DecisionKnowledgeProject project = decisionKnowledgeElement.getProject();
		if (project.isIssueStrategy()) {
			ApplicationProperties applicationProperties = ComponentAccessor.getApplicationProperties();
			this.link = ImmutableMap.of("href", applicationProperties.getString(APKeys.JIRA_BASEURL) + "/browse/"
					+ decisionKnowledgeElement.getKey(), "target", "_blank");
		}
		if (isCollapsed) {
			this.collapsed = ImmutableMap.of("collapsed", isCollapsed);
		}
	}

	public Node(DecisionKnowledgeElement decisionKnowledgeElement, Link link, boolean isCollapsed) {
		this(decisionKnowledgeElement, isCollapsed);
		switch (link.getType()) {
		case "support":
			if (decisionKnowledgeElement.getId() == link.getSourceElement().getId()) {
				this.nodeContent = ImmutableMap.of("name", "Pro-argument", "title",
						decisionKnowledgeElement.getSummary(), "desc", decisionKnowledgeElement.getKey());
				this.htmlClass = "pro";

			}
			break;
		case "attack":
			if (decisionKnowledgeElement.getId() == link.getSourceElement().getId()) {
				this.nodeContent = ImmutableMap.of("name", "Con-argument", "title",
						decisionKnowledgeElement.getSummary(), "desc", decisionKnowledgeElement.getKey());
				this.htmlClass = "contra";
			}
			break;
		default:
			break;
		}
	}

	public Map<String, String> getNodeContent() {
		return nodeContent;
	}

	public void setNodeContent(Map<String, String> nodeContent) {
		this.nodeContent = nodeContent;
	}

	public Map<String, String> getLink() {
		return link;
	}

	public void setLink(Map<String, String> link) {
		this.link = link;
	}

	public String getHtmlClass() {
		return htmlClass;
	}

	public void setHtmlClass(String htmlClass) {
		this.htmlClass = htmlClass;
	}

	public long getHtmlId() {
		return htmlId;
	}

	public void setHtmlId(long htmlId) {
		this.htmlId = htmlId;
	}

	public String getInnerHTML() {
		return innerHTML;
	}

	public void setInnerHTML(String innerHTML) {
		this.innerHTML = innerHTML;
	}

	public List<Node> getChildren() {
		return children;
	}

	public void setChildren(List<Node> children) {
		this.children = children;
	}

	public Map<String, Map<String, String>> getConnectors() {
		return connectors;
	}

	public void setConnectors(Map<String, Map<String, String>> connectors) {
		this.connectors = connectors;
	}

	public Map<String, Boolean> getCollapsed() {
		return collapsed;
	}

	public void setCollapsed(Map<String, Boolean> collapsed) {
		this.collapsed = collapsed;
	}
}
