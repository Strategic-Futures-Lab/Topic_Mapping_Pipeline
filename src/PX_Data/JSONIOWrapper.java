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

/**
 * This class provides static methods for reading and writing JSON files.
 * It also includes static methods to parse JSON objects and arrays (from {@link org.json.simple})
 * to Java data structures.
 *
 * @author P. Le Bras
 * @version 1
 */
public class JSONIOWrapper {

    /**
     * Method to parse a JSONArray containing integers and return an integer array.
     * @param array JSON Array to parse.
     * @return The array containing integers.
     */
    public static int[] getIntArray(JSONArray array){
        int[] res = new int[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = Math.toIntExact((long) array.get(i));
        return res;
    }

    /**
     * Method to parse a JSONArray containing floats and return a double array.
     * @param array JSON Array to parse.
     * @return The array containing doubles.
     */
    public static double[] getDoubleArray(JSONArray array){
        double[] res = new double[array.size()];
        for(int i = 0; i < array.size(); i++)
                res[i] = (double) array.get(i);
        return res;
    }

    /**
     * Method to parse a JSONArray containing strings and return a String array.
     * @param array JSON Array to parse.
     * @return The array containing Strings.
     */
    public static String[] getStringArray(JSONArray array){
        String[] res = new String[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = (String) array.get(i);
        return res;
    }

    /**
     * Method to parse a JSONArray containing nested JSONArrays and return an array listing JSONArrays.
     * @param array JSON Array to parse.
     * @return The array containing JSONArrays.
     */
    public static JSONArray[] getJSONArrayArray(JSONArray array){
        JSONArray[] res = new JSONArray[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = (JSONArray) array.get(i);
        return res;
    }

    /**
     * Method to parse a JSONArray containing nested JSONObjects and return an array listing JSONObjects.
     * @param array JSON Array to parse.
     * @return The array containing JSONObjects.
     */
    public static JSONObject[] getJSONObjectArray(JSONArray array){
        JSONObject[] res = new JSONObject[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = (JSONObject) array.get(i);
        return res;
    }

    /**
     * Method to parse a JSONObject containing (string: integer) pairs and return it as a HashMap.
     * @param map JSON Object to parse.
     * @return The [String, Integer] HashMap.
     */
    public static HashMap<String, Integer> getIntMap(JSONObject map){
        HashMap<String, Integer> res = new HashMap<String, Integer>();
        for(String key: (Iterable<String>) map.keySet()){
            res.put(key, (int) map.get(key));
        }
        return res;
    }

    /**
     * Method to parse a JSONObject containing (string: float) pairs and return it as a HashMap.
     * @param map JSON Object to parse.
     * @return The [String, Double] HashMap.
     */
    public static HashMap<String, Double> getDoubleMap(JSONObject map){
        HashMap<String, Double> res = new HashMap<String, Double>();
        for(String key: (Iterable<String>) map.keySet()){
            res.put(key, (double) map.get(key));
        }
        return res;
    }

    /**
     * Method to parse a JSONObject containing (string: string) pairs and return it as a HashMap.
     * @param map JSON Object to parse.
     * @return The [String, String] HashMap.
     */
    public static HashMap<String, String> getStringMap(JSONObject map){
        HashMap<String, String> res = new HashMap<String, String>();
        for(String key: (Iterable<String>) map.keySet()){
            res.put(key, (String) map.get(key));
        }
        return res;
    }

    /**
     * Method to parse a JSONObject containing (string: object/array) pairs and return it as a HashMap.
     * @param map JSON Object to parse.
     * @return The [String, Object] HashMap (Object can then be cast as JSONObject or JSONArray).
     */
    public static HashMap<String, Object> getJSONObjectMap(JSONObject map){
        HashMap<String, Object> res = new HashMap<String, Object>();
        for(String key: (Iterable<String>) map.keySet()){
            res.put(key, map.get(key));
        }
        return res;
    }

    /**
     * Method to read a JSON file and save it into a JSONObject instance.
     * @param filename JSON file name.
     * @return The JSONObject.
     */
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

    /**
     * Method to write a JSONObject instance onto a JSON file.
     * @param obj JSONObject to write.
     * @param filename JSON file name.
     */
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

    /**
     * Method to read a JSON file and save it into a JSONObject instance, uses {@link LogPrint} for logging.
     * @param filename JSON file name.
     * @param depth Depth level for logs.
     * @return The JSONObject.
     */
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

    /**
     * Method to read a JSON file containing an array and save it into a JSONArray instance, uses {@link LogPrint} for logging.
     * @param filename JSON file name.
     * @param depth Depth level for logs.
     * @return The JSONArray.
     */
    public static JSONArray LoadJSONArray(String filename, int depth){
        LogPrint.printNewStep("Loading "+filename, depth);

        JSONArray root = new JSONArray();

        JSONParser parser = new JSONParser();
        try (FileReader file = new FileReader(filename)){
            root = (JSONArray) parser.parse(file);
            LogPrint.printCompleteStep();
        }
        catch (IOException | ParseException e){
            LogPrint.printNoteError("Error while loading "+filename);
            e.printStackTrace();
        }
        return root;
    }

    /**
     * Method to write a JSONObject instance onto a JSON file, ses {@link LogPrint} for logging.
     * @param obj JSONObject to write.
     * @param filename JSON file name.
     * @param depth Depth level for logs.
     */
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

    /**
     * Method to write a JSONArray instance onto a JSON file, uses {@link LogPrint} for logging.
     * @param obj JSONArray to write.
     * @param filename JSON file name.
     * @param depth Depth level for logs.
     */
    public static void SaveJSONArray(JSONArray obj, String filename, int depth){
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
