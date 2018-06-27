package de.uhd.ifi.se.decision.documentation.jira.model;

import java.util.List;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.springframework.stereotype.Component;

/**
 * @description Interface for decision knowledge elements
 */
@Component
@JsonDeserialize(as = DecisionKnowledgeElementImpl.class)
public interface DecisionKnowledgeElement {

	long getId();

	void setId(long id);

	String getSummary();

	void setSummary(String summary);

	String getDescription();

	void setDescription(String description);

	KnowledgeType getType();

	void setType(KnowledgeType type);

	void setType(String type);

	KnowledgeType getSuperType();

	String getProjectKey();

	void setProjectKey(String projectKey);

	DecisionKnowledgeProject getProject();

	/**
	 * The key resembles "<<projectKey>>-<<project internal id>>"
	 */
	String getKey();

	void setKey(String key);

	List<DecisionKnowledgeElement> getLinkedElements();

	List<Link> getOutwardLinks();

	List<Link> getInwardLinks();
}