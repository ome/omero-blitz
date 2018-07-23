    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef INSTRUMENTANNOTATIONLINK_ICE
 #define INSTRUMENTANNOTATIONLINK_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 module omero {
   module model {
     class Instrument;
     class Annotation;
     class Details;
     ["protected"] class InstrumentAnnotationLink
     extends omero::model::IObject
     {
       omero::RInt version;
       omero::RInt getVersion();
       void setVersion(omero::RInt theVersion);
       omero::model::Instrument parent;
       omero::model::Instrument getParent();
       void setParent(omero::model::Instrument theParent);
       omero::model::Annotation child;
       omero::model::Annotation getChild();
       void setChild(omero::model::Annotation theChild);
       void link(Instrument theParent, Annotation theChild);
     };
   };
 };
 #endif // INSTRUMENTANNOTATIONLINK_ICE
