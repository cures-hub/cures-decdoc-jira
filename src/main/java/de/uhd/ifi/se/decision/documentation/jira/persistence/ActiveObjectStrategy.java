package de.uhd.ifi.se.decision.documentation.jira.persistence;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.transaction.TransactionCallback;

import de.uhd.ifi.se.decision.documentation.jira.decisionknowledge.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.documentation.jira.decisionknowledge.IDecisionKnowledgeElementEntity;
import de.uhd.ifi.se.decision.documentation.jira.decisionknowledge.ILinkEntity;
import de.uhd.ifi.se.decision.documentation.jira.decisionknowledge.Link;
import de.uhd.ifi.se.decision.documentation.jira.util.ComponentGetter;
import net.java.ao.Query;

/**
 * @description Implements the PersistenceStrategy abstract class. Uses the active
 *              object framework to store decision knowledge.
 */
public class ActiveObjectStrategy extends PersistenceStrategy {
	private static final Logger LOGGER = LoggerFactory.getLogger(ActiveObjectStrategy.class);

	@Override
	public DecisionKnowledgeElement insertDecisionKnowledgeElement(DecisionKnowledgeElement dec, ApplicationUser user) {
		if (dec == null) {
			LOGGER.error("AOStrategy insertDecisionKnowledgeElement the DecisionRepresentation is null");
			return null;
		}
		if (user == null) {
			LOGGER.error("AOStrategy insertDecisionKnowledgeElement the ApplicationUser is null");
			return null;
		}
		final ActiveObjects ao = ComponentGetter.getAo();
		System.out.println(ao);
		IDecisionKnowledgeElementEntity decComponent = ao
				.executeInTransaction(new TransactionCallback<IDecisionKnowledgeElementEntity>() {
					@Override
					public IDecisionKnowledgeElementEntity doInTransaction() {
						IDecisionKnowledgeElementEntity decComponent = ao.create(IDecisionKnowledgeElementEntity.class);
						//decComponent.setKey(dec.getProjectKey().toUpperCase() + "-" + decComponent.getId());
						decComponent.setName(dec.getName());
						decComponent.setDescription(dec.getDescription());
						decComponent.setType(dec.getType());
						decComponent.setProjectKey(dec.getProjectKey());
						decComponent.save();
						return decComponent;
					}
				});
		if (decComponent == null) {
			return null;
		}
		dec.setId(decComponent.getId());
		return dec;
	}

	@Override
	public boolean updateDecisionKnowledgeElement(final DecisionKnowledgeElement dec, ApplicationUser user) {
		final ActiveObjects ao = ComponentGetter.getAo();
		IDecisionKnowledgeElementEntity decComponent = ao
				.executeInTransaction(new TransactionCallback<IDecisionKnowledgeElementEntity>() {
					@Override
					public IDecisionKnowledgeElementEntity doInTransaction() {
						for (IDecisionKnowledgeElementEntity decComponent : ao
								.find(IDecisionKnowledgeElementEntity.class)) {
							if (decComponent.getId() == dec.getId()) {
								decComponent.setDescription(dec.getDescription());
								decComponent.save();
								return decComponent;
							}
						}
						return null;
					}
				});
		if (decComponent != null) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean deleteDecisionKnowledgeElement(final DecisionKnowledgeElement dec, final ApplicationUser user) {
		final ActiveObjects ao = ComponentGetter.getAo();
		return ao.executeInTransaction(new TransactionCallback<Boolean>() {
			@Override
			public Boolean doInTransaction() {
				for (IDecisionKnowledgeElementEntity decComponent : ao.find(IDecisionKnowledgeElementEntity.class)) {
					if (decComponent.getId() == dec.getId()) {
						try {
							decComponent.getEntityManager().delete(decComponent);
						} catch (SQLException e) {
							return false;
						} finally {
							for (ILinkEntity linkEntity : ao.find(ILinkEntity.class)) {
								if (linkEntity.getIngoingId() == dec.getId()
										|| linkEntity.getOutgoingId() == dec.getId()) {
									try {
										linkEntity.getEntityManager().delete(linkEntity);
									} catch (SQLException e) {
										return false;
									}
								}
							}
						}
						return true;
					}
				}
				return false;
			}
		});
	}

	@Override
	public long insertLink(final Link link, ApplicationUser user) {
		final ActiveObjects ao = ComponentGetter.getAo();
		return ao.executeInTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction() {
				boolean linkAlreadyExists = false;
				long linkId = 0;
				for (ILinkEntity linkEntity : ao.find(ILinkEntity.class)) {
					if (linkEntity.getIngoingId() == link.getIngoingId()
							&& linkEntity.getOutgoingId() == link.getOutgoingId()) {
						linkAlreadyExists = true;
						linkId = linkEntity.getId();
					}
				}
				if (!linkAlreadyExists) {
					IDecisionKnowledgeElementEntity decCompIngoing;
					IDecisionKnowledgeElementEntity[] decCompIngoingArray = ao.find(
							IDecisionKnowledgeElementEntity.class, Query.select().where("ID = ?", link.getIngoingId()));
					if (decCompIngoingArray.length == 1) {
						decCompIngoing = decCompIngoingArray[0];
					} else {
						// entity with ingoingId does not exist
						decCompIngoing = null;
					}

					IDecisionKnowledgeElementEntity decCompOutgoing;
					IDecisionKnowledgeElementEntity[] decCompOutgoingArray = ao.find(
							IDecisionKnowledgeElementEntity.class,
							Query.select().where("ID = ?", link.getOutgoingId()));
					if (decCompOutgoingArray.length == 1) {
						decCompOutgoing = decCompOutgoingArray[0];
					} else {
						// entity with outgoingId does not exist
						decCompOutgoing = null;
					}
					if (decCompIngoing != null && decCompOutgoing != null) {
						if (decCompIngoing.getProjectKey().equals(decCompOutgoing.getProjectKey())) {
							// entities exist and are in the same project
							final ILinkEntity linkEntity = ao.create(ILinkEntity.class);
							linkEntity.setIngoingId(link.getIngoingId());
							linkEntity.setOutgoingId(link.getOutgoingId());
							linkEntity.setLinkType(link.getLinkType());
							linkEntity.save();
							linkId = linkEntity.getId();
						} else {
							LOGGER.error("entities to be linked are not in the same project");
							return (long) 0;
						}
					} else {
						LOGGER.error("one of the entities to be linked does not exist");
						return (long) 0;
					}
				} else {
					LOGGER.error("Link already exists");
					return linkId;
				}
				return linkId;
			}
		});
	}

	@Override
	public void deleteLink(Link link, ApplicationUser user) {

	}

	@Override
	public List<Link> getInwardLinks(DecisionKnowledgeElement element) {
		return null;
	}

	@Override
	public List<Link> getOutwardLinks(DecisionKnowledgeElement element) {
		return null;
	}

	@Override
	public List<DecisionKnowledgeElement> getUnlinkedDecisionComponents(final long id, String projectKey) {
		List<DecisionKnowledgeElement> decList = null;
		ProjectManager projectManager = ComponentAccessor.getProjectManager();
		Project project = projectManager.getProjectObjByKey(projectKey);
		if (project != null) {
			final ActiveObjects ao = ComponentGetter.getAo();
			decList = ao.executeInTransaction(new TransactionCallback<List<DecisionKnowledgeElement>>() {
				@Override
				public List<DecisionKnowledgeElement> doInTransaction() {
					final List<DecisionKnowledgeElement> decList = new ArrayList<DecisionKnowledgeElement>();
					IDecisionKnowledgeElementEntity[] decisionsArray = ao.find(IDecisionKnowledgeElementEntity.class,
							Query.select().where("ID = ?", id));
					// id is primaryKey for DecisionComponents therefore there can be 0-1
					// decisioncomponent returned by this query
					IDecisionKnowledgeElementEntity decComponent = null;
					if (decisionsArray.length == 1) {
						decComponent = decisionsArray[0];
					}
					if (decComponent != null) {
						final List<IDecisionKnowledgeElementEntity> linkedDecList = new ArrayList<IDecisionKnowledgeElementEntity>();
						for (ILinkEntity link : ao.find(ILinkEntity.class,
								Query.select().where("INGOING_ID != ? AND OUTGOING_ID = ?", id, id))) {
							for (IDecisionKnowledgeElementEntity decisionComponent : ao.find(
									IDecisionKnowledgeElementEntity.class,
									Query.select().where("ID = ? AND PROJECT_KEY = ?", link.getIngoingId(),
											decComponent.getProjectKey()))) {
								linkedDecList.add(decisionComponent);
							}
						}
						for (ILinkEntity link : ao.find(ILinkEntity.class,
								Query.select().where("INGOING_ID = ? AND OUTGOING_ID != ?", id, id))) {
							for (IDecisionKnowledgeElementEntity decisionComponent : ao.find(
									IDecisionKnowledgeElementEntity.class,
									Query.select().where("ID = ? AND PROJECT_KEY = ?", link.getOutgoingId(),
											decComponent.getProjectKey()))) {
								linkedDecList.add(decisionComponent);
							}
						}
						IDecisionKnowledgeElementEntity[] decisionArray = ao.find(IDecisionKnowledgeElementEntity.class,
								Query.select().where("ID != ? AND PROJECT_KEY = ?", id, decComponent.getProjectKey()));
						for (IDecisionKnowledgeElementEntity decisionComponent : decisionArray) {
							if (!linkedDecList.contains(decisionComponent)) {
								DecisionKnowledgeElement simpleDec = new DecisionKnowledgeElement();
								simpleDec.setId(decisionComponent.getId());
//								simpleDec.setText(decisionComponent.getKey() + " / " + decisionComponent.getName()
//										+ " / " + decisionComponent.getType());
								decList.add(simpleDec);
							}
						}
					}
					return decList;
				}
			});
		}
		return decList;
	}

	@Override
	public DecisionKnowledgeElement getDecisionKnowledgeElement(String key) {
		final ActiveObjects ao = ComponentGetter.getAo();
		IDecisionKnowledgeElementEntity dec = ao
				.executeInTransaction(new TransactionCallback<IDecisionKnowledgeElementEntity>() {
					@Override
					public IDecisionKnowledgeElementEntity doInTransaction() {
						IDecisionKnowledgeElementEntity[] decisionsArray = ao
								.find(IDecisionKnowledgeElementEntity.class, Query.select().where("KEY = ?", key));
						// id is primaryKey for DecisionComponents therefore there can be 0-1
						// decisioncomponent returned by this query
						IDecisionKnowledgeElementEntity decComponent = null;
						if (decisionsArray.length == 1) {
							decComponent = decisionsArray[0];
						}
						return decComponent;
					}
				});
		if (dec != null) {
			DecisionKnowledgeElement decisionKnowledgeElement = new DecisionKnowledgeElement(dec.getId(), dec.getName(),
					dec.getDescription(), dec.getType(), dec.getProjectKey(), dec.getKey(), dec.getSummary());
			return decisionKnowledgeElement;
		}
		return null;
	}

	@Override
	public List<DecisionKnowledgeElement> getChildren(DecisionKnowledgeElement decisionKnowledgeElement) {
			List<Link> inwardLinks = this.getInwardLinks(decisionKnowledgeElement);
			List<Link> outwardLinks= this.getOutwardLinks(decisionKnowledgeElement);
			List<DecisionKnowledgeElement> children = new ArrayList<>();

			for(Link inwardLink:inwardLinks){
				//TODO resurch how to work with the Link class
			}

			for(Link outwardLink:outwardLinks){
				//TODO implementation
			}
			return children;
	}

	@Override
	public List<DecisionKnowledgeElement> getParents(DecisionKnowledgeElement decisionKnowledgeElement) {
		return null;
	}

	private DecisionKnowledgeElement castToDecisionKowledgeElement(IDecisionKnowledgeElementEntity entity) {
		DecisionKnowledgeElement element = new DecisionKnowledgeElement(entity.getId(), entity.getName(),
				entity.getDescription(), entity.getType(), entity.getProjectKey(), entity.getKey(),
				entity.getSummary());
		return element;
	}

	@Override
	public List<DecisionKnowledgeElement> getDecisionKnowledgeElements(String projectKey) {
		// TODO Auto-generated method stub
		return null;
	}
}