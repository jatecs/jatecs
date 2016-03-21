/*
 * This file is part of JaTeCS.
 *
 * JaTeCS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JaTeCS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JaTeCS.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The software has been mainly developed by (in alphabetical order):
 * - Andrea Esuli (andrea.esuli@isti.cnr.it)
 * - Tiziano Fagni (tiziano.fagni@isti.cnr.it)
 * - Alejandro Moreo Fernández (alejandro.moreo@isti.cnr.it)
 * Other past contributors were:
 * - Giacomo Berardi (giacomo.berardi@isti.cnr.it)
 */

package it.cnr.jatecs.indexes.DB.troveCompact;

import it.cnr.jatecs.indexes.DB.interfaces.*;
import it.cnr.jatecs.io.IStorageManager;

import java.io.IOException;

/**
 * Utility class which handles the data storage of Trove index.
 *
 * @author Tiziano Fagni
 */
public class TroveReadWriteHelper {

    /**
     * Write on the specified storage manager the given index. The index will be
     * saved with the name "name".
     *
     * @param storageManager The storage manager to use.
     * @param index          The index to write.
     * @param name           The name used to save the index.
     * @param overwrite      True if a previous version of the named index must be
     *                       replaced, false otherwise (in this case the existant index
     *                       will not be replaced).
     * @throws NullPointerException     Raised if storage manager is 'null' or index is 'null'.
     * @throws IllegalArgumentException Raised if the specified index name is invalid.
     * @throws IllegalStateException    Raised if the specified storage manager is not open.
     */
    public static void writeIndex(IStorageManager storageManager, IIndex index,
                                  String name, boolean overwrite) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");

        if (index == null)
            throw new NullPointerException("The specified index is 'null'");

        if (name == null || name.isEmpty())
            throw new IllegalArgumentException(
                    "The specified index name is 'null' or empty");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open!");

        TroveIndexRW indexRW = new TroveIndexRW(null, null, null, null, null,
                null, null);
        String indexName = name + storageManager.getPathSeparator() + "index";
        indexRW.write(storageManager, index, indexName, overwrite);

        writeFeatures(storageManager, index.getFeatureDB(), name, overwrite);
        writeDocuments(storageManager, index.getDocumentDB(), name, overwrite);
        writeCategories(storageManager, index.getCategoryDB(), name, overwrite);
        writeDomain(storageManager, index.getDomainDB(), name, overwrite);
        writeContent(storageManager, index.getContentDB(), name, overwrite);
        writeWeighting(storageManager, index.getWeightingDB(), name, overwrite);
        writeClassification(storageManager, index.getClassificationDB(), name,
                overwrite);
    }

    /**
     * Read the specified indexName from the given storage manager. The content
     * DB inside the index will be loaded optimized with type
     * {@link TroveContentDBType#Default}. The classification DB inside the
     * index will be loaded with type {@link TroveClassificationDBType#Default}.
     *
     * @param storageManager The storage manager to use.
     * @param indexName      The index name.
     * @param contentDBType  The content DB optimization type.
     * @throws NullPointerException     Raised if storage manager is 'null' or content DB type is
     *                                  'null' or 'classification DB type is 'null'.
     * @throws IllegalArgumentException Raised if index name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public static IIndex readIndex(IStorageManager storageManager,
                                   String indexName) throws IOException {
        return readIndex(storageManager, indexName, TroveContentDBType.Default,
                TroveClassificationDBType.Default);
    }

    /**
     * Read the specified indexName from the given storage manager. The content
     * DB inside the index will be loaded optimized as specified in
     * "contentDBType". The classification DB inside the index will be loaded as
     * specified in "classificationDBType".
     *
     * @param storageManager       The storage manager to use.
     * @param indexName            The index name.
     * @param contentDBType        The content DB optimization type.
     * @param classificationDBType The classification DB optimization type.
     * @return The read index object.
     * @throws NullPointerException     Raised if storage manager is 'null' or content DB type is
     *                                  'null' or 'classification DB type is 'null'.
     * @throws IllegalArgumentException Raised if index name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public static IIndex readIndex(IStorageManager storageManager,
                                   String indexName, TroveContentDBType contentDBType,
                                   TroveClassificationDBType classificationDBType) {

        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (indexName == null || indexName.isEmpty())
            throw new IllegalArgumentException("The index name <" + indexName
                    + "> is invalid");
        if (contentDBType == null)
            throw new NullPointerException("The content DB type is 'null'");
        if (classificationDBType == null)
            throw new NullPointerException(
                    "The classification DB type is 'null'");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open!");

        IFeatureDB featuresDB = readFeatures(storageManager, indexName);
        IDocumentDB documentsDB = readDocuments(storageManager, indexName);
        ICategoryDB categoriesDB = readCategories(storageManager, indexName);
        IDomainDB domainDB = readDomain(storageManager, indexName,
                categoriesDB, featuresDB);
        IContentDB contentDB = readContent(storageManager, indexName,
                documentsDB, featuresDB, contentDBType);
        IWeightingDB weightingDB = readWeighting(storageManager, indexName,
                contentDB);
        IClassificationDB classificationDB = readClassification(storageManager,
                indexName, documentsDB, categoriesDB, classificationDBType);

        TroveIndexRW indexRW = new TroveIndexRW(featuresDB, documentsDB,
                categoriesDB, domainDB, contentDB, weightingDB,
                classificationDB);
        String dbName = indexName + storageManager.getPathSeparator() + "index";
        return indexRW.read(storageManager, dbName);
    }

    /**
     * Read the named domain DB from the specified storage manager.
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IDomainDB readDomain(IStorageManager storageManager,
                                       String dbName) {
        return readDomain(storageManager, dbName,
                readCategories(storageManager, dbName),
                readFeatures(storageManager, dbName));
    }

    /**
     * Read the named domain DB from the specified storage manager.
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @param categoriesDB   The categories DB.
     * @param featuresDB     The features DB.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null' or categories DB is
     *                               'null' or features DB is 'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IDomainDB readDomain(IStorageManager storageManager,
                                       String dbName, ICategoryDB categoriesDB, IFeatureDB featuresDB) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");
        if (categoriesDB == null)
            throw new NullPointerException("The categories DB is 'null'");
        if (featuresDB == null)
            throw new NullPointerException("The features DB is 'null'");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveDomainDBRW domainDBRW = new TroveDomainDBRW(categoriesDB,
                featuresDB);
        String name = dbName + storageManager.getPathSeparator() + "domain";
        return domainDBRW.read(storageManager, name);
    }

    /**
     * Read the named classification DB from the specified storage manager. The
     * classification DB will be loaded with
     * {@link TroveClassificationDBType#Default} type.
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IClassificationDB readClassification(
            IStorageManager storageManager, String dbName) {
        return readClassification(storageManager, dbName,
                TroveClassificationDBType.Default);
    }

    /**
     * Read the named classification DB from the specified storage manager. The
     * classification DB will be loaded with optimization type as specified in
     * "DBType".
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @param DBType         The optimization type used when loading the DB.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null' or db type is 'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IClassificationDB readClassification(
            IStorageManager storageManager, String dbName,
            TroveClassificationDBType DBType) {
        return readClassification(storageManager, dbName,
                readDocuments(storageManager, dbName),
                readCategories(storageManager, dbName), DBType);
    }

    /**
     * Read the named classification DB from the specified storage manager. The
     * classification DB will be loaded with
     * {@link TroveClassificationDBType#Default} type.
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @param documentsDB    The documents DB.
     * @param categoriesDB   The categories DB.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null' or documentsDB is
     *                               'null' or categoriesDB is 'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IClassificationDB readClassification(
            IStorageManager storageManager, String dbName,
            IDocumentDB documentsDB, ICategoryDB categoriesDB) {
        return readClassification(storageManager, dbName, documentsDB,
                categoriesDB, TroveClassificationDBType.Default);
    }

    /**
     * Read the named classification DB from the specified storage manager. The
     * classification DB will be loaded with optimization type as specified in
     * "DBType".
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @param documentsDB    The documents DB.
     * @param categoriesDB   The categories DB.
     * @param DBType         The optimization type used when loading the DB.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null' or documentsDB is
     *                               'null' or categoriesDB is 'null' or db type is 'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IClassificationDB readClassification(
            IStorageManager storageManager, String dbName,
            IDocumentDB documentsDB, ICategoryDB categoriesDB,
            TroveClassificationDBType DBType) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");
        if (documentsDB == null)
            throw new NullPointerException(
                    "The specified documents DB is 'null'");
        if (categoriesDB == null)
            throw new NullPointerException(
                    "The specified categories DB is 'null'");
        if (DBType == null)
            throw new NullPointerException("The specified DB type is 'null'");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveClassificationDBRW classificationDBRW = new TroveClassificationDBRW(
                documentsDB, categoriesDB);
        String name = dbName + storageManager.getPathSeparator()
                + "classification";
        return classificationDBRW.read(storageManager, name, DBType);
    }

    /**
     * Read the named content DB from the specified storage manager. The content
     * DB will be loaded with {@link TroveContentDBType#Default} type.
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IContentDB readContent(IStorageManager storageManager,
                                         String dbName) {
        return readContent(storageManager, dbName, TroveContentDBType.Default);
    }

    /**
     * Read the named content DB from the specified storage manager. The content
     * DB will be loaded with {@link TroveContentDBType#Default} type.
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @param contentDBType  The content DB type used when loading the DB.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null' or content DB type is
     *                               'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IContentDB readContent(IStorageManager storageManager,
                                         String dbName, TroveContentDBType contentDBType) {
        return readContent(storageManager, dbName,
                readDocuments(storageManager, dbName),
                readFeatures(storageManager, dbName), contentDBType);
    }

    /**
     * Read the named content DB from the specified storage manager. The content
     * DB will be loaded with {@link TroveContentDBType#Default} type.
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @param documentsDB    The documents DB.
     * @param featuresDB     The features DB.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null' or documentsDB is
     *                               'null' or categoriesDB is 'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IContentDB readContent(IStorageManager storageManager,
                                         String dbName, IDocumentDB documentsDB, IFeatureDB featuresDB) {
        return readContent(storageManager, dbName, documentsDB, featuresDB,
                TroveContentDBType.Default);
    }

    /**
     * Read the named content DB from the specified storage manager. The content
     * DB will be loaded with {@link TroveContentDBType#Default} type.
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @param documentsDB    The documents DB.
     * @param featuresDB     The features DB.
     * @param contentDBType  The content DB type used when loading the DB.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null' or documentsDB is
     *                               'null' or categoriesDB is 'null' or content DB type is
     *                               'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IContentDB readContent(IStorageManager storageManager,
                                         String dbName, IDocumentDB documentsDB, IFeatureDB featuresDB,
                                         TroveContentDBType contentDBType) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");
        if (documentsDB == null)
            throw new NullPointerException("The documents DB is 'null'");
        if (featuresDB == null)
            throw new NullPointerException("The features DB is 'null'");
        if (contentDBType == null)
            throw new NullPointerException("The content DB type is 'null'");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveContentDBRW contentDBRW = new TroveContentDBRW(documentsDB,
                featuresDB);
        String name = dbName + storageManager.getPathSeparator() + "content";
        return contentDBRW.read(storageManager, name, contentDBType);
    }

    /**
     * Read the named features DB from the specified storage manager.
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IFeatureDB readFeatures(IStorageManager storageManager,
                                          String dbName) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveFeaturesDBRW featuresDBRW = new TroveFeaturesDBRW();
        String name = dbName + storageManager.getPathSeparator() + "features";
        return featuresDBRW.read(storageManager, name);
    }

    /**
     * Read the named weighting DB from the specified storage manager.
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IWeightingDB readWeighting(IStorageManager storageManager,
                                             String dbName) {
        return readWeighting(storageManager, dbName,
                readContent(storageManager, dbName));
    }

    /**
     * Read the named weighting DB from the specified storage manager.
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null' or contentDB is
     *                               'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IWeightingDB readWeighting(IStorageManager storageManager,
                                             String dbName, IContentDB contentDB) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");
        if (contentDB == null)
            throw new NullPointerException("The content DB is 'null'");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveWeightingDBRW weightingDBRW = new TroveWeightingDBRW(contentDB);
        String name = dbName + storageManager.getPathSeparator() + "weighting";
        IWeightingDB weightingDB = weightingDBRW.read(storageManager, name);
        return weightingDB;
    }

    /**
     * Read the named categories DB from the specified storage manager.
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static ICategoryDB readCategories(IStorageManager storageManager,
                                             String dbName) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveCategoriesDBRW categoriesDBRW = new TroveCategoriesDBRW();
        String name = dbName + storageManager.getPathSeparator() + "categories";
        return categoriesDBRW.read(storageManager, name);
    }

    /**
     * Read the named documents DB from the specified storage manager.
     *
     * @param storageManager The storage manager.
     * @param dbName         The db name.
     * @return The read DB.
     * @throws NullPointerException  Raised if the storage manager is 'null'.
     * @throws IllegalStateException Raised if the specified db name is invalid.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IDocumentDB readDocuments(IStorageManager storageManager,
                                            String dbName) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveDocumentsDBRW documentsDBRW = new TroveDocumentsDBRW();
        String name = dbName + storageManager.getPathSeparator() + "documents";
        return documentsDBRW.read(storageManager, name);
    }

    /**
     * Generate a new index from the given index where content DB and
     * classification DB are loaded with type respectively indicated by
     * "contentDBType" and "classificationDBType".
     *
     * @param storageManager       The storage manager to use.
     * @param index                The source index.
     * @param contentDBType        The content DB type to load.
     * @param classificationDBType The classification DB type to load.
     * @return The new index.
     * @throws NullPointerException  Raised if storage manager is 'null' or index is 'null' or
     *                               content DB type is 'null' or classification DB type is
     *                               'null'.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public static IIndex generateIndex(IStorageManager storageManager,
                                       IIndex index, TroveContentDBType contentDBType,
                                       TroveClassificationDBType classificationDBType) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (index == null)
            throw new NullPointerException("The specified index is 'null'");
        if (contentDBType == null)
            throw new NullPointerException("The content DB type is' null'");
        if (classificationDBType == null)
            throw new NullPointerException(
                    "The classification DB type is 'null'");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        String dbName = "tmpIdx_" + System.currentTimeMillis();

        writeIndex(storageManager, index, dbName, true);
        IIndex idx = readIndex(storageManager, dbName, contentDBType,
                classificationDBType);
        return idx;
    }

    /**
     * Write specified domain DB on the given storage manager.
     *
     * @param storageManager The storage manager to use.
     * @param domainDB       The DB to write.
     * @param dbName         The name assigned to DB.
     * @param overwrite      True if a DB with the same name exists and must be
     *                       overwritten, false otherwise.
     * @throws NullPointerException     Raised if the storage manager is 'null' or the DB is 'null'.
     * @throws IllegalArgumentException Raised if the DB name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public static void writeDomain(IStorageManager storageManager,
                                   IDomainDB domainDB, String dbName, boolean overwrite) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (domainDB == null)
            throw new NullPointerException("The specified domain DB is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveDomainDBRW domainDBRW = new TroveDomainDBRW(
                domainDB.getCategoryDB(), domainDB.getFeatureDB());
        String name = dbName + storageManager.getPathSeparator() + "domain";
        domainDBRW.write(storageManager, domainDB, name, overwrite);
        writeFeatures(storageManager, domainDB.getFeatureDB(), dbName,
                overwrite);
        writeCategories(storageManager, domainDB.getCategoryDB(), dbName,
                overwrite);
    }

    /**
     * Write specified content DB on the given storage manager.
     *
     * @param storageManager The storage manager to use.
     * @param contentDB      The DB to write.
     * @param dbName         The name assigned to DB.
     * @param overwrite      True if a DB with the same name exists and must be
     *                       overwritten, false otherwise.
     * @throws NullPointerException     Raised if the storage manager is 'null' or the DB is 'null'.
     * @throws IllegalArgumentException Raised if the DB name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public static void writeContent(IStorageManager storageManager,
                                    IContentDB contentDB, String dbName, boolean overwrite) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (contentDB == null)
            throw new NullPointerException("The specified content DB is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveContentDBRW contentDBRW = new TroveContentDBRW(
                contentDB.getDocumentDB(), contentDB.getFeatureDB());
        String name = dbName + storageManager.getPathSeparator() + "content";
        contentDBRW.write(storageManager, contentDB, name, overwrite);
    }

    /**
     * Write specified documents DB on the given storage manager.
     *
     * @param storageManager The storage manager to use.
     * @param documentsDB    The DB to write.
     * @param dbName         The name assigned to DB.
     * @param overwrite      True if a DB with the same name exists and must be
     *                       overwritten, false otherwise.
     * @throws NullPointerException     Raised if the storage manager is 'null' or the DB is 'null'.
     * @throws IllegalArgumentException Raised if the DB name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public static void writeDocuments(IStorageManager storageManager,
                                      IDocumentDB documentsDB, String dbName, boolean overwrite) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (documentsDB == null)
            throw new NullPointerException(
                    "The specified documents DB is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveDocumentsDBRW documentsDBRW = new TroveDocumentsDBRW();
        String name = dbName + storageManager.getPathSeparator() + "documents";
        documentsDBRW.write(storageManager, documentsDB, name, overwrite);
    }

    /**
     * Write specified features DB on the given storage manager.
     *
     * @param storageManager The storage manager to use.
     * @param featuresDB     The DB to write.
     * @param dbName         The name assigned to DB.
     * @param overwrite      True if a DB with the same name exists and must be
     *                       overwritten, false otherwise.
     * @throws NullPointerException     Raised if the storage manager is 'null' or the DB is 'null'.
     * @throws IllegalArgumentException Raised if the DB name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public static void writeFeatures(IStorageManager storageManager,
                                     IFeatureDB featuresDB, String dbName, boolean overwrite) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (featuresDB == null)
            throw new NullPointerException(
                    "The specified features DB is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveFeaturesDBRW featuresDBRW = new TroveFeaturesDBRW();
        String name = dbName + storageManager.getPathSeparator() + "features";
        featuresDBRW.write(storageManager, featuresDB, name, overwrite);
    }

    /**
     * Write specified categories DB on the given storage manager.
     *
     * @param storageManager The storage manager to use.
     * @param categoriesDB   The DB to write.
     * @param dbName         The name assigned to DB.
     * @param overwrite      True if a DB with the same name exists and must be
     *                       overwritten, false otherwise.
     * @throws NullPointerException     Raised if the storage manager is 'null' or the DB is 'null'.
     * @throws IllegalArgumentException Raised if the DB name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public static void writeCategories(IStorageManager storageManager,
                                       ICategoryDB categoriesDB, String dbName, boolean overwrite) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (categoriesDB == null)
            throw new NullPointerException(
                    "The specified categories DB is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveCategoriesDBRW categoriesDBRW = new TroveCategoriesDBRW();
        String name = dbName + storageManager.getPathSeparator() + "categories";
        categoriesDBRW.write(storageManager, categoriesDB, name, overwrite);
    }

    /**
     * Write specified weighting DB on the given storage manager.
     *
     * @param storageManager The storage manager to use.
     * @param weightingDB    The DB to write.
     * @param dbName         The name assigned to DB.
     * @param overwrite      True if a DB with the same name exists and must be
     *                       overwritten, false otherwise.
     * @throws NullPointerException     Raised if the storage manager is 'null' or the DB is 'null'.
     * @throws IllegalArgumentException Raised if the DB name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public static void writeWeighting(IStorageManager storageManager,
                                      IWeightingDB weightingDB, String dbName, boolean overwrite) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (weightingDB == null)
            throw new NullPointerException(
                    "The specified weighting DB is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveWeightingDBRW weightingDBRW = new TroveWeightingDBRW(
                weightingDB.getContentDB());
        String name = dbName + storageManager.getPathSeparator() + "weighting";
        weightingDBRW.write(storageManager, weightingDB, name, overwrite);
    }

    /**
     * Write specified classification DB on the given storage manager.
     *
     * @param storageManager   The storage manager to use.
     * @param classificationDB The DB to write.
     * @param dbName           The name assigned to DB.
     * @param overwrite        True if a DB with the same name exists and must be
     *                         overwritten, false otherwise.
     * @throws NullPointerException     Raised if the storage manager is 'null' or the DB is 'null'.
     * @throws IllegalArgumentException Raised if the DB name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public static void writeClassification(IStorageManager storageManager,
                                           IClassificationDB classificationDB, String dbName, boolean overwrite) {
        if (storageManager == null)
            throw new NullPointerException(
                    "The specified storage manager is 'null'");
        if (classificationDB == null)
            throw new NullPointerException(
                    "The specified classification DB is 'null'");
        if (dbName == null || dbName.isEmpty())
            throw new IllegalArgumentException("The db name <" + dbName
                    + "> is invalid");

        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TroveClassificationDBRW classificationDBRW = new TroveClassificationDBRW(
                classificationDB.getDocumentDB(),
                classificationDB.getCategoryDB());
        String name = dbName + storageManager.getPathSeparator()
                + "classification";
        classificationDBRW.write(storageManager, classificationDB, name,
                overwrite);
    }

}
