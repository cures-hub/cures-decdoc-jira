package de.uhd.ifi.se.decision.management.jira.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.transaction.TransactionCallback;

import de.uhd.ifi.se.decision.management.jira.ComponentGetter;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElementImpl;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.model.LinkImpl;
import de.uhd.ifi.se.decision.management.jira.webhook.WebhookConnector;
import net.java.ao.Query;

/**
 * Extends the abstract class AbstractPersistenceStrategy. Uses object
 * relational mapping with the help of the active object framework to store
 * decision knowledge.
 *
 * @see AbstractPersistenceManager
 */
@JsonAutoDetect
public class ActiveObjectPersistenceManager extends AbstractPersistenceManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(ActiveObjectPersistenceManager.class);
	private static final ActiveObjects ACTIVE_OBJECTS = ComponentGetter.getActiveObjects();
	private static final String PREFIX = DocumentationLocation.getIdentifier(DocumentationLocation.ACTIVEOBJECT);

	private static DecisionKnowledgeElementInDatabase setParameters(DecisionKnowledgeElement element,
			DecisionKnowledgeElementInDatabase databaseEntry) {
		String summary = element.getSummary();
		if (summary != null) {
			databaseEntry.setSummary(summary);
		}
		String description = element.getDescription();
		if (description != null) {
			databaseEntry.setSummary(description);
		}
		databaseEntry.setType(element.getType().replaceProAndConWithArgument().toString());
		return databaseEntry;
	}

	public ActiveObjectPersistenceManager(String projectKey) {
		this.projectKey = projectKey;
		this.documentationLocation = DocumentationLocation.ACTIVEOBJECT;
	}

	@Override
	public boolean deleteDecisionKnowledgeElement(DecisionKnowledgeElement decisionKnowledgeElement,
			ApplicationUser user) {
		if (decisionKnowledgeElement == null) {
			return false;
		}
		new WebhookConnector(projectKey).sendElementChanges(decisionKnowledgeElement);
		return deleteDecisionKnowledgeElement(decisionKnowledgeElement.getId(), user);
	}

	@Override
	public boolean deleteDecisionKnowledgeElement(long id, ApplicationUser user) {
		boolean isDeleted = false;
		for (DecisionKnowledgeElementInDatabase databaseEntry : ACTIVE_OBJECTS
				.find(DecisionKnowledgeElementInDatabase.class, Query.select().where("ID = ?", id))) {
			GenericLinkManager.deleteLinksForElement(PREFIX + id);
			isDeleted = DecisionKnowledgeElementInDatabase.deleteElement(databaseEntry);
		}
		return isDeleted;
	}

	@Override
	public DecisionKnowledgeElement getDecisionKnowledgeElement(long id) {
		DecisionKnowledgeElementInDatabase decisionKnowledgeElement = ACTIVE_OBJECTS
				.executeInTransaction(new TransactionCallback<DecisionKnowledgeElementInDatabase>() {
					@Override
					public DecisionKnowledgeElementInDatabase doInTransaction() {
						DecisionKnowledgeElementInDatabase[] decisionKnowledgeElement = ACTIVE_OBJECTS
								.find(DecisionKnowledgeElementInDatabase.class, Query.select().where("ID = ?", id));
						// 0 or 1 decision knowledge elements might be returned by this query
						if (decisionKnowledgeElement.length == 1) {
							return decisionKnowledgeElement[0];
						}
						return null;
					}
				});
		if (decisionKnowledgeElement != null) {
			return new DecisionKnowledgeElementImpl(decisionKnowledgeElement);
		}
		return null;
	}

	@Override
	public DecisionKnowledgeElement getDecisionKnowledgeElement(String key) {
		// Split key into project key and id
		String idAsString = null;
		try {
			idAsString = key.split("-")[1];
		} catch (ArrayIndexOutOfBoundsException e) {
			LOGGER.error("Key cannot be split into the project key and id.");
		}
		if (idAsString != null) {
			long id = Long.parseLong(idAsString);
			DecisionKnowledgeElement element = getDecisionKnowledgeElement(id);
			if (element != null) {
				return element;
			}
		}
		LOGGER.error("No decision knowledge element with " + key + " could be found.");
		return null;
	}

	@Override
	public List<DecisionKnowledgeElement> getDecisionKnowledgeElements() {
		List<DecisionKnowledgeElement> decisionKnowledgeElements = null;
		if (this.projectKey != null) {
			decisionKnowledgeElements = ACTIVE_OBJECTS
					.executeInTransaction(new TransactionCallback<List<DecisionKnowledgeElement>>() {
						@Override
						public List<DecisionKnowledgeElement> doInTransaction() {
							final List<DecisionKnowledgeElement> decisionKnowledgeElements = new ArrayList<DecisionKnowledgeElement>();
							DecisionKnowledgeElementInDatabase[] decisionArray = ACTIVE_OBJECTS.find(
									DecisionKnowledgeElementInDatabase.class,
									Query.select().where("PROJECT_KEY = ?", projectKey));
							for (DecisionKnowledgeElementInDatabase entity : decisionArray) {
								decisionKnowledgeElements.add(new DecisionKnowledgeElementImpl(entity));
							}
							return decisionKnowledgeElements;
						}
					});
		}
		return decisionKnowledgeElements;
	}

	@Override
	public List<DecisionKnowledgeElement> getElementsLinkedWithInwardLinks(
			DecisionKnowledgeElement decisionKnowledgeElement) {
		List<Link> inwardLinks = this.getInwardLinks(decisionKnowledgeElement);
		List<DecisionKnowledgeElement> sourceElements = new ArrayList<DecisionKnowledgeElement>();
		for (Link link : inwardLinks) {
			sourceElements.add(new DecisionKnowledgeElementImpl(
					ACTIVE_OBJECTS.executeInTransaction(new TransactionCallback<DecisionKnowledgeElementInDatabase>() {
						@Override
						public DecisionKnowledgeElementInDatabase doInTransaction() {
							DecisionKnowledgeElementInDatabase[] entityList = ACTIVE_OBJECTS.find(
									DecisionKnowledgeElementInDatabase.class,
									Query.select().where("ID = ?", link.getSourceElement().getId()));
							if (entityList.length == 1) {
								return entityList[0];
							}
							LOGGER.error("Inward link has no element to return.");
							return null;
						}
					})));
		}
		return sourceElements;
	}

	@Override
	public List<DecisionKnowledgeElement> getElementsLinkedWithOutwardLinks(
			DecisionKnowledgeElement decisionKnowledgeElement) {
		List<Link> outwardLinks = this.getOutwardLinks(decisionKnowledgeElement);
		List<DecisionKnowledgeElement> destinationElements = new ArrayList<DecisionKnowledgeElement>();

		ACTIVE_OBJECTS.find(LinkInDatabase.class);
		for (Link link : outwardLinks) {
			destinationElements.add(new DecisionKnowledgeElementImpl(
					ACTIVE_OBJECTS.executeInTransaction(new TransactionCallback<DecisionKnowledgeElementInDatabase>() {
						@Override
						public DecisionKnowledgeElementInDatabase doInTransaction() {
							DecisionKnowledgeElementInDatabase[] entityList = ACTIVE_OBJECTS.find(
									DecisionKnowledgeElementInDatabase.class,
									Query.select().where("ID = ?", link.getDestinationElement().getId()));
							if (entityList.length == 1) {
								return entityList[0];
							}
							LOGGER.error("Outward link has no element to return.");
							return null;
						}
					})));
		}
		return destinationElements;
	}

	@Override
	public List<Link> getInwardLinks(DecisionKnowledgeElement element) {
		List<Link> inwardLinks = new ArrayList<Link>();
		LinkInDatabase[] links = ACTIVE_OBJECTS.find(LinkInDatabase.class,
				Query.select().where("ID_OF_DESTINATION_ELEMENT = ?", PREFIX + element.getId()));
		for (LinkInDatabase link : links) {
			Link inwardLink = new LinkImpl(link);
			inwardLink.setDestinationElement(element);
			long elementId = (long) Integer.parseInt(link.getIdOfSourceElement().substring(1));
			inwardLink.setSourceElement(this.getDecisionKnowledgeElement(elementId));
			inwardLinks.add(inwardLink);
		}
		return inwardLinks;
	}

	@Override
	public List<Link> getOutwardLinks(DecisionKnowledgeElement element) {
		List<Link> outwardLinks = new ArrayList<Link>();
		LinkInDatabase[] links = ACTIVE_OBJECTS.find(LinkInDatabase.class,
				Query.select().where("ID_OF_SOURCE_ELEMENT = ?", PREFIX + element.getId()));
		for (LinkInDatabase link : links) {
			Link outwardLink = new LinkImpl(link);
			outwardLink.setSourceElement(element);
			long elementId = (long) Integer.parseInt(link.getIdOfDestinationElement().substring(1));
			outwardLink.setDestinationElement(this.getDecisionKnowledgeElement(elementId));
			outwardLinks.add(outwardLink);
		}
		return outwardLinks;
	}
	
	@Override
	public DecisionKnowledgeElement insertDecisionKnowledgeElement(DecisionKnowledgeElement element,
			ApplicationUser user) {
		DecisionKnowledgeElementInDatabase databaseEntry = ACTIVE_OBJECTS
				.executeInTransaction(new TransactionCallback<DecisionKnowledgeElementInDatabase>() {
					@Override
					public DecisionKnowledgeElementInDatabase doInTransaction() {
						DecisionKnowledgeElementInDatabase databaseEntry = ACTIVE_OBJECTS
								.create(DecisionKnowledgeElementInDatabase.class);
						databaseEntry.setKey(element.getProject().getProjectKey().toUpperCase(Locale.ENGLISH) + "-"
								+ databaseEntry.getId());
						databaseEntry = setParameters(element, databaseEntry);
						databaseEntry.setProjectKey(element.getProject().getProjectKey());
						databaseEntry.save();
						return databaseEntry;
					}
				});
		if (databaseEntry == null) {
			LOGGER.error("Insertion of decision knowledge element into database failed.");
			return null;
		}
		element.setId(databaseEntry.getId());
		element.setKey(databaseEntry.getKey());
		new WebhookConnector(projectKey).sendElementChanges(element);
		element.setDocumentationLocation(DocumentationLocation.ACTIVEOBJECT);
		return element;
	}

	@Override
	public boolean updateDecisionKnowledgeElement(DecisionKnowledgeElement element, ApplicationUser user) {
		DecisionKnowledgeElementInDatabase databaseEntry = ACTIVE_OBJECTS
				.executeInTransaction(new TransactionCallback<DecisionKnowledgeElementInDatabase>() {
					@Override
					public DecisionKnowledgeElementInDatabase doInTransaction() {
						for (DecisionKnowledgeElementInDatabase databaseEntry : ACTIVE_OBJECTS
								.find(DecisionKnowledgeElementInDatabase.class)) {
							if (databaseEntry.getId() == element.getId()) {
								databaseEntry = setParameters(element, databaseEntry);
								databaseEntry.save();
								return databaseEntry;
							}
						}
						return null;
					}
				});
		if (databaseEntry == null) {
			LOGGER.error("Updating of decision knowledge element in database failed.");
			return false;
		}
		new WebhookConnector(projectKey).sendElementChanges(element);
		return true;
	}
}