5.8.1 (March 2025)
------------------

- Replace usage of Java Util Logging (JUL) by SLF4J ([#155](https://github.com/ome/omero-blitz/pull/155)) 
- Bump omero-server and omero-common-test to 5.7.1

5.8.0 (November 2024)
---------------------

 - Build with Java 11 ([#154](https://github.com/ome/omero-blitz/pull/154))
 - Review API documentation of the omero.grid.Table ([#152](https://github.com/ome/omero-blitz/pull/152))
 - Bump omero-server version to 5.7.0
 - Bump omero-common-test version to 5.7.0

5.7.4 (July 2024)
-----------------

 - Improve checksum validation performance for large filesets ([#150](https://github.com/ome/omero-blitz/pull/150))
 - Bump omero-server version to 5.6.12
 - Bump omero-common-test version to 5.6.7

5.7.3 (May 2024)
----------------

 - Fix ordering of OriginalFile objects registered in saveFileset() ([#148](https://github.com/ome/omero-blitz/pull/148))
 - Bump omero-server version to 5.6.11
 - Bump omero-common-test version to 5.6.5

5.7.2 (December 2023)
---------------------

 - Bump logback-classic to version 1.3.14 ([#137](https://github.com/ome/omero-blitz/pull/137))
 - Bump omero-server version to 5.6.10
 - Bump omero-common-test version to 5.6.4

5.7.1 (September 2023)
----------------------

 - Revert "Java client init: no "anon" among jdk.tls.disabledAlgorithms"
 ([#146](https://github.com/ome/omero-blitz/pull/146))
 - Bump omero-server version to 5.6.9
 - Bump omero-common-test version to 5.6.3
 - Add top-level Git mailmap

5.7.0 (July 2023)
-----------------

- Remove support for ADH and let Ice choose the default SSL protocol ([#139](https://github.com/ome/omero-blitz/pull/139))

5.6.3 (July 2023)
-----------------

- Bump omero-common-test version to 5.6.2
- Bump omero-server version to 5.6.8

5.6.2 (March 2023)
------------------

- Set logback dependency to 1.2.x ([#136](https://github.com/ome/omero-blitz/pull/136))

5.6.1 (March 2023)
------------------

- Push to releases folder on artifactory ([#134](https://github.com/ome/omero-blitz/pull/134))
- Declare logback-classic as explicit dependency ([#133](https://github.com/ome/omero-blitz/pull/133))
- Bump omero-common-test version to 5.6.1
- Bump omero-server version to 5.6.7

5.6.0 (December 2022)
---------------------

- Command line importer developer user experience improvements ([#130](https://github.com/ome/omero-blitz/pull/130))
- Explicitly include details.externalInfo when relinking duplicate objects ([#129](https://github.com/ome/omero-blitz/pull/129))
- Bump TestNG to 7.5 ([#124](https://github.com/ome/omero-blitz/pull/124))
- Bump omero-common-test version to 5.6.0
- Bump omero-server version to 5.6.5


5.5.12 (June 2022)
------------------

- Add python/icedoc zip artifacts to the omeroBlitz Gradle publication ([#128](https://github.com/ome/omero-blitz/pull/128))

5.5.11 (June 2022)
------------------

- Bump omero-server version to 5.6.4 ([#127](https://github.com/ome/omero-blitz/pull/127))
- Bump omero-common-test version to 5.5.10
- Bump org.openmicroscopy.project plugin to 5.5.4 ([#127](https://github.com/ome/omero-blitz/pull/127))
- Add Gradle publication workflow ([#127](https://github.com/ome/omero-blitz/pull/127))

5.5.10 (April 2022)
------------------

- Fix conversion NPE for PIXELS and REFERENCEFRAME ([#126](https://github.com/ome/omero-blitz/pull/126))
- Bump omero-server version to 5.6.3
- Bump omero-common-test version to 5.5.9

5.5.9 (April 2022)
------------------

- Use ice 3.6.5 ([#125](https://github.com/ome/omero-blitz/pull/125))
- Use Maven central ([#115](https://github.com/ome/omero-blitz/pull/115))
- Bump plugin version ([#117](https://github.com/ome/omero-blitz/pull/117))
- Use Gradle 6 ([#114](https://github.com/ome/omero-blitz/pull/114))
- Use GitHub action ([#113](https://github.com/ome/omero-blitz/pull/113))
- Bump omero-server version to 5.6.2
- Bump omero-common-test version to 5.5.8

5.5.8 (September 2020)
----------------------

- Allow clients to connect with newer TLS ([#108](https://github.com/ome/omero-blitz/pull/108))
- Remove unused repository code ([#102](https://github.com/ome/omero-blitz/pull/102))
- Correct bug in listing import processes ([#101](https://github.com/ome/omero-blitz/pull/101))
- Allow duplication of file-based objects ([#100](https://github.com/ome/omero-blitz/pull/100))
- How to report "omero import" bugs now ([#96](https://github.com/ome/omero-blitz/pull/96))
- Bump omero-server version to 5.6.1
- Bump omero-common-test version to 5.5.7

5.5.7 (July 2020)
-----------------

- Have reset password use SQL to bypass 2019-SV3 restrictions ([#85](https://github.com/ome/omero-blitz/pull/85))
- Remove junit dependency ([#86](https://github.com/ome/omero-blitz/pull/86))
- Implement setROIName in OMEROMetadataStoreClient ([#90](https://github.com/ome/omero-blitz/pull/90))
- info in log ([#93](https://github.com/ome/omero-blitz/pull/93))
- Fix import-as by group owners ([#94](https://github.com/ome/omero-blitz/pull/94))
- Bump omero-server version to 5.6.0

5.5.6 (March 2020)
------------------

- Make the OMERO.blitz component name explicit in the log
  ([#78](https://github.com/ome/omero-blitz/pull/78))
- Expand user session creation abilities
  ([#80](https://github.com/ome/omero-blitz/pull/80))
- Display unit test output instead of caching it
  ([#81](https://github.com/ome/omero-blitz/pull/81))
- Expand documentation of API for Chgrp2, Chmod2, Chown2
  ([#82](https://github.com/ome/omero-blitz/pull/82))
- Security vulnerability fix for
  [2019-SV1](https://www.openmicroscopy.org/security/advisories/2019-SV1-reader-used-files/)
- Bump omero-server version to 5.5.6

5.5.5 (Dec 2019)
----------------

- Better handle import keep-alives (#65)
- Better handle the closing of Table proxies (#72)
- Better handle unit conversion (#76)
- Remove references to bin/omero from docs (#75)

5.5.4 (Sep 2019)
----------------

- Add deprecation warning to Blitz objects (#62)
- Increase VerifyDepthMax for websocket login (#66)
- Prepare Blitz objects for Python 3 support (#69, #70)

5.5.3 (July 2019)
-----------------

- Bump omero-server and omero-common-test version.
- Fix Ice SSL ciphers (#58, #59)
- Properly close table services (#57, #60)
- Add -T Project:name:x/Dataset:name:y support (#61)

5.5.2 (June 2019)
-----------------

- Bump omero-server and omero-common-test version.

5.5.1 (May 2019)
----------------

- Update omero-server dependency version.
- Support websocket and url style connection strings.

5.5.0 (May 2019)
----------------

- Move clients' graph requests onto background threads.
- Generate Ice doc.
- Move repository location properties.
- Fix remaining Javadoc warnings.
- Reduce use of Guava.
- Remove findbugs pulled in by subethamail.
- Remove the Java Gateway code.
- Partially migrate Properties file from the openmicroscopy repository.
- Fix Javadoc warnings.
- Run units test in Travis.
- Deprecate setCaseSentivice in favor of new setCaseSensitive.
- Deprecate vestigial data provider for deleted test.
- Add DEBUG logging for successful repository file deletions.
- Safely order repository file deletions.
- Move OMEROImportFixture into test directory.
- LegalGraphTargets can work for any GraphQuery instance.
- Java SSL fixes for clients (rebased from metadata54).
- Remove Repository.pixels and deprecate BfPixelsStoreI.
- Fix "cannot link" bug with import target containers.
- Adjust some Blitz API responses to signal "OK".
- Remove from comments confusing references to OMERO 4.x code.
- Update Hibernate DTD URL to current recommendation.
- Version loading tweak.
- Fix comments in FS Blitz API.
- Include map annotations in image metadata export.
- Add License file.
- Use new Gradle build system.
- Extract omero-blitz from the openmicroscopy repository.
