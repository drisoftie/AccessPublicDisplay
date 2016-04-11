package de.stuttgart.uni.vis.access.server.service.bl;

import org.simpleframework.xml.core.Persister;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import de.stuttgart.uni.vis.access.common.domain.Feed;

/**
 * @author Alexander Dridiger
 */
public class ProviderNews {

    private static ProviderNews inst;

    private Feed feedNews;

    private ProviderNews() {
        inst = this;
    }

    public static ProviderNews inst() {
        if (inst == null) {
            new ProviderNews();
        }
        return inst;
    }

    public boolean hasNewsInfo() {
        return feedNews != null;
    }

    public Feed getFeedNews() {
        return feedNews;
    }

    public void createNews() {
        //        https://www.bing.com/news/search?q=Stuttgart&first=0&format=RSS
        //        https://news.google.com/news/feeds?num=2&ned=de&q=Stuttgart&output=rss
        InputStream fis;
        try {
            HttpsURLConnection conn = (HttpsURLConnection) new URL("https://news.google.com/news/feeds?num=2&ned=de&q=Stuttgart&output=rss")
                    .openConnection();

            fis = conn.getInputStream();
            StringBuilder  inputStringBuilder = new StringBuilder();
            BufferedReader bufferedReader     = new BufferedReader(new InputStreamReader(fis));
            String         line               = bufferedReader.readLine();
            while (line != null) {
                inputStringBuilder.append(line);
                inputStringBuilder.append('\n');
                line = bufferedReader.readLine();
            }
            String    s          = inputStringBuilder.toString();
            Persister serializer = new Persister();
            feedNews = serializer.read(Feed.class, s);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //        try {
        //            feedNews = XmlParserUtil.parseRss("https://news.google.com/news/feeds?num=2&q=Stuttgart&output=rss");
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //        }
    }
}
