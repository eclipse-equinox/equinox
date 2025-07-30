/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.resolver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.felix.resolver.Candidates.FaultyResourcesReport;

/**
 * The Backlog has the following purpose:
 * <p>
 * If we where testing permutations, it could happen that we hit one that will
 * result in unresolved resources (e.g. a package has no more providers), but
 * this is usually not what we want. In such case we simply check if there is
 * possibly another permutation that can be used and has all resources resolved
 * so we need not to bother further check a not optimal solution.
 * </p>
 * <p>
 * On the other hand if the result itself is a failed resolution because there
 * exist no such thing where all resources are resolved, we want at least return
 * the one with the lowest count of unresolved resources requirements.
 * </p>
 * <p>
 * For this case we record all dropped permutations and if nothing more is left
 * in the session choose the one with the current lowest failures to be
 * processed next. If some are equally good we choose them in the order they
 * where put into the backlog.
 * </p>
 */
public class Backlog {

    private final ResolveSession session;
    private final Map<Candidates, FaultyResourcesReport> backlog = new LinkedHashMap<>();

    public Backlog(ResolveSession session) {
        this.session = session;
    }

    public Candidates getNext() {
        Candidates candidates;
        while ((candidates = session.getNextPermutation()) != null) {
            ResolutionError substituteError = candidates.checkSubstitutes();
            FaultyResourcesReport report = candidates.getFaultyResources();
            if (!report.isMissing() || session.isCancelled()) {
                return candidates;
            }
            backlog.put(candidates, candidates.getFaultyResources());
        }
        if (backlog.isEmpty()) {
            return null;
        }
        Entry<Candidates, FaultyResourcesReport> bestEntry = null;
        for (Entry<Candidates, FaultyResourcesReport> entry : backlog.entrySet()) {
            if (bestEntry == null || entry.getValue().isBetterThan(bestEntry.getValue())) {
                bestEntry = entry;
            }
        }
        Candidates result = bestEntry.getKey();
        backlog.remove(result);
        return result;
    }

}
