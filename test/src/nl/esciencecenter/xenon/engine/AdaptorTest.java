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

package nl.esciencecenter.xenon.engine;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.XenonPropertyDescription;
import nl.esciencecenter.xenon.XenonPropertyDescription.Component;
import nl.esciencecenter.xenon.XenonPropertyDescription.Type;
import nl.esciencecenter.xenon.credentials.Credentials;
import nl.esciencecenter.xenon.engine.Adaptor;
import nl.esciencecenter.xenon.engine.XenonEngine;
import nl.esciencecenter.xenon.engine.XenonProperties;
import nl.esciencecenter.xenon.engine.XenonPropertyDescriptionImplementation;
import nl.esciencecenter.xenon.engine.util.ImmutableArray;
import nl.esciencecenter.xenon.files.Files;
import nl.esciencecenter.xenon.jobs.Jobs;

import org.junit.Test;

/**
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * 
 */
public class AdaptorTest {

    class TestAdaptor extends Adaptor {

        public TestAdaptor(XenonEngine xenonEngine, String name, String description, ImmutableArray<String> supportedSchemes,
                ImmutableArray<String> supportedLocations, ImmutableArray<XenonPropertyDescription> validProperties, 
                XenonProperties p) throws XenonException {

            super(xenonEngine, name, description, supportedSchemes, supportedLocations, validProperties, p);
        }

        @Override
        public Map<String, String> getAdaptorSpecificInformation() {
            return null;
        }

        @Override
        public Files filesAdaptor() throws XenonException {
            return null;
        }

        @Override
        public Jobs jobsAdaptor() throws XenonException {
            return null;
        }

        @Override
        public Credentials credentialsAdaptor() throws XenonException {
            return null;
        }

        @Override
        public void end() {
        }

    }

    @Test
    public void test0() throws XenonException {

        ImmutableArray<String> schemes = new ImmutableArray<String>("SCHEME1", "SCHEME2");
        ImmutableArray<String> locations = new ImmutableArray<String>("L1", "L2");

        TestAdaptor t = new TestAdaptor(null, "test", "DESCRIPTION", schemes, locations, 
                new ImmutableArray<XenonPropertyDescription>(), new XenonProperties());

        String[] tmp = t.getSupportedSchemes();

        assert (tmp != null);
        assert (Arrays.equals(schemes.asArray(), tmp));
    }

    @Test
    public void test1() throws XenonException {

        ImmutableArray<String> schemes = new ImmutableArray<String>("SCHEME1", "SCHEME2");
        ImmutableArray<String> locations = new ImmutableArray<String>("L1", "L2");

        ImmutableArray<XenonPropertyDescription> supportedProperties = new ImmutableArray<XenonPropertyDescription>(
                new XenonPropertyDescriptionImplementation("xenon.adaptors.test.p1", Type.STRING, EnumSet.of(Component.XENON),
                        "aap2", "test property p1"),

                new XenonPropertyDescriptionImplementation("xenon.adaptors.test.p2", Type.STRING, EnumSet.of(Component.XENON),
                        "noot2", "test property p2"));

        XenonProperties prop = new XenonProperties(supportedProperties, new HashMap<String, String>());
        TestAdaptor t = new TestAdaptor(null, "test", "DESCRIPTION", schemes, locations, supportedProperties, prop);

        XenonPropertyDescription[] p = t.getSupportedProperties();

        assert (p != null);
        assert (p.length == 2);
        assert (p[0].getName().equals("xenon.adaptors.test.p1"));
        assert (p[0].getDefaultValue().equals("aap2"));
        assert (p[1].getName().equals("xenon.adaptors.test.p2"));
        assert (p[1].getDefaultValue().equals("noot2"));
    }

    @Test
    public void test2() throws XenonException {

        ImmutableArray<String> schemes = new ImmutableArray<String>("SCHEME1", "SCHEME2");
        ImmutableArray<String> locations = new ImmutableArray<String>("L1", "L2");

        ImmutableArray<XenonPropertyDescription> supportedProperties = new ImmutableArray<XenonPropertyDescription>(
                new XenonPropertyDescriptionImplementation("xenon.adaptors.test.p1", Type.STRING, EnumSet.of(Component.XENON),
                        "aap2", "test property p1"),

                new XenonPropertyDescriptionImplementation("xenon.adaptors.test.p2", Type.STRING, EnumSet.of(Component.XENON),
                        "noot2", "test property p2"));

        Map<String, String> m = new HashMap<>();
        m.put("xenon.adaptors.test.p1", "mies");
        m.put("xenon.adaptors.test.p2", "zus");

        XenonProperties prop = new XenonProperties(supportedProperties, new HashMap<String, String>());
        TestAdaptor t = new TestAdaptor(null, "test", "DESCRIPTION", schemes, locations, supportedProperties, prop);

        XenonPropertyDescription[] p = t.getSupportedProperties();

        assert (p != null);
        assert (p.length == 2);
        assert (p[0].getName().equals("xenon.adaptors.test.p1"));
        assert (p[0].getDefaultValue().equals("mies"));
        assert (p[1].getName().equals("xenon.adaptors.test.p2"));
        assert (p[1].getDefaultValue().equals("zus"));
    }

    @Test(expected = XenonException.class)
    public void test3() throws XenonException {

        ImmutableArray<String> schemes = new ImmutableArray<String>("SCHEME1", "SCHEME2");
        ImmutableArray<String> locations = new ImmutableArray<String>("L1", "L2");

        ImmutableArray<XenonPropertyDescription> supportedProperties = new ImmutableArray<XenonPropertyDescription>(
                new XenonPropertyDescriptionImplementation("xenon.adaptors.test.p1", Type.STRING, EnumSet.of(Component.XENON),
                        "aap2", "test property p1"),

                new XenonPropertyDescriptionImplementation("xenon.adaptors.test.p2", Type.STRING, EnumSet.of(Component.XENON),
                        "noot2", "test property p2"));

        Map<String, String> p = new HashMap<>();
        p.put("xenon.adaptors.test.p3", "mies");

        XenonProperties prop = new XenonProperties(supportedProperties, p);

        new TestAdaptor(null, "test", "DESCRIPTION", schemes, locations, supportedProperties, prop);
    }

}
