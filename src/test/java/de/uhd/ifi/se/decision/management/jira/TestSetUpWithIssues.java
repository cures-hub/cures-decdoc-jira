package de.uhd.ifi.se.decision.management.jira;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.runner.RunWith;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.avatar.AvatarManager;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.option.OptionSetManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.issuetype.MockIssueType;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.mock.MockConstantsManager;
import com.atlassian.jira.mock.MockProjectManager;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.mock.issue.MockIssue;
import com.atlassian.jira.project.MockProject;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.MockApplicationUser;
import com.atlassian.jira.user.util.MockUserManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.VelocityParamFactory;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.velocity.VelocityManager;

import de.uhd.ifi.se.decision.management.jira.extraction.TestTextSplitter;
import de.uhd.ifi.se.decision.management.jira.mocks.MockAvatarManager;
import de.uhd.ifi.se.decision.management.jira.mocks.MockCommentManager;
import de.uhd.ifi.se.decision.management.jira.mocks.MockDatabase;
import de.uhd.ifi.se.decision.management.jira.mocks.MockIssueLinkManager;
import de.uhd.ifi.se.decision.management.jira.mocks.MockIssueLinkTypeManager;
import de.uhd.ifi.se.decision.management.jira.mocks.MockIssueManagerSelfImpl;
import de.uhd.ifi.se.decision.management.jira.mocks.MockIssueService;
import de.uhd.ifi.se.decision.management.jira.mocks.MockIssueTypeManager;
import de.uhd.ifi.se.decision.management.jira.mocks.MockJiraHomeForTesting;
import de.uhd.ifi.se.decision.management.jira.mocks.MockPluginSettingsFactory;
import de.uhd.ifi.se.decision.management.jira.mocks.MockProjectRoleManager;
import de.uhd.ifi.se.decision.management.jira.mocks.MockSearchService;
import de.uhd.ifi.se.decision.management.jira.mocks.MockTransactionTemplate;
import de.uhd.ifi.se.decision.management.jira.mocks.MockVelocityManager;
import de.uhd.ifi.se.decision.management.jira.mocks.MockVelocityParamFactory;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.text.PartOfJiraIssueText;
import de.uhd.ifi.se.decision.management.jira.persistence.JiraIssueTextPersistenceManager;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(MockDatabase.class)
public abstract class TestSetUpWithIssues {
	private static ProjectManager projectManager;
	private static IssueManager issueManager;
	private static ConstantsManager constantsManager;
	protected static MockIssue issue;
	private static ApplicationUser user;
	protected static EntityManager entityManager;

	public static void initialization() {
		initComponentAccessor();
		createProjectIssueStructure();
		initComponentGetter();
	}

	public static void initComponentAccessor() {
		projectManager = new MockProjectManager();
		issueManager = new MockIssueManagerSelfImpl();
		constantsManager = new MockConstantsManager();

		UserManager userManager = initUserManager();

		new MockComponentWorker().init().addMock(IssueManager.class, issueManager)
				.addMock(IssueLinkManager.class, new MockIssueLinkManager())
				.addMock(IssueLinkTypeManager.class, new MockIssueLinkTypeManager())
				.addMock(IssueService.class, new MockIssueService()).addMock(ProjectManager.class, projectManager)
				.addMock(UserManager.class, userManager).addMock(ConstantsManager.class, constantsManager)
				.addMock(ProjectRoleManager.class, new MockProjectRoleManager())
				.addMock(VelocityManager.class, new MockVelocityManager())
				.addMock(VelocityParamFactory.class, new MockVelocityParamFactory())
				.addMock(AvatarManager.class, new MockAvatarManager())
				.addMock(IssueTypeManager.class, new MockIssueTypeManager())
				.addMock(IssueTypeSchemeManager.class, mock(IssueTypeSchemeManager.class))
				.addMock(FieldConfigScheme.class, mock(FieldConfigScheme.class))
				.addMock(PluginSettingsFactory.class, new MockPluginSettingsFactory())
				.addMock(OptionSetManager.class, mock(OptionSetManager.class))
				.addMock(CommentManager.class, new MockCommentManager())
				.addMock(JiraHome.class, new MockJiraHomeForTesting())
				.addMock(SearchService.class, new MockSearchService())
				.addMock(TransactionTemplate.class, new MockTransactionTemplate())
				.addMock(PluginSettingsFactory.class, mock(PluginSettingsFactory.class));
	}

	public static UserManager initUserManager() {
		UserManager userManager = new MockUserManager();
		user = new MockApplicationUser("NoFails");
		ApplicationUser user2 = new MockApplicationUser("WithFails");
		ApplicationUser user3 = new MockApplicationUser("NoSysAdmin");
		ApplicationUser user4 = new MockApplicationUser("SysAdmin");
		((MockUserManager) userManager).addUser(user);
		((MockUserManager) userManager).addUser(user2);
		((MockUserManager) userManager).addUser(user3);
		((MockUserManager) userManager).addUser(user4);
		return userManager;
	}

	public static void initComponentGetter() {
		ActiveObjects activeObjects = new TestActiveObjects(entityManager);
		initComponentGetter(activeObjects, new MockTransactionTemplate(),
				new de.uhd.ifi.se.decision.management.jira.mocks.MockUserManager());
	}

	public static void initComponentGetter(ActiveObjects activeObjects, TransactionTemplate transactionTemplate,
			de.uhd.ifi.se.decision.management.jira.mocks.MockUserManager mockUserManager) {
		new ComponentGetter(new MockPluginSettingsFactory(), transactionTemplate, null, null, new MockSearchService(),
				mockUserManager, null, activeObjects);
	}

	private static void createProjectIssueStructure() {
		Project project = new MockProject(1, "TEST");
		((MockProject) project).setKey("TEST");
		((MockProjectManager) projectManager).addProject(project);

		List<IssueType> jiraIssueTypes = createJiraIssueTypesForDecisionKnowledgeTypes();

		List<KnowledgeType> types = Arrays.asList(KnowledgeType.values());
		addJiraIssue(30, "TEST-" + 30, jiraIssueTypes.get(13), project);

		for (int i = 2; i < jiraIssueTypes.size() + 2; i++) {
			issue = addJiraIssue(i, "TEST-" + i, jiraIssueTypes.get(i - 2), project);
			if (i > types.size() - 4) {
				issue.setParentId((long) 3);
			}
		}
		IssueType issueType = new MockIssueType(50, "Class");
		issue = addJiraIssue(50, "TEST-" + 50, issueType, project);
		issue.setParentId((long) 3);

		Project condecProject = new MockProject(3, "CONDEC");
		((MockProject) condecProject).setKey("CONDEC");
		((MockProjectManager) projectManager).addProject(condecProject);
		addJiraIssue(1234, "CONDEC-" + 1234, jiraIssueTypes.get(2), condecProject);
	}

	private static List<IssueType> createJiraIssueTypesForDecisionKnowledgeTypes() {
		List<IssueType> jiraIssueTypes = new ArrayList<IssueType>();
		int i = 0;
		for (KnowledgeType type : KnowledgeType.values()) {
			IssueType issueType = new MockIssueType(i, type.name().toLowerCase(Locale.ENGLISH));
			((MockConstantsManager) constantsManager).addIssueType(issueType);
			jiraIssueTypes.add(issueType);
			i++;
		}
		return jiraIssueTypes;
	}

	private static MockIssue addJiraIssue(int id, String key, IssueType issueType, Project project) {
		MutableIssue issue = new MockIssue(id, key);
		((MockIssue) issue).setProjectId(project.getId());
		issue.setProjectObject(project);
		issue.setIssueType(issueType);
		issue.setSummary("Test");
		issue.setDescription("Test");
		((MockIssueManagerSelfImpl) issueManager).addIssue(issue);
		return (MockIssue) issue;
	}

	public MockIssue createGlobalIssue() {
		if (issue != null) {
			return issue;
		}
		Project project = ComponentAccessor.getProjectManager().getProjectByCurrentKey("TEST");
		issue = new MockIssue(30, "TEST-" + 30);
		((MockIssue) issue).setProjectId(project.getId());
		issue.setProjectObject(project);
		IssueType issueType = new MockIssueType(1, KnowledgeType.DECISION.toString().toLowerCase(Locale.ENGLISH));
		issue.setIssueType(issueType);
		issue.setSummary("Test");
		return issue;
	}

	public static MockIssue createGlobalIssueWithComment() {
		List<PartOfJiraIssueText> comment = TestTextSplitter.getSentencesForCommentText("{issue} testobject {issue}");
		PartOfJiraIssueText sentence = comment.get(0);
		sentence.setJiraIssueId(createIssue().getId());
		JiraIssueTextPersistenceManager.insertDecisionKnowledgeElement(sentence, new MockApplicationUser("NoFails"));

		return (MockIssue) sentence.getJiraIssue();
	}

	public static MockIssue createIssue() {
		Project project = ComponentAccessor.getProjectManager().getProjectByCurrentKey("TEST");
		MockIssue issue = new MockIssue(30, "TEST-" + 30);
		((MockIssue) issue).setProjectId(project.getId());
		issue.setProjectObject(project);
		IssueType issueType = new MockIssueType(1, KnowledgeType.DECISION.toString().toLowerCase(Locale.ENGLISH));
		issue.setIssueType(issueType);
		issue.setSummary("Test");
		return issue;
	}
}