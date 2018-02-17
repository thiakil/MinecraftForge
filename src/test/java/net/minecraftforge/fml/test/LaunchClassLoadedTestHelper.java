package net.minecraftforge.fml.test;

import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.launchwrapper.LogWrapper;
import net.minecraftforge.test.BiomeSpawnableListTest;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;

/**
 * Created by Thiakil on 17/02/2018.
 */
public class LaunchClassLoadedTestHelper
{
    private Class<?> innerClass;
    private Object innerInstance;

    public LaunchClassLoadedTestHelper(String innerClassName, String... requiredTransformers) throws Exception
    {
        //Enumhelper uses ASM, need to load it with the LaunchClassLoader
        LaunchClassLoader classLoader = new LaunchClassLoader(getClassloaderURLs());
        for (String t : requiredTransformers)
        {
            classLoader.addTransformerExclusion(t);
            classLoader.registerTransformer(t);
        }
        innerClass = classLoader.findClass(innerClassName);
        innerInstance = innerClass.getConstructors()[0].newInstance();
    }

    public void runTestMethod(String methodName) throws Exception
    {
        innerClass.getDeclaredMethod(methodName).invoke(innerInstance);
    }

    private static URL[] getClassloaderURLs(){
        URL[] urls;
        if (BiomeSpawnableListTest.class.getClassLoader() instanceof URLClassLoader){
            urls = ((URLClassLoader) BiomeSpawnableListTest.class.getClassLoader()).getURLs();
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
    private static String appendPath(String pathTo, String pathFrom) {
        if (pathTo == null || pathTo.length() == 0) {
            return pathFrom;
        } else if (pathFrom == null || pathFrom.length() == 0) {
            return pathTo;
        } else {
            return pathTo  + File.pathSeparator + pathFrom;
        }
    }

    private static URL[] pathToURLs(String path) {
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
