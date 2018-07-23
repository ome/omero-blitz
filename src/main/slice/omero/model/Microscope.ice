    /*
    **   Generated by blitz/resources/templates/combined.vm
    **   See ../../README.ice for information on these types.
    **
    **   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
    **   Use is subject to license terms supplied in LICENSE.txt
    **
    */
 #ifndef MICROSCOPE_ICE
 #define MICROSCOPE_ICE
 #include <omero/model/IObject.ice>
 #include <omero/RTypes.ice>
 #include <omero/model/RTypes.ice>
 #include <omero/System.ice>
 #include <omero/Collections.ice>
 module omero {
   module model {
     class MicroscopeType;
     class Details;
     ["protected"] class Microscope
     extends omero::model::IObject
     {
       omero::RInt version;
       omero::RInt getVersion();
       void setVersion(omero::RInt theVersion);
       omero::RString manufacturer;
       omero::RString getManufacturer();
       void setManufacturer(omero::RString theManufacturer);
       omero::RString model;
       omero::RString getModel();
       void setModel(omero::RString theModel);
       omero::RString lotNumber;
       omero::RString getLotNumber();
       void setLotNumber(omero::RString theLotNumber);
       omero::RString serialNumber;
       omero::RString getSerialNumber();
       void setSerialNumber(omero::RString theSerialNumber);
       omero::model::MicroscopeType type;
       omero::model::MicroscopeType getType();
       void setType(omero::model::MicroscopeType theType);
     };
   };
 };
 #endif // MICROSCOPE_ICE
