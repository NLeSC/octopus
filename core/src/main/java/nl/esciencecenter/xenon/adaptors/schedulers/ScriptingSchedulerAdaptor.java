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
package nl.esciencecenter.xenon.adaptors.schedulers;

import nl.esciencecenter.xenon.UnknownAdaptorException;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.XenonPropertyDescription;
import nl.esciencecenter.xenon.schedulers.Scheduler;

public abstract class ScriptingSchedulerAdaptor extends SchedulerAdaptor {

    protected ScriptingSchedulerAdaptor(String name, String description, String[] locations, XenonPropertyDescription[] properties) throws XenonException {
        super(name, description, locations, properties);
    }

    @Override
    public XenonPropertyDescription[] getSupportedProperties() {
        try {
            return ScriptingUtils.mergeValidProperties(super.getSupportedProperties(), Scheduler.getAdaptorDescription("ssh").getSupportedProperties(),
                    Scheduler.getAdaptorDescription("local").getSupportedProperties());
        } catch (UnknownAdaptorException e) {
            return getSupportedProperties();
        }
    }
}
