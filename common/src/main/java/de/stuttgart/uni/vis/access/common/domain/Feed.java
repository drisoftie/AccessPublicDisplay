package de.stuttgart.uni.vis.access.common.domain;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.util.Collection;

/**
 * @author Alexander Dridiger
 */
@Root(name = "rss", strict = false)
public class Feed {

    @Element(name = "title", required = false)
    @Path("channel")
    private String title;

    @Element(name = "link", required = false)
    @Path("channel")
    private String link;

    @Element(name = "description", required = false)
    @Path("channel")
    private String description;

    @ElementList(inline = true, required = false)
    @Path("channel")
    private Collection<RssItem> items;

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

    public Collection<RssItem> getItems() {
        return items;
    }

    public void setItems(Collection<RssItem> items) {
        this.items = items;
    }
}
