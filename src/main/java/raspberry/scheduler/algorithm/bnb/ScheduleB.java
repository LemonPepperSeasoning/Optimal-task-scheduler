package raspberry.scheduler.algorithm.bnb;

import java.util.*;
import java.util.function.Consumer;

import raspberry.scheduler.algorithm.astar.ScheduleAStar;
import raspberry.scheduler.algorithm.common.Schedule;
import raspberry.scheduler.algorithm.common.ScheduledTask;
import raspberry.scheduler.graph.INode;


/**
 * The Schedule class represents a partial schedule of the tasks
 * Schedule has a Linked list implementation connected to parents
 *
 * @author Takahiro
 */
public class ScheduleB extends Schedule implements Comparable<ScheduleB>, Iterable<ScheduleB> {


    private int _overallFinishTime; // t: Total weight
    private int _maxPid; //The largest pid currently used to schedule. This ranges from 1 ~ n. (not 0 ~ n-1)
    private Hashtable<INode, Integer> _inDegreeTable;

    private int _upperBound;    // For BNB. Represents the worst case. <- Bad schedling.
    private int _lowerBound;   // For BNB. Represents the base case. <- perfect schedling.


    public ScheduleB(ScheduledTask scheduleTask, Hashtable<INode, Integer> inDegreeTable) {
        super(scheduleTask);
        _inDegreeTable = inDegreeTable;
        _maxPid = scheduleTask.getProcessorID();
        _overallFinishTime = scheduleTask.getFinishTime();
    }

    /**
     * Constructor for partial schedule
     */
    public ScheduleB(ScheduleB parent, ScheduledTask scheduleTask, Hashtable<INode, Integer> inDegreeTable) {
        super(parent, scheduleTask);
        _inDegreeTable = inDegreeTable;
        if (scheduleTask.getProcessorID() > parent.getMaxPid()) {
            _maxPid = scheduleTask.getProcessorID();
        } else {
            _maxPid = parent.getMaxPid();
        }
        _overallFinishTime = Math.max(parent._overallFinishTime, scheduleTask.getFinishTime());
    }

    /**
     * Add lower bound to this schedule.
     *
     * @param l : lower bound value.
     */
    public void addLowerBound(int l) {
        if (getParent() == null) {
            _lowerBound = l; //Pretty sure this is better
//            _lowerBound = super.getScheduledTask().getFinishTime(); //old code
        } else {
            _lowerBound = Math.max(getParent().getLowerBound(), l);
        }
    }

    /**
     * Compare two Schedule instance. Uses to put Schedule in priority Queue
     *
     * @param schedule : A schedule to compare to
     * @return : 1 : if s's total weight is smaller,
     * -1 : if s's total weight is bigger,
     * 0 : Two scheudle has same total weight.
     */
    @Override
    public int compareTo(ScheduleB schedule) {
        return _lowerBound > schedule.getLowerBound() ? 1 : _lowerBound < schedule.getLowerBound() ? -1 : 0;
    }


    /**
     * Check if two Schedule instance is the same. (this is for detecting duplicate scheduling)
     *
     * @param otherSchedule : the other schedule instance we are comparing to.
     * @return Boolean : True : if its the same.
     * False: if its different.
     */
    public boolean equals2(Object otherSchedule) {
        if (otherSchedule == this) {
            return true;
        } else if (!(otherSchedule instanceof ScheduleB)) {
            return false;
        } else {
            ScheduleB oSchedule = (ScheduleB) otherSchedule;
            if (getSize() != oSchedule.getSize()) {
                return false;
            } else if (getMaxPid() != oSchedule.getMaxPid()) {
                return false;
            } else {
                // Group by pid. Compare match
                Hashtable<INode, int[]> scheduling = getPath();
                Hashtable<INode, int[]> scheduling2 = oSchedule.getPath();

                Hashtable<Integer, Hashtable<String, Integer>> hash4scheduling = new Hashtable<Integer, Hashtable<String, Integer>>();
                Hashtable<Integer, Hashtable<String, Integer>> hash4scheduling2 = new Hashtable<Integer, Hashtable<String, Integer>>();

                for (INode s : scheduling.keySet()) {
                    Hashtable<String, Integer> tmp = hash4scheduling.get(scheduling.get(s)[2]); //get(0) gets pid
                    if (tmp == null) {
                        tmp = new Hashtable<String, Integer>();
                    }
                    tmp.put(s.getName(), scheduling.get(s)[0]);
                    hash4scheduling.put(scheduling.get(s)[2], tmp);
                }
                for (INode s : scheduling2.keySet()) {
                    Hashtable<String, Integer> tmp = hash4scheduling2.get(scheduling2.get(s)[2]); //get(0) gets pid
                    if (tmp == null) {
                        tmp = new Hashtable<String, Integer>();
                    }
                    tmp.put(s.getName(), scheduling2.get(s)[0]);
                    hash4scheduling2.put(scheduling2.get(s)[2], tmp);
                }

                for (Hashtable<String, Integer> i : hash4scheduling.values()) {
                    Boolean foundMatch = false;
                    for (Hashtable<String, Integer> j : hash4scheduling2.values()) {
                        if (i.equals(j)) {
                            foundMatch = true;
                            break;
                        }
                    }
                    if (!foundMatch) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    //Risky version of equals. Dont know if this actually outputs optimal path.

    /**
     * Check if two Schedule instance is the same. (this is for detecting duplicate scheduling)
     *
     * @param otherSchedule : Schedule we are comparing against.
     * @return true if its the same. false if it is not the same schedule.
     */
    public boolean equals3(Object otherSchedule) {
        if (otherSchedule == this) {
            return true;
        } else if (!(otherSchedule instanceof ScheduleB)) {
            return false;
        } else {
            ScheduleB schedule = (ScheduleB) otherSchedule;
            if (this.getSize() != schedule.getSize()) {
                return false;
            } else if (this.getMaxPid() != schedule.getMaxPid()) {
                return false;
            } else {
                Set<List<Integer>> hash4scheduling = new HashSet<List<Integer>>();
                Set<List<Integer>> hash4scheduling2 = new HashSet<List<Integer>>();

                ArrayList<ScheduledTask> scheduledTasks = new ArrayList<ScheduledTask>();
                this.forEach(cSchedule -> scheduledTasks.add(cSchedule.getScheduledTask()));

                ArrayList<ScheduledTask> scheduledTasks2 = new ArrayList<ScheduledTask>();
                schedule.forEach(cSchedule -> scheduledTasks2.add(cSchedule.getScheduledTask()));

                for (ScheduledTask task : scheduledTasks) {
                    hash4scheduling.add(Arrays.asList(task.getName().hashCode(), task.getStartTime()));
                }
                for (ScheduledTask task : scheduledTasks2) {
                    hash4scheduling2.add(Arrays.asList(task.getName().hashCode(), task.getStartTime()));
                }
                return hash4scheduling.equals(hash4scheduling2);
            }
        }
    }

    /**
     * Get set of task for Equals() function
     *
     * @return Set of Integer Array : representing the schedule
     */
    public Set<int[]> getTaskForEqual() {
        Set<int[]> tmp;
        if (getParent() == null) {
            tmp = new HashSet<int[]>();
        } else {
            tmp = getParent().getTaskForEqual();
        }
        tmp.add(new int[]{super.getScheduledTask().getStartTime(),
                super.getScheduledTask().getTask().getName().hashCode()});
        return tmp;
    }

    /**
     * Gets the full path of the partial schedule.
     * (as Schedule instance is linked with parents like linked list)
     *
     * @return : Hashtable :  key : task (INode)
     * Value : List of Integers. ( size of 3 )
     * index 0 : start time of the task
     * index 1 : finsih time of the task
     * index 2 : processor id of the task.
     */
    public Hashtable<INode, int[]> getPath() {
        Hashtable<INode, int[]> tmp;
        if (getParent() == null) {
            tmp = new Hashtable<INode, int[]>();
        } else {
            tmp = getParent().getPath();
        }
        tmp.put(super.getScheduledTask().getTask(),
                new int[]{super.getScheduledTask().getStartTime(),
                        super.getScheduledTask().getFinishTime(),
                        super.getScheduledTask().getProcessorID()});
        return tmp;
    }

    /**
     * Get a hash value.
     * (I didnt override the ACTUAL hash function, I needed two kinds of hash function.
     * v1 : perfect hash function that gurantees almost no collision & it is calculated using all variable contained in this class.
     * v2 : hash function that produce some what unique hash value depending on the "scheduling" hashtable.
     * )
     * This function is v2.
     *
     * @return int : representing the hash value of "scheduling" hashtable.
     */
    public int getHash() {
        final int prime = 17;
        int value = 0;
        Hashtable<INode, int[]> scheduling = getPath();
        for (INode i : scheduling.keySet()) {
            value = prime * value + (scheduling.get(i)[0]);
            value = prime * value + (i.getName().hashCode()); //Might be fine just doing i.hasCode()
        }
        value = prime * value + getSize();
        return value;
    }

    /*
    Getter and Setters
     */

    /**
     * get Total weight
     *
     * @return _total
     */
    public int getOverallFinishTime() {
        return _overallFinishTime;
    }

    /**
     * get Start Time the time this node start running.
     *
     * @return _startTime the time this node start running.
     */
    public int getStartTime() {
        return super.getScheduledTask().getStartTime();
    }

    /**
     * get node being scheduled
     *
     * @return _node the node being  scheduled
     */
    public INode getNode() {
        return super.getScheduledTask().getTask();
    }

    /**
     * get parent schedule the Parent Schedule
     *
     * @return _parent the Parent Schedule
     */
    public ScheduleB getParent() {
        return (ScheduleB) super.getParent();
    }


    public int getLowerBound() {
        return _lowerBound;
    }

    /**
     * get max pid The largest pid currently used to schedule
     *
     * @return _maxPid The largest pid currently used to schedule
     */
    public int getMaxPid() {
        return _maxPid;
    }

    public Hashtable<INode, Integer> getIndegreeTable() {
        return _inDegreeTable;
    }

    @Override
    public Iterator<ScheduleB> iterator() {
        ScheduleB head = this;
        return new Iterator<ScheduleB>() {
            ScheduleB current = head;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public ScheduleB next() {
                if (hasNext()) {
                    ScheduleB data = current;
                    current = current.getParent();
                    return data;
                }
                return null;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove not implemented.");
            }
        };
    }

    @Override
    public void forEach(Consumer<? super ScheduleB> action) {
        Iterable.super.forEach(action);
    }

    @Override
    public Spliterator<ScheduleB> spliterator() {
        return Iterable.super.spliterator();
    }

}