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
package nl.esciencecenter.xenon.engine.jobs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import nl.esciencecenter.xenon.Util;
import nl.esciencecenter.xenon.engine.XenonEngine;
import nl.esciencecenter.xenon.engine.jobs.JobImplementation;
import nl.esciencecenter.xenon.engine.jobs.JobsEngine;
import nl.esciencecenter.xenon.engine.jobs.SchedulerImplementation;
import nl.esciencecenter.xenon.jobs.Job;
import nl.esciencecenter.xenon.jobs.JobDescription;
import nl.esciencecenter.xenon.jobs.JobStatus;
import nl.esciencecenter.xenon.jobs.Scheduler;

import org.junit.Test;

public class JobsEngineTest {

    @Test
    public void testToString() throws Exception {

        XenonEngine oe = Util.createXenonEngine(new HashMap<String, String>());
        JobsEngine je = new JobsEngine(oe);

        assertTrue(je.toString().equals("JobsEngine [xenonEngine=" + oe + "]"));
    }

    @Test
    public void testGetJobStatusesWithException() throws Exception {

        XenonEngine oe = Util.createXenonEngine(new HashMap<String, String>());
        JobsEngine je = new JobsEngine(oe);

        JobDescription desc = new JobDescription();

        Scheduler s = new SchedulerImplementation("test", "id1", "test", "", new String[] { "testq" }, null, null, true,
                true, true);

        Job job = new JobImplementation(s, "id1", desc, true, true);

        JobStatus[] status = je.getJobStatuses(job);

        assertNotNull(status);
        assertTrue(status.length == 1);
        assertNotNull(status[0]);
        assertTrue(status[0].hasException());
    }

    @Test
    public void testGetJobStatusesWithException2() throws Exception {

        XenonEngine oe = Util.createXenonEngine(new HashMap<String, String>());
        JobsEngine je = new JobsEngine(oe);

        JobDescription desc = new JobDescription();

        Scheduler s = new SchedulerImplementation("test1", "id1", "test", "", new String[] { "testq" }, null, null,
                true, true, true);

        Job job1 = new JobImplementation(s, "id1", desc, true, true);

        s = new SchedulerImplementation("test1", "id1", "test", "", new String[] { "testq" }, null, null, true, true,
                true);

        Job job2 = new JobImplementation(s, "id2", desc, true, true);

        JobStatus[] status = je.getJobStatuses(job1, null, job2);

        assertNotNull(status);
        assertTrue(status.length == 3);
        assertNotNull(status[0]);
        assertNull(status[1]);
        assertNotNull(status[2]);
        assertTrue(status[0].hasException());
        assertTrue(status[2].hasException());
    }

    //    
    //    
    //    
    //    @Test
    //    public void testJobsEngine() {
    //        fail("Not yet implemented");
    //    }
    //
    //    @Test
    //    public void testNewScheduler() {
    //        fail("Not yet implemented");
    //    }
    //
    //    @Test
    //    public void testGetJobStatus() {
    //        fail("Not yet implemented");
    //    }
    //
    //    @Test
    //    public void testGetJobStatuses() {
    //        fail("Not yet implemented");
    //    }
    //
    //    @Test
    //    public void testCancelJob() {
    //        fail("Not yet implemented");
    //    }
    //
    //    @Test
    //    public void testNewJobDescription() {
    //        fail("Not yet implemented");
    //    }
    //
    //    @Test
    //    public void testGetQueueNames() {
    //        fail("Not yet implemented");
    //    }
    //
    //    @Test
    //    public void testGetJobs() {
    //        fail("Not yet implemented");
    //    }

    //    @Test
    //    public void testSubmitJob_StubbedLocalAdaptor_LocalJob() throws XenonIOException, XenonException, URISyntaxException {
    //        URI sheduler_location = new URI("local:///");
    //        JobDescription job_description = new JobDescription();
    //        SchedulerImplementation scheduler = new SchedulerImplementation("local", "1", sheduler_location, 
    //                new String[] { "single" }, null, null, true, true, true);
    //        
    //        // stub adaptor
    //        XenonEngine xenon = mock(XenonEngine.class);
    //        Adaptor adaptor = mock(Adaptor.class);
    //        Jobs job_adaptor = mock(Jobs.class);
    //        Job expected_job = new JobImplementation(job_description, scheduler, UUID.randomUUID(), "1", false, true);
    //        when(xenon.getAdaptorFor("local")).thenReturn(adaptor);
    //        when(xenon.getAdaptor("local")).thenReturn(adaptor);
    //        when(adaptor.jobsAdaptor()).thenReturn(job_adaptor);
    //        when(job_adaptor.newScheduler(sheduler_location, null, null)).thenReturn(scheduler);
    //        when(job_adaptor.submitJob(scheduler, job_description)).thenReturn(expected_job);
    //        JobsEngine engine = new JobsEngine(xenon);
    //
    //        Job job = engine.submitJob(scheduler, job_description);
    //
    //        assertThat(job, is(expected_job));
    //    }
}
