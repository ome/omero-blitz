    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef PLATEACQUISITION_ICE
 #define PLATEACQUISITION_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 module omero {
   module model {
     class Plate;
     class WellSample;
     class PlateAcquisitionAnnotationLink;
     class Annotation;
     class Details;
     ["java:type:java.util.ArrayList"] sequence<omero::model::WellSample> PlateAcquisitionWellSampleSeq;
     ["java:type:java.util.ArrayList"] sequence<omero::model::PlateAcquisitionAnnotationLink> PlateAcquisitionAnnotationLinksSeq;
     ["java:type:java.util.ArrayList"] sequence<omero::model::Annotation> PlateAcquisitionLinkedAnnotationSeq;
     ["protected"] class PlateAcquisition
     extends omero::model::IObject
     {
       omero::RInt version;
       omero::RInt getVersion();
       void setVersion(omero::RInt theVersion);
       omero::RString name;
       omero::RString getName();
       void setName(omero::RString theName);
       omero::RTime startTime;
       omero::RTime getStartTime();
       void setStartTime(omero::RTime theStartTime);
       omero::RTime endTime;
       omero::RTime getEndTime();
       void setEndTime(omero::RTime theEndTime);
       omero::RInt maximumFieldCount;
       omero::RInt getMaximumFieldCount();
       void setMaximumFieldCount(omero::RInt theMaximumFieldCount);
       omero::model::Plate plate;
       omero::model::Plate getPlate();
       void setPlate(omero::model::Plate thePlate);
       PlateAcquisitionWellSampleSeq wellSampleSeq;
       bool wellSampleLoaded;
       /*
        * Unloads the wellSample collection. Any access to this
        * collection will throw an omero.UnloadedCollectionException.
        *
        * See sizeOfWellSample() on how to test for unloaded collections.
        * See reloadWellSample() on how to reset the value.
        *
        */
       void unloadWellSample();
       int sizeOfWellSample();
       PlateAcquisitionWellSampleSeq copyWellSample();
       // See language-specific iterator methods
       void addWellSample(WellSample target);
       /*
        * Adds all the members of the PlateAcquisitionWellSampleSeq sequence to
        * the wellSampleSeq field.
        */
       void addAllWellSampleSet(PlateAcquisitionWellSampleSeq targets);
       void removeWellSample(WellSample theTarget);
       /*
        * Removes all the members of the PlateAcquisitionWellSampleSeq sequence from
        * the wellSampleSeq field.
        */
       void removeAllWellSampleSet(PlateAcquisitionWellSampleSeq targets);
       void clearWellSample();

       /*
        * Allows reloading the protected wellSample collection
        * from another instance of PlateAcquisition. The argument's collection
        * will be unloaded and all member entities will have their
        * inverse property altered.
        *
        * The argument's id must match and it's update id must be present and
        * greater than or equal to that of the current object.
        */
       void reloadWellSample(PlateAcquisition toCopy);
       PlateAcquisitionAnnotationLinksSeq annotationLinksSeq;
       bool annotationLinksLoaded;
       omero::sys::CountMap annotationLinksCountPerOwner;
       /*
        * Unloads the annotationLinks collection. Any access to this
        * collection will throw an omero.UnloadedCollectionException.
        *
        * See sizeOfAnnotationLinks() on how to test for unloaded collections.
        * See reloadAnnotationLinks() on how to reset the value.
        *
        */
       void unloadAnnotationLinks();
       int sizeOfAnnotationLinks();
       PlateAcquisitionAnnotationLinksSeq copyAnnotationLinks();
       // See language-specific iterator methods
       void addPlateAcquisitionAnnotationLink(PlateAcquisitionAnnotationLink target);
       /*
        * Adds all the members of the PlateAcquisitionAnnotationLinksSeq sequence to
        * the annotationLinksSeq field.
        */
       void addAllPlateAcquisitionAnnotationLinkSet(PlateAcquisitionAnnotationLinksSeq targets);
       void removePlateAcquisitionAnnotationLink(PlateAcquisitionAnnotationLink theTarget);
       /*
        * Removes all the members of the PlateAcquisitionAnnotationLinksSeq sequence from
        * the annotationLinksSeq field.
        */
       void removeAllPlateAcquisitionAnnotationLinkSet(PlateAcquisitionAnnotationLinksSeq targets);
       void clearAnnotationLinks();

       /*
        * Allows reloading the protected annotationLinks collection
        * from another instance of PlateAcquisition. The argument's collection
        * will be unloaded and all member entities will have their
        * inverse property altered.
        *
        * The argument's id must match and it's update id must be present and
        * greater than or equal to that of the current object.
        */
       void reloadAnnotationLinks(PlateAcquisition toCopy);
       omero::sys::CountMap getAnnotationLinksCountPerOwner();
       PlateAcquisitionAnnotationLink linkAnnotation(Annotation addition);

       /*
        * Add the link to the current instance and if bothSides is true AND
        * the other side of the link is loaded, add the current instance to
        * it as well.
        */
       void addPlateAcquisitionAnnotationLinkToBoth(omero::model::PlateAcquisitionAnnotationLink link, bool bothSides);
       PlateAcquisitionAnnotationLinksSeq findPlateAcquisitionAnnotationLink(Annotation removal);
       void unlinkAnnotation(Annotation removal);

       /*
        * Remove the link from the current instance and if bothSides is true AND
        * the other side of the link is loaded, remove the current instance from
        * it as well.
        */
       void removePlateAcquisitionAnnotationLinkFromBoth(omero::model::PlateAcquisitionAnnotationLink link, bool bothSides);
       PlateAcquisitionLinkedAnnotationSeq linkedAnnotationList();
       omero::RString description;
       omero::RString getDescription();
       void setDescription(omero::RString theDescription);
     };
   };
 };
 #endif // PLATEACQUISITION_ICE
