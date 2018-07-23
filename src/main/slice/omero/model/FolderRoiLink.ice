    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef FOLDERROILINK_ICE
 #define FOLDERROILINK_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 module omero {
   module model {
     class Folder;
     class Roi;
     class Details;
     ["protected"] class FolderRoiLink
     extends omero::model::IObject
     {
       omero::RInt version;
       omero::RInt getVersion();
       void setVersion(omero::RInt theVersion);
       omero::model::Folder parent;
       omero::model::Folder getParent();
       void setParent(omero::model::Folder theParent);
       omero::model::Roi child;
       omero::model::Roi getChild();
       void setChild(omero::model::Roi theChild);
       void link(Folder theParent, Roi theChild);
     };
   };
 };
 #endif // FOLDERROILINK_ICE
