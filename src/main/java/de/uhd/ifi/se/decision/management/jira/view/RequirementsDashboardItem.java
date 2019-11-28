package de.uhd.ifi.se.decision.management.jira.view;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import com.google.common.collect.Maps;


import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class RequirementsDashboardItem  implements ContextProvider {

    @Override
    public void init(final Map<String, String> params) throws PluginParseException {
        /**
         * No special behaviour is foreseen for now.
         */
    }

    @Override
    public Map<String, Object> getContextMap(final Map<String, Object> context) {
        final Map<String, Object> newContext = Maps.newHashMap(context);

        Map<String, Object> projectContext = attachProjectsMaps();
        newContext.putAll(projectContext);

        SecureRandom random = new SecureRandom();
        String uid = String.valueOf(random.nextInt(10000));
        String selectId = "condec-dashboard-item-project-selection"+uid;
        newContext.put("selectID", selectId);
        newContext.put("dashboardUID", uid);

        return newContext;
    }

    private Map<String, Object> attachProjectsMaps() {
        Map<String, Object> newContext = new HashMap<>();
        Map<String, String> projectNameMap = new TreeMap<String, String>();
        for (Project project : ComponentAccessor.getProjectManager().getProjects()) {
            String projectKey = project.getKey();
            String projectName = project.getName();
            projectNameMap.put(projectKey,projectName);
        }
        newContext.put("projectNamesMap", projectNameMap);
        return newContext;
    }
}

