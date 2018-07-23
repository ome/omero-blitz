    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef AFFINETRANSFORM_ICE
 #define AFFINETRANSFORM_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 module omero {
   module model {
     class Details;
     ["protected"] class AffineTransform
     extends omero::model::IObject
     {
       omero::RInt version;
       omero::RInt getVersion();
       void setVersion(omero::RInt theVersion);
       omero::RDouble a00;
       omero::RDouble getA00();
       void setA00(omero::RDouble theA00);
       omero::RDouble a10;
       omero::RDouble getA10();
       void setA10(omero::RDouble theA10);
       omero::RDouble a01;
       omero::RDouble getA01();
       void setA01(omero::RDouble theA01);
       omero::RDouble a11;
       omero::RDouble getA11();
       void setA11(omero::RDouble theA11);
       omero::RDouble a02;
       omero::RDouble getA02();
       void setA02(omero::RDouble theA02);
       omero::RDouble a12;
       omero::RDouble getA12();
       void setA12(omero::RDouble theA12);
     };
   };
 };
 #endif // AFFINETRANSFORM_ICE
