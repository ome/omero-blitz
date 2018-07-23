    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef FILESETENTRY_ICE
 #define FILESETENTRY_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 module omero {
   module model {
     class Fileset;
     class OriginalFile;
     class Details;
     ["protected"] class FilesetEntry
     extends omero::model::IObject
     {
       omero::RInt version;
       omero::RInt getVersion();
       void setVersion(omero::RInt theVersion);
       omero::model::Fileset fileset;
       omero::model::Fileset getFileset();
       void setFileset(omero::model::Fileset theFileset);
       omero::model::OriginalFile originalFile;
       omero::model::OriginalFile getOriginalFile();
       void setOriginalFile(omero::model::OriginalFile theOriginalFile);
       omero::RString clientPath;
       omero::RString getClientPath();
       void setClientPath(omero::RString theClientPath);
     };
   };
 };
 #endif // FILESETENTRY_ICE
