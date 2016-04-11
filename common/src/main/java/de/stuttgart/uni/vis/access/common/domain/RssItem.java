package de.stuttgart.uni.vis.access.common.domain;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * @author Alexander Dridiger
 */
@Root(name = "item", strict = false)
public class RssItem {

    @Element(name = "title", required = false)
    private String title;

    @Element(name = "link", required = false)
    private String link;

    @Element(name = "description", required = false)
    private String description;

    @Element(name = "pubdate", required = false)
    private String pubdate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPubdate() {
        return pubdate;
    }

    public void setPubdate(String pubdate) {
        this.pubdate = pubdate;
    }
}
