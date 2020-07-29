package P3_TopicModelling.TopicModelCore;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

public class MalletLogHandler extends Handler {

    private ArrayList<LogRecord> llRecords = new ArrayList<>();
    private ArrayList<LogRecord> rec = new ArrayList<>();

    public List<String> getLLRecords(){
        return llRecords.parallelStream().map(r->r.getMessage()).collect(Collectors.toList());
    }

    @Override
    public void publish(LogRecord record) {
        if(record.getMessage().contains("LL/token")) // recording loglikelihood logs
            llRecords.add(record);
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
