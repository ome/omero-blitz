    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef PROJECTIONAXIS_ICE
 #define PROJECTIONAXIS_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 module omero {
   module model {

     module enums {
         const string ProjectionAxisT = "T";
         const string ProjectionAxisModuloT = "ModuloT";
         const string ProjectionAxisZ = "Z";
         const string ProjectionAxisModuloZ = "ModuloZ";
     };

     class Details;
     ["protected"] class ProjectionAxis
     extends omero::model::IObject
     {
       omero::RString value;
       omero::RString getValue();
       void setValue(omero::RString theValue);
     };
   };
 };
 #endif // PROJECTIONAXIS_ICE
