package IO;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for logging times;
 * Uses and singleton and provides static methods
 */
public class Timer {

    private HashMap<String, Long> starts;
    private HashMap<String, Long> times;

    private static Timer instance;

    private Timer(){
        starts = new HashMap<>();
        times = new HashMap<>();
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
            instance.times.put(key, (System.currentTimeMillis()-instance.starts.get(key)/(long)1000));
            instance.starts.remove(key);
        }
    }

    /**
     * Logs using the Console class the stopped timers (with elapsed times recorded);
     * Will clear the list of stopped timers
     */
    public static void print(){
        if(instance != null){
            for(Map.Entry<String, Long> time: instance.times.entrySet()){
                Console.note(time.getKey()+": " + Math.floorDiv(time.getValue(), 60) + " m, " + time.getValue() % 60 + " s.");
            }
            instance.times.clear();
        }
    }

}
