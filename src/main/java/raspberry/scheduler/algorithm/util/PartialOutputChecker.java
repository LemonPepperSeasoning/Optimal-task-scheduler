package raspberry.scheduler.algorithm.util;

import raspberry.scheduler.algorithm.common.OutputSchedule;
import raspberry.scheduler.graph.exceptions.EdgeDoesNotExistException;
import raspberry.scheduler.graph.IEdge;
import raspberry.scheduler.graph.IGraph;
import raspberry.scheduler.graph.INode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Partial output checker,
 *
 * @author Young, Neville, Jonathon
 */
public class PartialOutputChecker {

    /**
     * Test if the output schedule violates any constraint according to
     * the dependency graph.
     * @return true if output schedule does not violate any constraint
     * @throws EdgeDoesNotExistException given edges doesn't exists
     */
    public static boolean isValid(IGraph graph, OutputSchedule outputSchedule) throws EdgeDoesNotExistException {

        int i = 1;
        Collection<INode> nodes = new ArrayList<>();
        while (i <= outputSchedule.getTotalProcessorNum()){
            nodes.addAll(outputSchedule.getNodes(i));
            i++;
        }

        // check if there's any overlap
        if (_isOverlap(graph, outputSchedule, nodes)) {
            return false;
        }

        // check dependency violation
        for (INode node : nodes) {
            List<IEdge> ingoingEdges = graph.getIngoingEdges(node.getName());
            for (IEdge edge : ingoingEdges) {
                INode parentNode = edge.getParent();
                int parentEndTime = outputSchedule.getStartTime(parentNode) + parentNode.getValue();

                /*
                 * if node and parent node are on the different processor
                 * return false if node start earlier than parent node plus communication time
                 */
                if ((outputSchedule.getProcessorNum(parentNode) != outputSchedule.getProcessorNum(node))
                        && ((graph.getEdgeWeight(parentNode, node) + parentEndTime)) > outputSchedule.getStartTime(node)) {
                    return false;
                    /*
                     * node are on the same processor,
                     * return false if node start earlier than parent node.
                     */
                } else if (parentEndTime > outputSchedule.getStartTime(node)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * overlap occurs when the following happens:
     * - a child node begins execution prior to the parent node
     * - a child node begins execution while parent node is still in progress.
     * - node with no dependency relation overlap
     * @return True if there are overlap
     */
    private static boolean _isOverlap(IGraph graph, OutputSchedule output, Collection<INode> nodes) {
        for (INode node1 : nodes) {
            int startTime1 = output.getStartTime(node1);
            int endTime1 = output.getStartTime(node1) + node1.getValue();
            // check all other nodes to see if it is overlapping
            for (INode node2 : nodes) {
                int startTime2 = output.getStartTime(node2);
                // check same processor node for all other node that is not node 1
                if (node1 != node2
                        && output.getProcessorNum(node1) == output.getProcessorNum(node2)) {

                    // if node 2 start in between node 1 computation
                    if (startTime2 < endTime1 && startTime2 > startTime1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}