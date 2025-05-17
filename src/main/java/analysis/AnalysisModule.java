package analysis;

import data.Topic;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.List;

/**
 * Super class for model analysis modules, containing typical properties and methods
 *
 * @author P. Le Bras
 * @version 1
 */
public class AnalysisModule {

    // list of topics
    protected List<Topic> topics;

    // filename for list of topics
    protected String topicsFile;

    protected void loadTopics() throws IOException, ParseException {
        topics = Topic.loadTopics(topicsFile);
    }
}
