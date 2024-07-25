package IO;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SERHelper {

    /**
     * Helper method to serialise Java objects
     * @param obj Object to serialise
     * @param filename File name where to save serialised object
     * @param depth Depth level for logs
     * @throws IOException If the file cannot be created/written
     */
    public static void serialiseObject(Serializable obj, String filename, int depth) throws IOException {
        Console.log("Serialising "+obj.getClass()+" to "+filename, depth);
        FileOutputStream fileOut = new FileOutputStream(filename);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(obj);
        out.close();
        fileOut.close();
        Console.tick();
    }

}
