package nl.esciencecenter.octopus.tests.engine;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.jar.JarFile;

import nl.esciencecenter.octopus.engine.loader.JarFileSystem;
import nl.esciencecenter.octopus.engine.loader.JarFsClassLoader;

public class JarLoaderTest {

    @org.junit.Test
    public void test1() throws Exception {
        JarFileSystem fs = new JarFileSystem(new JarFile(new File(
                "dist/octopus-adaptor-local.jar")));
    }

    /**
     * This depends on the additional adaptors
     * @throws Exception
     */
    @org.junit.Test
    @org.junit.Ignore
    public void test2() throws Exception {
        HashSet<JarFileSystem> fileSystems = new HashSet<JarFileSystem>();

        //fileSystems.add(new JarFileSystem(new JarFile(new File(
        //        "lib/estep-deploy-adaptors-additional.jar"))));

        fileSystems.add(new JarFileSystem(new JarFile(new File(
                "dist/octopus-adaptor-local.jar"))));

        JarFsClassLoader sharedLoader = new JarFsClassLoader(fileSystems, "Shared");

        JarFsClassLoader loader = new JarFsClassLoader(fileSystems, "Globus", sharedLoader);

        Class theclass = loader.loadClass("org.apache.webdav.lib.methods.XMLResponseMethodBase");

        System.err.println("fields: " + Arrays.toString(theclass.getFields()));

        System.err.println("Classes: " + Arrays.toString(theclass.getDeclaredClasses()));

        System.err.println("Package: " + theclass.getPackage());

        InputStream in = loader.getResourceAsStream("Xerces_Ver_3_0_0EA3.info");

        byte[] buffer = new byte[10240];

        int read = in.read(buffer);

        System.out.write(buffer, 0, read);
        System.out.println();

        InputStream in2 = loader.getResourceAsStream("bouncycastle.LICENSE");

        read = in2.read(buffer);

        System.out.write(buffer, 0, read);
        System.out.println();
    }

}
