/*
 *   $Id$
 *
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 *
 */
package omero.model;

import java.util.List;

public class SmartMaskI extends omero.model.MaskI implements SmartShape {
    public List<Point> asPath() {
        throw new UnsupportedOperationException();
    }
}