package de.uhd.ifi.se.decision.management.jira.persistence;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.transaction.TransactionCallback;

import de.uhd.ifi.se.decision.management.jira.ComponentGetter;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.model.LinkImpl;
import de.uhd.ifi.se.decision.management.jira.persistence.tables.LinkInDatabase;
import net.java.ao.Query;

public class GenericLinkManager {

	private static final ActiveObjects ACTIVE_OBJECTS = ComponentGetter.getActiveObjects();

	public static void clearInvalidLinks() {
		ACTIVE_OBJECTS.executeInTransaction(new TransactionCallback<LinkInDatabase>() {
			@Override
			public LinkInDatabase doInTransaction() {
				LinkInDatabase[] linkElements = ACTIVE_OBJECTS.find(LinkInDatabase.class);
				for (LinkInDatabase linkElement : linkElements) {
					try {
						Link link = new LinkImpl(linkElement.getIdOfSourceElement(),
								linkElement.getIdOfDestinationElement());
						if (!link.isValid()) {
							deleteLinkElementFromDatabase(linkElement);
						}
					} catch (Exception e) {
						deleteLinkElementFromDatabase(linkElement);
					}
				}
				return null;
			}
		});
	}

	public static boolean deleteLink(Link link) {
		DecisionKnowledgeElement sourceElement = link.getSourceElement();
		String sourceIdWithPrefix = sourceElement.getDocumentationLocation().getIdentifier() + sourceElement.getId();
		DecisionKnowledgeElement destinationElement = link.getDestinationElement();
		String destinationIdWithPrefix = destinationElement.getDocumentationLocation().getIdentifier()
				+ destinationElement.getId();

		return deleteLink(sourceIdWithPrefix, destinationIdWithPrefix);
	}

	public static boolean deleteLink(String sourceIdWithPrefix, String targetIdWithPrefix) {
		return ACTIVE_OBJECTS.executeInTransaction(new TransactionCallback<Boolean>() {
			@Override
			public Boolean doInTransaction() {
				return deleteLinkWithoutTransaction(sourceIdWithPrefix, targetIdWithPrefix);
			}
		});
	}

	private static void deleteLinkElementFromDatabase(LinkInDatabase linkElement) {
		try {
			linkElement.getEntityManager().delete(linkElement);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void deleteLinksForElement(String elementIdWithPrefix) {
		LinkInDatabase[] linksInDatabase = ACTIVE_OBJECTS.find(LinkInDatabase.class);
		for (LinkInDatabase linkInDatabase : linksInDatabase) {
			if (linkInDatabase.getIdOfDestinationElement().equals(elementIdWithPrefix)
					|| linkInDatabase.getIdOfSourceElement().equals(elementIdWithPrefix)) {
				try {
					linkInDatabase.getEntityManager().delete(linkInDatabase);
				} catch (SQLException e) {
				}
			}
		}
	}

	private static Boolean deleteLinkWithoutTransaction(String sourceIdWithPrefix, String targetIdWithPrefix) {
		for (LinkInDatabase linkInDatabase : ACTIVE_OBJECTS.find(LinkInDatabase.class)) {
			if (linkInDatabase.getIdOfDestinationElement().equals(targetIdWithPrefix)
					&& linkInDatabase.getIdOfSourceElement().equals(sourceIdWithPrefix)) {
				try {
					linkInDatabase.getEntityManager().delete(linkInDatabase);
					return true;
				} catch (SQLException e) {
					return false;
				}
			}
		}
		return false;
	}

	public static long getId(String idWithPrefix) {
		return (long) Integer.parseInt(idWithPrefix.substring(1));
	}

	public static DecisionKnowledgeElement getIssueFromAOTable(long dkeId) {
		ActiveObjectPersistenceManager aos = new ActiveObjectPersistenceManager("");
		return aos.getDecisionKnowledgeElement(dkeId);
	}

	public static List<Link> getLinksForElement(DecisionKnowledgeElement element) {
		String elementIdWithPrefix = element.getDocumentationLocationAsString() + element.getId();
		LinkInDatabase[] linksInDatabase = ACTIVE_OBJECTS.find(LinkInDatabase.class, Query.select().where(
				"ID_OF_DESTINATION_ELEMENT = ? OR ID_OF_SOURCE_ELEMENT = ?", elementIdWithPrefix, elementIdWithPrefix));

		List<Link> links = new ArrayList<Link>();
		for (LinkInDatabase linkInDatabase : linksInDatabase) {
			Link link = new LinkImpl(linkInDatabase);
			links.add(link);
		}
		return links;
	}

	/**
	 * Gets all links from an element.
	 *
	 * @param elementIdWithPrefix
	 *            the id of an decision knowledge element with identifier. Example:
	 *            "i1234" for Issue, "s1337" for sentence. "1337" will not work
	 * @return the generic links for element
	 */
	public static List<Link> getLinksForElement(String elementIdWithPrefix) {
		LinkInDatabase[] linksInDatabase = ACTIVE_OBJECTS.find(LinkInDatabase.class, Query.select().where(
				"ID_OF_DESTINATION_ELEMENT = ? OR ID_OF_SOURCE_ELEMENT = ?", elementIdWithPrefix, elementIdWithPrefix));

		List<Link> links = new ArrayList<Link>();
		for (LinkInDatabase linkInDatabase : linksInDatabase) {
			Link link = new LinkImpl(linkInDatabase);
			links.add(link);
		}
		return links;
	}

	public static long insertLink(Link link, ApplicationUser user) {
		if (isLinkAlreadyInDatabase(link) != -1) {
			return isLinkAlreadyInDatabase(link);
		}
		if (!link.isValid()) {
			return -1;
		}

		final LinkInDatabase linkInDatabase = ACTIVE_OBJECTS.create(LinkInDatabase.class);		
		DecisionKnowledgeElement sourceElement = link.getSourceElement();
		String documentationLocationOfSourceElement = sourceElement.getDocumentationLocation().getIdentifier();
		linkInDatabase
				.setIdOfSourceElement(documentationLocationOfSourceElement + sourceElement.getId());
		linkInDatabase.setSourceDocumentationLocation(documentationLocationOfSourceElement);
		
		DecisionKnowledgeElement destinationElement = link.getDestinationElement();
		String documentationLocationOfDestinationElement = destinationElement.getDocumentationLocation().getIdentifier();
		linkInDatabase.setIdOfDestinationElement(
				documentationLocationOfDestinationElement + destinationElement.getId());		
		linkInDatabase.setDestDocumentationLocation(documentationLocationOfDestinationElement);
		linkInDatabase.setType(link.getType());
		linkInDatabase.save();
		ACTIVE_OBJECTS.find(LinkInDatabase.class);
		return linkInDatabase.getId();
	}

	private static long isLinkAlreadyInDatabase(Link link) {
		for (LinkInDatabase linkInDatabase : ACTIVE_OBJECTS.find(LinkInDatabase.class)) {
			// also checks the inverse link
			if (linkInDatabase.getIdOfSourceElement().equals(link.getIdOfSourceElementWithPrefix())
					&& linkInDatabase.getIdOfDestinationElement().equals(link.getIdOfDestinationElementWithPrefix())
					|| linkInDatabase.getIdOfDestinationElement().equals(link.getIdOfSourceElementWithPrefix())
							&& linkInDatabase.getIdOfSourceElement()
									.equals(link.getIdOfDestinationElementWithPrefix())) {
				return linkInDatabase.getId();
			}
		}
		return -1;
	}
}
