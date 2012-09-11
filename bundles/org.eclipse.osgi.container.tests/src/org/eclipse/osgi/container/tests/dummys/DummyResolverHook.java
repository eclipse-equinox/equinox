package org.eclipse.osgi.container.tests.dummys;

import java.util.*;
import org.eclipse.osgi.framework.report.ResolutionReport;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.*;

public class DummyResolverHook implements ResolutionReport.Listener, ResolverHook {
	private final List<ResolutionReport> reports = new ArrayList<ResolutionReport>();

	@Override
	public void filterResolvable(Collection<BundleRevision> candidates) {
		// TODO Auto-generated method stub

	}

	@Override
	public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
		// TODO Auto-generated method stub

	}

	@Override
	public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
		// TODO Auto-generated method stub

	}

	@Override
	public void end() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleResolutionReport(ResolutionReport report) {
		reports.add(report);
	}

	public List<ResolutionReport> getResolutionReports() {
		return reports;
	}
}
