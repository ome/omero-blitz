/*
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 *
 */

#ifndef OMERO_TABLES_ICE
#define OMERO_TABLES_ICE

#include <omero/ModelF.ice>
#include <omero/RTypes.ice>
#include <omero/System.ice>
#include <omero/Collections.ice>
#include <omero/Repositories.ice>
#include <omero/ServerErrors.ice>


/*
 * The Tables API is intended to provide a storage mechanism
 * for tabular data.
 *
 * See https://docs.openmicroscopy.org/latest/omero/developers/Tables.html
 */
module omero {


    /*
     * Forward declaration
     */
    module api {
        interface ServiceFactory;
    };

    module grid {

    //
    // User-consumable types dealing with
    // measurements/results (""tables"").
    // ========================================================================
    //

        /**
         * Base type for dealing working with tabular data. For efficiency,
         * data is grouped by type, i.e. column. These value objects are passed
         * through the {@code Table} interface.
         **/
        class Column {

            string name;
            string description;

        };

        class FileColumn extends Column {
            omero::api::LongArray values;
        };

        class ImageColumn extends Column {
            omero::api::LongArray values;
        };

        class DatasetColumn extends Column {
            omero::api::LongArray values;
        };

        class RoiColumn extends Column {
            omero::api::LongArray values;
        };

        class WellColumn extends Column {
            omero::api::LongArray values;
        };

        class PlateColumn extends Column {
            omero::api::LongArray values;
        };

        class BoolColumn extends Column {
            omero::api::BoolArray values;
        };

        class DoubleColumn extends Column {
            omero::api::DoubleArray values;
        };

        class LongColumn extends Column {
            omero::api::LongArray values;
        };

        class StringColumn extends Column {
            long size;
            omero::api::StringArray values;
        };

        class FloatArrayColumn extends Column {
            long size;
            omero::api::FloatArrayArray values;
        };

        class DoubleArrayColumn extends Column {
            long size;
            omero::api::DoubleArrayArray values;
        };

        class LongArrayColumn extends Column {
            long size;
            omero::api::LongArrayArray values;
        };

        //
        // Inline ROIs.
        //

        /**
         * Column requiring special handling.
         **/
        class MaskColumn extends Column {
            omero::api::LongArray imageId;
            omero::api::IntegerArray theZ;
            omero::api::IntegerArray theT;
            omero::api::DoubleArray x;
            omero::api::DoubleArray y;
            omero::api::DoubleArray w;
            omero::api::DoubleArray h;
            omero::api::ByteArrayArray bytes;
        };

        sequence<Column> ColumnArray;

        class Data {

            long lastModification;
            omero::api::LongArray rowNumbers;
            ColumnArray columns;

        };

        ["ami"] interface Table {


            //
            // Reading ======================================================
            //

            idempotent
            omero::model::OriginalFile
                getOriginalFile()
                throws omero::ServerError;

            /**
             * Returns empty columns.
             **/
            idempotent
            ColumnArray
                getHeaders()
                throws omero::ServerError;

            /**
             * Return the number of rows of a table.
             **/
            idempotent
            long
                getNumberOfRows()
                throws omero::ServerError;

            /**
             * Run a query on a table.
             *
             * The meaning of the start and stop parameters are the same as in the
             * built-in Python slices. Setting start, step and stop to 0 is interpreted
             * as running the query against all rows.
             *
             * @param condition A query string - see the
             * <a href="https://omero.readthedocs.io/en/stable/developers/Tables.html#tables-query-language">tables query language</a>
             * for more details.
             * @param variables A mapping of strings and variable values to be substituted into
             * condition. This can often be left empty.
             * @param start The start of the range of rows to consider.
             * @param stop The end of the range of rows to consider.
             * @param step The stepping interval of the range of rows to consider. Set to 0
             * to disable stepping.
             * @return A list of row indices matching the condition which can be passed as a
             * parameter of {@link #readCoordinates} or {@link #slice}.
             **/
            idempotent
            omero::api::LongArray
                getWhereList(string condition, omero::RTypeDict variables, long start, long stop, long step)
                throws omero::ServerError;

            /**
             * Read a set of entire rows in the table.
             *
             * @param rowNumbers A list of row indices to be retrieved from the table.
             * The indices may be non-consecutive and must contain at least one element
             * or an {@link omero.ApiUsageException} will be thrown.
             * @return The requested rows as a {@link omero.grid.Data} object. The results
             * will be returned in the same order as the row indices.
             **/
            idempotent
            Data
                readCoordinates(omero::api::LongArray rowNumbers)
                throws omero::ServerError;

            /**
             * Read a subset of columns and consecutive rows from a table.
             *
             * The meaning of the start and stop parameters are the same as in the
             * built-in Python slices. Setting both start and stop to 0 is
             * interpreted as returning all rows.
             *
             * @param colNumbers A list of column indices to be retrieved from the table.
             * The indices may be non-consecutive and must contain at least one element
             * or an {@link omero.ApiUsageException} will be thrown.
             * @param start The first element of the range of rows to retrieve. Must be non null.
             * @param stop The stop of the range of rows to retrieve. Must be non null.
             * @return The requested columns and rows as a {@link omero.grid.Data} object.
             **/
            idempotent
            Data
                read(omero::api::LongArray colNumbers, long start, long stop)
                throws omero::ServerError;

            /**
             * Read a subset of columns and consecutive rows from a table.
             *
             * @param colNumbers A list of column indices to be retrieved from the table.
             * The indices may be non-consecutive. If set to empty or null, all columns
             * will be returned.
             * @param rowNumbers A list of row indices to be retrieved from the table.
             * The indices may be non-consecutive. If set empty or null, all rows will
             * be returned.
             * @return The requested columns and rows as a {@link omero.grid.Data} object.
             * The results will be returned in the same order as the column and row indices.
             **/
            idempotent
            Data
                slice(omero::api::LongArray colNumbers, omero::api::LongArray rowNumbers)
                throws omero::ServerError;

            //
            // Writing ========================================================
            //

            void
                addData(ColumnArray cols)
                throws omero::ServerError;

            /**
             * Allows the user to modify a Data instance passed back
             * from a query method and have the values modified. It
             * is critical that the {@code Data.lastModification} and the
             * {@code Data.rowNumbers} fields are properly set. An exception
             * will be thrown if the data has since been modified.
             **/
            void update(Data modifiedData)
                throws omero::ServerError;

            //
            // Metadata =======================================================
            //

            idempotent
            omero::RTypeDict
                getAllMetadata()
                throws omero::ServerError;

            idempotent
            omero::RType
                getMetadata(string key)
                throws omero::ServerError;

            idempotent
            void
                setAllMetadata(omero::RTypeDict dict)
                throws omero::ServerError;

            idempotent
            void
                setMetadata(string key, omero::RType value)
                throws omero::ServerError;

            //
            // Life-cycle =====================================================
            //

            /**
             * Initializes the structure based on
             **/
            void
                initialize(ColumnArray cols)
                throws omero::ServerError;

            /**
             * Adds a column and returns the position index of the new column.
             **/
            int
                addColumn(Column col)
                throws omero::ServerError;

            /**
             **/
            void
                delete()
                throws omero::ServerError;

            /**
             **/
            void
                close()
                throws omero::ServerError;

        };


    //
    // Interfaces and types running the backend.
    // Used by OMERO.blitz to manage the public
    // omero.api types.
    // ========================================================================
    //

        ["ami"] interface Tables {

            /**
             * Returns the Repository which this Tables service is watching.
             **/
            idempotent
             omero::grid::Repository*
                getRepository()
                throws omero::ServerError;

            /**
             * Returns the Table service for the given ""OMERO.tables"" file.
             * This service will open the file locally to access the data.
             * After any modification, the file will be saved locally and
             * the server asked to update the database record. This is done
             * via services in the {@link omero.api.ServiceFactory}.
             */
            idempotent
            Table*
                getTable(omero::model::OriginalFile file, omero::api::ServiceFactory* sf)
                throws omero::ServerError;


        };

    };


};

#endif
