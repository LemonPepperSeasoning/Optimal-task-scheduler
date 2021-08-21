
package raspberry.scheduler.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import raspberry.scheduler.algorithm.astar.Astar;
import raspberry.scheduler.algorithm.astar.AstarVisualiser;
import raspberry.scheduler.algorithm.common.OutputSchedule;
import raspberry.scheduler.cli.CLIConfig;
import raspberry.scheduler.graph.IGraph;
import raspberry.scheduler.io.GraphReader;
import raspberry.scheduler.io.Writer;
import java.io.IOException;


/**
 * This class launches the front end visualisation.
 */
public class App extends Application {

    private static CLIConfig _config;
    private static GraphReader _reader;

    public static void main(CLIConfig config, GraphReader reader) {
        System.out.println("Launched with visualisation");
        _config = config;
        _reader = reader;
        launch();

    }

    @Override
    public void start(Stage primaryStage) throws IOException {
            Parent root = FXMLLoader.load(getClass().getResource("/view/MainView.fxml"));
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);

            primaryStage.setResizable(false);
            primaryStage.show();
            primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent e) {
                    Platform.exit();
                    System.exit(1);
                }
            });

            new Thread(() -> {
                    startAlgo();
            }).start();
    }

    public static CLIConfig GetCLIConfig() {
        return _config;
    }

    /**
     * Read the graph and start the algorithm
     */
    private void startAlgo() {
        try {
            IGraph graph = _reader.read();
            AstarVisualiser astar = new AstarVisualiser(graph, _config.get_numProcessors(), Integer.MAX_VALUE);
            OutputSchedule outputSchedule = astar.findPath();
            Writer writer = new Writer(_config.getOutputFile(), graph, outputSchedule);
            writer.write();
        } catch(IOException e){
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

}
