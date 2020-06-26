package PX_Data;

import PY_Helper.LogPrint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class JSONIOWrapper {

    public static int[] getIntArray(JSONArray array){
        int[] res = new int[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = Math.toIntExact((long) array.get(i));
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

    public static JSONArray[] getJSONArrayArray(JSONArray array){
        JSONArray[] res = new JSONArray[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = (JSONArray) array.get(i);
        return res;
    }

    public static JSONObject[] getJSONObjectArray(JSONArray array){
        JSONObject[] res = new JSONObject[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = (JSONObject) array.get(i);
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

    public static HashMap<String, Object> getJSONObjectMap(JSONObject map){
        HashMap<String, Object> res = new HashMap<String, Object>();
        for(String key: (Iterable<String>) map.keySet()){
            res.put(key, map.get(key));
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
        try {
            File file = new File(filename);
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);
            String str = obj.toJSONString();
            writer.write(str);
            writer.flush();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        finally {
            System.out.println(filename+" Saved!");
        }
    }

    public static JSONObject LoadJSON(String filename, int depth){
        LogPrint.printNewStep("Loading "+filename, depth);

        JSONObject root = new JSONObject();

        JSONParser parser = new JSONParser();
        try (FileReader file = new FileReader(filename)){
            root = (JSONObject) parser.parse(file);
            LogPrint.printCompleteStep();
        }
        catch (IOException | ParseException e){
            LogPrint.printNoteError("Error while loading "+filename);
            e.printStackTrace();
        }
        return root;
    }

    public static void SaveJSON(JSONObject obj, String filename, int depth){
        LogPrint.printNewStep("Saving "+filename, depth);
        try {
            File file = new File(filename);
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);
            String str = obj.toJSONString();
            writer.write(str);
            writer.flush();

            LogPrint.printCompleteStep();
        }
        catch(IOException e){
            LogPrint.printNoteError("Error while saving "+filename);
            e.printStackTrace();
        }
    }
}
