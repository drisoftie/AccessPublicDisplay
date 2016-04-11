/**
 * **************************************************************************
 * <p>
 * Copyright 2012-2013 Sony Corporation
 * <p>
 * The information contained here-in is the property of Sony corporation and
 * is not to be disclosed or used without the prior written permission of
 * Sony corporation. This copyright extends to all media in which this
 * information may be preserved including magnetic storage, computer
 * print-out or visual display.
 * <p>
 * Contains proprietary information, copyright and database rights Sony.
 * Decompilation prohibited save as permitted by law. No using, disclosing,
 * reproducing, accessing or modifying without Sony prior written consent.
 * <p>
 * **************************************************************************
 */
package de.stuttgart.uni.vis.access.server.util;

import org.simpleframework.xml.core.Persister;

import de.stuttgart.uni.vis.access.common.domain.Feed;

/**
 * @author Alexander Dridiger
 */
public class XmlParserUtil {

    public static Feed parseRss(String url) throws Exception {
        Persister serializer = new Persister();

        return serializer.read(Feed.class, url);
    }
}