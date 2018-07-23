    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef CODOMAINMAPCONTEXT_ICE
 #define CODOMAINMAPCONTEXT_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 module omero {
   module model {
     class ChannelBinding;
     class Details;
     ["protected"] class CodomainMapContext
     extends omero::model::IObject
     {
       omero::RInt version;
       omero::RInt getVersion();
       void setVersion(omero::RInt theVersion);
       omero::model::ChannelBinding channelBinding;
       omero::model::ChannelBinding getChannelBinding();
       void setChannelBinding(omero::model::ChannelBinding theChannelBinding);
     };
   };
 };
 #endif // CODOMAINMAPCONTEXT_ICE
