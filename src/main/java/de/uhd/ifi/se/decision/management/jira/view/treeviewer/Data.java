package de.uhd.ifi.se.decision.management.jira.view.treeviewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;

import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.model.text.PartOfJiraIssueText;

/**
 * Model class for Tree Viewer nodes
 */
public class Data {

	@XmlElement
	private String id;

	@XmlElement
	private String text;

	@XmlElement
	private List<Data> children;

	@XmlElement(name = "data")
	private KnowledgeElement element;

	@XmlElement
	private String icon;

	@XmlElement(name = "a_attr")
	private Map<String, String> a_attr;

	@XmlElement(name = "li_attr")
	private Map<String, String> li_attr;

	public Data() {
	}

	public Data(KnowledgeElement knowledgeElement) {
		this.id = "tv" + String.valueOf(knowledgeElement.getId());
		this.text = knowledgeElement.getSummary();
		this.icon = KnowledgeType.getIconUrl(knowledgeElement);
		this.element = knowledgeElement;
		if (knowledgeElement.getDescription() != null && !knowledgeElement.getDescription().isBlank()
				&& !knowledgeElement.getDescription().equals("undefined")) {
			this.a_attr = ImmutableMap.of("title", knowledgeElement.getDescription());
		}
		this.li_attr = ImmutableMap.of("class", "issue");
		if (knowledgeElement instanceof PartOfJiraIssueText) {
			this.li_attr = ImmutableMap.of("class", "sentence", "sid", "s" + knowledgeElement.getId());
		}
		String textColor = knowledgeElement.getStatus().getColor();
		if (!textColor.isBlank()) {
			a_attr = new ImmutableMap.Builder<String, String>().putAll(a_attr).put("style", "color:" + textColor)
					.build();
		}
	}

	public Data(KnowledgeElement knowledgeElement, Link link) {
		this(knowledgeElement);
		this.icon = KnowledgeType.getIconUrl(knowledgeElement, link.getType());
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public List<Data> getChildren() {
		if (children == null) {
			return new ArrayList<Data>();
		}
		return children;
	}

	public void setChildren(List<Data> children) {
		this.children = children;
	}

	public Object getElement() {
		return element;
	}

	public String getIcon() {
		return icon;
	}
}