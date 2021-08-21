package raspberry.scheduler.algorithm.astar;

import java.util.*;
import java.util.List;

import raspberry.scheduler.algorithm.Algorithm;
import raspberry.scheduler.algorithm.common.OutputSchedule;
import raspberry.scheduler.algorithm.common.ScheduledTask;
import raspberry.scheduler.algorithm.common.Solution;
import raspberry.scheduler.app.visualisation.model.AlgoObservable;
import raspberry.scheduler.graph.*;

import raspberry.scheduler.graph.exceptions.EdgeDoesNotExistException;

/**
 * Implementation of A star algorithm.
 *
 * @author Takahiro
 */
public class Astar implements Algorithm {
    private IGraph _graph;
    int _numP;
    int _numNode;
    int _maxCriticalPath;
    PriorityQueue<ScheduleAStar> _pq;
    Hashtable<String, Integer> _heuristic = new Hashtable<String, Integer>();
    Hashtable<Integer, ArrayList<ScheduleAStar>> _visited;

    int _upperBound;
    private AlgoObservable _observable;

    /**
     * Constructor for A*
     *
     * @param graphToSolve  : graph to solve (graph represents the task and dependencies)
     * @param numProcessors : number of processor we can use to schedule tasks.
     */
    public Astar(IGraph graphToSolve, int numProcessors, int upperBound) {
        _graph = graphToSolve;
        _pq = new PriorityQueue<ScheduleAStar>();
        _visited = new Hashtable<Integer, ArrayList<ScheduleAStar>>();
        _numP = numProcessors;
        _numNode = _graph.getNumNodes();
        _upperBound = upperBound;
        _observable = AlgoObservable.getInstance();
    }

    /**
     * Compute the optimal scheduling
     *
     * @return OutputSchedule : the optimal path/scheduling.
     */
    @Override
    public OutputSchedule findPath() {
        /*
         * find the path
         * "master" stores, schedule and its counterTable.
         * "rootTable" is the table all counterTable is based of off.
         * --> stores a node and number of incoming edges.
         */
        getH();

//        Hashtable<Schedule, Hashtable<INode, Integer>> master = new Hashtable<Schedule, Hashtable<INode, Integer>>();
        Hashtable<INode, Integer> rootTable = this.getRootTable();

        for (INode node : rootTable.keySet()) {
            if (rootTable.get(node) == 0) {

//                ScheduleAStar newSchedule = new ScheduleAStar(
//                        0, null, node, 1, getChildTable(rootTable, node));
//
                ScheduleAStar newSchedule = new ScheduleAStar(
                        new ScheduledTask(1,node, 0),
                        getChildTable(rootTable, node)
                );

                newSchedule.addHeuristic(
                        Collections.max(Arrays.asList(
                                h(newSchedule),
                                h1(getChildTable(rootTable, node), newSchedule)
                        )));
//                master.put(newSchedule, getChildTable(rootTable, i));
                _pq.add(newSchedule);
            }
        }

        ScheduleAStar cSchedule;
        int duplicate = 0; // Duplicate counter, Used for debugging purposes.
        _observable.setIterations(0);
        _observable.setIsFinish(false);
      //  System.out.println(_observable.getIterations());
        while (true) {
//            System.out.printf("PQ SIZE: %d\n", _pq.size());
            _observable.increment();
            //System.out.println(_observable.getIterations());
            cSchedule = _pq.poll();

            Solution cScheduleSolution = new Solution(cSchedule, _numP);
            _observable.setSolution(cScheduleSolution);

            ArrayList<ScheduleAStar> listVisitedForSize = _visited.get(cSchedule.getHash());

            if (listVisitedForSize != null && isIrrelevantDuplicate(listVisitedForSize, cSchedule)) {
                duplicate++;
                continue;
            } else {
                if (listVisitedForSize == null) {
                    listVisitedForSize = new ArrayList<ScheduleAStar>();
                    _visited.put(cSchedule.getHash(), listVisitedForSize);
                }
                listVisitedForSize.add(cSchedule);
            }

            // Return if all task is scheduled
            if (cSchedule.getSize() == _numNode) {
                break;
            }
//            Hashtable<INode, Integer> cTable = master.get(cSchedule);
//            master.remove(cSchedule);
            Hashtable<INode, Integer> cTable = cSchedule._inDegreeTable;
            // Find the next empty processor. (
            int currentMaxPid = cSchedule.getMaxPid();
            int pidBound;
            if (currentMaxPid + 1 > _numP) {
                pidBound = _numP;
            } else {
                pidBound = currentMaxPid + 1;
            }
            for (INode node : cTable.keySet()) {
                if (cTable.get(node) == 0) {
                    for (int pid = 1; pid <= pidBound; pid++) {
                        int start = calculateEarliestStartTime(cSchedule, pid, node);


                        Hashtable<INode, Integer> newTable = getChildTable(cTable, node);

                        ScheduleAStar newSchedule = new ScheduleAStar(
                                cSchedule,
                                new ScheduledTask(pid, node, start),
                                newTable);

                        newSchedule.addHeuristic(
                                Collections.max(Arrays.asList(
                                        h(newSchedule),
                                        h1(newTable, newSchedule)
                                )));

                        if (newSchedule.getTotal() <= _upperBound){
                            ArrayList<ScheduleAStar> listVisitedForSizeV2 = _visited.get(newSchedule.getHash());
                            if (listVisitedForSizeV2 != null && isIrrelevantDuplicate(listVisitedForSizeV2, newSchedule)) {
                                duplicate++;
                            }else{
                                _pq.add(newSchedule);
                            }
                        }


                    }
                }
            }
        }
        System.out.printf("PQ SIZE: %d\n", _pq.size());
        System.out.printf("\nDUPLCIATE : %d\n", duplicate);

        _observable.setIsFinish(true);
        _observable.setSolution(new Solution(cSchedule,_numP));

        return new Solution(cSchedule, _numP);
    }

    /**
     * For each task that was scheduled last in the processor.
     * -> find the largest cost
     * --> where cost = finish time of the task + heuristic of the task
     *
     * @param cSchedule : schedule of which we are trying to find heuristic cost for.
     * @return integer : represeting the heuristic cost
     */
    public int h(ScheduleAStar cSchedule) {
        int max = 0;
        for (String s : cSchedule.getLastForEachProcessor().values()) {
            int tmp = _heuristic.get(s) + cSchedule.getScheduling().get(s).get(1) +
                    _graph.getNode(s).getValue();
            if (tmp > max) {
                max = tmp;
            }
        }
        return max - cSchedule.getFinishTime();
    }

    /**
     * Find the best case scheduling where all task are evenly spread out throughout the different processors.
     *
     * @param x         : Hashtable representing the outDegree table. (All the tasks in the table has not been scheduled yet)
     * @param cSchedule : current schedule . Used to find the last task which was scheduled for each processor.
     * @return Integer : Representing the best case scheduling.
     */
    public int h1(Hashtable<INode, Integer> x, ScheduleAStar cSchedule) {
        int sum = 0;
        for (String s : cSchedule.getLastForEachProcessor().values()) {
            sum += cSchedule.getScheduling().get(s).get(1) +
                    _graph.getNode(s).getValue();
        }
        for (INode i : x.keySet()) {
            sum += i.getValue();
        }
        return sum / _numP - cSchedule.getFinishTime();
    }

    /**
     * Computes the earliest time we can schedule a task in a specific processor.
     *
     * @param parentSchedule   : parent schedule of this partial schedule.
     * @param processorId      : the specific processor we want to schedule task into.
     * @param nodeToBeSchedule : node/task to be scheduled.
     * @return Integer : representing the earliest time. (start time)
     */
    public int calculateEarliestStartTime(ScheduleAStar parentSchedule, int processorId, INode nodeToBeSchedule) {
        // Find last finish parent node
        // Find last finish time for current processor id.
        ScheduleAStar last_processorId_use = null; //last time processor with "processorId" was used.
        ScheduleAStar cParentSchedule = parentSchedule;

        while (cParentSchedule != null) {
            if (cParentSchedule.getPid() == processorId) {
                last_processorId_use = cParentSchedule;
                break;
            }
            cParentSchedule = cParentSchedule.getParent();
        }

        //last time parent was used. Needs to check for all processor.
        int finished_time_of_last_parent = 0;
        if (last_processorId_use != null) {
            finished_time_of_last_parent = last_processorId_use.getFinishTime();
        }

        cParentSchedule = parentSchedule;
        while (cParentSchedule != null) {
            // for edges in current parent scheduled node
            INode last_scheduled_node = cParentSchedule.getNode();
            for (IEdge edge : _graph.getOutgoingEdges(last_scheduled_node.getName())) {

                // if edge points to  === childNode
                if (edge.getChild() == nodeToBeSchedule && cParentSchedule.getPid() != processorId) {
                    //last_parent_processor[ cParentSchedule.p_id ] = true;
                    try {
                        int communicationWeight = _graph.getEdgeWeight(cParentSchedule.getNode(), nodeToBeSchedule);
                        //  finished_time_of_last_parent  <
                        if (finished_time_of_last_parent < (cParentSchedule.getFinishTime() + communicationWeight)) {
                            finished_time_of_last_parent = cParentSchedule.getFinishTime() + communicationWeight;
                        }
                    } catch (EdgeDoesNotExistException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
            cParentSchedule = cParentSchedule.getParent();
        }
        return finished_time_of_last_parent;
    }

    /**
     * Creates initial outDegree table for the graph.
     *
     * @return : Hashtable : Key : Node
     * Value : Integer representing number of outDegree edges.
     */
    public Hashtable<INode, Integer> getRootTable() {
        Hashtable<INode, Integer> tmp = new Hashtable<INode, Integer>();
        for (INode i : _graph.getAllNodes()) {
            tmp.put(i, 0);
        }
        for (INode i : _graph.getAllNodes()) {
            for (IEdge j : _graph.getOutgoingEdges(i.getName())) {
                tmp.replace(j.getChild(), tmp.get(j.getChild()) + 1);
            }
        }
        return tmp;
    }

    /**
     * Creates outDegree table for child node.
     *
     * @param parentTable : parent schedule's outDegree table.
     * @param x           : Node :that was just scheduled
     * @return : Hashtable : Key : Node
     * Value : Integer representing number of outDegree edges.
     */
    public Hashtable<INode, Integer> getChildTable(Hashtable<INode, Integer> parentTable, INode x) {
        Hashtable<INode, Integer> tmp = new Hashtable<INode, Integer>(parentTable);
        tmp.remove(x);
        for (IEdge i : _graph.getOutgoingEdges(x.getName())) {
            tmp.replace(i.getChild(), tmp.get(i.getChild()) - 1);
        }
        return tmp;
    }


    /**
     * Creates a maximum dependency path table.
     * Also find the maximum critical path cost of the graph.
     * where key : String <- task's name.
     * value : int <- maximum path cost.
     */
    public void getH() {
        _heuristic = new Hashtable<String, Integer>();
        for (INode i : _graph.getAllNodes()) {
            _heuristic.put(i.getName(), getHRecursive(i));
        }
        _maxCriticalPath = Collections.max(_heuristic.values());
    }

    /**
     * Recursive call child node to get the cost. Get the maximum cost path and return.
     *
     * @param n : INode : task that we are trying to find the heuristic weight.
     * @return integer : Maximum value of n's child path.
     */
    public int getHRecursive(INode n) {
        List<IEdge> e = _graph.getOutgoingEdges(n.getName());
        if (e.size() == 0) {
            return 0;
        } else if (e.size() == 1) {
            return getHRecursive(e.get(0).getChild()) + e.get(0).getChild().getValue();
        }
        int max = 0;
        for (IEdge i : e) {
            int justCost = getHRecursive(i.getChild()) + i.getChild().getValue();
            if (max < justCost) {
                max = justCost;
            }
        }
        return max;
    }

    /**
     * TODO : FIND OUT IF WE ACTUALLY NEED THIS FUNCTION
     * OR THIS -> "listVisitedForSize.contains(cSchedule)" JUST WORKS FINE.
     * Find out if the duplicate schedule exists.
     * -> if we find one, check if the heuristic is larger or smaller.
     * --> if its larger then we dont need to reopen.
     * --> if its smaller we need to reopen.
     *
     * @param scheduleList : list of visited schedule. (with same getHash() value)
     * @param cSchedule    : schedule that we are trying to find if duplicate exists of not.
     * @return True : if reopening is not needed
     * False : if reopening needs to happend for this schedule.
     */
    public Boolean isIrrelevantDuplicate(ArrayList<ScheduleAStar> scheduleList, ScheduleAStar cSchedule) {
        for (ScheduleAStar s : scheduleList) {
            if ( s.equals2(cSchedule) ){
                if ( s.getTotal() > cSchedule.getTotal()) {
//                    System.out.printf("%d -> %d\n", s.getTotal(), cSchedule.getTotal());
                    return false;
                }else{
                    return true;
                }
            }
        }
        return false;
    }
}