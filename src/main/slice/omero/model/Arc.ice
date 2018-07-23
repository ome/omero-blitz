    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef ARC_ICE
 #define ARC_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 #include <omero/model/LightSource.ice>
 module omero {
   module model {
     class ArcType;
     class Power;
     class Instrument;
     class LightSourceAnnotationLink;
     class Annotation;
     class Details;
     ["protected"] class Arc
     extends omero::model::LightSource
     {
       omero::model::ArcType type;
       omero::model::ArcType getType();
       void setType(omero::model::ArcType theType);
     };
   };
 };
 #endif // ARC_ICE
