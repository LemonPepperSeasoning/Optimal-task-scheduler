package raspberry.scheduler;

import raspberry.scheduler.algorithm.astar.Astar;
import raspberry.scheduler.algorithm.bNb.BNB2;
import raspberry.scheduler.algorithm.common.OutputSchedule;
import raspberry.scheduler.cli.CLIConfig;
import raspberry.scheduler.cli.CLIParser;
import raspberry.scheduler.cli.exception.ParserException;
import raspberry.scheduler.graph.IGraph;
import raspberry.scheduler.io.GraphReader;
import raspberry.scheduler.io.Logger;
import raspberry.scheduler.io.Writer;
import raspberry.scheduler.app.*;

import java.io.IOException;

public class Main {
    public static final boolean COLLECT_STATS_ENABLE = true;
    private static double _startTime;
    public static void main(String[] inputs) throws NumberFormatException {
        try {
            CLIConfig CLIConfig = CLIParser.parser(inputs);
            GraphReader reader = new GraphReader(CLIConfig.getDotFile());

            // Start visualisation if appropriate argument is given.
            if (CLIConfig.getVisualise()) {
                startVisualisation(CLIConfig, reader);
            } else {
                for(int i = 0; i<10; i++) {
                    IGraph graph = reader.read();
                    if (COLLECT_STATS_ENABLE) {
                        _startTime = System.nanoTime();
                    }
                    Astar astar = new Astar(graph, CLIConfig.get_numProcessors(), Integer.MAX_VALUE);
                    OutputSchedule outputSchedule = astar.findPath();
                    if (COLLECT_STATS_ENABLE) {
                        //  Logger.log(CLIConfig, _startTime, System.nanoTime());
                        Logger.log("AStar", CLIConfig.getDotFile(), CLIConfig.get_numProcessors(), System.nanoTime() - _startTime);
                    }
                    Writer writer = new Writer(CLIConfig.getOutputFile(), graph, outputSchedule);
                    writer.write();
                }
            }
        } catch (IOException | ParserException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void startVisualisation(CLIConfig config, GraphReader reader) {
//        new Thread(()-> {
        App.main(config, reader);
//        }).start();
    }
}