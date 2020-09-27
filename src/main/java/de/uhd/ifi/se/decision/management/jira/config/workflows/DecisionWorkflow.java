package de.uhd.ifi.se.decision.management.jira.config.workflows;

public class DecisionWorkflow {

	public static String getXMLDescriptor() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
				+ "<!DOCTYPE workflow PUBLIC \"-//OpenSymphony Group//DTD OSWorkflow 2.8//EN\" \"http://www.opensymphony.com/osworkflow/workflow_2_8.dtd\">\r\n"
				+ "<workflow>\r\n"
				+ "  <meta name=\"jira.description\">Workflow for decisions (solutions to decision problems)</meta>\r\n"
				+ "  <initial-actions>\r\n"
				+ "    <action id=\"1\" name=\"Create\">\r\n"
				+ "      <validators>\r\n"
				+ "        <validator name=\"\" type=\"class\">\r\n"
				+ "          <arg name=\"permission\">Create Issue</arg>\r\n"
				+ "          <arg name=\"class.name\">com.atlassian.jira.workflow.validator.PermissionValidator</arg>\r\n"
				+ "        </validator>\r\n"
				+ "      </validators>\r\n"
				+ "      <results>\r\n"
				+ "        <unconditional-result old-status=\"null\" status=\"open\" step=\"2\">\r\n"
				+ "          <post-functions>\r\n"
				+ "            <function type=\"class\">\r\n"
				+ "              <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.IssueCreateFunction</arg>\r\n"
				+ "            </function>\r\n"
				+ "            <function type=\"class\">\r\n"
				+ "              <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.IssueReindexFunction</arg>\r\n"
				+ "            </function>\r\n"
				+ "            <function type=\"class\">\r\n"
				+ "              <arg name=\"eventTypeId\">1</arg>\r\n"
				+ "              <arg name=\"class.name\">com.atlassian.jira.workflow.function.event.FireIssueEventFunction</arg>\r\n"
				+ "            </function>\r\n"
				+ "          </post-functions>\r\n"
				+ "        </unconditional-result>\r\n"
				+ "      </results>\r\n"
				+ "    </action>\r\n"
				+ "  </initial-actions>\r\n"
				+ "  <common-actions>\r\n"
				+ "    <action id=\"41\" name=\"Set decided\">\r\n"
				+ "      <meta name=\"jira.description\"></meta>\r\n"
				+ "      <meta name=\"jira.fieldscreen.id\"></meta>\r\n"
				+ "      <results>\r\n"
				+ "        <unconditional-result old-status=\"null\" status=\"null\" step=\"2\">\r\n"
				+ "          <post-functions>\r\n"
				+ "            <function type=\"class\">\r\n"
				+ "              <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.UpdateIssueStatusFunction</arg>\r\n"
				+ "            </function>\r\n"
				+ "            <function type=\"class\">\r\n"
				+ "              <arg name=\"class.name\">com.atlassian.jira.workflow.function.misc.CreateCommentFunction</arg>\r\n"
				+ "            </function>\r\n"
				+ "            <function type=\"class\">\r\n"
				+ "              <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.GenerateChangeHistoryFunction</arg>\r\n"
				+ "            </function>\r\n"
				+ "            <function type=\"class\">\r\n"
				+ "              <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.IssueReindexFunction</arg>\r\n"
				+ "            </function>\r\n"
				+ "            <function type=\"class\">\r\n"
				+ "              <arg name=\"eventTypeId\">13</arg>\r\n"
				+ "              <arg name=\"class.name\">com.atlassian.jira.workflow.function.event.FireIssueEventFunction</arg>\r\n"
				+ "            </function>\r\n"
				+ "          </post-functions>\r\n"
				+ "        </unconditional-result>\r\n"
				+ "      </results>\r\n"
				+ "    </action>\r\n"
				+ "    <action id=\"11\" name=\"Set challenged\">\r\n"
				+ "      <meta name=\"jira.description\"></meta>\r\n"
				+ "      <meta name=\"jira.fieldscreen.id\"></meta>\r\n"
				+ "      <results>\r\n"
				+ "        <unconditional-result old-status=\"null\" status=\"null\" step=\"3\">\r\n"
				+ "          <post-functions>\r\n"
				+ "            <function type=\"class\">\r\n"
				+ "              <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.UpdateIssueStatusFunction</arg>\r\n"
				+ "            </function>\r\n"
				+ "            <function type=\"class\">\r\n"
				+ "              <arg name=\"class.name\">com.atlassian.jira.workflow.function.misc.CreateCommentFunction</arg>\r\n"
				+ "            </function>\r\n"
				+ "            <function type=\"class\">\r\n"
				+ "              <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.GenerateChangeHistoryFunction</arg>\r\n"
				+ "            </function>\r\n"
				+ "            <function type=\"class\">\r\n"
				+ "              <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.IssueReindexFunction</arg>\r\n"
				+ "            </function>\r\n"
				+ "            <function type=\"class\">\r\n"
				+ "              <arg name=\"eventTypeId\">13</arg>\r\n"
				+ "              <arg name=\"class.name\">com.atlassian.jira.workflow.function.event.FireIssueEventFunction</arg>\r\n"
				+ "            </function>\r\n"
				+ "          </post-functions>\r\n"
				+ "        </unconditional-result>\r\n"
				+ "      </results>\r\n"
				+ "    </action>\r\n"
				+ "  </common-actions>\r\n"
				+ "  <steps>\r\n"
				+ "    <step id=\"2\" name=\"Decided\">\r\n"
				+ "      <meta name=\"jira.status.id\">" + WorkflowXMLDescriptorProvider.getStatusId("Decided") + "</meta>\r\n"
				+ "      <actions>\r\n"
				+ "<common-action id=\"11\" />\r\n"
				+ "        <action id=\"31\" name=\"Set rejected\">\r\n"
				+ "          <meta name=\"jira.description\"></meta>\r\n"
				+ "          <meta name=\"jira.fieldscreen.id\"></meta>\r\n"
				+ "          <results>\r\n"
				+ "            <unconditional-result old-status=\"null\" status=\"null\" step=\"4\">\r\n"
				+ "              <post-functions>\r\n"
				+ "                <function type=\"class\">\r\n"
				+ "                  <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.UpdateIssueStatusFunction</arg>\r\n"
				+ "                </function>\r\n"
				+ "                <function type=\"class\">\r\n"
				+ "                  <arg name=\"class.name\">com.atlassian.jira.workflow.function.misc.CreateCommentFunction</arg>\r\n"
				+ "                </function>\r\n"
				+ "                <function type=\"class\">\r\n"
				+ "                  <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.GenerateChangeHistoryFunction</arg>\r\n"
				+ "                </function>\r\n"
				+ "                <function type=\"class\">\r\n"
				+ "                  <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.IssueReindexFunction</arg>\r\n"
				+ "                </function>\r\n"
				+ "                <function type=\"class\">\r\n"
				+ "                  <arg name=\"eventTypeId\">13</arg>\r\n"
				+ "                  <arg name=\"class.name\">com.atlassian.jira.workflow.function.event.FireIssueEventFunction</arg>\r\n"
				+ "                </function>\r\n"
				+ "              </post-functions>\r\n"
				+ "            </unconditional-result>\r\n"
				+ "          </results>\r\n"
				+ "        </action>\r\n"
				+ "      </actions>\r\n"
				+ "    </step>\r\n"
				+ "    <step id=\"3\" name=\"Challenged\">\r\n"
				+ "      <meta name=\"jira.status.id\">" + WorkflowXMLDescriptorProvider.getStatusId("Challenged") + "</meta>\r\n"
				+ "      <actions>\r\n"
				+ "<common-action id=\"41\" />\r\n"
				+ "        <action id=\"21\" name=\"Set rejected\">\r\n"
				+ "          <meta name=\"jira.description\"></meta>\r\n"
				+ "          <meta name=\"jira.fieldscreen.id\"></meta>\r\n"
				+ "          <results>\r\n"
				+ "            <unconditional-result old-status=\"null\" status=\"null\" step=\"4\">\r\n"
				+ "              <post-functions>\r\n"
				+ "                <function type=\"class\">\r\n"
				+ "                  <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.UpdateIssueStatusFunction</arg>\r\n"
				+ "                </function>\r\n"
				+ "                <function type=\"class\">\r\n"
				+ "                  <arg name=\"class.name\">com.atlassian.jira.workflow.function.misc.CreateCommentFunction</arg>\r\n"
				+ "                </function>\r\n"
				+ "                <function type=\"class\">\r\n"
				+ "                  <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.GenerateChangeHistoryFunction</arg>\r\n"
				+ "                </function>\r\n"
				+ "                <function type=\"class\">\r\n"
				+ "                  <arg name=\"class.name\">com.atlassian.jira.workflow.function.issue.IssueReindexFunction</arg>\r\n"
				+ "                </function>\r\n"
				+ "                <function type=\"class\">\r\n"
				+ "                  <arg name=\"eventTypeId\">13</arg>\r\n"
				+ "                  <arg name=\"class.name\">com.atlassian.jira.workflow.function.event.FireIssueEventFunction</arg>\r\n"
				+ "                </function>\r\n"
				+ "              </post-functions>\r\n"
				+ "            </unconditional-result>\r\n"
				+ "          </results>\r\n"
				+ "        </action>\r\n"
				+ "      </actions>\r\n"
				+ "    </step>\r\n"
				+ "    <step id=\"4\" name=\"Rejected\">\r\n"
				+ "      <meta name=\"jira.status.id\">" + WorkflowXMLDescriptorProvider.getStatusId("Rejected") + "</meta>\r\n"
				+ "      <actions>\r\n"
				+ "<common-action id=\"11\" />\r\n"
				+ "<common-action id=\"41\" />\r\n"
				+ "      </actions>\r\n"
				+ "    </step>\r\n"
				+ "  </steps>\r\n"
				+ "</workflow>";
	}
}
