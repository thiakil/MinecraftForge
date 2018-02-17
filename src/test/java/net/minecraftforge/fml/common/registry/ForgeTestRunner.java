package net.minecraftforge.fml.common.registry;

import com.google.common.collect.Sets;
import net.minecraft.launchwrapper.LogWrapper;
import org.apache.logging.log4j.Level;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Uses {@code ResettingClassLoader} to load the test class. Minecraft and Forge
 * classes are loaded using the separate class loader.
 *
 * Use of a separate class loader allows classes to be reloaded for each test
 * class, which is handy when you're testing frameworks that make use of static
 * members.
 *
 * The selective quarantining is required because if the test class and its
 * 'children' are all loaded by a different class loader, then the {@code Test}
 * annotations yield different {@code Class} instances. JUnit then thinks there
 * are no runnable methods, because it looks them up by Class.
 *
 * This is a simplified copy of https://github.com/BinaryTweed/quarantining-test-runner
 * tailored for Minecraft use.
 *
 */
public class ForgeTestRunner extends Runner
{
    private final Object innerRunner;
    private final Class<?> innerRunnerClass;

    public ForgeTestRunner(Class<?> testFileClass) throws InitializationError
    {
        Class<? extends Runner> delegateRunningTo = JUnit4.class;

        String testFileClassName = testFileClass.getName();
        String delegateRunningToClassName = delegateRunningTo.getName();

        String[] allPatterns = new String[]{testFileClassName, delegateRunningToClassName};

        ResettingClassLoader classLoader = new ResettingClassLoader(allPatterns);

        try
        {
            innerRunnerClass = classLoader.loadClass(delegateRunningToClassName);
            Class<?> testClass = classLoader.loadClass(testFileClassName);
            innerRunner = innerRunnerClass.cast(innerRunnerClass.getConstructor(Class.class).newInstance(testClass));
        }
        catch (Exception e)
        {
            throw new InitializationError(e);
        }
    }


    @Override
    public Description getDescription()
    {
        try
        {
            return (Description) innerRunnerClass.getMethod("getDescription").invoke(innerRunner);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not get description", e);
        }
    }

    @Override
    public void run(RunNotifier notifier)
    {
        try
        {
            System.setProperty("forge.disableVanillaGameData", "false");
            innerRunnerClass.getMethod("run", RunNotifier.class).invoke(innerRunner, notifier);
        }
        catch (Exception e)
        {
            notifier.fireTestFailure(new Failure(getDescription(), e));
        }
    }

    /**
     * If a class name starts with any of the supplied patterns, it is loaded by
     * <em>this</em> classloader; otherwise it is loaded by the parent classloader.
     */
    private static class ResettingClassLoader extends URLClassLoader
    {
        private final Set<String> quarantinedClassNames;

        /**
         * @param quarantinedClassNames prefixes to match against when deciding how to load a class
         */
        public ResettingClassLoader(String... quarantinedClassNames)
        {
            super(getClassloaderURLs());

            this.quarantinedClassNames = Sets.newHashSet();
            Collections.addAll(this.quarantinedClassNames, quarantinedClassNames);
            Collections.addAll(this.quarantinedClassNames, "net.minecraft", "net.minecraftforge");
        }


        /**
         * If a class name starts with any of the supplied patterns, it is loaded by
         * <em>this</em> classloader; otherwise it is loaded by the parent classloader.
         *
         * @param name class to load
         */
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException
        {
            boolean quarantine = false;

            for (String quarantinedPattern : quarantinedClassNames)
            {
                if (name.startsWith(quarantinedPattern))
                {
                    quarantine = true;
                    break;
                }
            }

            //Avoid JVM duplicate class def error
            if (name.equals("net.minecraft.launchwrapper.LaunchClassLoader")){
                quarantine = false;
            }

            if (quarantine)
            {
                try
                {
                    return findClass(name);
                }
                catch (ClassNotFoundException e)
                {
                    throw e;
                }
            }

            return super.loadClass(name);
        }

        private static URL[] getClassloaderURLs(){
            URL[] urls;
            if (ResettingClassLoader.class.getClassLoader() instanceof URLClassLoader){
                urls = ((URLClassLoader) ResettingClassLoader.class.getClassLoader()).getURLs();
            } else {
                String classPath = appendPath(System.getProperty("java.class.path"), System.getProperty("env.class.path"));
                urls = pathToURLs(classPath);
            }
            return urls;
        }

        private static URL fileToURL(File file) {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                LogWrapper.log(Level.ERROR, e, "Could not convert {} to URL", file.toString());
                return null;
            }
        }

        //the following 2 methods are largely copied from internal sun.* classes
        public static String appendPath(String pathTo, String pathFrom) {
            if (pathTo == null || pathTo.length() == 0) {
                return pathFrom;
            } else if (pathFrom == null || pathFrom.length() == 0) {
                return pathTo;
            } else {
                return pathTo  + File.pathSeparator + pathFrom;
            }
        }

        public static URL[] pathToURLs(String path) {
            StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
            URL[] urls = new URL[st.countTokens()];
            int count = 0;
            while (st.hasMoreTokens()) {
                URL url = fileToURL(new File(st.nextToken()));
                if (url != null) {
                    urls[count++] = url;
                }
            }
            if (urls.length != count) {
                URL[] tmp = new URL[count];
                System.arraycopy(urls, 0, tmp, 0, count);
                urls = tmp;
            }
            return urls;
        }
    }
}
