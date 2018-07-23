    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef POINT_ICE
 #define POINT_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 #include <omero/model/Shape.ice>
 module omero {
   module model {
     class Roi;
     class AffineTransform;
     class Length;
     class Length;
     class ShapeAnnotationLink;
     class Annotation;
     class Details;
     ["protected"] class Point
     extends omero::model::Shape
     {
       omero::RDouble x;
       omero::RDouble getX();
       void setX(omero::RDouble theX);
       omero::RDouble y;
       omero::RDouble getY();
       void setY(omero::RDouble theY);
       omero::RString textValue;
       omero::RString getTextValue();
       void setTextValue(omero::RString theTextValue);
     };
   };
 };
 #endif // POINT_ICE
