package com.getsimplex.steptimer.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

/**
 * Created by Administrator on 1/12/2016.
 */
public class Configuration {

    private static Boolean configFileOnClassPath = false;
    public static Config getConfiguration(){
        Config config = ConfigFactory.load();
        configFileOnClassPath = false;
        try{

            configFileOnClassPath = "true".equals(config.getString("simplex"));

        } catch (Exception e){ //we didn't find any application.conf files on the classpath (example, standalone jar)

            File configFile = new File("/Applications/steptimerwebsocket/application.conf");
            config = ConfigFactory.parseFile(configFile);
        }
        return config;
    }

    public static Boolean isConfigFileOnClassPath(){
        return configFileOnClassPath;
    }
}

