/*
 *------------------------------------------------------------------------------
 *  Copyright (C) 2017 University of Dundee & Open Microscopy Environment.
 *  All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package omero.gateway.model;

import java.awt.Color;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import omero.model.RectangleI;
import omero.model.Shape;


/**
 * 
 * @author Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @since 5.3
 */
@Test(groups = "unit")
public class ShapeSettingsTest {

    @DataProvider(name = "colors")
    public static Object[][] colors() {
       return new Integer[][] {
               {Integer.valueOf(255), Integer.valueOf(0), Integer.valueOf(0),
                   Integer.valueOf(255), Integer.valueOf(-16776961)}, //Red
               {Integer.valueOf(0), Integer.valueOf(255), Integer.valueOf(0),
                   Integer.valueOf(255), Integer.valueOf(16711935)}, //Green
               {Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(255),
                   Integer.valueOf(255), Integer.valueOf(65535)}, //Blue
               {Integer.valueOf(0), Integer.valueOf(255), Integer.valueOf(255),
                   Integer.valueOf(255), Integer.valueOf(16777215)}, //Cyan
               {Integer.valueOf(255), Integer.valueOf(0), Integer.valueOf(255),
                   Integer.valueOf(255), Integer.valueOf(-16711681)}, //Magenta
               {Integer.valueOf(255), Integer.valueOf(255), Integer.valueOf(0),
                   Integer.valueOf(255), Integer.valueOf(-65281)}, //Yellow
               {Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0),
                   Integer.valueOf(255), Integer.valueOf(255)}, //Black
               {Integer.valueOf(255), Integer.valueOf(255), Integer.valueOf(255),
                   Integer.valueOf(255), Integer.valueOf(-1)}, //White
               {Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0),
                   Integer.valueOf(127), Integer.valueOf(127)}, //Transparent black
               {Integer.valueOf(127), Integer.valueOf(127), Integer.valueOf(127),
                   Integer.valueOf(127), Integer.valueOf(2139062143)} //Grey
               };
    }

    /**
     * Tests the encoding/decoding of the fill color.
     */
    @Test(dataProvider = "colors")
    public void testFillColorMapping(Integer r, Integer g, Integer b, Integer a,
            Integer rgba) {
        Color c = new Color(r, g, b, a);
        RectangleData rect = new RectangleData(0, 0, 10, 10);
        ShapeSettingsData data = new ShapeSettingsData((Shape) rect.asIObject());
        data.setFill(c);
        Shape s = (Shape) data.asIObject();
        Assert.assertNotNull(s.getFillColor());
        Assert.assertEquals(s.getFillColor().getValue(), rgba.intValue());
        Color c1 = data.getFill();
        Assert.assertEquals(c1.getRed(), r.intValue());
        Assert.assertEquals(c1.getGreen(), g.intValue());
        Assert.assertEquals(c1.getBlue(), b.intValue());
        Assert.assertEquals(c1.getAlpha(), a.intValue());
    }

    /**
     * Tests the encoding/decoding of the stroke color.
     */
    @Test(dataProvider = "colors")
    public void testStrokeColorMapping(Integer r, Integer g, Integer b, Integer a,
            Integer rgba) {
        Color c = new Color(r, g, b, a);
        RectangleData rect = new RectangleData(0, 0, 10, 10);
        ShapeSettingsData data = new ShapeSettingsData((Shape) rect.asIObject());
        data.setStroke(c);
        Shape s = (Shape) data.asIObject();
        Assert.assertNotNull(s.getStrokeColor());
        Assert.assertEquals(s.getStrokeColor().getValue(), rgba.intValue());
        Color c1 = data.getStroke();
        Assert.assertEquals(c1.getRed(), r.intValue());
        Assert.assertEquals(c1.getGreen(), g.intValue());
        Assert.assertEquals(c1.getBlue(), b.intValue());
        Assert.assertEquals(c1.getAlpha(), a.intValue());
    }
}
