    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef THUMBNAILGENERATIONJOB_ICE
 #define THUMBNAILGENERATIONJOB_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 #include <omero/model/Job.ice>
 module omero {
   module model {
     class JobStatus;
     class JobOriginalFileLink;
     class OriginalFile;
     class Details;
     ["protected"] class ThumbnailGenerationJob
     extends omero::model::Job
     {
     };
   };
 };
 #endif // THUMBNAILGENERATIONJOB_ICE
