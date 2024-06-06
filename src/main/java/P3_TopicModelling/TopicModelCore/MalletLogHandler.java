package P3_TopicModelling.TopicModelCore;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

/**
 * Class capturing certain logs from MALLET to facilitate their export onto log files.
 *
 * @author P. Le Bras
 * @version 1
 */
@Deprecated
public class MalletLogHandler extends Handler {

    /** List of log-likelihood logs recorded from MALLET. */
    private final ArrayList<LogRecord> llRecords = new ArrayList<>();
    /** List of topic logs recorded from MALLET. */
    private final ArrayList<LogRecord> topicRecords = new ArrayList<>();
    /** List of generic logs recorded from MALLET. Currently unused. */
    private final ArrayList<LogRecord> rec = new ArrayList<>();

    /** Boolean flag indicating if the final log for the sampling process has passed or not.
     * If passed, logs are from the maximisation process. */
    private boolean samplingFinished = false;

    /**
     * Method returning all log messages concerning the model's log-likelihood.
     * @return List of log messages.
     */
    public List<String> getLLRecords(){
        return llRecords.parallelStream().map(r->r.getMessage()).collect(Collectors.toList());
    }

    /**
     * Method returning all log messages concerning the topics.
     * @return List of log messages.
     */
    public List<String> getTopicRecords(){
        return topicRecords.parallelStream().map(r->r.getMessage()).collect(Collectors.toList());
    }

    /**
     * Method receiving a log from MALLET and recording it in the appropriate list.
     * @param record Log record to filter and store.
     */
    @Override
    public void publish(LogRecord record) {
        if(record.getMessage().contains("LL/token")) { // recording loglikelihood logs
            llRecords.add(record);
        } else if(record.getMessage().contains("beta")){ // recording beta logs
        } else if(record.getMessage().contains("<") || record.getMessage().contains("[")) { // other logs
        } else if(record.getMessage().contains("Total time:")){ // recording last log
            samplingFinished = true;
        } else {
            if(samplingFinished){ // maximisation logs
                // TODO parse these logs, contain log-likelihood info
            } else { // only topic logs left
                topicRecords.add(record);
            }
        }
        // else // recording other logs (e.g.topics over time)
        //     rec.add(record);
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}
