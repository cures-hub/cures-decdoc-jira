package de.uhd.ifi.se.decision.management.jira.webhook;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.issue.link.IssueLinkCreatedEvent;
import com.atlassian.jira.event.issue.link.IssueLinkDeletedEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElementImpl;
import de.uhd.ifi.se.decision.management.jira.persistence.ConfigPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WebhookEventListener {

    @Autowired
    public WebhookEventListener(@JiraImport EventPublisher eventPublisher){
        eventPublisher.register(this);
    }

    @EventListener
    public void onIssueEvent(IssueEvent issueEvent){
        String projectKey = issueEvent.getProject().getKey();
        if(ConfigPersistence.isWebhookEnabled(projectKey)){
            Long eventTypeId = issueEvent.getEventTypeId();
            DecisionKnowledgeElement decisionKnowledgeElement = new DecisionKnowledgeElementImpl(issueEvent.getIssue());

            if (eventTypeId.equals(EventType.ISSUE_CREATED_ID)|| eventTypeId.equals(EventType.ISSUE_UPDATED_ID)) {
                WebhookConnector connector = new WebhookConnector(projectKey);
                connector.sendElementChanges(decisionKnowledgeElement);
            }
            if (eventTypeId.equals(EventType.ISSUE_DELETED_ID)){
                WebhookConnector webhookConnector = new WebhookConnector(projectKey);
                webhookConnector.deleteElement(decisionKnowledgeElement, issueEvent.getUser());
            }
        }
    }

    @EventListener
    public void onLinkCreatedIssueEvent(IssueLinkCreatedEvent linkCreatedEvent){
        String projectKey = linkCreatedEvent.getIssueLink().getSourceObject().getProjectObject().getKey();
        DecisionKnowledgeElement decisionKnowledgeElement = new DecisionKnowledgeElementImpl(linkCreatedEvent.getIssueLink().getSourceObject());
        if(ConfigPersistence.isWebhookEnabled(projectKey)){
            WebhookConnector connector = new WebhookConnector(projectKey);
            connector.sendElementChanges(decisionKnowledgeElement);
        }
    }

    @EventListener
    public void onLinkDeletedIssueEvent(IssueLinkDeletedEvent linkDeletedEvent){
        String projectKey = linkDeletedEvent.getIssueLink().getSourceObject().getProjectObject().getKey();
        DecisionKnowledgeElement decisionKnowledgeElement = new DecisionKnowledgeElementImpl(linkDeletedEvent.getIssueLink().getSourceObject());
        if(ConfigPersistence.isWebhookEnabled(projectKey)){
            WebhookConnector connector = new WebhookConnector(projectKey);
            connector.sendElementChanges(decisionKnowledgeElement);
        }
    }
}
