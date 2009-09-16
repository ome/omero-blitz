/*
 *   $Id$
 *   
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.roi.test;

import static omero.rtypes.rlong;
import static omero.rtypes.rstring;
import static omero.rtypes.rtime;

import java.util.List;

import omero.constants.namespaces.NSMEASUREMENT;
import omero.model.FileAnnotation;
import omero.model.FileAnnotationI;
import omero.model.Format;
import omero.model.FormatI;
import omero.model.Image;
import omero.model.ImageAnnotationLink;
import omero.model.ImageAnnotationLinkI;
import omero.model.ImageI;
import omero.model.OriginalFile;
import omero.model.OriginalFileI;
import omero.model.Plate;
import omero.model.PlateI;
import omero.model.Roi;
import omero.model.RoiAnnotationLink;
import omero.model.RoiAnnotationLinkI;
import omero.model.Shape;
import omero.model.Well;
import omero.model.WellI;
import omero.model.WellSample;
import omero.model.WellSampleI;

import org.testng.annotations.Test;

/**
 *
 */
@Test(groups = { "integration", "rois" })
public class ImageMeasurementsTest extends AbstractRoiITest {

    Image i;

    private void makeImage() throws Exception {
        i = new ImageI();
        i.setName(rstring("RoiPerformanceTest"));
        i.setAcquisitionDate(rtime(0));
        i = assertSaveAndReturn(i);
        i.unload();
    }
    
    @Test
    public void testMeasurements() throws Exception {
        makeImage();
        Roi roi = createRoi(i, "meas", geomTool.random(1).toArray(new Shape[0]));

        FileAnnotation fa = new FileAnnotationI();
        fa.setNs(rstring(NSMEASUREMENT.value));
        OriginalFile file = new OriginalFileI();
        file.setName(rstring("meas"));
        file.setSha1(rstring("meas"));
        file.setPath(rstring("meas"));
        file.setAtime(rtime(0));
        file.setCtime(rtime(0));
        file.setMtime(rtime(0));
        file.setSize(rlong(0));
        Format fmt = new FormatI();
        fmt.setValue(rstring("OMERO.tables"));
        file.setFormat(fmt);
        fa.setFile(file);
        fa = assertSaveAndReturn(fa);

        Plate plate = new PlateI();
        plate.setName(rstring("meas"));
        plate.linkAnnotation(fa);
        Well well = new WellI();
        WellSample sample = new WellSampleI();
        sample.setImage(i);
        well.addWellSample(sample);
        plate.addWell(well);

        plate = assertSaveAndReturn(plate);
        RoiAnnotationLink rlink = new RoiAnnotationLinkI();
        ImageAnnotationLink ilink = new ImageAnnotationLinkI();
        rlink.link(roi, fa);
        ilink.link(i, fa);
        assertSaveAndReturn(rlink);
        assertSaveAndReturn(ilink);
        
        List<FileAnnotation> fas = assertGetImageMeasurements(i.getId().getValue());
        assertEquals(fas.toString(), 1, fas.size());
    }

}