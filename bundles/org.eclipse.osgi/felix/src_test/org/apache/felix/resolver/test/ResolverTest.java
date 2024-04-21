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
package org.apache.felix.resolver.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.resolver.test.util.BundleCapability;
import org.apache.felix.resolver.test.util.BundleRequirement;
import org.apache.felix.resolver.test.util.GenericCapability;
import org.apache.felix.resolver.test.util.GenericRequirement;
import org.apache.felix.resolver.test.util.PackageCapability;
import org.apache.felix.resolver.test.util.PackageRequirement;
import org.apache.felix.resolver.test.util.ResolveContextImpl;
import org.apache.felix.resolver.test.util.ResourceImpl;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.resolver.Resolver;

public class ResolverTest
{
    @Test
    public void testScenario1() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario1(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);
        assertEquals(2, wireMap.size());

        Resource aRes = findResource("A", wireMap.keySet());
        List<Wire> aWires = wireMap.get(aRes);
        assertEquals(0, aWires.size());

        Resource bRes = findResource("B", wireMap.keySet());
        List<Wire> bWires = wireMap.get(bRes);
        assertEquals(1, bWires.size());
        Wire bWire = bWires.iterator().next();
        assertEquals(aRes, bWire.getProvider());
        assertEquals(bRes, bWire.getRequirer());
        Capability cap = bWire.getCapability();
        assertEquals(PackageNamespace.PACKAGE_NAMESPACE, cap.getNamespace());
        assertEquals(1, cap.getAttributes().size());
        assertEquals("foo", cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
        assertEquals(0, cap.getDirectives().size());
        assertEquals(aRes, cap.getResource());

        Requirement req = bWire.getRequirement();
        assertEquals(1, req.getDirectives().size());
        assertEquals("(osgi.wiring.package=foo)", req.getDirectives().get(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE));
        assertEquals(0, req.getAttributes().size());
        assertEquals(PackageNamespace.PACKAGE_NAMESPACE, req.getNamespace());
        assertEquals(bRes, req.getResource());
    }

    @Test
    public void testScenario2() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario2(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);
        assertEquals(2, wireMap.size());

        Resource bRes = findResource("B", wireMap.keySet());
        List<Wire> bWires = wireMap.get(bRes);
        assertEquals(0, bWires.size());

        Resource cRes = findResource("C", wireMap.keySet());
        List<Wire> cWires = wireMap.get(cRes);
        assertEquals(2, cWires.size());

        boolean foundFoo = false;
        boolean foundBar = false;
        for (Wire w : cWires)
        {
            assertEquals(bRes, w.getProvider());
            assertEquals(cRes, w.getRequirer());

            Capability cap = w.getCapability();
            assertEquals(PackageNamespace.PACKAGE_NAMESPACE, cap.getNamespace());
            assertEquals(bRes, cap.getResource());
            Map<String, Object> attrs = cap.getAttributes();
            assertEquals(1, attrs.size());
            Object pkg = attrs.get(PackageNamespace.PACKAGE_NAMESPACE);
            if ("foo".equals(pkg))
            {
                foundFoo = true;
                assertEquals(0, cap.getDirectives().size());
            }
            else if ("bar".equals(pkg))
            {
                foundBar = true;
                assertEquals(1, cap.getDirectives().size());
                assertEquals("foo", cap.getDirectives().get(PackageNamespace.CAPABILITY_USES_DIRECTIVE));
            }
        }
        assertTrue(foundFoo);
        assertTrue(foundBar);
    }

    @Test
    public void testScenario3() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario3(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);
        assertEquals(3, wireMap.size());

        Resource cRes = findResource("C", wireMap.keySet());
        List<Wire> cWires = wireMap.get(cRes);
        assertEquals(0, cWires.size());

        Resource dRes = findResource("D", wireMap.keySet());
        List<Wire> dWires = wireMap.get(dRes);
        assertEquals(1, dWires.size());
        Wire dWire = dWires.iterator().next();
        assertEquals(cRes, dWire.getProvider());
        assertEquals(dRes, dWire.getRequirer());
        Capability dwCap = dWire.getCapability();
        assertEquals(PackageNamespace.PACKAGE_NAMESPACE, dwCap.getNamespace());
        assertEquals(1, dwCap.getAttributes().size());
        assertEquals("resources", dwCap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
        assertEquals(0, dwCap.getDirectives().size());
        assertEquals(cRes, dwCap.getResource());

        Resource eRes = findResource("E", wireMap.keySet());
        List<Wire> eWires = wireMap.get(eRes);
        assertEquals(2, eWires.size());

        boolean foundC = false;
        boolean foundD = false;
        for (Wire w : eWires)
        {
            assertEquals(eRes, w.getRequirer());

            Capability cap = w.getCapability();
            if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
            {
                assertEquals("resources", cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
                assertEquals(0, cap.getDirectives().size());
                assertEquals(cRes, cap.getResource());
                foundC = true;

                Requirement req = w.getRequirement();
                assertEquals(PackageNamespace.PACKAGE_NAMESPACE, req.getNamespace());
                assertEquals(eRes, req.getResource());
                assertEquals(0, req.getAttributes().size());
                assertEquals(1, req.getDirectives().size());
                assertEquals("(osgi.wiring.package=resources)", req.getDirectives().get("filter"));
            }
            else if (cap.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE))
            {
                assertEquals("D", cap.getAttributes().get(BundleNamespace.BUNDLE_NAMESPACE));
                assertEquals(1, cap.getDirectives().size());
                assertEquals("resources", cap.getDirectives().get(Namespace.CAPABILITY_USES_DIRECTIVE));
                assertEquals(dRes, cap.getResource());
                foundD = true;

                Requirement req = w.getRequirement();
                assertEquals(BundleNamespace.BUNDLE_NAMESPACE, req.getNamespace());
                assertEquals(eRes, req.getResource());
                assertEquals(0, req.getAttributes().size());
                assertEquals(1, req.getDirectives().size());
                assertEquals("(osgi.wiring.bundle=D)", req.getDirectives().get("filter"));
            }
        }
        assertTrue(foundC);
        assertTrue(foundD);
    }

    @Test
    public void testScenario4() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario4(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        try
        {
            resolver.resolve(rci);
            fail("Should have thrown a resolution exception as bundle A in scenario 4 cannot be resolved due to constraint violations.");
        }
        catch (ResolutionException re)
        {
            // good
        }
    }

    @Test
    public void testScenario5() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario5(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        try
        {
            resolver.resolve(rci);
            fail("Should have thrown a resolution exception as bundle A in scenario 5 cannot be resolved due to constraint violations.");
        }
        catch (ResolutionException re)
        {
            // good
        }
    }

    @Test
    public void testScenario6() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario6(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);

        int aResources = 0;
        for (Resource r : wireMap.keySet())
        {
            if ("A".equals(getResourceName(r)))
            {
                aResources++;

                List<Wire> wires = wireMap.get(r);
                assertEquals(4, wires.size());
                List<String> providers = new ArrayList<String>();
                for (Wire w : wires)
                {
                    providers.add(getResourceName(w.getProvider()));
                }
                Collections.sort(providers);
                assertEquals(Arrays.asList("B", "C", "D", "D"), providers);
            }
        }
        assertEquals("Should have found two resolved resources named 'A'", 2, aResources);
    }

    @Test
    public void testScenario7() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario7(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);
        assertEquals(4, wireMap.size());

        Resource aRes = findResource("A", wireMap.keySet());
        List<Wire> aWires = wireMap.get(aRes);
        assertEquals(0, aWires.size());

        Resource f1Res = findResource("F1", wireMap.keySet());
        List<Wire> f1Wires = wireMap.get(f1Res);
        assertEquals(1, f1Wires.size());
        Wire f1Wire = f1Wires.get(0);
        assertEquals(f1Res, f1Wire.getRequirer());
        assertEquals(aRes, f1Wire.getProvider());
        Requirement req = f1Wire.getRequirement();
        assertEquals(HostNamespace.HOST_NAMESPACE, req.getNamespace());
        assertEquals(0, req.getAttributes().size());
        assertEquals(f1Res, req.getResource());
        assertEquals(1, req.getDirectives().size());
        assertEquals("(osgi.wiring.host=A)", req.getDirectives().get("filter"));
        Capability cap = f1Wire.getCapability();
        assertEquals(aRes, cap.getResource());
        assertEquals(HostNamespace.HOST_NAMESPACE, cap.getNamespace());
        assertEquals(0, cap.getDirectives().size());
        assertEquals(1, cap.getAttributes().size());
        assertEquals("A", cap.getAttributes().get(HostNamespace.HOST_NAMESPACE));

        Resource f2Res = findResource("F2", wireMap.keySet());
        List<Wire> f2Wires = wireMap.get(f2Res);
        assertEquals(1, f2Wires.size());
        Wire f2Wire = f2Wires.get(0);
        assertEquals(f2Res, f2Wire.getRequirer());
        assertEquals(aRes, f2Wire.getProvider());
        Requirement req2 = f2Wire.getRequirement();
        assertEquals(HostNamespace.HOST_NAMESPACE, req2.getNamespace());
        assertEquals(0, req2.getAttributes().size());
        assertEquals(f2Res, req2.getResource());
        assertEquals(1, req2.getDirectives().size());
        assertEquals("(osgi.wiring.host=A)", req2.getDirectives().get("filter"));
        Capability cap2 = f1Wire.getCapability();
        assertEquals(aRes, cap2.getResource());
        assertEquals(HostNamespace.HOST_NAMESPACE, cap2.getNamespace());
        assertEquals(0, cap2.getDirectives().size());
        assertEquals(1, cap2.getAttributes().size());
        assertEquals("A", cap2.getAttributes().get(HostNamespace.HOST_NAMESPACE));

        Resource bRes = findResource("B", wireMap.keySet());
        List<Wire> bWires = wireMap.get(bRes);
        assertEquals(1, bWires.size());
        Wire bWire = bWires.get(0);
        assertEquals(bRes, bWire.getRequirer());
        assertEquals(f2Res, bWire.getProvider());
        Requirement bReq = bWire.getRequirement();
        assertEquals(IdentityNamespace.IDENTITY_NAMESPACE, bReq.getNamespace());
        assertEquals(0, bReq.getAttributes().size());
        assertEquals(bRes, bReq.getResource());
        assertEquals(1, bReq.getDirectives().size());
        assertEquals("(osgi.identity=F2)", bReq.getDirectives().get("filter"));
    }

    @Test
    public void testScenario8() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario8(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);

        Resource res2 = findResource("res2", wireMap.keySet());
        Resource res4 = findResource("res4", wireMap.keySet());
        Resource res5 = findResource("res5", wireMap.keySet());

        assertNotNull(res2);
        assertNotNull(res4);
        assertNotNull(res5);

        List<Wire> wires2 = wireMap.get(res2);
        assertEquals(2, wires2.size());
        // should be wired to res4 and res5

        List<Wire> wires4 = wireMap.get(res4);
        assertEquals(1, wires4.size());
        // should be wired to res5

        List<Wire> wires5 = wireMap.get(res5);
        assertEquals(0, wires5.size());
        // should not be wired to any of its optional requirements to res6

        assertEquals(3, wireMap.size());
    }

    @Test
    public void testScenario9() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario9(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);

        Resource resB = findResource("B", wireMap.keySet());
        Resource resA = findResource("A", wirings.keySet());
        Resource resC1 = findResource("C1", wirings.keySet());

        assertNotNull(resB);
        assertNotNull(resC1);

        assertEquals(1, wireMap.size());

        List<Wire> wiresB = wireMap.get(resB);
        assertEquals(2, wiresB.size());
        // should be wired to A and C1
        assertEquals(resA, wiresB.get(0).getProvider());
        assertEquals(resC1, wiresB.get(1).getProvider());
    }

    /**
     * Test dynamic resolution with a resolved fragment
     */
    @Test
    public void testScenario10() throws Exception
    {
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();

        ResourceImpl a1 = new ResourceImpl("A");
        Capability a1_hostCap = addCap(a1, HostNamespace.HOST_NAMESPACE, "A");

        ResourceImpl f1 = new ResourceImpl("F1", IdentityNamespace.TYPE_FRAGMENT, Version.emptyVersion);
        Requirement f1_hostReq = addReq(f1, HostNamespace.HOST_NAMESPACE, "A");
        Capability f1_pkgCap = addCap(f1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        ResourceImpl b1 = new ResourceImpl("B");
        Requirement b_pkgReq1 = addReq(b1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        candMap.put(b_pkgReq1, Collections.singletonList(f1_pkgCap));

        Map<Resource, List<Wire>> wires = new HashMap<Resource, List<Wire>>();
        wires.put(a1, new ArrayList<Wire>());
        wires.put(b1, new ArrayList<Wire>());
        wires.put(f1, new ArrayList<Wire>());
        wires.get(f1).add(new SimpleWire(f1_hostReq, a1_hostCap));

        Map<Resource, List<Wire>> invertedWires = new HashMap<Resource, List<Wire>>();
        invertedWires.put(a1, new ArrayList<Wire>());
        invertedWires.put(b1, new ArrayList<Wire>());
        invertedWires.put(f1, new ArrayList<Wire>());
        invertedWires.get(a1).add(new SimpleWire(f1_hostReq, a1_hostCap));

        wirings.put(a1, new SimpleWiring(a1, Arrays.asList(a1_hostCap, f1_pkgCap), wires, invertedWires));
        wirings.put(b1, new SimpleWiring(b1, Collections.<Capability>emptyList(), wires, invertedWires));
        wirings.put(f1, new SimpleWiring(f1, Collections.<Capability>emptyList(), wires, invertedWires));

        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, Collections.<Resource>emptyList(), Collections.<Resource> emptyList());

        List<Capability> caps = new ArrayList<Capability>();
        caps.add(f1_pkgCap);
        Map<Resource, List<Wire>> wireMap = resolver.resolveDynamic(rci, wirings.get(b1), b_pkgReq1);

        assertEquals(1, wireMap.size());
        List<Wire> wiresB = wireMap.get(b1);
        assertNotNull(wiresB);
        assertEquals(1, wiresB.size());
        // should be wired to A through the fragment capability
        assertEquals(a1, wiresB.get(0).getProvider());
        assertEquals(f1_pkgCap, wiresB.get(0).getCapability());
    }

    /**
     * Test dynamic resolution with an unresolved fragment
     */
    @Test
    public void testScenario11() throws Exception
    {
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();

        ResourceImpl a1 = new ResourceImpl("A");
        Capability a1_hostCap = addCap(a1, HostNamespace.HOST_NAMESPACE, "A");

        ResourceImpl f1 = new ResourceImpl("F1", IdentityNamespace.TYPE_FRAGMENT, Version.emptyVersion);
        Requirement f1_hostReq = addReq(f1, HostNamespace.HOST_NAMESPACE, "A");
        Capability f1_pkgCap = addCap(f1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        ResourceImpl b1 = new ResourceImpl("B");
        Requirement b_pkgReq1 = addReq(b1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        candMap.put(b_pkgReq1, Collections.singletonList(f1_pkgCap));
        candMap.put(f1_hostReq, Collections.singletonList(a1_hostCap));

        Map<Resource, List<Wire>> wires = new HashMap<Resource, List<Wire>>();
        wires.put(a1, new ArrayList<Wire>());
        wires.put(b1, new ArrayList<Wire>());

        Map<Resource, List<Wire>> invertedWires = new HashMap<Resource, List<Wire>>();
        invertedWires.put(a1, new ArrayList<Wire>());
        invertedWires.put(b1, new ArrayList<Wire>());

        wirings.put(a1, new SimpleWiring(a1, Collections.<Capability>emptyList(), wires, invertedWires));
        wirings.put(b1, new SimpleWiring(b1, Collections.<Capability>emptyList(), wires, invertedWires));

        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, Collections.<Resource>emptyList(), Collections.<Resource> emptyList());

        List<Capability> caps = new ArrayList<Capability>();
        caps.add(f1_pkgCap);
        try {
            resolver.resolveDynamic(rci, wirings.get(b1), b_pkgReq1);
            fail("Should fail to dynamic requirement to fragment when host is resolved already.");
        } catch (ResolutionException e) {
            // expected
            assertTrue(e.getUnresolvedRequirements().contains(b_pkgReq1));
        }

        // now remove host wiring
        wirings.remove(a1);
        caps.clear();
        caps.add(f1_pkgCap);
        Map<Resource, List<Wire>> wireMap = resolver.resolveDynamic(rci, wirings.get(b1), b_pkgReq1);

        assertEquals(3, wireMap.size());
        List<Wire> wiresB = wireMap.get(b1);
        assertNotNull(wiresB);
        assertEquals(1, wiresB.size());
        // should be wired to A through the fragment capability
        assertEquals(a1, wiresB.get(0).getProvider());
        assertEquals(f1_pkgCap, wiresB.get(0).getCapability());
    }

    /**
     * Test dynamic resolution with an unresolvable host
     */
    @Test(expected = ResolutionException.class)
    public void testScenario12() throws Exception
    {
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();

        ResourceImpl a1 = new ResourceImpl("A");
        Capability a1_hostCap = addCap(a1, HostNamespace.HOST_NAMESPACE, "A");

        ResourceImpl b1 = new ResourceImpl("B");
        Requirement b_pkgReq1 = addReq(b1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        ResourceImpl c1 = new ResourceImpl("C");
        Capability c_hostCap = addCap(c1, HostNamespace.HOST_NAMESPACE, "A");
        Capability c_pkgCap = addCap(c1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");
        Requirement c_pkgReq1 = addReq(c1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.b");

        candMap.put(b_pkgReq1, Collections.singletonList(c_pkgCap));
        candMap.put(c_pkgReq1, Collections.<Capability>emptyList());

        Map<Resource, List<Wire>> wires = new HashMap<Resource, List<Wire>>();
        wires.put(a1, new ArrayList<Wire>());
        wires.put(b1, new ArrayList<Wire>());

        Map<Resource, List<Wire>> invertedWires = new HashMap<Resource, List<Wire>>();
        invertedWires.put(a1, new ArrayList<Wire>());
        invertedWires.put(b1, new ArrayList<Wire>());

        wirings.put(a1, new SimpleWiring(a1, Collections.<Capability>emptyList(), wires, invertedWires));
        wirings.put(b1, new SimpleWiring(b1, Collections.<Capability>emptyList(), wires, invertedWires));

        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, Collections.<Resource>emptyList(), Collections.<Resource> emptyList());

        List<Capability> caps = new ArrayList<Capability>();
        caps.add(c_pkgCap);
        Map<Resource, List<Wire>> wireMap = resolver.resolveDynamic(rci, wirings.get(b1), b_pkgReq1);

        assertEquals(0, wireMap.size());
    }

    @Test
    public void testScenario13() throws Exception
    {
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();

        ResourceImpl a1 = new ResourceImpl("A");
        Capability a1_hostCap = addCap(a1, HostNamespace.HOST_NAMESPACE, "A");

        ResourceImpl f1 = new ResourceImpl("F1", IdentityNamespace.TYPE_FRAGMENT, Version.emptyVersion);
        Requirement f1_hostReq = addReq(f1, HostNamespace.HOST_NAMESPACE, "A");
        Capability f1_pkgCap = addCap(f1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        ResourceImpl b1 = new ResourceImpl("B");
        Requirement b_pkgReq1 = addReq(b1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        candMap.put(b_pkgReq1, Collections.singletonList(f1_pkgCap));
        candMap.put(f1_hostReq, Collections.singletonList(a1_hostCap));


        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, Collections.<Resource> singletonList(b1), Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);

        assertEquals(3, wireMap.size());
        List<Wire> wiresB = wireMap.get(b1);
        assertNotNull(wiresB);
        assertEquals(1, wiresB.size());
        // should be wired to A through the fragment capability
        assertEquals(a1, wiresB.get(0).getProvider());
        assertEquals(f1_pkgCap, wiresB.get(0).getCapability());
    }

    @Test
    public void testScenario14() throws Exception
    {
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();

        ResourceImpl a1 = new ResourceImpl("A", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("1.0.0"));
        Capability a1_hostCap = addCap(a1, HostNamespace.HOST_NAMESPACE, "A");
        Capability a1_pkgCap = addCap(a1, PackageNamespace.PACKAGE_NAMESPACE, "a");
        Requirement a1_pkgReq = addReq(a1, PackageNamespace.PACKAGE_NAMESPACE, "a.impl");

        ResourceImpl a2 = new ResourceImpl("A", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("2.0.0"));
        Capability a2_hostCap = addCap(a2, HostNamespace.HOST_NAMESPACE, "A");
        Capability a2_pkgCap = addCap(a2, PackageNamespace.PACKAGE_NAMESPACE, "a");
        Requirement a2_pkgReq = addReq(a2, PackageNamespace.PACKAGE_NAMESPACE, "a.impl");

        ResourceImpl a3 = new ResourceImpl("A", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("3.0.0"));
        Capability a3_hostCap = addCap(a3, HostNamespace.HOST_NAMESPACE, "A");
        Capability a3_pkgCap = addCap(a3, PackageNamespace.PACKAGE_NAMESPACE, "a");
        Requirement a3_pkgReq = addReq(a3, PackageNamespace.PACKAGE_NAMESPACE, "a.impl");

        ResourceImpl f1 = new ResourceImpl("F1", IdentityNamespace.TYPE_FRAGMENT, Version.emptyVersion);
        Requirement f1_hostReq = addReq(f1, HostNamespace.HOST_NAMESPACE, "A");
        Capability f1_pkgCap = addCap(f1, PackageNamespace.PACKAGE_NAMESPACE, "a.impl");
        Requirement f1_pkgReq = addReq(f1, PackageNamespace.PACKAGE_NAMESPACE, "a");

        ResourceImpl b1 = new ResourceImpl("B");
        Requirement b_pkgReq1 = addReq(b1, PackageNamespace.PACKAGE_NAMESPACE, "a");

        candMap.put(a1_pkgReq, Collections.singletonList(f1_pkgCap));
        candMap.put(a2_pkgReq, Collections.singletonList(f1_pkgCap));
        candMap.put(a3_pkgReq, Collections.singletonList(f1_pkgCap));
        candMap.put(b_pkgReq1, Arrays.asList(a3_pkgCap, a2_pkgCap, a1_pkgCap));
        candMap.put(f1_pkgReq, Arrays.asList(a3_pkgCap, a2_pkgCap, a1_pkgCap));
        candMap.put(f1_hostReq, Arrays.asList(a3_hostCap, a2_hostCap, a1_hostCap));


        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, Arrays.<Resource> asList(b1, a1, a2, a3), Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);

        // all bundles should be resolved
        assertEquals(5, wireMap.size());
        List<Wire> wiresB = wireMap.get(b1);
        assertNotNull(wiresB);
        assertEquals(1, wiresB.size());
        assertEquals(a3, wiresB.get(0).getProvider());
        assertEquals(a3_pkgCap, wiresB.get(0).getCapability());

        // There should be three hosts
        List<Wire> wiresF1 = wireMap.get(f1);
        assertNotNull(wiresF1);
        assertEquals(3, wiresF1.size());
    }

    @Test
    public void testScenario15() throws Exception
    {
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);

        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();

        ResourceImpl exporter = new ResourceImpl("exporter", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("1.0.0"));
        Capability exporter_hostCap = addCap(exporter, HostNamespace.HOST_NAMESPACE, "exporter");
        Capability exporter_pkgCap = addCap(exporter, PackageNamespace.PACKAGE_NAMESPACE, "exporter");

        ResourceImpl exporterFrag = new ResourceImpl("exporter.frag", IdentityNamespace.TYPE_FRAGMENT, Version.emptyVersion);
        Requirement exporterFrag_hostReq = addReq(exporterFrag, HostNamespace.HOST_NAMESPACE, "exporter");

        ResourceImpl host1 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("1.0.0"));
        Capability host1_hostCap = addCap(host1, HostNamespace.HOST_NAMESPACE, "host");
        Requirement host1_pkgReq = addReq(host1, PackageNamespace.PACKAGE_NAMESPACE, "exporter");

        ResourceImpl host2 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("2.0.0"));
        Capability host2_hostCap = addCap(host2, HostNamespace.HOST_NAMESPACE, "host");
        Requirement host2_pkgReq = addReq(host2, PackageNamespace.PACKAGE_NAMESPACE, "exporter");

        ResourceImpl host3 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("3.0.0"));
        Capability host3_hostCap = addCap(host3, HostNamespace.HOST_NAMESPACE, "host");
        Requirement host3_pkgReq = addReq(host3, PackageNamespace.PACKAGE_NAMESPACE, "exporter");

        ResourceImpl host4 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("4.0.0"));
        Capability host4_hostCap = addCap(host4, HostNamespace.HOST_NAMESPACE, "host");
        Requirement host4_pkgReq = addReq(host4, PackageNamespace.PACKAGE_NAMESPACE, "exporter");

        ResourceImpl host5 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("5.0.0"));
        Capability host5_hostCap = addCap(host5, HostNamespace.HOST_NAMESPACE, "host");
        Requirement host5_pkgReq = addReq(host5, PackageNamespace.PACKAGE_NAMESPACE, "exporter");

        ResourceImpl host6 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("6.0.0"));
        Capability host6_hostCap = addCap(host6, HostNamespace.HOST_NAMESPACE, "host");
        Requirement host6_pkgReq = addReq(host6, PackageNamespace.PACKAGE_NAMESPACE, "exporter");

        ResourceImpl host7 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("7.0.0"));
        Capability host7_hostCap = addCap(host7, HostNamespace.HOST_NAMESPACE, "host");
        Requirement host7_pkgReq = addReq(host7, PackageNamespace.PACKAGE_NAMESPACE, "exporter");

        ResourceImpl host8 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("8.0.0"));
        Capability host8_hostCap = addCap(host8, HostNamespace.HOST_NAMESPACE, "host");
        Requirement host8_pkgReq = addReq(host8, PackageNamespace.PACKAGE_NAMESPACE, "exporter");

        ResourceImpl hostFrag = new ResourceImpl("host.frag", IdentityNamespace.TYPE_FRAGMENT, Version.emptyVersion);
        Requirement hostFrag_hostReq = addReq(hostFrag, HostNamespace.HOST_NAMESPACE, "host");
        Requirement hostFrag_pkgReq = addReq(hostFrag, PackageNamespace.PACKAGE_NAMESPACE, "exporter");

        candMap.put(exporterFrag_hostReq, Collections.singletonList(exporter_hostCap));
        candMap.put(host1_pkgReq, Collections.singletonList(exporter_pkgCap));
        candMap.put(host2_pkgReq, Collections.singletonList(exporter_pkgCap));
        candMap.put(host3_pkgReq, Collections.singletonList(exporter_pkgCap));
        candMap.put(host4_pkgReq, Collections.singletonList(exporter_pkgCap));
        candMap.put(host5_pkgReq, Collections.singletonList(exporter_pkgCap));
        candMap.put(host6_pkgReq, Collections.singletonList(exporter_pkgCap));
        candMap.put(host7_pkgReq, Collections.singletonList(exporter_pkgCap));
        candMap.put(host8_pkgReq, Collections.singletonList(exporter_pkgCap));
        candMap.put(hostFrag_pkgReq, Collections.singletonList(exporter_pkgCap));
        candMap.put(hostFrag_hostReq,
            Arrays.asList(host1_hostCap, host2_hostCap, host3_hostCap, host4_hostCap, host5_hostCap, host6_hostCap, host7_hostCap, host8_hostCap));

        ResolveContextImpl rci = new ResolveContextImpl(Collections.<Resource, Wiring> emptyMap(), candMap,
            Arrays.<Resource> asList(host1, host2, host3, host4, exporter, exporterFrag, host5, host6, host7, host8, hostFrag),
            Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);

        // all bundles should be resolved
        assertEquals(11, wireMap.size());

        // There should be 8 hosts
        List<Wire> wiresHostFrag = wireMap.get(hostFrag);
        assertNotNull(wiresHostFrag);
        assertEquals(8, wiresHostFrag.size());

        List<Wire> wiresHost1 = wireMap.get(host1);
        assertNotNull(wiresHost1);
    }

    @Test
    public void testScenario16() throws Exception
    {
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);

        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();

        ResourceImpl exporter = new ResourceImpl("exporter", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("1.0.0"));
        Capability exporter_hostCap = addCap(exporter, HostNamespace.HOST_NAMESPACE, "exporter");
        Capability exporter_pkgCap = addCap(exporter, PackageNamespace.PACKAGE_NAMESPACE, "exporter");

        ResourceImpl exporterFrag = new ResourceImpl("exporter.frag", IdentityNamespace.TYPE_FRAGMENT, Version.emptyVersion);
        Requirement exporterFrag_hostReq = addReq(exporterFrag, HostNamespace.HOST_NAMESPACE, "exporter");

        ResourceImpl host1 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("1.0.0"));
        Capability host1_hostCap = addCap(host1, HostNamespace.HOST_NAMESPACE, "host");

        ResourceImpl host2 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("2.0.0"));
        Capability host2_hostCap = addCap(host2, HostNamespace.HOST_NAMESPACE, "host");

        ResourceImpl host3 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("3.0.0"));
        Capability host3_hostCap = addCap(host3, HostNamespace.HOST_NAMESPACE, "host");

        ResourceImpl host4 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("4.0.0"));
        Capability host4_hostCap = addCap(host4, HostNamespace.HOST_NAMESPACE, "host");

        ResourceImpl host5 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("5.0.0"));
        Capability host5_hostCap = addCap(host4, HostNamespace.HOST_NAMESPACE, "host");

        ResourceImpl host6 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("6.0.0"));
        Capability host6_hostCap = addCap(host4, HostNamespace.HOST_NAMESPACE, "host");

        ResourceImpl host7 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("7.0.0"));
        Capability host7_hostCap = addCap(host4, HostNamespace.HOST_NAMESPACE, "host");

        ResourceImpl host8 = new ResourceImpl("host", IdentityNamespace.TYPE_BUNDLE, Version.parseVersion("8.0.0"));
        Capability host8_hostCap = addCap(host4, HostNamespace.HOST_NAMESPACE, "host");

        ResourceImpl hostFrag = new ResourceImpl("host.frag", IdentityNamespace.TYPE_FRAGMENT, Version.emptyVersion);
        Requirement hostFrag_hostReq = addReq(hostFrag, HostNamespace.HOST_NAMESPACE, "host");
        Requirement hostFrag_pkgReq = addReq(hostFrag, PackageNamespace.PACKAGE_NAMESPACE, "exporter");

        candMap.put(exporterFrag_hostReq, Collections.singletonList(exporter_hostCap));
        candMap.put(hostFrag_pkgReq, Collections.singletonList(exporter_pkgCap));

        candMap.put(hostFrag_hostReq,
            Arrays.asList(host1_hostCap, host2_hostCap, host3_hostCap, host4_hostCap, host5_hostCap, host6_hostCap, host7_hostCap, host8_hostCap));

        ResolveContextImpl rci = new ResolveContextImpl(Collections.<Resource, Wiring> emptyMap(), candMap,
            Arrays.<Resource> asList(host1, host2, host3, host4, exporter, exporterFrag, hostFrag, host5, host6, host7, host8),
            Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);

        // all bundles should be resolved
        assertEquals(11, wireMap.size());

    }

    @Test
    public void testPackageSources() throws Exception {
        Method m = ResolverImpl.class.getDeclaredMethod("getPackageSources",
                Capability.class, Map.class);
        m.setAccessible(true);

        Capability cap = Mockito.mock(Capability.class);
        assertEquals(Collections.emptySet(),
                m.invoke(null, cap, new HashMap<Resource, ResolverImpl.Packages>()));

        Capability cap2 = Mockito.mock(Capability.class);
        Resource res2 = Mockito.mock(Resource.class);
        Mockito.when(cap2.getResource()).thenReturn(res2);
        Map<Resource, ResolverImpl.Packages> map2 = new HashMap<Resource, ResolverImpl.Packages>();
        map2.put(res2, new ResolverImpl.Packages(res2));
        assertEquals(Collections.emptySet(), m.invoke(null, cap2, map2));

        Capability cap3 = Mockito.mock(Capability.class);
        Resource res3 = Mockito.mock(Resource.class);
        Mockito.when(cap3.getResource()).thenReturn(res3);
        Map<Resource, ResolverImpl.Packages> map3 = new HashMap<Resource, ResolverImpl.Packages>();
        ResolverImpl.Packages pkgs3 = new ResolverImpl.Packages(res3);
        Set<Capability> srcCaps3 = Collections.singleton(Mockito.mock(Capability.class));
        Map<Capability, Set<Capability>> srcMap3 = Collections.singletonMap(
                cap3, srcCaps3);
        pkgs3.m_sources.putAll(srcMap3);
        map3.put(res3, pkgs3);
        assertEquals(srcCaps3, m.invoke(null, cap3, map3));

    }

    @Test
    public void testScenario17_1() throws Exception
    {
        ResolveContext rci = populateScenario17(false, false, false);
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);
        resolver.resolve(rci);
    }

    @Test
    public void testScenario17_2() throws Exception
    {
        ResolveContext rci = populateScenario17(false, false, true);
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);
        resolver.resolve(rci);
    }

    @Test
    public void testScenario17_3() throws Exception
    {
        ResolveContext rci = populateScenario17(true, false, false);
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);
        resolver.resolve(rci);
    }

    @Test
    public void testScenario17_4() throws Exception
    {
        ResolveContext rci = populateScenario17(true, false, true);
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);
        resolver.resolve(rci);
    }

    @Test
    public void testScenario17_5() throws Exception
    {
        ResolveContext rci = populateScenario17(false, true, true);
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);
        resolver.resolve(rci);
    }

    @Test
    public void testScenario17_6() throws Exception
    {
        ResolveContext rci = populateScenario17(true, true, true);
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);
        resolver.resolve(rci);
    }

    @Test
    public void testScenario18() throws Exception
    {
        ResolveContext rci = populateScenario18();
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);
        resolver.resolve(rci);
    }

    @Test
    public void testScenario19() throws Exception
    {
        ResolveContext rci = populateScenario19();
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);
        Map<Resource, List<Wire>> result = resolver.resolve(rci);

        assertEquals("Wrong number of resolved bundles", 9, result.size());
    }

    private ResolveContext populateScenario17(boolean realSubstitute,
        boolean felixResolveContext, boolean existingWirings)
    {
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        ResourceImpl core = new ResourceImpl("core");
        Capability core_pkgCap = addCap(core, PackageNamespace.PACKAGE_NAMESPACE, "pkg1");
        Capability core_bundleCap = addCap(core, BundleNamespace.BUNDLE_NAMESPACE,
            "core");
        Requirement core_pkgReq = addReq(core, PackageNamespace.PACKAGE_NAMESPACE,
            "pkg1");

        ResourceImpl misc = new ResourceImpl("misc");
        Capability misc_pkgCap = addCap(misc, PackageNamespace.PACKAGE_NAMESPACE, "pkg1");
        Capability misc_bundleCap = addCap(misc, BundleNamespace.BUNDLE_NAMESPACE,
            "misc");
        Requirement misc_bundleReq = addReq(misc, BundleNamespace.BUNDLE_NAMESPACE,
            "core");

        ResourceImpl importsCore = new ResourceImpl("importsCore");
        Capability importsCore_pkgCap = addCap(importsCore,
            PackageNamespace.PACKAGE_NAMESPACE, "pkg2", "pkg1");
        Requirement importsCore_pkgReq = addReq(importsCore,
            PackageNamespace.PACKAGE_NAMESPACE, "pkg1");

        ResourceImpl requiresMisc = new ResourceImpl("requiresMisc");
        Requirement requiresMisc_pkgReq = addReq(requiresMisc,
            PackageNamespace.PACKAGE_NAMESPACE, "pkg2");
        Requirement requiresMisc_bundleReq = addReq(requiresMisc,
            BundleNamespace.BUNDLE_NAMESPACE, "misc");

        ResourceImpl substitutesCore = new ResourceImpl("substitutesCore");
        Capability substitutesCore_pkgCap = addCap(substitutesCore,
            PackageNamespace.PACKAGE_NAMESPACE, "pkg1");

        candMap.put(core_pkgReq, Collections.singletonList(
            realSubstitute ? substitutesCore_pkgCap : core_pkgCap));
        candMap.put(misc_bundleReq, Collections.singletonList(core_bundleCap));
        candMap.put(importsCore_pkgReq, Collections.singletonList(
            realSubstitute ? substitutesCore_pkgCap : core_pkgCap));
        candMap.put(requiresMisc_pkgReq, Collections.singletonList(importsCore_pkgCap));
        candMap.put(requiresMisc_bundleReq, Collections.singletonList(misc_bundleCap));

        Map<Resource, List<Wire>> wires = new HashMap<Resource, List<Wire>>();
        wires.put(substitutesCore, new ArrayList<Wire>());
        wires.put(core, new ArrayList<Wire>());
        if (realSubstitute)
        {
            wires.get(core).add(new SimpleWire(core_pkgReq, substitutesCore_pkgCap));
        }
        wires.put(misc, new ArrayList<Wire>());
        wires.get(misc).add(new SimpleWire(misc_bundleReq, core_bundleCap));

        Wiring coreWiring = null;
        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        if (existingWirings)
        {
            Map<Resource, List<Wire>> invertedWires = new HashMap<Resource, List<Wire>>();
            invertedWires.put(substitutesCore, new ArrayList<Wire>());
            if (realSubstitute)
            {
                invertedWires.get(substitutesCore).add(
                    new SimpleWire(core_pkgReq, substitutesCore_pkgCap));
            }
            invertedWires.put(core, new ArrayList<Wire>());
            invertedWires.get(core).add(new SimpleWire(misc_bundleReq, core_bundleCap));
            invertedWires.put(misc, new ArrayList<Wire>());

            wirings.put(substitutesCore, new SimpleWiring(substitutesCore,
                Arrays.asList(substitutesCore_pkgCap), wires, invertedWires));

            coreWiring = new SimpleWiring(core,
                Arrays.asList(core_bundleCap, core_pkgCap), wires, invertedWires);
            wirings.put(core, coreWiring);
            wirings.put(misc, new SimpleWiring(misc,
                Arrays.asList(misc_bundleCap, misc_pkgCap), wires, invertedWires));
        }
        Collection<Resource> mandatory = Collections.<Resource> singletonList(requiresMisc);
        if (felixResolveContext) {
            Map<Wiring, List<Wire>> substitutions = new HashMap<Wiring, List<Wire>>();
            if (realSubstitute && coreWiring != null)
            {
                substitutions.put(coreWiring, Arrays.<Wire> asList(
                    new SimpleWire(core_pkgReq, substitutesCore_pkgCap)));
            }
            return new ResolveContextImpl.FelixResolveContextImpl(wirings, candMap,
                mandatory, Collections.<Resource> emptyList(), substitutions);
        }
        else
        {
            return new ResolveContextImpl(wirings, candMap, mandatory,
                Collections.<Resource> emptyList());
        }
    }

    private ResolveContext populateScenario18()
    {
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();

        ResourceImpl core1 = new ResourceImpl("core1");
        Capability core1_pkgCap1 = addCap(core1, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg1");
        Capability core1_pkgCap2 = addCap(core1, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg2", "corepkg1");
        Capability core1_pkgCap3 = addCap(core1, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg3", "corepkg2");

        ResourceImpl core2 = new ResourceImpl("core2");
        Capability core2_pkgCap1 = addCap(core2, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg1");
        Capability core2_pkgCap2 = addCap(core2, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg2", "corepkg1");
        Capability core2_pkgCap3 = addCap(core2, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg3", "corepkg2");
        Requirement core2_pkgReq1 = addReq(core2, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg1");
        Requirement core2_pkgReq2 = addReq(core2, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg2");
        Requirement core2_pkgReq3 = addReq(core2, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg3");

        ResourceImpl core3 = new ResourceImpl("core3");
        Capability core3_pkgCap1 = addCap(core3, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg1");
        Capability core3_pkgCap2 = addCap(core3, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg2", "corepkg1");
        Capability core3_pkgCap3 = addCap(core3, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg3", "corepkg2");
        Requirement core3_pkgReq1 = addReq(core3, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg1");
        Requirement core3_pkgReq2 = addReq(core3, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg2");
        Requirement core3_pkgReq3 = addReq(core3, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg3");

        ResourceImpl client1 = new ResourceImpl("client1");
        Capability client1_pkgCap = addCap(client1, PackageNamespace.PACKAGE_NAMESPACE,
            "clientpkg1", "corepkg3");
        Requirement client1_pkgReq1 = addReq(client1, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg3");

        ResourceImpl client2 = new ResourceImpl("client2");
        Capability client2_pkgCap = addCap(client2, PackageNamespace.PACKAGE_NAMESPACE,
            "clientpkg1", "corepkg3");
        Requirement client2_pkgReq1 = addReq(client2, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg3");

        ResourceImpl bundle1 = new ResourceImpl("bundle1");
        Requirement bundle1_pkgReq1 = addReq(bundle1, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg1");
        Requirement bundle1_pkgReq2 = addReq(bundle1, PackageNamespace.PACKAGE_NAMESPACE,
            "clientpkg1");

        ResourceImpl bundle2 = new ResourceImpl("bundle2");
        Requirement bundle2_pkgReq1 = addReq(bundle2, PackageNamespace.PACKAGE_NAMESPACE,
            "corepkg1");

        candMap.put(core2_pkgReq1, Arrays.asList(core3_pkgCap1, core2_pkgCap1));
        candMap.put(core2_pkgReq2, Arrays.asList(core3_pkgCap2, core2_pkgCap2));
        candMap.put(core2_pkgReq3, Arrays.asList(core3_pkgCap3, core2_pkgCap3));

        candMap.put(core3_pkgReq1, Arrays.asList(core3_pkgCap1, core2_pkgCap1));
        candMap.put(core3_pkgReq2, Arrays.asList(core3_pkgCap2, core2_pkgCap2));
        candMap.put(core3_pkgReq3, Arrays.asList(core3_pkgCap3, core2_pkgCap3));

        candMap.put(client1_pkgReq1,
            Arrays.asList(core3_pkgCap3, core2_pkgCap3, core1_pkgCap3));
        candMap.put(client2_pkgReq1, Arrays.asList(core3_pkgCap3));

        candMap.put(bundle1_pkgReq1, Arrays.asList(core1_pkgCap1));
        candMap.put(bundle1_pkgReq2, Arrays.asList(client1_pkgCap));

        candMap.put(bundle2_pkgReq1, Arrays.asList(core3_pkgCap1));

        Collection<Resource> mandatory = Arrays.<Resource> asList(core1, core2, core3,
            client1, client2, bundle1, bundle2);
        return new ResolveContextImpl(Collections.<Resource, Wiring> emptyMap(), candMap,
            mandatory, Collections.<Resource> emptyList());
    }

    private ResolveContext populateScenario19()
    {
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();

        ResourceImpl split1 = new ResourceImpl("split1");
        Capability split1_bundle = addCap(split1, BundleNamespace.BUNDLE_NAMESPACE, "splil1");
        Capability split1_pkg = addCap(split1, PackageNamespace.PACKAGE_NAMESPACE,
            "split.pkg");

        ResourceImpl split2 = new ResourceImpl("split2");
        Capability split2_bundle = addCap(split2, BundleNamespace.BUNDLE_NAMESPACE, "splil2");
        Capability split2_pkg = addCap(split2, PackageNamespace.PACKAGE_NAMESPACE,
            "split.pkg");

        ResourceImpl split3 = new ResourceImpl("split3");
        Capability split3_bundle = addCap(split3, BundleNamespace.BUNDLE_NAMESPACE, "splil3");
        Capability split3_pkg = addCap(split3, PackageNamespace.PACKAGE_NAMESPACE,
            "split.pkg");

        ResourceImpl split4 = new ResourceImpl("split4");
        Capability split4_bundle = addCap(split4, BundleNamespace.BUNDLE_NAMESPACE,
            "splil4");
        Capability split4_pkg = addCap(split4, PackageNamespace.PACKAGE_NAMESPACE,
            "split.pkg");

        GenericRequirement split3_split1Req = (GenericRequirement) addReq(split3,
            BundleNamespace.BUNDLE_NAMESPACE, "split1");
        split3_split1Req.addDirective(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE,
            BundleNamespace.VISIBILITY_REEXPORT);
        GenericRequirement split3_split2Req = (GenericRequirement) addReq(split3,
            BundleNamespace.BUNDLE_NAMESPACE, "split2");
        split3_split2Req.addDirective(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE,
            BundleNamespace.VISIBILITY_REEXPORT);

        ResourceImpl reexportSplit1 = new ResourceImpl("reexportSplit1");
        Capability reexportSplit1_bundle = addCap(reexportSplit1, BundleNamespace.BUNDLE_NAMESPACE, "reexportSplit1");
        GenericRequirement reexportSplit1_split1Req = (GenericRequirement) addReq(
            reexportSplit1, BundleNamespace.BUNDLE_NAMESPACE, "split1");
        reexportSplit1_split1Req.addDirective(
            BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE,
            BundleNamespace.VISIBILITY_REEXPORT);

        ResourceImpl reexportSplit3 = new ResourceImpl("reexportSplit3");
        Capability reexportSplit3_bundle = addCap(reexportSplit3, BundleNamespace.BUNDLE_NAMESPACE, "reexportSplit3");
        GenericRequirement reexportSplit3_split3Req = (GenericRequirement) addReq(
            reexportSplit3, BundleNamespace.BUNDLE_NAMESPACE, "split3");
        reexportSplit3_split3Req.addDirective(
            BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE,
            BundleNamespace.VISIBILITY_REEXPORT);

        ResourceImpl exportUsesSplit3 = new ResourceImpl("exportUsesSplit3");
        Capability exportUsesSplit3_bundle = addCap(exportUsesSplit3, BundleNamespace.BUNDLE_NAMESPACE, "exportUsesSplit3");
        GenericCapability exportUsesSplit_pkg = (GenericCapability) addCap(
            exportUsesSplit3, PackageNamespace.PACKAGE_NAMESPACE, "export.pkg");
        exportUsesSplit_pkg.addDirective(Namespace.CAPABILITY_USES_DIRECTIVE,
            "split.pkg");
        GenericRequirement exportUsesSplit3_split3Req = (GenericRequirement) addReq(
            exportUsesSplit3, BundleNamespace.BUNDLE_NAMESPACE, "reexportSplit3");

        ResourceImpl requireExportAndSplit1 = new ResourceImpl("requireExportAndSplit1");
        Requirement requireExportAndSplit1_split1Req = addReq(requireExportAndSplit1,
            BundleNamespace.BUNDLE_NAMESPACE, "reexportSplit1");
        Requirement requireExportAndSplit1_exportReq = addReq(requireExportAndSplit1,
            BundleNamespace.BUNDLE_NAMESPACE, "exportUsesSplit3");

            ResourceImpl importExportAndSplit = new ResourceImpl("importExportAndSplit");
            Requirement importExportAndSplit_importExport = addReq(importExportAndSplit,
                PackageNamespace.PACKAGE_NAMESPACE, "export.pkg");
            Requirement importExportAndSplit_importSplit = addReq(importExportAndSplit,
                PackageNamespace.PACKAGE_NAMESPACE, "split.pkg");

        candMap.put(split3_split1Req, Arrays.asList(split1_bundle));
        candMap.put(split3_split2Req, Arrays.asList(split2_bundle));
        candMap.put(reexportSplit1_split1Req, Arrays.asList(split1_bundle));
        candMap.put(reexportSplit3_split3Req, Arrays.asList(split3_bundle));
        candMap.put(exportUsesSplit3_split3Req, Arrays.asList(reexportSplit3_bundle));
        candMap.put(requireExportAndSplit1_split1Req, Arrays.asList(reexportSplit1_bundle));
        candMap.put(requireExportAndSplit1_exportReq,
            Arrays.asList(exportUsesSplit3_bundle));
        candMap.put(importExportAndSplit_importSplit,
            Arrays.asList(split4_pkg, split3_pkg));
        candMap.put(importExportAndSplit_importExport,
            Arrays.asList((Capability) exportUsesSplit_pkg));

        Collection<Resource> mandatory = Arrays.<Resource> asList(split1, split2,
            split3, split4, reexportSplit1, reexportSplit3, exportUsesSplit3,
            requireExportAndSplit1, importExportAndSplit);
        return new ResolveContextImpl(Collections.<Resource, Wiring> emptyMap(), candMap,
            mandatory, Collections.<Resource> emptyList());
    }

    private static String getResourceName(Resource r)
    {
        return r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0).getAttributes()
                .get(IdentityNamespace.IDENTITY_NAMESPACE).toString();
    }

    private static Resource findResource(String identity, Collection<Resource> resources)
    {
        for (Resource r : resources)
        {
            if (identity.equals(getResourceName(r)))
                return r;
        }
        return null;
    }

    private static List<Resource> populateScenario1(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        ResourceImpl exporter = new ResourceImpl("A");
        exporter.addCapability(new PackageCapability(exporter, "foo"));
        ResourceImpl importer = new ResourceImpl("B");
        importer.addRequirement(new PackageRequirement(importer, "foo"));
        candMap.put(importer.getRequirements(null).get(0), exporter.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE));
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(importer);
        return resources;
    }

    private static List<Resource> populateScenario2(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        List<Capability> fooCands = new ArrayList<Capability>();
        List<Capability> barCands = new ArrayList<Capability>();

        // A
        ResourceImpl a = new ResourceImpl("A");
        PackageCapability p = new PackageCapability(a, "foo");
        a.addCapability(p);
        fooCands.add(p);

        // B
        ResourceImpl b = new ResourceImpl("B");
        p = new PackageCapability(b, "foo");
        b.addCapability(p);
        fooCands.add(p);

        p = new PackageCapability(b, "bar");
        p.addDirective(PackageNamespace.CAPABILITY_USES_DIRECTIVE, "foo");
        b.addCapability(p);
        barCands.add(p);

        // C
        ResourceImpl c = new ResourceImpl("C");
        Requirement r = new PackageRequirement(c, "foo");
        c.addRequirement(r);
        candMap.put(r, fooCands);

        r = new PackageRequirement(c, "bar");
        c.addRequirement(r);
        candMap.put(r, barCands);

        // Mandatory resources
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(c);
        return resources;
    }

    private static List<Resource> populateScenario3(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        List<Capability> dResourcesCands = new ArrayList<Capability>();
        List<Capability> eBundleDCands = new ArrayList<Capability>();
        List<Capability> eResourcesCands = new ArrayList<Capability>();

        // B
        ResourceImpl b = new ResourceImpl("B");
        PackageCapability pc = new PackageCapability(b, "resources");
        b.addCapability(pc);
        eResourcesCands.add(pc);

        // C
        ResourceImpl c = new ResourceImpl("C");
        pc = new PackageCapability(c, "resources");
        c.addCapability(pc);
        eResourcesCands.add(pc);
        dResourcesCands.add(pc);

        // D
        ResourceImpl d = new ResourceImpl("D");
        pc = new PackageCapability(d, "export");
        pc.addDirective(Namespace.CAPABILITY_USES_DIRECTIVE, "resources");
        d.addCapability(pc);

        BundleCapability bc = new BundleCapability(d, "D");
        bc.addDirective(Namespace.CAPABILITY_USES_DIRECTIVE, "resources");
        d.addCapability(bc);
        eBundleDCands.add(bc);

        Requirement r = new PackageRequirement(d, "resources");
        d.addRequirement(r);
        candMap.put(r, dResourcesCands);

        // E
        ResourceImpl e = new ResourceImpl("E");
        r = new BundleRequirement(e, "D");
        e.addRequirement(r);
        candMap.put(r, eBundleDCands);

        r = new PackageRequirement(e, "resources");
        e.addRequirement(r);
        candMap.put(r, eResourcesCands);

        // Mandatory resources
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(e);
        return resources;
    }

    private static List<Resource> populateScenario4(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        ResourceImpl a = new ResourceImpl("A");
        a.addRequirement(new BundleRequirement(a, "B"));
        a.addRequirement(new BundleRequirement(a, "C"));

        ResourceImpl b = new ResourceImpl("B");
        b.addCapability(new BundleCapability(b, "B"));
        b.addCapability(new PackageCapability(b, "p1"));

        ResourceImpl c = new ResourceImpl("C");
        c.addRequirement(new BundleRequirement(c, "D"));
        c.addCapability(new BundleCapability(c, "C"));
        PackageCapability p2 = new PackageCapability(c, "p2");
        p2.addDirective(Namespace.CAPABILITY_USES_DIRECTIVE, "p1");
        c.addCapability(p2);

        ResourceImpl d = new ResourceImpl("D");
        d.addCapability(new BundleCapability(d, "D"));
        d.addCapability(new PackageCapability(d, "p1"));

        candMap.put(a.getRequirements(null).get(0), b.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(a.getRequirements(null).get(1), c.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(c.getRequirements(null).get(0), d.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(a);
        return resources;
    }

    private static List<Resource> populateScenario5(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        ResourceImpl x = new ResourceImpl("X");
        x.addRequirement(new BundleRequirement(x, "A"));

        ResourceImpl a = new ResourceImpl("A");
        a.addCapability(new BundleCapability(a, "A"));
        a.addRequirement(new BundleRequirement(a, "B"));
        a.addRequirement(new BundleRequirement(a, "C"));

        ResourceImpl b = new ResourceImpl("B");
        b.addCapability(new BundleCapability(b, "B"));
        b.addCapability(new PackageCapability(b, "p1"));

        ResourceImpl c = new ResourceImpl("C");
        c.addRequirement(new BundleRequirement(c, "D"));
        c.addCapability(new BundleCapability(c, "C"));
        PackageCapability p2 = new PackageCapability(c, "p2");
        p2.addDirective(Namespace.CAPABILITY_USES_DIRECTIVE, "p1");
        c.addCapability(p2);

        ResourceImpl d = new ResourceImpl("D");
        d.addCapability(new BundleCapability(d, "D"));
        d.addCapability(new PackageCapability(d, "p1"));

        candMap.put(x.getRequirements(null).get(0), a.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(a.getRequirements(null).get(0), b.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(a.getRequirements(null).get(1), c.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(c.getRequirements(null).get(0), d.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(x);
        return resources;
    }

    private static List<Resource> populateScenario6(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        ResourceImpl a1 = new ResourceImpl("A");
        a1.addRequirement(new PackageRequirement(a1, "p1"));
        a1.addRequirement(new PackageRequirement(a1, "p2"));
        Requirement a1Req = new GenericRequirement(a1, "generic");
        a1Req.getDirectives().put(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, Namespace.CARDINALITY_MULTIPLE);
        a1.addRequirement(a1Req);

        ResourceImpl a2 = new ResourceImpl("A");
        a2.addRequirement(new BundleRequirement(a2, "B"));
        a2.addRequirement(new BundleRequirement(a2, "C"));
        Requirement a2Req = new GenericRequirement(a2, "generic");
        a2Req.getDirectives().put(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, Namespace.CARDINALITY_MULTIPLE);
        a2.addRequirement(a2Req);

        ResourceImpl b1 = new ResourceImpl("B");
        b1.addCapability(new BundleCapability(b1, "B"));
        Capability b1_p2 = new PackageCapability(b1, "p2");
        b1_p2.getDirectives().put(Namespace.CAPABILITY_USES_DIRECTIVE, "p1");
        b1.addCapability(b1_p2);
        b1.addRequirement(new PackageRequirement(b1, "p1"));

        ResourceImpl b2 = new ResourceImpl("B");
        b2.addCapability(new BundleCapability(b2, "B"));
        Capability b2_p2 = new PackageCapability(b2, "p2");
        b2_p2.getDirectives().put(Namespace.CAPABILITY_USES_DIRECTIVE, "p1");
        b2.addCapability(b2_p2);
        b2.addRequirement(new PackageRequirement(b2, "p1"));

        ResourceImpl c1 = new ResourceImpl("C");
        c1.addCapability(new BundleCapability(c1, "C"));
        Capability c1_p1 = new PackageCapability(c1, "p1");
        c1.addCapability(c1_p1);

        ResourceImpl c2 = new ResourceImpl("C");
        c2.addCapability(new BundleCapability(c2, "C"));
        Capability c2_p1 = new PackageCapability(c2, "p1");
        c2.addCapability(c2_p1);

        ResourceImpl d1 = new ResourceImpl("D");
        GenericCapability d1_generic = new GenericCapability(d1, "generic");
        d1_generic.addDirective(Namespace.CAPABILITY_USES_DIRECTIVE, "p1,p2");
        d1.addCapability(d1_generic);
        d1.addRequirement(new PackageRequirement(d1, "p1"));
        d1.addRequirement(new PackageRequirement(d1, "p2"));

        ResourceImpl d2 = new ResourceImpl("D");
        GenericCapability d2_generic = new GenericCapability(d2, "generic");
        d2_generic.addDirective(Namespace.CAPABILITY_USES_DIRECTIVE, "p1,p2");
        d2.addCapability(d2_generic);
        d2.addRequirement(new PackageRequirement(d2, "p1"));
        d2.addRequirement(new PackageRequirement(d2, "p2"));

        candMap.put(a1.getRequirements(null).get(0), Arrays.asList(c2_p1));
        candMap.put(a1.getRequirements(null).get(1), Arrays.asList(b2_p2));
        candMap.put(a1.getRequirements(null).get(2), Arrays.asList((Capability) d1_generic, (Capability) d2_generic));
        candMap.put(a2.getRequirements(null).get(0), c2.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(a2.getRequirements(null).get(1), b2.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(a2.getRequirements(null).get(2), Arrays.asList((Capability) d1_generic, (Capability) d2_generic));
        candMap.put(b1.getRequirements(null).get(0), Arrays.asList(c1_p1, c2_p1));
        candMap.put(b2.getRequirements(null).get(0), Arrays.asList(c1_p1, c2_p1));
        candMap.put(d1.getRequirements(null).get(0), Arrays.asList(c1_p1, c2_p1));
        candMap.put(d1.getRequirements(null).get(1), Arrays.asList(b1_p2, b2_p2));
        candMap.put(d2.getRequirements(null).get(0), Arrays.asList(c1_p1, c2_p1));
        candMap.put(d2.getRequirements(null).get(1), Arrays.asList(b1_p2, b2_p2));
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(a1);
        resources.add(a2);
        return resources;
    }

    private static List<Resource> populateScenario7(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        ResourceImpl a1 = new ResourceImpl("A");
        GenericCapability a1_hostCap = new GenericCapability(a1, HostNamespace.HOST_NAMESPACE);
        a1_hostCap.addAttribute(HostNamespace.HOST_NAMESPACE, "A");
        a1.addCapability(a1_hostCap);

        ResourceImpl f1 = new ResourceImpl("F1", IdentityNamespace.TYPE_FRAGMENT, Version.emptyVersion);
        GenericRequirement f1_hostReq = new GenericRequirement(f1, HostNamespace.HOST_NAMESPACE);
        f1_hostReq.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(" + HostNamespace.HOST_NAMESPACE + "=A)");
        f1.addRequirement(f1_hostReq);

        ResourceImpl f2 = new ResourceImpl("F2", IdentityNamespace.TYPE_FRAGMENT, Version.emptyVersion);
        GenericRequirement f2_hostReq = new GenericRequirement(f2, HostNamespace.HOST_NAMESPACE);
        f2_hostReq.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(" + HostNamespace.HOST_NAMESPACE + "=A)");
        f2.addRequirement(f2_hostReq);

        ResourceImpl b1 = new ResourceImpl("B");
        GenericRequirement b1_identityReq = new GenericRequirement(b1, IdentityNamespace.IDENTITY_NAMESPACE);
        b1_identityReq.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=F2)");
        b1.addRequirement(b1_identityReq);

        candMap.put(f1.getRequirements(null).get(0), a1.getCapabilities(HostNamespace.HOST_NAMESPACE));
        candMap.put(f2.getRequirements(null).get(0), a1.getCapabilities(HostNamespace.HOST_NAMESPACE));
        candMap.put(b1.getRequirements(null).get(0), f2.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(a1);
        resources.add(f1);
        resources.add(f2);
        resources.add(b1);
        return resources;
    }

    private static List<Resource> populateScenario8(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        ResourceImpl res2 = new ResourceImpl("res2");
        Requirement req25 = addReq(res2, IdentityNamespace.IDENTITY_NAMESPACE, "res5");
        Requirement req24 = addReq(res2, IdentityNamespace.IDENTITY_NAMESPACE, "res4");
        Requirement req23 = addReq(res2, IdentityNamespace.IDENTITY_NAMESPACE, "res3", true);

        ResourceImpl res3 = new ResourceImpl("res3");
        Requirement req32 = addReq(res3, IdentityNamespace.IDENTITY_NAMESPACE, "res2");
        Requirement req3x = addReq(res3, "foo", "bar");

        ResourceImpl res4 = new ResourceImpl("res4");
        Requirement req45 = addReq(res4, IdentityNamespace.IDENTITY_NAMESPACE, "res5");

        ResourceImpl res5 = new ResourceImpl("res5");
        Requirement req5x1 = addReq(res5, BundleNamespace.BUNDLE_NAMESPACE, "package1", true);
        Requirement req5x2 = addReq(res5, BundleNamespace.BUNDLE_NAMESPACE, "package2", true);

        ResourceImpl res6 = new ResourceImpl("res6");
        Capability cap6x1 = addCap(res6, BundleNamespace.BUNDLE_NAMESPACE, "package1");
        Capability cap6x2 = addCap(res6, BundleNamespace.BUNDLE_NAMESPACE, "package2");
        Requirement req63 = addReq(res6, IdentityNamespace.IDENTITY_NAMESPACE, "res3");

        candMap.put(req25, res5.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        candMap.put(req24, res4.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        candMap.put(req23, res3.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        candMap.put(req32, res2.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        candMap.put(req45, res5.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        candMap.put(req63, res3.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        candMap.put(req3x, Arrays.<Capability>asList());
        candMap.put(req5x1, Arrays.<Capability>asList(cap6x1));
        candMap.put(req5x2, Arrays.<Capability>asList(cap6x2));
        return Arrays.<Resource>asList(res2);
    }

    private static List<Resource> populateScenario9(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap) {
        ResourceImpl c1 = new ResourceImpl("C1");
        Capability c1_pkgCap  = addCap(c1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.c");

        ResourceImpl c2 = new ResourceImpl("C2");
        Capability c2_pkgCap  = addCap(c2, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.c");

        ResourceImpl a1 = new ResourceImpl("A");
        Capability a1_hostCap = addCap(a1, HostNamespace.HOST_NAMESPACE, "A");

        ResourceImpl f1 = new ResourceImpl("F1", IdentityNamespace.TYPE_FRAGMENT, Version.emptyVersion);
        Requirement f1_hostReq = addReq(f1, HostNamespace.HOST_NAMESPACE, "A");
        Requirement f1_pkgReq = addReq(f1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.c");
        Capability f1_pkgCap = addCap(f1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a", "org.foo.c");

        ResourceImpl b1 = new ResourceImpl("B");
        Requirement b_pkgReq1 = addReq(b1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");
        Requirement b_pkgReq2 = addReq(b1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.c");

        candMap.put(b_pkgReq1, Collections.singletonList(f1_pkgCap));
        candMap.put(b_pkgReq2, Arrays.asList(c2_pkgCap, c1_pkgCap));

        Map<Resource, List<Wire>> wires = new HashMap<Resource, List<Wire>>();
        wires.put(c1, new ArrayList<Wire>());
        wires.put(c2, new ArrayList<Wire>());
        wires.put(a1, new ArrayList<Wire>());
        wires.put(f1, new ArrayList<Wire>());
        wires.get(f1).add(new SimpleWire(f1_hostReq, a1_hostCap));
        wires.get(a1).add(new SimpleWire(f1_pkgReq, c1_pkgCap, a1, c1));

        Map<Resource, List<Wire>> invertedWires = new HashMap<Resource, List<Wire>>();
        invertedWires.put(c1, new ArrayList<Wire>());
        invertedWires.put(c2, new ArrayList<Wire>());
        invertedWires.put(a1, new ArrayList<Wire>());
        invertedWires.put(f1, new ArrayList<Wire>());
        invertedWires.get(a1).add(new SimpleWire(f1_hostReq, a1_hostCap));
        invertedWires.get(c1).add(new SimpleWire(f1_pkgReq, c1_pkgCap, a1, c1));

        wirings.put(a1, new SimpleWiring(a1, Arrays.asList(a1_hostCap, f1_pkgCap), wires, invertedWires));
        wirings.put(f1, new SimpleWiring(f1, Collections.<Capability>emptyList(), wires, invertedWires));
        wirings.put(c1, new SimpleWiring(c1, Collections.singletonList(c1_pkgCap), wires, invertedWires));
        wirings.put(c2, new SimpleWiring(c2, Collections.singletonList(c2_pkgCap), wires, invertedWires));

        return Collections.<Resource>singletonList(b1);
    }

    private static Capability addCap(ResourceImpl res, String namespace, String value)
    {
        return addCap(res, namespace, value, null);
    }

    private static Capability addCap(ResourceImpl res, String namespace, String value, String uses)
    {
        GenericCapability cap = new GenericCapability(res, namespace);
        cap.addAttribute(namespace, value);
        if (uses != null)
        {
            cap.addDirective("uses", uses);
        }
        res.addCapability(cap);
        return cap;
    }

    private static Requirement addReq(ResourceImpl res, String namespace, String value)
    {
        return addReq(res, namespace, value, false);
    }

    private static Requirement addReq(ResourceImpl res, String namespace, String value, boolean optional)
    {
        GenericRequirement req = new GenericRequirement(res, namespace);
        req.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(" + namespace + "=" + value + ")");
        if (optional) {
            req.addDirective(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
        }
        res.addRequirement(req);
        return req;
    }

    private static class SimpleWire implements Wire {
        final Requirement requirement;
        final Capability capability;
        final Resource requirer;
        final Resource provider;

        public SimpleWire(Requirement requirement, Capability capability) {
            this(requirement, capability, requirement.getResource(), capability.getResource());
        }

        public SimpleWire(Requirement requirement, Capability capability, Resource requirer, Resource provider) {
            this.requirement = requirement;
            this.capability = capability;
            this.requirer = requirer;
            this.provider = provider;
        }

        public Capability getCapability() {
            return capability;
        }

        public Requirement getRequirement() {
            return requirement;
        }

        public Resource getRequirer() {
            return requirer;
        }

        public Resource getProvider() {
            return provider;
        }
    }

    private static class SimpleWiring implements Wiring {
        final Resource resource;
        final Map<Resource, List<Wire>> wires;
        final Map<Resource, List<Wire>> invertedWires;
        List<Capability> resourceCapabilities;
        List<Requirement> resourceRequirements;

        private SimpleWiring(Resource resource, List<Capability> resourceCapabilities, Map<Resource, List<Wire>> wires, Map<Resource, List<Wire>> invertedWires) {
            this.resource = resource;
            this.wires = wires;
            this.invertedWires = invertedWires;
            this.resourceCapabilities = resourceCapabilities;
        }

        public List<Capability> getResourceCapabilities(String namespace) {
            if (resourceCapabilities == null) {
                resourceCapabilities = new ArrayList<Capability>();
                for (Wire wire : invertedWires.get(resource)) {
                    if (!resourceCapabilities.contains(wire.getCapability())) {
                        resourceCapabilities.add(wire.getCapability());
                    }
                }
            }
            if (namespace != null) {
                List<Capability> caps = new ArrayList<Capability>();
                for (Capability cap : resourceCapabilities) {
                    if (namespace.equals(cap.getNamespace())) {
                        caps.add(cap);
                    }
                }
                return caps;
            }
            return resourceCapabilities;
        }

        public List<Requirement> getResourceRequirements(String namespace) {
            if (resourceRequirements == null) {
                resourceRequirements = new ArrayList<Requirement>();
                for (Wire wire : wires.get(resource)) {
                    if (!resourceRequirements.contains(wire.getRequirement())) {
                        resourceRequirements.add(wire.getRequirement());
                    }
                }
            }
            if (namespace != null) {
                List<Requirement> reqs = new ArrayList<Requirement>();
                for (Requirement req : resourceRequirements) {
                    if (namespace.equals(req.getNamespace())) {
                        reqs.add(req);
                    }
                }
                return reqs;
            }
            return resourceRequirements;
        }

        public List<Wire> getProvidedResourceWires(String namespace) {
            List<Wire> providedWires = invertedWires.get(resource);
            if (namespace != null) {
                List<Wire> wires = new ArrayList<Wire>();
                for (Wire wire : providedWires) {
                    if (namespace.equals(wire.getRequirement().getNamespace())) {
                        wires.add(wire);
                    }
                }
                return wires;
            }
            return providedWires;
        }

        public List<Wire> getRequiredResourceWires(String namespace) {
            List<Wire> requiredWires = wires.get(resource);
            if (namespace != null) {
                List<Wire> wires = new ArrayList<Wire>();
                for (Wire wire : requiredWires) {
                    if (namespace.equals(wire.getCapability().getNamespace())) {
                        wires.add(wire);
                    }
                }
                return wires;
            }
            return requiredWires;
        }

        public Resource getResource() {
            return resource;
        }
    }
}

