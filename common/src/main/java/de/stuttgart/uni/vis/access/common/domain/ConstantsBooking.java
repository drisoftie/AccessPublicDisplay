package de.stuttgart.uni.vis.access.common.domain;

/**
 * @author Alexander Dridiger
 */
public class ConstantsBooking {

    public enum StateBooking {

        START("book"),
        FINISH("finish"),
        TABLE("table"),
        DISH("dish"),
        TIME("time"),
        PERSONS("persons");

        String state;

        StateBooking(String state) {
            this.state = state;
        }

        public String getState() {
            return state;
        }
    }
}
