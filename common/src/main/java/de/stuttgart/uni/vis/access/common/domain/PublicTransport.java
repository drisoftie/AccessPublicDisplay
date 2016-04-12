package de.stuttgart.uni.vis.access.common.domain;

/**
 * @author Alexander Dridiger
 */
public class PublicTransport {

    private String        line;
    private String        time;
    private String        departureIn;
    private PubTranspType type;

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDepartureIn() {
        return departureIn;
    }

    public void setDepartureIn(String departureIn) {
        this.departureIn = departureIn;
    }

    public PubTranspType getType() {
        return type;
    }

    public void setType(PubTranspType type) {
        this.type = type;
    }
}
