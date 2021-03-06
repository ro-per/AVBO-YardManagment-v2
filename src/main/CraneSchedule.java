package main;

import comparator.MoveComparator;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Data
public class CraneSchedule {
    private List<Crane> cranes;
    private Map<Integer, List<Coordinate2D>> timeline;
    private Map<Integer, List<Coordinate2D>> temp_timeline;
    private static final Logger logger = Logger.getLogger(CraneSchedule.class.getName());

    public static int time;

    // stuff needed for case1(), case2(), case3()
    private int craneID, otherCraneID, deltaX, deltaY, deltaDif, deltaHelper, xFactor, yFactor, timeToBeTravelled;
    private Crane crane;
    //    private Coordinate2D start, stop;
    private boolean xPositive, yPositive;
    private int x, y;

    private int startTime = 0;

    Coordinate2D yard_min = new Coordinate2D(-1, 0);
    Coordinate2D yard_max = new Coordinate2D(Yard.L + 1, 0);

    private List<CraneMove> moves;


    public CraneSchedule() {
        logger.setLevel(Level.OFF);
        cranes = new ArrayList<>(2);
        timeline = new HashMap<>();
        time = 0;
        moves = new ArrayList<>();
    }

    private void setOtherCrane() {
        if (this.crane.getId() == 2) otherCraneID = 1;
        else otherCraneID = 2;
    }

    /**
     * @param container container which has to be moved
     * @param place     coordinate of the place where the container needs to be placed
     * @return true if move could be planned -> state yard changes
     */
    public boolean canMove(Container container, Coordinate2D place, int q) {
        Coordinate2D cCoo = container.getCenter();
        this.crane = cranes.get(q - 1);
        this.craneID = crane.getId();
        this.setOtherCrane();

        Coordinate2D craneCoo = timeline.get(time).get(craneID - 1);

        int temp = time;
        while (craneCoo.compareOR(yard_min, yard_max)) {
            temp--;
            craneCoo = timeline.get(temp).get(craneID - 1);
        }

        startTime = this.time;

        CraneMove m1, m2, m3, m4;
        if (move(craneCoo, cCoo)) {

            // PRINT OUT BEFORE PICKING UP
            m1 = new CraneMove(time, crane.getId(), cCoo.getX(), cCoo.getY(), container.getId());
            // PICK UP
            addDropAndPickUpTimeState(crane);
            // PRINT OUT AFTER PICKING UP
            m2 = new CraneMove(time, crane.getId(), cCoo.getX(), cCoo.getY());


            boolean b = move(cCoo, place);
            //drop time
            if (b) {
                // PRINT OUT BEFORE DROPPING UP
                m3 = new CraneMove(time, crane.getId(), place.getX(), place.getY(), container.getId());
                // PICK UP
                addDropAndPickUpTimeState(crane);
                // PRINT OUT AFTER DROPPING UP
                m4 = new CraneMove(time, crane.getId(), place.getX(), place.getY());


                moves.add(m1);
                moves.add(m2);
                moves.add(m3);
                moves.add(m4);
            }


            printTimeLine();
            return b;
        } else
            return false;
    }

    private boolean move(Coordinate2D start, Coordinate2D stop) {
        int temp_t = -1;
        deltaX = Math.abs(start.getX() - stop.getX());
        deltaY = Math.abs(start.getY() - stop.getY());
        deltaDif = Math.abs(deltaX - deltaY);
        deltaHelper = deltaDif + 1;


        determineDirection(start, stop); // left or right

        switch (determineRouteType()) {
            case 1:
                timeToBeTravelled = deltaX;
                temp_t = case1(start, stop);
                break;
            case 2:

                timeToBeTravelled = deltaY;
                temp_t = case2(start, stop);
                break;
            case 3:

                timeToBeTravelled = deltaY;
                temp_t = case3(start, stop);
                break;
            default:
                logger.severe("Took crane that's not possible: craneid=" + craneID);
                break;
        }

        if (temp_t < time + timeToBeTravelled) {
            //ystem.out.println("Error on time:" + temp_t);

            Coordinate2D coo;
            if (craneID == 1) coo = yard_min;
            else coo = yard_max;

            while (temp_t > startTime) {

                timeline.get(temp_t).set(craneID - 1, coo);
                temp_t--;
            }

            logger.info("Move undone");
            printTimeLine();
            return false;
        }

        this.time += timeToBeTravelled;
        return true;

    }

    private int determineRouteType() {
        //DETERMINE THE CASE
        if (deltaX - deltaY >= 0) {
            return 1;
        } else {
            if (craneID == 1) {
                if (xPositive) return 2;
                else return 3;
            } else if (craneID == 2) {
                if (xPositive) return 3;
                else return 2;
            }
        }
        return -1;
    }

    private void determineDirection(Coordinate2D start, Coordinate2D stop) {
        xPositive = start.getX() - stop.getX() < 0;
        yPositive = start.getY() - stop.getY() < 0;

        if (xPositive) xFactor = 1;
        else xFactor = -1;

        if (yPositive) yFactor = 1;
        else yFactor = -1;

    }

    /**
     * Distance to be travelled over x-axis is larger then or equals distance to be travelled over y-axis
     * <p>
     * time
     * craneCoo coordinate of crane on time move is tried
     * start    start coordinate of move
     * stop     stop coordinate of move
     * true if move could be planned
     *
     * @param start
     * @param stop
     */
    private int case1(Coordinate2D start, Coordinate2D stop) {
        // first x,y then x
        boolean planned;
        x = start.getX();
        y = start.getY();

        int t0, t1, t2;
        t0 = time;
        t1 = time + deltaY;
        t2 = time + deltaX + 1;

        for (int t = t0; t < t1; t++) {
            planned = tryMove(t, new Coordinate2D(x, y));
            if (!planned) return t;
            x = x + (xFactor);
            y = y + (yFactor);
        }
        for (int t = t1; t < t2; t++) {
            planned = tryMove(t, new Coordinate2D(x, y));
            if (!planned) return t;
            x = x + (xFactor);
        }

        // All moves could be done
        return t2;
    }

    /**
     * Distance to be travelled over y-axis is larger then or equals distance to be travelled over x-axis
     * Moves that will happen: first y++ until deltaDif is reached, followed by x++ and y++ simultaneously
     * <p>
     * time
     * craneCoo
     * start    start coordinate of move
     * stop     stop coordinate of move
     * deltaDif difference between distance over x-axis and y-axis
     *
     * @param start
     * @param stop
     * @return
     */
    private int case2(Coordinate2D start, Coordinate2D stop) {
        // first y then x,y

        boolean planned;
        x = start.getX();
        y = start.getY();

        int t0, t1, t2;
        t0 = time;
        t1 = time + deltaDif;
        t2 = time + deltaY + 1;

        for (int t = t0; t < t1; t++) {
            planned = tryMove(t, new Coordinate2D(x, y));
            if (!planned) return t;
            y = y + (yFactor);
        }
        for (int t = t1; t < t2; t++) {
            planned = tryMove(t, new Coordinate2D(x, y));
            if (!planned) return t;
            x = x + (xFactor);
            y = y + (yFactor);
        }

        // All moves could be done
        return t2;
    }

    /**
     * Distance to be travelled over y-axis is larger then or equals distance to be travelled over x-axis
     * Moves that will happen: first x++ and y++ simultaneously until deltaDif is reached, followed by y++
     * <p>
     * time
     * craneCoo
     * start    start coordinate of move
     * stop     stop coordinate of move
     * deltaDif difference between distance over x-axis and y-axis
     *
     * @param start
     * @param stop
     * @return
     */
    private int case3(Coordinate2D start, Coordinate2D stop) {
        // first x,y then y

        boolean planned;
        x = start.getX();
        y = start.getY();

        int t0, t1, t2;
        t0 = time;
        t1 = time + deltaX;
        t2 = time + deltaY + 1;

        for (int t = t0; t < t1; t++) {
            planned = tryMove(t, new Coordinate2D(x, y));
            if (!planned) return t;
            x = x + (xFactor);
            y = y + (yFactor);
        }
        for (int t = t1; t < t2; t++) {
            planned = tryMove(t, new Coordinate2D(x, y));
            if (!planned) return t;
            y = y + (yFactor);
        }

        // All moves could be done
        return t2;
    }

    private boolean tryMove(int t, Coordinate2D coo) {

        List<Coordinate2D> coordinate2DS1 = timeline.get(t);

        if (timeline.containsKey(t)) {
            if (coordinate2DS1.get(craneID - 1) != null) {
                if (!coordinate2DS1.get(craneID - 1).compareOR(yard_min, yard_max)) {
                    if (!(coordinate2DS1.get(craneID - 1).equals(coo))) {
                        logger.warning("Error t:" + t + " currCoo:" + coo + " timeline:" + coordinate2DS1.get(craneID - 1));
                    }
                }
            }
        } else {
            //Add new
            List<Coordinate2D> coordinate2DS = new ArrayList<>(2);
            coordinate2DS.add(yard_min);
            coordinate2DS.add(yard_max);
            timeline.put(t, coordinate2DS);
            coordinate2DS1 = coordinate2DS;
        }
        // collision detection
        boolean collision = false;

        timeline.get(t).set(craneID - 1, coo);

        if (xPositive && craneID == 1) {
            if (x + cranes.get(craneID - 1).getDelta() > timeline.get(t).get(otherCraneID - 1).getX()) {
                collision = true;
            }
        }
        if (!xPositive && craneID == 2) {
            if (x - cranes.get(craneID - 1).getDelta() < timeline.get(t).get(otherCraneID - 1).getX()) {
                collision = true;
            }
        }

        if (collision) {
            logger.severe("Collisions !!! t:" + t + " current crane" + coordinate2DS1.get(craneID - 1) + " other crane" + coordinate2DS1.get(otherCraneID - 1));
            printTimeLine();
            return false;
        }


        return true;
    }


    private void addDropAndPickUpTimeState(Crane q) {

        for (int i = 0; i < Crane.DROP_TIME; i++) {
            time++;
            if (timeline.get(time) == null) {
                timeline.put(time, new ArrayList<>());
                Coordinate2D prev = timeline.get(time - 1).get(0);
                timeline.get(time).add(prev);
                prev = timeline.get(time - 1).get(1);
                timeline.get(time).add(prev);
            }
        }
    }

    private void addState(int t, Crane q, Coordinate2D coo) {

        if (timeline.get(t) == null) {
            timeline.put(t, new ArrayList<>());
        }

        List<Coordinate2D> temp = timeline.get(t);


        timeline.get(t).add(q.getId() - 1, coo);
    }


    public void addCrane(Crane crane) {
        this.cranes.add(crane);
        addState(0, crane, crane.getStartCoordinate());

        moves.add(new CraneMove(time, crane.getId(), crane.getStartCoordinate().getX(), crane.getStartCoordinate().getY()));

    }

    public String getPlanning() {
        StringBuilder sb = new StringBuilder();

        moves.sort(new MoveComparator());

        for (CraneMove cm : moves) {
            sb.append(cm).append("\n");
        }


        return sb.toString();
    }

    public void printTimeLine() {
        int size = timeline.size();

        List<List<Coordinate2D>> temp = new ArrayList<>(size);

        for (int t = 0; t < size; t++) temp.add(timeline.get(t));

        int t = 0;
        for (List<Coordinate2D> couple : temp) {

            StringBuilder sb = new StringBuilder();
            sb.append("t").append(t);
            sb.append("\t q1:").append(couple.get(0));
            sb.append("\t q2:").append(couple.get(1));
            //System.out.println(sb.toString());
            t++;
        }
    }
}
