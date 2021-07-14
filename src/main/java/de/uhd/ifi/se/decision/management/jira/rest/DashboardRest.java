package de.uhd.ifi.se.decision.management.jira.rest;


import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.management.jira.config.AuthenticationManager;
import de.uhd.ifi.se.decision.management.jira.filtering.FilterSettings;
import de.uhd.ifi.se.decision.management.jira.quality.completeness.RationaleCompletenessCalculator;
import de.uhd.ifi.se.decision.management.jira.quality.completeness.RationaleCoverageCalculator;
import de.uhd.ifi.se.decision.management.jira.quality.generalmetrics.GeneralMetricCalculator;

/**
 * REST resource for dashboards
 */
@Path("/dashboard")
public class DashboardRest {

	/**
	 * Get general metrics for the elements of the knowledge graph filtered by the filter settings.
	 *
	 * @param request
	 * @param filterSettings
	 * @return GeneralMetricsCalculator
	 * An object containing the metrics.
	 */
	@Path("/generalMetrics")
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getGeneralMetrics(@Context HttpServletRequest request, FilterSettings filterSettings) {
		if (request == null || filterSettings == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "There is no project selected"))
					.build();
		}

		ApplicationUser user = AuthenticationManager.getUser(request);

		GeneralMetricCalculator generalMetricsCalculator = new GeneralMetricCalculator(user, filterSettings);

		return Response.status(Status.OK).entity(generalMetricsCalculator).build();
	}

	/**
	 * Get metrics about the intra-rationale completeness for the elements of the knowledge graph filtered
	 * by the filter settings.
	 *
	 * @param request
	 * @param filterSettings
	 * @return RationaleCompletenessCalculator
	 * An object containing the metrics.
	 */
	@Path("/rationaleCompleteness")
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getRationaleCompleteness(@Context HttpServletRequest request, FilterSettings filterSettings) {
		if (request == null || filterSettings == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "There is no project selected"))
					.build();
		}

		RationaleCompletenessCalculator rationaleCompletenessCalculator =
			new RationaleCompletenessCalculator(filterSettings);

		return Response.status(Status.OK).entity(rationaleCompletenessCalculator).build();
	}

	/**
	 * Get metrics about the rationale coverage for the elements of the knowledge graph filtered
	 * by the filter settings.
	 *
	 * @param request
	 * @param filterSettings
	 * @return RationaleCoverageCalculator
	 * An object containing the metrics.
	 */
	@Path("/rationaleCoverage")
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getRationaleCoverage(@Context HttpServletRequest request, FilterSettings filterSettings,
			@QueryParam("sourceKnowledgeTypes") String sourceKnowledgeTypes) {
		if (request == null || filterSettings == null || sourceKnowledgeTypes == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "There is no project selected"))
					.build();
		}

		RationaleCoverageCalculator rationaleCoverageCalculator = new RationaleCoverageCalculator(filterSettings, sourceKnowledgeTypes);

		return Response.status(Status.OK).entity(rationaleCoverageCalculator).build();
	}
}