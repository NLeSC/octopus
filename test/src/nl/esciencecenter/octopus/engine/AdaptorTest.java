/*
 * Copyright 2013 Netherlands eScience Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.esciencecenter.octopus.engine;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import nl.esciencecenter.octopus.OctopusException;
import nl.esciencecenter.octopus.OctopusPropertyDescription;
import nl.esciencecenter.octopus.OctopusPropertyDescription.Component;
import nl.esciencecenter.octopus.OctopusPropertyDescription.Type;
import nl.esciencecenter.octopus.credentials.Credentials;
import nl.esciencecenter.octopus.engine.util.ImmutableArray;
import nl.esciencecenter.octopus.files.Files;
import nl.esciencecenter.octopus.jobs.Jobs;

import org.junit.Test;

/**
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * 
 */
public class AdaptorTest {

    class TestAdaptor extends Adaptor {

        public TestAdaptor(OctopusEngine octopusEngine, String name, String description, ImmutableArray<String> supportedSchemes,
                ImmutableArray<String> supportedLocations, ImmutableArray<OctopusPropertyDescription> validProperties, 
                OctopusProperties p) throws OctopusException {

            super(octopusEngine, name, description, supportedSchemes, supportedLocations, validProperties, p);
        }

        @Override
        public Map<String, String> getAdaptorSpecificInformation() {
            return null;
        }

        @Override
        public Files filesAdaptor() throws OctopusException {
            return null;
        }

        @Override
        public Jobs jobsAdaptor() throws OctopusException {
            return null;
        }

        @Override
        public Credentials credentialsAdaptor() throws OctopusException {
            return null;
        }

        @Override
        public void end() {
        }

    }

    @Test
    public void test0() throws OctopusException {

        ImmutableArray<String> schemes = new ImmutableArray<String>("SCHEME1", "SCHEME2");
        ImmutableArray<String> locations = new ImmutableArray<String>("L1", "L2");

        TestAdaptor t = new TestAdaptor(null, "test", "DESCRIPTION", schemes, locations, 
                new ImmutableArray<OctopusPropertyDescription>(), new OctopusProperties());

        String[] tmp = t.getSupportedSchemes();

        assert (tmp != null);
        assert (Arrays.equals(schemes.asArray(), tmp));
    }

    @Test
    public void test1() throws OctopusException {

        ImmutableArray<String> schemes = new ImmutableArray<String>("SCHEME1", "SCHEME2");
        ImmutableArray<String> locations = new ImmutableArray<String>("L1", "L2");

        ImmutableArray<OctopusPropertyDescription> supportedProperties = new ImmutableArray<OctopusPropertyDescription>(
                new OctopusPropertyDescriptionImplementation("octopus.adaptors.test.p1", Type.STRING, EnumSet.of(Component.OCTOPUS),
                        "aap2", "test property p1"),

                new OctopusPropertyDescriptionImplementation("octopus.adaptors.test.p2", Type.STRING, EnumSet.of(Component.OCTOPUS),
                        "noot2", "test property p2"));

        OctopusProperties prop = new OctopusProperties(supportedProperties, new HashMap<String, String>());
        TestAdaptor t = new TestAdaptor(null, "test", "DESCRIPTION", schemes, locations, supportedProperties, prop);

        OctopusPropertyDescription[] p = t.getSupportedProperties();

        assert (p != null);
        assert (p.length == 2);
        assert (p[0].getName().equals("octopus.adaptors.test.p1"));
        assert (p[0].getDefaultValue().equals("aap2"));
        assert (p[1].getName().equals("octopus.adaptors.test.p2"));
        assert (p[1].getDefaultValue().equals("noot2"));
    }

    @Test
    public void test2() throws OctopusException {

        ImmutableArray<String> schemes = new ImmutableArray<String>("SCHEME1", "SCHEME2");
        ImmutableArray<String> locations = new ImmutableArray<String>("L1", "L2");

        ImmutableArray<OctopusPropertyDescription> supportedProperties = new ImmutableArray<OctopusPropertyDescription>(
                new OctopusPropertyDescriptionImplementation("octopus.adaptors.test.p1", Type.STRING, EnumSet.of(Component.OCTOPUS),
                        "aap2", "test property p1"),

                new OctopusPropertyDescriptionImplementation("octopus.adaptors.test.p2", Type.STRING, EnumSet.of(Component.OCTOPUS),
                        "noot2", "test property p2"));

        Map<String, String> m = new HashMap<>();
        m.put("octopus.adaptors.test.p1", "mies");
        m.put("octopus.adaptors.test.p2", "zus");

        OctopusProperties prop = new OctopusProperties(supportedProperties, new HashMap<String, String>());
        TestAdaptor t = new TestAdaptor(null, "test", "DESCRIPTION", schemes, locations, supportedProperties, prop);

        OctopusPropertyDescription[] p = t.getSupportedProperties();

        assert (p != null);
        assert (p.length == 2);
        assert (p[0].getName().equals("octopus.adaptors.test.p1"));
        assert (p[0].getDefaultValue().equals("mies"));
        assert (p[1].getName().equals("octopus.adaptors.test.p2"));
        assert (p[1].getDefaultValue().equals("zus"));
    }

    @Test(expected = OctopusException.class)
    public void test3() throws OctopusException {

        ImmutableArray<String> schemes = new ImmutableArray<String>("SCHEME1", "SCHEME2");
        ImmutableArray<String> locations = new ImmutableArray<String>("L1", "L2");

        ImmutableArray<OctopusPropertyDescription> supportedProperties = new ImmutableArray<OctopusPropertyDescription>(
                new OctopusPropertyDescriptionImplementation("octopus.adaptors.test.p1", Type.STRING, EnumSet.of(Component.OCTOPUS),
                        "aap2", "test property p1"),

                new OctopusPropertyDescriptionImplementation("octopus.adaptors.test.p2", Type.STRING, EnumSet.of(Component.OCTOPUS),
                        "noot2", "test property p2"));

        Map<String, String> p = new HashMap<>();
        p.put("octopus.adaptors.test.p3", "mies");

        OctopusProperties prop = new OctopusProperties(supportedProperties, p);

        new TestAdaptor(null, "test", "DESCRIPTION", schemes, locations, supportedProperties, prop);
    }

}