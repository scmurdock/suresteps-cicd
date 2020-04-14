package com.getsimplex.steptimer.utils;


import com.typesafe.config.Config;
import redis.clients.jedis.Jedis;

import java.util.Set;

/**
 * Created by Admin on 8/18/2016.
 */
public class JedisClient {

    private static Config config = Configuration.getConfiguration();
    private static String password = config.getString("redis.password");
    private static String host = config.getString("redis.host");
    private static String port = config.getString("redis.port");
    private static String dbName = config.getString("redis.db");
    private static String url = "redis://:"+password+"@"+host+":"+port+"/"+dbName;
    private static Jedis jedis  = new Jedis(url);

    public static Jedis getJedis(){
        try{
            jedis.ping();
        }

        catch (Exception e){
            jedis = new Jedis(url);
        }
    return jedis;
    }

    public void set(String key, String value) throws Exception{
        int tries =0;
        try{
            tries ++;
            jedis.set(key,value);
        } catch(Exception e){
            if (tries<1000) {
                getJedis();
                set(key, value);
            } else{
                throw new Exception ("Tried 1000 times setting key:"+key+ " and value:"+value+" without success");
            }
        }
    }

    public  Boolean exists(String key) throws Exception{
        int tries =0;
        try{
            tries++;
            return jedis.exists(key);
        }

        catch (Exception e ){
            if (tries<1000)
            {
                getJedis();
                return exists(key);
            }

            else {
                throw new Exception ("Tried 1000 times exists on key:"+key+" without success");
            }
        }
    }

    public Set<String> zrange(String key, int start, int end) throws Exception{
        int tries =0;
        try {
            tries ++;
            return jedis.zrange(key,start,end);
        }

        catch (Exception e){
            if (tries<1000)
            {
                getJedis();
                return zrange(key,start,end);
            }
            else{
                throw new Exception("Tried 1000 times to get range:"+key+" start:"+start+" end:"+end+" without success");
            }
        }
    }

    public String get(String key) throws Exception{
        int tries = 0;
        try{
            tries ++;
            return jedis.get(key);
        }
        catch (Exception e){
            if (tries<1000) {
                getJedis();
                return get(key);
            }else{
                throw new Exception("Tried 1000 times to get key:"+key+" without success");
            }

        }
    }

    public void disconnect(){
        jedis.disconnect();
    }

}
