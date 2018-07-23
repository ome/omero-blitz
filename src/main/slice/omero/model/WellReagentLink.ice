    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef WELLREAGENTLINK_ICE
 #define WELLREAGENTLINK_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 module omero {
   module model {
     class Well;
     class Reagent;
     class Details;
     ["protected"] class WellReagentLink
     extends omero::model::IObject
     {
       omero::RInt version;
       omero::RInt getVersion();
       void setVersion(omero::RInt theVersion);
       omero::model::Well parent;
       omero::model::Well getParent();
       void setParent(omero::model::Well theParent);
       omero::model::Reagent child;
       omero::model::Reagent getChild();
       void setChild(omero::model::Reagent theChild);
       void link(Well theParent, Reagent theChild);
     };
   };
 };
 #endif // WELLREAGENTLINK_ICE
