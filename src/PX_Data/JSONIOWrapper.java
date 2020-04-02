package PX_Data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class JSONIOWrapper {

    public static int[] getIntArray(JSONArray array){
        int[] res = new int[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = (int) array.get(i);
        return res;
    }

    public static double[] getDoubleArray(JSONArray array){
        double[] res = new double[array.size()];
        for(int i = 0; i < array.size(); i++)
                res[i] = (double) array.get(i);
        return res;
    }

    public static String[] getStringArray(JSONArray array){
        String[] res = new String[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = (String) array.get(i);
        return res;
    }

    public static HashMap<String, Integer> getIntMap(JSONObject map){
        HashMap<String, Integer> res = new HashMap<String, Integer>();
        for(String key: (Iterable<String>) map.keySet()){
            res.put(key, (int) map.get(key));
        }
        return res;
    }

    public static HashMap<String, Double> getDoubleMap(JSONObject map){
        HashMap<String, Double> res = new HashMap<String, Double>();
        for(String key: (Iterable<String>) map.keySet()){
            res.put(key, (double) map.get(key));
        }
        return res;
    }

    public static HashMap<String, String> getStringMap(JSONObject map){
        HashMap<String, String> res = new HashMap<String, String>();
        for(String key: (Iterable<String>) map.keySet()){
            res.put(key, (String) map.get(key));
        }
        return res;
    }

    public static JSONObject LoadJSON(String filename){
        System.out.println("Loading "+filename+" ...");

        JSONObject root = new JSONObject();

        JSONParser parser = new JSONParser();
        try (FileReader file = new FileReader(filename)){
            root = (JSONObject) parser.parse(file);
        }
        catch (IOException | ParseException e){
            e.printStackTrace();
        }
        finally {
            System.out.println(filename+" Loaded!");
        }
        return root;
    }

    public static void SaveJSON(JSONObject obj, String filename){
        System.out.println("Saving "+filename+" ...");
        try (FileWriter file = new FileWriter(filename)){
            String str = obj.toJSONString();
            file.write(str);
            file.flush();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        finally {
            System.out.println(filename+" Saved!");
        }
    }
}
