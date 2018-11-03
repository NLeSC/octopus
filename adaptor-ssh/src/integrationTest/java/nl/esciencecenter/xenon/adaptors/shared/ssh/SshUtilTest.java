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
package nl.esciencecenter.xenon.adaptors.shared.ssh;

import org.apache.sshd.client.SshClient;
import org.junit.Test;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.credentials.Credential;
import nl.esciencecenter.xenon.credentials.PasswordCredential;

public abstract class SshUtilTest {

    public abstract String getLocation();

    public abstract Credential getCorrectCredential();

    // NOTE: We can only perform tests here where known_hosts is ignored. Otherwise we have no control over its content!
    @Test
    public void test_connect_no_config_no_host_check() throws Exception {
        SshClient client = SSHUtil.createSSHClient(false, false, false, false, false);
        SSHConnection session = SSHUtil.connect("test", client, getLocation(), getCorrectCredential(), 0, 10 * 1000);
        session.close();
    }

    // @Test
    // public void test_connect_no_config_with_host_check() throws Exception {
    // SshClient client = SSHUtil.createSSHClient(false, true, false, false, false);
    // ClientSession session = SSHUtil.connect("test", client, getLocation(), getCorrectCredential(), 10 * 1000, 0);
    // session.close();
    // }

    // @Test
    // public void test_connect_no_config_with_host_check_and_add() throws Exception {
    // SshClient client = SSHUtil.createSSHClient(false, true, true, false, false);
    // ClientSession session = SSHUtil.connect("test", client, getLocation(), getCorrectCredential(), 10 * 1000, 0);
    // session.close();
    // }

    @Test(expected = IllegalArgumentException.class)
    public void test_connect_FailsNullCredential() throws Exception {
        SshClient client = SSHUtil.createSSHClient(false, false, false, false, false);
        SSHUtil.connect("test", client, getLocation(), null, 0, 10 * 1000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_connect_FailsInvalidTimeout() throws Exception {
        SshClient client = SSHUtil.createSSHClient(false, false, false, false, false);
        SSHUtil.connect("test", client, getLocation(), getCorrectCredential(), 0, -1);
    }

    @Test(expected = XenonException.class)
    public void test_connect_FailsUsernameNull() throws Exception {
        SshClient client = SSHUtil.createSSHClient(false, false, false, false, false);
        SSHUtil.connect("test", client, getLocation(), new PasswordCredential(null, "foobar".toCharArray()), 0, 10 * 1000);
    }

}
