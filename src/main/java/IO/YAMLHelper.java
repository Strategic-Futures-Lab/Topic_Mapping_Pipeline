package IO;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Helper class for loading and parsing data from a YAML file
 */
public class YAMLHelper {

    /**
     * Super class for parsing related exceptions
     */
    public static class YAMLParseException extends Exception{
        public YAMLParseException(String msg){ super(msg); }
    }

    /**
     * Exception related to keys (in maps/dictionaries) with the wrong type
     */
    public static class WrongKeyTypeException extends YAMLParseException{
        public WrongKeyTypeException(String msg){ super(msg); }
    }

    /**
     * Exception related to values (in maps/dictionaries/lists) with the wrong type
     */
    public static class WrongValueTypeException extends YAMLParseException{
        public WrongValueTypeException(String msg){ super(msg); }
    }

    /**
     * Exception related to structures without the expected map/dictionary format
     */
    public static class NotAMapException extends YAMLParseException{
        public NotAMapException(String msg){ super(msg); }
    }

    /**
     * Exception related to structures without the expected list format
     */
    public static class NotAListException extends YAMLParseException{
        public NotAListException(String msg){ super(msg); }
    }

    /**
     * Loads and reads a YAML file
     * @param filename Name of the file to read
     * @return A map of key (String)/value (Object) pairs found in the file
     * @throws FileNotFoundException If the file in question does not exist (e.g., typo in filename)
     * @throws ClassCastException If the content in the YAML is not in the right key/value format
     */
    public static HashMap<String, Object> loadYAMLFile(String filename) throws FileNotFoundException, ClassCastException {
        Yaml yaml = new Yaml();
        InputStream input = new FileInputStream(filename);
        Console.log("Reading YAML: "+filename);
        HashMap<String,Object> yamlMap = yaml.load(input);
        Console.tick();
        return yamlMap;
    }

    /**
     * Parses an object into a map String -> Object
     * @param o The object to parse
     * @return The parsed map
     * @throws WrongKeyTypeException If one of the key is not a String
     * @throws NotAMapException If the object is not a valid map
     */
    public static HashMap<String, Object> parseMap(Object o) throws WrongKeyTypeException, NotAMapException {
        HashMap<String, Object> res = new HashMap<>();
        if(o instanceof HashMap<?,?>){
            HashMap map = (HashMap) o;
            for(Object k: map.keySet()){
                if(k instanceof String){
                    res.put((String)k, map.get(k));
                } else {
                    throw new WrongKeyTypeException("String expected");
                }
            }
        } else {
            throw new NotAMapException("List of key/value pairs expected");
        }
        return res;
    }

    /**
     * Parses an object into an array of Strings
     * @param o The object to parse
     * @return The parsed array
     * @throws WrongValueTypeException If one of the item is not a String
     * @throws NotAListException If the object is not a valid list
     */
    public static ArrayList<String> parseStringList(Object o) throws WrongValueTypeException, NotAListException {
        ArrayList<String> res = new ArrayList<>();
        if(o instanceof ArrayList<?>){
            ArrayList list = (ArrayList) o;
            for(Object s: list){
                if(s instanceof String){
                    res.add((String) s);
                } else {
                    throw new WrongValueTypeException("String expected");
                }
            }
        } else {
            throw new NotAListException("List of strings expected");
        }
        return res;
    }

    /**
     * Parses an object into a String
     * @param o The object to parse
     * @return The parsed String
     * @throws WrongValueTypeException If the object is not a String
     */
    public static String parseString(Object o) throws WrongValueTypeException {
        if(o instanceof String){
            return (String) o;
        } else {
            throw new WrongValueTypeException("String expected");
        }
    }

    /**
     * Parses an object into an integer
     * @param o The object to parse
     * @return The parsed integer
     * @throws WrongValueTypeException If the object is not an integer
     */
    public static int parseInt(Object o) throws WrongValueTypeException {
        if(o instanceof Integer){
            return (Integer) o;
        } else {
            throw new WrongValueTypeException("Integer expected");
        }
    }

    /**
     * Parses an object into a double
     * @param o The object to parse
     * @return The parsed double
     * @throws WrongValueTypeException If the object is not a double
     */
    public static double parseDouble(Object o) throws WrongValueTypeException {
        if(o instanceof Double){
            return (Double) o;
        } else {
            throw new WrongValueTypeException("Float expected");
        }
    }

    /**
     * Parses an object into a boolean
     * @param o The object to parse
     * @return The parsed boolean
     * @throws WrongValueTypeException If the object is not a boolean
     */
    public static boolean parseBoolean(Object o) throws WrongValueTypeException {
        if(o instanceof Boolean){
            return (Boolean) o;
        } else {
            throw new WrongValueTypeException("Boolean expected");
        }
    }
}
