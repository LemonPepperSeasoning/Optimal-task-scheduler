package raspberry.scheduler.algorithm.common;

import raspberry.scheduler.algorithm.astar.Astar;
import raspberry.scheduler.algorithm.astar.ScheduleAStar;
import raspberry.scheduler.algorithm.util.OutputChecker;
import raspberry.scheduler.graph.*;
import raspberry.scheduler.graph.exceptions.EdgeDoesNotExistException;

import java.util.*;


public class EquivalenceChecker {


    private IGraph _graph;
    private int _numProcessors;
    private int _counter = 0;

    public EquivalenceChecker(IGraph graph, int numProcessors) {
        _graph = graph;
        _numProcessors = numProcessors;
    }


    public boolean weAreDoomed(ScheduleAStar schedule) {

        // declaration
        ScheduleAStar cSchedule = schedule;
        ScheduledTask m = schedule.getScheduledTask();
        int TMax = m.getFinishTime();


        ArrayList<ScheduledTask> processorTaskList = schedule.getAllTaskInProcessor(m.getProcessorID());
        // sort by accenting start time
        processorTaskList.sort(Comparator.comparingInt(ScheduledTask::getStartTime));

        Hashtable<Integer, ScheduledTask> indexTable = new Hashtable<Integer, ScheduledTask>();
        int i = 1;
        while (i < processorTaskList.size() + 1) {
            indexTable.put(i, processorTaskList.get(i - 1));
            i++;
        }
        int secondlastindex = indexTable.size() - 1;
        i = secondlastindex;
        //System.out.println(indexTable.toString());
        while (i > 0
                && _graph.getIndex(m.getTask()) <
                _graph.getIndex(indexTable.get(i).getTask())) {
            System.out.println("================================================================================");
            ScheduleAStar before = cSchedule;
            System.out.println("NOT SWAPPED:  " + cSchedule);
            cSchedule = swap2(cSchedule, m, indexTable.get(i));
            System.out.println(String.format("SWAPPED task %s with %s:  ", m, indexTable.get(i)) + cSchedule.toString());
            if (!isCorrectlySwapped(before, cSchedule, m, indexTable.get(i))) {
                System.out.println("WRONG");
            }
//            Object temp = cSchedule.getScheduledTask(indexTable
//                    .get(secondlastindex)
//                    .getTask());
//            System.out.println("TEMP is not null: " + temp);

            // task in the same processor from task to swap to send to last index
            ArrayList<ScheduledTask> inputList = new ArrayList<>();
            for (int index = i; index <= secondlastindex; index ++){
                inputList.add(indexTable.get(index));
            }
            System.out.println("------------- OUT GOING COMS OK CHECK ");
            if (cSchedule.getScheduledTask(indexTable.get(secondlastindex).getTask())
                    .getFinishTime() <= TMax
                    && outgoingCommsOK(inputList, cSchedule, schedule)) {
                _counter++;
                System.out.println("\n------------- END CHECK\n");
                System.out.println("================================================================================");
                System.out.println("counter is: " + _counter);
//                try {
//                    if (!OutputChecker.isValid(_graph, new Solution(cSchedule, 10))) {
//                        System.out.println("We are Screwed!!!!!!!!!!!!!!!!!!!!!!!!!");
//                    }
//                } catch (EdgeDoesNotExistException e) {
//                    e.printStackTrace();
//                }
                return true;
            }
            System.out.println("\n------------- END CHECK\n");
            System.out.println("================================================================================");
            i--;

        }
        return false;


//        i
//        3: i ← l − 1
//        4: while i ≥ 0 ∧ index(m) < index(ni) do


//        5: Swap position of m and ni
//        6: Schedule m and ni      nl−1 each as early as possible
//        7: if tf (nl−1) ≤ tmax ∧ OutgoingCommsOK(ni. . . nl−1) then
//        8: return EQUIVALENT
//        9: i ← i − 1
//        10: return NOT EQUIVALENT

    }


    private ScheduleAStar swap2(ScheduleAStar schedule, ScheduledTask m, ScheduledTask taskToSwap) {
        ArrayList<ScheduledTask> tmp = new ArrayList<>();
        ScheduleAStar cSchedule = schedule;
        while (cSchedule != null) {
            tmp.add(cSchedule.getScheduledTask());
            cSchedule = cSchedule.getParent();
        }
        Collections.reverse(tmp);

        ScheduleAStar result;
        cSchedule = schedule;

        ArrayList<ScheduledTask> prevTask = new ArrayList<>();
        ArrayList<ScheduledTask> afterTask = new ArrayList<>();

        while (cSchedule != null){
            if ( cSchedule.getScheduledTask().getTask() == m.getTask()){
                break;
            }else{
                afterTask.add(cSchedule.getScheduledTask());
                cSchedule = cSchedule.getParent();
            }
        }

        while (cSchedule != null) {
            if (cSchedule.getScheduledTask().getTask().getName().equals(taskToSwap.getTask().getName())) {
                cSchedule = cSchedule.getParent();
                break;
            }
            if ( !(cSchedule.getScheduledTask().getTask().getName().equals(m.getTask().getName() ))
                    && !(cSchedule.getScheduledTask().getTask().getName().equals(taskToSwap.getTask().getName()))) {
                prevTask.add(cSchedule.getScheduledTask());
            }
            cSchedule = cSchedule.getParent();
        }
        Collections.reverse(prevTask);

        INode swapNode = taskToSwap.getTask();
        List<IEdge> childOfSwap = _graph.getOutgoingEdges(swapNode);

        INode mNode = m.getTask();
        List<IEdge> parentOfM = _graph.getIngoingEdges(mNode);

        ArrayList<ScheduledTask> newOrdering = new ArrayList<ScheduledTask>();
        Collections.reverse(prevTask);
        boolean swapIsAdded = false;
        for (ScheduledTask st : prevTask) {
            if (!swapIsAdded) {
                for (IEdge e : childOfSwap) {
                    if (e.getChild() == st.getTask() && !swapIsAdded ) {
                        newOrdering.add(taskToSwap);
                        swapIsAdded = true;
                    }
                }
            }
            newOrdering.add(st);
        }
        if (!swapIsAdded) {
            newOrdering.add(taskToSwap);
        }

        ArrayList<ScheduledTask> newOrdering2 = new ArrayList<ScheduledTask>();
        Collections.reverse(newOrdering);
        boolean mIsAdded = false;
        ScheduledTask prevScheduledTask = null;
        for (ScheduledTask st : newOrdering) {
            if (!mIsAdded) {
                for (IEdge e : parentOfM) {
                    if ( e.getParent() == st.getTask() && !mIsAdded) {
                        newOrdering2.add(m);
                        mIsAdded = true;
                    }else if ( st.getTask() == taskToSwap.getTask() && !mIsAdded ){
                        newOrdering2.add(m);
                        mIsAdded = true;
                    }
                }
            }
            newOrdering2.add(st);
        }
        if (!mIsAdded) {
            newOrdering2.add(m);
        }

        if (newOrdering2.get(0).getTask() == taskToSwap.getTask()
                && newOrdering2.get(newOrdering2.size()-1).getTask() == m.getTask() ){
            ScheduledTask tmp2 = newOrdering.get(0);
            newOrdering2.set(0, newOrdering2.get(newOrdering2.size()-1));
            newOrdering2.set(newOrdering2.size()-1, tmp2);
        }
        if (newOrdering2.size() >= 2){
            if (newOrdering2.get( newOrdering2.size()-2).getTask() == taskToSwap.getTask()
                    && newOrdering2.get( newOrdering2.size()-1 ).getTask() == m.getTask() ){
                ScheduledTask tmp2 = newOrdering.get(newOrdering2.size()-2);
                newOrdering2.set(newOrdering2.size()-2, newOrdering2.get(newOrdering2.size()-1));
                newOrdering2.set(newOrdering2.size()-1, tmp2);
            }
        }

        if (prevTask.isEmpty()){
            newOrdering2 = new ArrayList<ScheduledTask>();
            newOrdering2.add( m );
            newOrdering2.add( taskToSwap );
        }

        int indexM = 0;
        int indexSwap = 0;
        int counter = 0;
        for (ScheduledTask i:newOrdering2){
            if ( i == m ){
                indexM = counter;
            }else if (i == taskToSwap){
                indexSwap = counter;
            }
            counter ++;
        }

        Collections.reverse(prevTask);
        if ( indexM > indexSwap ){
            System.out.printf("\nindex m: %d, swap: %d \n", indexM,indexSwap);
            System.out.println(tmp);
            System.out.println(prevTask);
            System.out.println(newOrdering);
            System.out.println(newOrdering2);
            System.out.println("NOT SWAPPED:  \n" + schedule);
        }

        result = cSchedule;
        //System.out.printf("ORDERING2 : %d , ORDERING1 : %d , prevTask: %d\n", newOrdering2.size(), newOrdering.size(), prevTask.size());
        for (ScheduledTask st : newOrdering2) {
            int earliestStartTime = Astar.calculateEarliestStartTime(result, st.getProcessorID(), st.getTask());
            ScheduledTask scheduleST = new ScheduledTask(st.getProcessorID(), st.getTask(), earliestStartTime);
            //scheduleST.setStartTime(earliestStartTime);

            if (result == null) {
                result = new ScheduleAStar(
                        scheduleST, null
                );

            } else {
                result = createSubSchedule(result, scheduleST);
            }
        }

        Collections.reverse(afterTask);
        for (ScheduledTask st : afterTask) {
            int earliestStartTime = Astar.calculateEarliestStartTime(result, st.getProcessorID(), st.getTask());
            ScheduledTask scheduleST = new ScheduledTask(st.getProcessorID(), st.getTask(), earliestStartTime);
            //scheduleST.setStartTime(earliestStartTime);

            if (result == null) {
                result = new ScheduleAStar(
                        scheduleST, null
                );
            } else {
                result = createSubSchedule(result, scheduleST);
            }
        }



        if (indexM > indexSwap){
            System.out.println(String.format("SWAPPED task %s with %s:  \n", m, taskToSwap) + schedule.toString());
        }
        if (result == null){
            System.out.println("NULL");
        }
        return result;
    }




    public boolean outgoingCommsOK(List<ScheduledTask> scheduledTasks, ScheduleAStar schedule, ScheduleAStar originalSchedule) {
        boolean flag = false;
        //1: for all nk ∈ {ni. . . nl−1} do
        for (ScheduledTask scheduledTask : scheduledTasks) {
            // 2: if ts(nk) > t_originStartTime (nk) then B check only if nk starts later
            int swappedTime = schedule.getScheduledTask(scheduledTask.getTask()).getStartTime();
            int originalTime = originalSchedule.getScheduledTask(scheduledTask.getTask()).getStartTime();
//            System.out.println("InputList: "+ scheduledTasks);
//            System.out.println("schedule" + schedule);
//            System.out.println("original: " +originalSchedule);
            // if after swap, scheduled Task start later (delay) -> not eq
            if (swappedTime > originalTime) {
                flag = true;
//                return false;
                for (IEdge outEdge : _graph.getOutgoingEdges(scheduledTask.getTask())) {
                    INode childNode = outEdge.getChild();
//                  4: T ← tf (nk) + c(ekc) B remote data arrival from nk
                    int T = scheduledTask.getFinishTime() + outEdge.getWeight();
//              5: if nc scheduled then
                    if (schedule.getScheduledTask(childNode) != null) {
                        ScheduledTask childScheduledTask = schedule.getScheduledTask(childNode);
                        System.out.println("Child task is " + childScheduledTask);
//              6: if ts(nc) > T ∧ proc(nc) 6= P then B on same proc always OK
                        if (childScheduledTask.getStartTime() > T && childScheduledTask.getProcessorID() != scheduledTask.getProcessorID()) {
                            System.out.println("Child Task: = " + childScheduledTask.getName()
                                +" (Data arrive time T = " + T + " AND child start time = " + childScheduledTask.getStartTime() + ")");
                            System.out.println("NOT OK");
                            return false;
                        }
//                        if (schedule.getScheduledTask(childNode).getStartTime()
//                            > originalSchedule.getScheduledTask(childNode).getStartTime()){
//                            return false;
//                        }

                    } else {
                            System.out.println("child task " + childNode.getName() + " is not scheduled");
                            return false;
                    }
                }
            }
        }

        if (flag) {
            System.out.println("FLAG");
            for (ScheduledTask scheduledTask : scheduledTasks) {
                System.out.printf("scheduledTask: " + scheduledTask.getName());
                for (IEdge outEdge : _graph.getOutgoingEdges(scheduledTask.getTask())) {
                    INode childNode = outEdge.getChild();
                    System.out.printf(" Child Task:%s ", childNode.getName());
                }
                System.out.println();
            }
        }
        System.out.println("\nOK schedule is deleted, :" + scheduledTasks);
        return true;
    }


    private ScheduleAStar createSubSchedule(ScheduleAStar schedule, ScheduledTask scheduledTask) {
//        Hashtable<INode, Integer> newTable = getChildTable(schedule._inDegreeTable, scheduledTask.getTask());

        return new ScheduleAStar(
                schedule,
                scheduledTask,
                null);
    }

    public Hashtable<INode, Integer> getChildTable(Hashtable<INode, Integer> parentTable, INode x) {
        Hashtable<INode, Integer> tmp = new Hashtable<INode, Integer>(parentTable);
        tmp.remove(x);
        for (IEdge i : _graph.getOutgoingEdges(x.getName())) {
            int somthing = tmp.get(i.getChild()) - 1;
            tmp.replace(i.getChild(), tmp.get(i.getChild()) - 1);
        }
        return tmp;
    }

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

    public boolean isCorrectlySwapped(ScheduleAStar before, ScheduleAStar after,
                                      ScheduledTask m, ScheduledTask taskToSwap){

        if (before.getSize() != after.getSize()){
            System.out.println("INCORRECT - Size is different");
            return false;
        }

        int mSwappedTime = after.getScheduledTask(m.getTask()).getStartTime();
        int tSwappedTime = after.getScheduledTask(taskToSwap.getTask()).getStartTime();
        int mOriginalTime = before.getScheduledTask(m.getTask()).getStartTime();
        int tOriginalTime = before.getScheduledTask(taskToSwap.getTask()).getStartTime();

        if ((mSwappedTime == mOriginalTime) && tSwappedTime ==tOriginalTime){
            System.out.println("INCORRECT - M AND SwappedTask HAVE THE SAME START TIME");
            return false;
        }
        // if m is later than task to be swapped,
        if (mSwappedTime > tSwappedTime){
            System.out.println("INCORRECT - m is scheduled later than (Task to be Swap)");
            return false;
        }

//        if (){
//
//        }
        ArrayList<ScheduledTask> beforeST = before.getAllTaskInProcessor(m.getProcessorID());
        Collections.sort(beforeST,(st1, st2) -> Integer.compare(st1.getStartTime(),st2.getStartTime()));
        ArrayList<ScheduledTask> afterST = after.getAllTaskInProcessor(m.getProcessorID());
        Collections.sort(afterST,(st1, st2) -> Integer.compare(st1.getStartTime(),st2.getStartTime()));

        for (int i = 0; i < beforeST.size(); i++){
            INode beforeNode = beforeST.get(i).getTask();
            INode afterNode = afterST.get(i).getTask();


            if (beforeNode != m.getTask() && beforeNode != taskToSwap.getTask()){
                if (beforeNode != afterNode){
                    System.out.println("INCORRECT - the order is not the same");
                    return false;
                }
            }
        }


        return true;

    }
}
