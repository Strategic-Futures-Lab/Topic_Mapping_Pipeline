package IO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for logging times;
 * Uses and singleton and provides static methods
 */
public class Timer {

    private HashMap<String, Long> starts;
    private ArrayList<String> times;

    private static Timer instance;

    private Timer(){
        starts = new HashMap<>();
        times = new ArrayList<>();
    }

    /**
     * Records a start time
     * @param key Name of the timer to start
     */
    public static void start(String key){
        if(instance == null) instance = new Timer();
        instance.starts.put(key, System.currentTimeMillis());
    }

    /**
     * Records an elapsed time since a timer started;
     * Will erase the start time for this timer;
     * Does nothing if the timer has not started
     * @param key Name of the timer to stop
     */
    public static void stop(String key){
        if(instance != null && instance.starts.containsKey(key)){
            long time = (System.currentTimeMillis()-instance.starts.get(key))/(long)1000;
            instance.times.add(key+": " + convert(time));
            instance.starts.remove(key);
        }
    }

    /**
     * Logs using the Console class the stopped timers (with elapsed times recorded);
     * Will clear the list of stopped timers
     */
    public static void print(){
        if(instance != null){
            for(String time: instance.times){
                Console.note(time);
            }
            instance.times.clear();
        }
    }

    /**
     * Converts a time number to a string representation of time
     * @param time time in seconds
     * @return Time string in minutes and seconds
     */
    public static String convert(long time){
        return Math.floorDiv(time, 60) + " m, " + time % 60 + " s.";
    }

    /**
     * Converts a time number to a string representation of time
     * @param time time in seconds
     * @return Time string in minutes and seconds
     */
    public static String convert(float time){
        return Math.floor(time/60) + " m, " + time % 60 + " s.";
    }
}
