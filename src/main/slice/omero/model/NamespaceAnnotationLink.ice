    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef NAMESPACEANNOTATIONLINK_ICE
 #define NAMESPACEANNOTATIONLINK_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 module omero {
   module model {
     class Namespace;
     class Annotation;
     class Details;
     ["protected"] class NamespaceAnnotationLink
     extends omero::model::IObject
     {
       omero::RInt version;
       omero::RInt getVersion();
       void setVersion(omero::RInt theVersion);
       omero::model::Namespace parent;
       omero::model::Namespace getParent();
       void setParent(omero::model::Namespace theParent);
       omero::model::Annotation child;
       omero::model::Annotation getChild();
       void setChild(omero::model::Annotation theChild);
       void link(Namespace theParent, Annotation theChild);
     };
   };
 };
 #endif // NAMESPACEANNOTATIONLINK_ICE
