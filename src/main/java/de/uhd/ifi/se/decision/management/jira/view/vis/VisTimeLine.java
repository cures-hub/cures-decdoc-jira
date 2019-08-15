package de.uhd.ifi.se.decision.management.jira.view.vis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

import com.atlassian.jira.user.ApplicationUser;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.persistence.AbstractPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.JiraIssueTextPersistenceManager;

public class VisTimeLine {

	private List<DecisionKnowledgeElement> elementList;

	@XmlElement
	private HashSet<VisTimeLineNode> dataSet;

	@XmlElement
	private HashSet<VisTimeLineGroup> groupSet;

	private List<ApplicationUser> userList;

	public VisTimeLine(String projectKey) {
		if (projectKey != null) {
			AbstractPersistenceManager strategy = AbstractPersistenceManager.getDefaultPersistenceStrategy(projectKey);
			elementList = strategy.getDecisionKnowledgeElements();
			AbstractPersistenceManager jiraIssueCommentPersistenceManager = new JiraIssueTextPersistenceManager(
					projectKey);
			elementList.addAll(jiraIssueCommentPersistenceManager.getDecisionKnowledgeElements());
		}
		createDataSet();
	}

	public VisTimeLine(List<DecisionKnowledgeElement> elements) {
		if (elements != null) {
			elementList = elements;
			createDataSet();
		}
	}

	public HashSet<VisTimeLineNode> getEvolutionData() {
		return dataSet;
	}

	public List<DecisionKnowledgeElement> getElementList() {
		return elementList;
	}

	public void setElementList(List<DecisionKnowledgeElement> elementList) {
		this.elementList = elementList;
	}

	private void createDataSet() {
		dataSet = new HashSet<>();
		groupSet = new HashSet<>();
		if(elementList != null) {
			Set<Long> usedApplicationUser = new HashSet<Long>();
			for (DecisionKnowledgeElement element : elementList) {
				AbstractPersistenceManager manager =
						AbstractPersistenceManager.getPersistenceManager(element.getProject().getProjectKey(),
								element.getDocumentationLocation());
				ApplicationUser user = manager.getCreator(element);
				if(!usedApplicationUser.contains(user.getId())){
					usedApplicationUser.add(user.getId());
					groupSet.add(new VisTimeLineGroup(user));
				}
				VisTimeLineNode node = new VisTimeLineNode(element);
				node.setGroup(user.getId());
				dataSet.add(node);
			}
		}
	}
}
