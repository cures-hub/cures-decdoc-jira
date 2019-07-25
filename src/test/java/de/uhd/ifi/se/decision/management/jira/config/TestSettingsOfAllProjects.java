package de.uhd.ifi.se.decision.management.jira.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.atlassian.jira.mock.MockProjectManager;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.mock.servlet.MockHttpServletResponse;
import com.atlassian.jira.project.MockProject;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.templaterenderer.TemplateRenderer;

import de.uhd.ifi.se.decision.management.jira.TestSetUpWithIssues;
import de.uhd.ifi.se.decision.management.jira.mocks.MockTemplateRenderer;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeProject;

public class TestSettingsOfAllProjects {

	private ProjectManager projectManager;
	private SettingsOfAllProjects servlet;
	private HttpServletRequest request;
	private HttpServletResponse response;

	@Before
	public void setUp() {
		TestSetUpWithIssues.initialization();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		TemplateRenderer renderer = new MockTemplateRenderer();
		servlet = new SettingsOfAllProjects(renderer);

		projectManager = new MockProjectManager();
		new MockComponentWorker().init().addMock(ProjectManager.class, projectManager);
	}

	@Test
	public void testDoGetNullNull() throws IOException {
		servlet.doGet(null, null);
	}

	@Test
	public void testDoGetNullFilled() throws IOException {
		servlet.doGet(null, response);
	}

	@Test
	public void testDoGetFilledNull() throws IOException {
		servlet.doGet(request, null);
	}

	@Test
	public void testDoGetNoSysFilled() throws IOException {
		request.setAttribute("NoSysAdmin", true);
		request.setAttribute("SysAdmin", false);
		servlet.doGet(request, response);
	}

	@Test
	public void testDoGetSysFilled() throws IOException {
		request.setAttribute("NoSysAdmin", false);
		request.setAttribute("SysAdmin", true);
		servlet.doGet(request, response);
	}

	@Test
	public void testRequestNullResponseNull() throws IOException, ServletException {
		assertFalse(servlet.isValidParameters(null, null));
	}

	@Test
	public void testRequestFilledResponseNull() throws IOException, ServletException {
		request.setAttribute("NoSysAdmin", false);
		request.setAttribute("SysAdmin", false);
		assertFalse(servlet.isValidUser(request));
	}

	@Test
	public void testRequestNullResponseFilled() throws IOException, ServletException {
		assertFalse(servlet.isValidParameters(null, response));
	}

	@Test
	public void testRequestFilledResponseFilled() throws IOException, ServletException {
		request.setAttribute("NoSysAdmin", false);
		request.setAttribute("SysAdmin", false);
		assertFalse(servlet.isValidUser(request));
	}

	@Test
	public void testNoUserManager() throws IOException, ServletException {
		((MockHttpServletRequest) request).setQueryString("Test");
		request.setAttribute("NoSysAdmin", true);
		assertFalse(servlet.isValidUser(request));
	}

	@Test
	public void testNoUserManagerQueryNull() throws IOException, ServletException {
		((MockHttpServletRequest) request).setQueryString(null);
		request.setAttribute("NoSysAdmin", true);
		assertFalse(servlet.isValidUser(request));
	}

	@Test
	public void testUserManager() throws IOException, ServletException {
		request.setAttribute("NoSysAdmin", false);
		request.setAttribute("SysAdmin", true);
		assertTrue(servlet.isValidUser(request));
	}

	@Test
	public void testGetProjectMapNoProject() {
		Map<String, DecisionKnowledgeProject> map = new ConcurrentHashMap<String, DecisionKnowledgeProject>();
		assertEquals(map, SettingsOfAllProjects.getProjectsMap());
	}

	@Test
	public void testGetProjectMapProjects() {
		Project project = new MockProject(1, "TEST");
		((MockProject) project).setKey("TEST");
		((MockProjectManager) projectManager).addProject(project);
		assertTrue(SettingsOfAllProjects.getProjectsMap().size() >= 0);
	}

	@Test
	public void testGetTemplatePath() {
		assertEquals("templates/settingsForAllProjects.vm", servlet.getTemplatePath());
	}

	@Test
	public void testGetVerlocityParametersNull() {
		assertEquals(0, servlet.getVelocityParameters(null).size());
	}

	@Test
	public void testGetVelocityParametersFilled() {
		Project project = new MockProject(1, "TEST");
		((MockProject) project).setKey("TEST");
		((MockProjectManager) projectManager).addProject(project);
		assertEquals(2, servlet.getVelocityParameters(request).size());
	}
}