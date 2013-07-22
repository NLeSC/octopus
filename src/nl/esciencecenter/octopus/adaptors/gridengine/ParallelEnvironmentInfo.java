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
package nl.esciencecenter.octopus.adaptors.gridengine;

/**
 * Class that holds some info on parallel environments used in Grid Engine.
 * 
 * @author Niels Drost
 * 
 */
class ParallelEnvironmentInfo {

    private final String name;
    private final int slots;
    private final String allocationRule;

    public ParallelEnvironmentInfo(String name, int slots, String allocationRule) {
        this.name = name;
        this.slots = slots;
        this.allocationRule = allocationRule;
    }

    public String getName() {
        return name;
    }

    public int getSlots() {
        return slots;
    }

    public String getAllocationRule() {
        return allocationRule;
    }

    @Override
    public String toString() {
        return "ParallelEnvironmentInfo [name=" + name + ", slots=" + slots + ", allocationRule=" + allocationRule + "]";
    }
}