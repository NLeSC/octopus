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
package nl.esciencecenter.xenon.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ClearSystemProperties;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

public class LocalFileSystemUtilsTestUserHomeUnset {

    @Rule
    public final ClearSystemProperties p3 = new ClearSystemProperties("user.home");

    @Rule
    public final ProvideSystemProperty p4 = new ProvideSystemProperty("user.name", "stefan");

    @Test
    public void test_noexpand_tilde_withPath() throws Exception {
        String result = LocalFileSystemUtils.expandTilde("~/filesystem-test-fixture/links/file0");
        assertEquals("~/filesystem-test-fixture/links/file0", result);
    }

    @Test
    public void test_expand_tilde() throws Exception {
        String result = LocalFileSystemUtils.expandTilde("~");
        assertEquals("~", result);
    }

    @Test
    public void test_expand_tilde_null() throws Exception {
        String result = LocalFileSystemUtils.expandTilde(null);
        assertNull(result);
    }

    @Test
    public void test_expand_tilde_notilde() throws Exception {
        String result = LocalFileSystemUtils.expandTilde("/aap/noot");
        assertEquals("/aap/noot", result);
    }

    @Test
    public void test_expand_tilde_wrongspot() throws Exception {
        String result = LocalFileSystemUtils.expandTilde("/aap/noot/~/mies");
        assertEquals("/aap/noot/~/mies", result);
    }

    @Test
    public void test_expand_tilde_userName() throws Exception {
        String result = LocalFileSystemUtils.expandTilde("~stefan/dir");
        assertEquals("~stefan/dir", result);
    }
}
