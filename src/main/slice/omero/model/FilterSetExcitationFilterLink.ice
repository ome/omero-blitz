    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef FILTERSETEXCITATIONFILTERLINK_ICE
 #define FILTERSETEXCITATIONFILTERLINK_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 module omero {
   module model {
     class FilterSet;
     class Filter;
     class Details;
     ["protected"] class FilterSetExcitationFilterLink
     extends omero::model::IObject
     {
       omero::RInt version;
       omero::RInt getVersion();
       void setVersion(omero::RInt theVersion);
       omero::model::FilterSet parent;
       omero::model::FilterSet getParent();
       void setParent(omero::model::FilterSet theParent);
       omero::model::Filter child;
       omero::model::Filter getChild();
       void setChild(omero::model::Filter theChild);
       void link(FilterSet theParent, Filter theChild);
     };
   };
 };
 #endif // FILTERSETEXCITATIONFILTERLINK_ICE
