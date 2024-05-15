package IO;

import PY_Helper.LogPrint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.HashMap;

/**
 * Helper class for reading and writing JSON files;
 * Includes static methods to parse JSON objects and arrays to Java data structures
 *
 * @author P. Le Bras
 * @version 2
 */
public class JSONHelper {

    /**
     * Parses a JSONArray containing integers and returns an integer array
     * @param array JSON Array to parse
     * @return The array containing integers
     */
    public static int[] getIntArray(JSONArray array){
        int[] res = new int[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = Math.toIntExact((long) array.get(i));
        return res;
    }

    /**
     * Parses a JSONArray containing floats and returns a double array
     * @param array JSON Array to parse
     * @return The array containing doubles
     */
    public static double[] getDoubleArray(JSONArray array){
        double[] res = new double[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = (double) array.get(i);
        return res;
    }

    /**
     * Parses a JSONArray containing strings and returns a String array
     * @param array JSON Array to parse
     * @return The array containing Strings
     */
    public static String[] getStringArray(JSONArray array){
        String[] res = new String[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = (String) array.get(i);
        return res;
    }

    /**
     * Parses a JSONArray containing nested JSONArrays and returns an array listing JSONArrays
     * @param array JSON Array to parse
     * @return The array containing JSONArrays
     */
    public static JSONArray[] getJSONArrayArray(JSONArray array){
        JSONArray[] res = new JSONArray[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = (JSONArray) array.get(i);
        return res;
    }

    /**
     * Parses a JSONArray containing nested JSONObjects and returns an array listing JSONObjects
     * @param array JSON Array to parse
     * @return The array containing JSONObjects
     */
    public static JSONObject[] getJSONObjectArray(JSONArray array){
        JSONObject[] res = new JSONObject[array.size()];
        for(int i = 0; i < array.size(); i++)
            res[i] = (JSONObject) array.get(i);
        return res;
    }

    /**
     * Parses a JSONObject containing (string: integer) pairs and returns it as a HashMap
     * @param map JSON Object to parse
     * @return The [String, Integer] HashMap
     */
    public static HashMap<String, Integer> getIntMap(JSONObject map){
        HashMap<String, Integer> res = new HashMap<String, Integer>();
        for(String key: (Iterable<String>) map.keySet()){
            res.put(key, (int) map.get(key));
        }
        return res;
    }

    /**
     * Parses a JSONObject containing (string: float) pairs and returns it as a HashMap
     * @param map JSON Object to parse
     * @return The [String, Double] HashMap
     */
    public static HashMap<String, Double> getDoubleMap(JSONObject map){
        HashMap<String, Double> res = new HashMap<String, Double>();
        for(String key: (Iterable<String>) map.keySet()){
            res.put(key, (double) map.get(key));
        }
        return res;
    }

    /**
     * Parses a JSONObject containing (string: string) pairs and returns it as a HashMap
     * @param map JSON Object to parse
     * @return The [String, String] HashMap
     */
    public static HashMap<String, String> getStringMap(JSONObject map){
        HashMap<String, String> res = new HashMap<String, String>();
        for(String key: (Iterable<String>) map.keySet()){
            res.put(key, (String) map.get(key));
        }
        return res;
    }

    /**
     * Parses a JSONObject containing (string: object/array) pairs and returns it as a HashMap
     * @param map JSON Object to parse
     * @return The [String, Object] HashMap (Object can then be cast as JSONObject or JSONArray)
     */
    public static HashMap<String, Object> getJSONObjectMap(JSONObject map){
        HashMap<String, Object> res = new HashMap<String, Object>();
        for(String key: (Iterable<String>) map.keySet()){
            res.put(key, map.get(key));
        }
        return res;
    }

    private static JSONObject loadObject(String filename) throws IOException, ParseException{
        JSONParser parser = new JSONParser();
        FileReader file = new FileReader(filename);
        return (JSONObject) parser.parse(file);
    }

    private static JSONArray loadArray(String filename) throws IOException, ParseException{
        JSONParser parser = new JSONParser();
        FileReader file = new FileReader(filename);
        return (JSONArray) parser.parse(file);
    }

    private static void saveObject(JSONObject obj, String filename) throws IOException{
        File file = new File(filename);
        file.getParentFile().mkdirs();
        FileWriter writer = new FileWriter(file);
        String str = obj.toJSONString();
        writer.write(str);
        writer.flush();
    }

    private static void saveArray(JSONArray obj, String filename) throws IOException{
        File file = new File(filename);
        file.getParentFile().mkdirs();
        FileWriter writer = new FileWriter(file);
        String str = obj.toJSONString();
        writer.write(str);
        writer.flush();
    }

    /**
     * Reads a JSON file and save it into a JSONObject instance
     * @param filename JSON file name
     * @return The JSONObject
     * @throws IOException If the file isn't found
     * @throws ParseException If the JSON parser fails
     */
    public static JSONObject loadJSON(String filename) throws IOException, ParseException{
        Console.log("Loading "+filename);
        JSONObject root = loadObject(filename);
        Console.tick();
        return root;
    }

    /**
     * Method to read a JSON file containing an array and save it into a JSONArray instance
     * @param filename JSON file name
     * @return The JSONArray
     * @throws IOException If the file isn't found
     * @throws ParseException If the JSON parser fails
     */
    public static JSONArray loadJSONArray(String filename) throws IOException, ParseException{
        Console.log("Loading "+filename);
        JSONArray root = loadArray(filename);
        Console.tick();
        return root;
    }

    /**
     * Writes a JSONObject instance onto a JSON file
     * @param obj JSONObject to write
     * @param filename JSON file name
     * @throws IOException If the file cannot be created/written
     */
    public static void saveJSON(JSONObject obj, String filename) throws IOException{
        Console.log("Saving "+filename);
        saveObject(obj, filename);
        Console.tick();
    }

    /**
     * Writes a JSONArray instance onto a JSON file
     * @param obj JSONArray to write
     * @param filename JSON file name
     * @throws IOException If the file cannot be created/written
     */
    public static void saveJSONArray(JSONArray obj, String filename) throws IOException{
        Console.log("Saving "+filename);
        saveArray(obj, filename);
        Console.tick();
    }

    /**
     * Method to read a JSON file and save it into a JSONObject instance
     * @param filename JSON file name
     * @param depth Depth level for logs
     * @return The JSONObject
     * @throws IOException If the file isn't found
     * @throws ParseException If the JSON parser fails
     */
    public static JSONObject loadJSON(String filename, int depth) throws IOException, ParseException{
        Console.log("Loading "+filename, depth);
        JSONObject root = loadObject(filename);
        Console.tick();
        return root;
    }

    /**
     * Method to read a JSON file containing an array and save it into a JSONArray instance
     * @param filename JSON file name
     * @param depth Depth level for logs
     * @return The JSONArray
     * @throws IOException If the file isn't found
     * @throws ParseException If the JSON parser fails
     */
    public static JSONArray loadJSONArray(String filename, int depth) throws IOException, ParseException{
        Console.log("Loading "+filename, depth);
        JSONArray root = loadArray(filename);
        Console.tick();
        return root;
    }

    /**
     * Writes a JSONObject instance onto a JSON file
     * @param obj JSONObject to write
     * @param filename JSON file name
     * @param depth Depth level for logs
     * @throws IOException If the file cannot be created/written
     */
    public static void saveJSON(JSONObject obj, String filename, int depth) throws IOException{
        Console.log("Saving "+filename, depth);
        saveObject(obj, filename);
        Console.tick();
    }

    /**
     * Writes a JSONArray instance onto a JSON file
     * @param obj JSONArray to write
     * @param filename JSON file name
     * @param depth Depth level for logs
     * @throws IOException If the file cannot be created/written
     */
    public static void saveJSONArray(JSONArray obj, String filename, int depth) throws IOException{
        Console.log("Saving "+filename, depth);
        saveArray(obj, filename);
        Console.tick();
    }
}
