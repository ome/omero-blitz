/*
 * Copyright (C) 2014-2020 University of Dundee & Open Microscopy Environment.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package omero.cmd.graphs;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

import ome.io.nio.OriginalFilesService;
import ome.io.nio.PixelsService;
import ome.io.nio.ThumbnailService;
import ome.model.IObject;
import ome.model.core.OriginalFile;
import ome.model.core.Pixels;
import ome.model.display.Thumbnail;
import ome.model.fs.Fileset;
import ome.model.fs.FilesetEntry;
import ome.security.ACLVoter;
import ome.security.basic.LightAdminPrivileges;
import ome.services.blitz.repo.CheckedPath;
import ome.services.blitz.repo.ManagedRepositoryI;
import ome.services.blitz.repo.PublicRepositoryI;
import ome.services.blitz.repo.path.FsFile;
import ome.services.delete.Deletion;
import ome.services.graphs.GraphException;
import ome.services.graphs.GraphPathBean;
import ome.services.graphs.GraphPolicy;
import ome.services.graphs.GraphTraversal;
import ome.services.graphs.PermissionsPredicate;
import ome.services.scripts.ScriptRepoHelper;
import ome.services.util.ReadOnlyStatus;
import ome.system.EventContext;
import ome.system.Roles;
import ome.system.ServiceFactory;
import ome.util.SqlAction;
import omero.ServerError;
import omero.cmd.Duplicate;
import omero.cmd.DuplicateResponse;
import omero.cmd.HandleI.Cancel;
import omero.cmd.ERR;
import omero.cmd.Helper;
import omero.cmd.IRequest;
import omero.cmd.Response;
import omero.util.PrefixSubstituter;

/**
 * Request to duplicate model objects.
 * @author m.t.b.carroll@dundee.ac.uk
 * @since 5.2.1
 */
public class DuplicateI extends Duplicate implements IRequest, ReadOnlyStatus.IsAware, WrappableRequest<Duplicate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateI.class);

    private static final Set<GraphPolicy.Ability> REQUIRED_ABILITIES = ImmutableSet.of();

    /* all bulk operations are batched; this size should be suitable for IN (:ids) for HQL */
    private static final int BATCH_SIZE = 256;

    private static enum Inclusion {
        /* the object is to be duplicated */
        DUPLICATE,
        /* the object is not to be duplicated, but it may be referenced from duplicates */
        REFERENCE,
        /* the object is not to be duplicated, nor is it to be referenced from duplicates */
        IGNORE
    };

    private static enum Location {
        /* Files/ */
        FILES,
        /* Pixels/ */
        PIXELS,
        /* Thumbnails/ */
        THUMBS,
        /* ManagedRepository/ */
        MANAGED;
    }

    private final ACLVoter aclVoter;
    private final GraphPathBean graphPathBean;
    private final Set<Class<? extends IObject>> targetClasses;
    private GraphPolicy graphPolicy;  /* not final because of adjustGraphPolicy */
    private final SetMultimap<String, String> unnullable;
    private final Map<Class<? extends IObject>, Location> locationsForClasses;
    private final EnumMap<Location, Function<IObject, Path>> pathServices = new EnumMap<>(Location.class);
    private final EnumMap<Location, Long> diskUsage = new EnumMap<>(Location.class);
    private final ManagedRepositoryI managedRepository;

    /* retain a reference here because ManagedRepositoryI.setFileCreationListener uses a weak reference */
    private Consumer<CheckedPath> fileCreationListener;

    /* a taxonomy based on the class hierarchy of model objects in Java */
    private SpecificityClassifier<Class<? extends IObject>, Inclusion> classifier;

    private List<Function<GraphPolicy, GraphPolicy>> graphPolicyAdjusters = new ArrayList<Function<GraphPolicy, GraphPolicy>>();
    private Helper helper;
    private GraphHelper graphHelper;
    private GraphTraversal graphTraversal;

    private GraphTraversal.PlanExecutor processor;

    private int targetObjectCount = 0;
    private int duplicatedObjectCount = 0;

    private final Map<IObject, IObject> originalsToDuplicates = new HashMap<IObject, IObject>();
    private final Map<Entry<String, Long>, IObject> originalClassIdToDuplicates = new HashMap<Entry<String, Long>, IObject>();
    private final Multimap<IObject, PropertyUpdate> propertiesToUpdate = ArrayListMultimap.create();
    private final SetMultimap<IObject, IObject> blockedBy = HashMultimap.create();
    private final Map<Long, String> originalFileRepos = new HashMap<>();
    private final List<Path> filesCreated = new ArrayList<>();
    private final PrefixSubstituter<FsFile> managedRepositoryRewrites;
    private final List<OriginalFile> wrongPathDuplicateFiles = new ArrayList<>();
    private final Set<Long> wrongPathDuplicatePixels = new HashSet<>();
    private final Map<Long, List<String>> unmappedPropertiesPixels = new HashMap<>();

    /**
     * Construct a new <q>duplicate</q> request; called from {@link GraphRequestFactory#getRequest(Class)}.
     * @param aclVoter ACL voter for permissions checking
     * @param securityRoles the security roles
     * @param graphPathBean the graph path bean to use
     * @param adminPrivileges the light administrator privileges helper
     * @param deletionInstance a deletion instance for deleting files
     * @param targetClasses legal target object classes for duplicate
     * @param graphPolicy the graph policy to apply for duplicate
     * @param unnullable properties that, while nullable, may not be nulled by a graph traversal operation
     * @param applicationContext the OMERO application context from Spring
     */
    public DuplicateI(ACLVoter aclVoter, Roles securityRoles, GraphPathBean graphPathBean, LightAdminPrivileges adminPrivileges,
            Deletion deletionInstance, Set<Class<? extends IObject>> targetClasses, GraphPolicy graphPolicy,
            SetMultimap<String, String> unnullable, ApplicationContext applicationContext) {
        this.aclVoter = aclVoter;
        this.graphPathBean = graphPathBean;
        this.targetClasses = targetClasses;
        this.graphPolicy = graphPolicy;
        this.unnullable = unnullable;
        this.managedRepositoryRewrites = new PrefixSubstituter<FsFile>(
                (prefix, file) -> file.getPathFrom(prefix) != null,
                (prefix, file) -> FsFile.concatenate(prefix, file),
                (prefix, file) -> file.getPathFrom(prefix));
        this.managedRepository = applicationContext.getBean(ManagedRepositoryI.class);
        this.locationsForClasses = ImmutableMap.of(
                OriginalFile.class, Location.FILES,
                Pixels.class, Location.PIXELS,
                Thumbnail.class, Location.THUMBS);
        final Function<Function<Long, String>, Function<IObject, Path>> pathFactory =
                new Function<Function<Long, String>, Function<IObject, Path>>() {
            @Override
            public Function<IObject, Path> apply(Function<Long, String> pathService) {
                return obj -> Paths.get(pathService.apply(obj.getId()));
            }
        };
        final Function<Long, String> pathInFiles, pathInPixels, pathInThumbs;
        pathInFiles = applicationContext.getBean("/OMERO/Files", OriginalFilesService.class)::getFilesPath;
        pathInPixels = applicationContext.getBean("/OMERO/Pixels", PixelsService.class)::getPixelsPath;
        pathInThumbs = applicationContext.getBean("/OMERO/Thumbs", ThumbnailService.class)::getThumbnailPath;
        this.pathServices.put(Location.FILES, pathFactory.apply(pathInFiles));
        this.pathServices.put(Location.PIXELS, pathFactory.apply(pathInPixels));
        this.pathServices.put(Location.THUMBS, pathFactory.apply(pathInThumbs));
    }

    @Override
    public Map<String, String> getCallContext() {
        return null;
    }

    /**
     * Run {@link Files#copy(Path, Path, java.nio.file.CopyOption...)} and note the disk space used.
     * @param location the location of the file in the binary repository
     * @param from the file to copy
     * @param to where to create the copy
     * @throws IOException from {@link Files#copy(Path, Path, java.nio.file.CopyOption...)}
     */
    private void copyFile(Location location, Path from, Path to) throws IOException {
        Files.copy(from, to);
        final long size = Files.size(to);
        if (size > 0) {
            diskUsage.merge(location, size, (x, y) -> x + y);
        }
    }

    /**
     * Create a duplicate of a file on the filesystem.
     * @param location the location of the file in the binary repository
     * @param from the file to duplicate
     * @param to where to create the duplicate
     * @throws IOException if the file duplication failed
     */
    private void duplicateFile(Location location, Path from, Path to) throws IOException {
        if (location == Location.MANAGED) {
            try {
                /* safe to create a hard link in managed repository */
                Files.createLink(to, from);
                LOGGER.debug("linked {} to {}", from, to);
            } catch (FileSystemException fse) {
                LOGGER.debug("failed to link {} to {}", from, to, fse);
                /* cannot link so fall back to a copy */
                copyFile(location, from, to);
                LOGGER.debug("copied {} to {}", from, to);
            }
        } else {
            /* copy rather than link as may violate integrity if different users can write to same file */
            Files.createDirectories(to.getParent());
            copyFile(location, from, to);
            LOGGER.debug("copied {} to {}", from, to);
        }
    }

    /**
     * Get the underlying filesystem path for the given object.
     * @param modelObject a model object
     * @return the location of the object in the binary repository, or {@code null} if none exists
     * @throws GraphException if the object should not be included in duplication
     */
    private Location getPath(IObject modelObject) throws GraphException {
        if (modelObject instanceof OriginalFile) {
            /* Files may require special handling. */
            final OriginalFile file = (OriginalFile) modelObject;
            String fileRepo = file.getRepo();
            if (fileRepo == null) {
                fileRepo = originalFileRepos.get(file.getId());
            }
            if (PublicRepositoryI.DIRECTORY_MIMETYPE.equals(file.getMimetype())) {
                /* Do not attempt to duplicate directories. */
                LOGGER.debug("not directly duplicating OriginalFile:{} on filesystem because it is a directory", file.getId());
                return null;
            }
            if (fileRepo != null) {
                if (ScriptRepoHelper.SCRIPT_REPO.equals(fileRepo)) {
                    /* Do not attempt to duplicate scripts. */
                    throw new GraphException(
                            "will not duplicate OriginalFile:" + file.getId() + " because it is in the scripts repository");
                } else if (managedRepository.getRepoUuid().equals(fileRepo)) {
                    return Location.MANAGED;
                } else {
                    throw new GraphException(
                            "cannot duplicate OriginalFile:" + file.getId() + " because it is in repository " + fileRepo);
                }
            }
            /* If we reach here then the file is in /Files/ */
        }
        final Class<? extends IObject> modelObjectClass;
        if (modelObject instanceof HibernateProxy) {
            modelObjectClass = Hibernate.getClass(modelObject);
        } else {
            modelObjectClass = modelObject.getClass();
        }
        for (final Map.Entry<Class<? extends IObject>, Location> locationForClass : locationsForClasses.entrySet()) {
            final Class<? extends IObject> locationClass = locationForClass.getKey();
            final Location location = locationForClass.getValue();
            if (locationClass.isAssignableFrom(modelObjectClass)) {
                return location;
            }
        }
        return null;
    }

    @Override
    public void init(Helper helper) {
        if (LOGGER.isDebugEnabled()) {
            final GraphUtil.ParameterReporter arguments = new GraphUtil.ParameterReporter();
            arguments.addParameter("typesToDuplicate", typesToDuplicate);
            arguments.addParameter("typesToReference", typesToReference);
            arguments.addParameter("typesToIgnore", typesToIgnore);
            arguments.addParameter("targetObjects", targetObjects);
            arguments.addParameter("childOptions", childOptions);
            arguments.addParameter("dryRun", dryRun);
            LOGGER.debug("request: " + arguments);
        }

        this.helper = helper;
        helper.setSteps(dryRun ? 3 : 14);
        this.graphHelper = new GraphHelper(helper, graphPathBean);

        classifier = new SpecificityClassifier<Class<? extends IObject>, Inclusion>(
                new SpecificityClassifier.ContainmentTester<Class<? extends IObject>>() {
                    @Override
                    public boolean isProperSupersetOf(Class<? extends IObject> parent, Class<? extends IObject> child) {
                        return parent != child && parent.isAssignableFrom(child);
                    }});

        try {
            classifier.addClass(Inclusion.DUPLICATE, graphHelper.getClassesFromNames(typesToDuplicate));
            classifier.addClass(Inclusion.REFERENCE, graphHelper.getClassesFromNames(typesToReference));
            classifier.addClass(Inclusion.IGNORE,    graphHelper.getClassesFromNames(typesToIgnore));
        } catch (IllegalArgumentException e) {
            throw helper.cancel(new ERR(), e, "bad-class");
        }

        graphPolicyAdjusters.add(0, new Function<GraphPolicy, GraphPolicy>() {
            @Override
            public GraphPolicy apply(GraphPolicy graphPolicy) {
                return SkipTailPolicy.getSkipTailPolicy(graphPolicy,
                        new Predicate<Class<? extends IObject>>() {
                    @Override
                    public boolean test(Class<? extends IObject> modelObject) {
                        final Inclusion classification = classifier.getClass(modelObject);
                        return classification == Inclusion.REFERENCE || classification == Inclusion.IGNORE;
                    }});
            }});

        graphPolicy.registerPredicate(new PermissionsPredicate());

        graphTraversal = graphHelper.prepareGraphTraversal(childOptions, REQUIRED_ABILITIES, graphPolicy, graphPolicyAdjusters,
                aclVoter, graphPathBean, unnullable, new InternalProcessor(), dryRun);

        graphPolicyAdjusters = null;

        final Path managedRepositoryRoot;
        try {
            final String rootPath = managedRepository.rootPath(helper.getSession(), helper.getServiceFactory());
            managedRepositoryRoot = Paths.get(rootPath);
        } catch (ServerError se) {
            final String message = "cannot determine root of managed repository";
            LOGGER.error(message, se);
            throw new IllegalStateException(message + ": " + se);
        }
        fileCreationListener = checked -> filesCreated.add(checked.fsFile.toPath(managedRepositoryRoot));
        managedRepository.setFileCreationListener(fileCreationListener);
        this.pathServices.put(Location.MANAGED, new Function<IObject, Path>() {
            @Override
            public Path apply(IObject object) {
                final OriginalFile file = (OriginalFile) object;
                return new FsFile(file.getPath() + file.getName()).toPath(managedRepositoryRoot);
            }
        });
    }

    /**
     * Note how to update a specific model object property.
     * @author m.t.b.carroll@dundee.ac.uk
     * @since 5.2.3
     */
    private static abstract class PropertyUpdate {
        protected final IObject duplicate;
        protected final String property;

        /**
         * Create a note regarding how to update a specific model object property.
         * @param duplicate the duplicate object holding the property
         * @param property the name of the property
         */
        PropertyUpdate(IObject duplicate, String property) {
            this.duplicate = duplicate;
            this.property = property;
        }

        /**
         * Update the model object property to which this instance corresponds.
         * @param mapping the mapping from the original object's links to other objects to those objects' corresponding duplicates,
         * returns {@code null} if no such duplicate exists so as to avoid stealing links from the original objects
         * @throws GraphException if the property value could not be updated
         */
        abstract void execute(Function<Object, IObject> mapping) throws GraphException;
    }

    /**
     * Note how to update a specific model object property that is directly accessible.
     * @author m.t.b.carroll@dundee.ac.uk
     * @since 5.2.3
     */
    private static class PropertyUpdateAccessible extends PropertyUpdate {
        protected final Object value;

        /**
         * Create a note regarding how to update a specific model object property.
         * @param duplicate the duplicate object holding the property
         * @param property the name of the property
         * @param value the property value from the original object
         */
        PropertyUpdateAccessible(IObject duplicate, String property, Object value) {
            super(duplicate, property);
            this.value = value;
        }

        @Override
        void execute(Function<Object, IObject> mapping) throws GraphException {
            final Object duplicateValue = GraphUtil.copyComplexValue(mapping, value);
            if (duplicateValue != null) {
                try {
                    PropertyUtils.setNestedProperty(duplicate, property, duplicateValue);
                } catch (NestedNullException | ReflectiveOperationException e) {
                    throw new GraphException(
                            "cannot set property " + property + " on duplicate " + duplicate.getClass().getName());
                }
            }
        }
    }

    /**
     * Note how to update a specific model object property that is accessible only via {@code iterate} and {@code add} methods.
     * @author m.t.b.carroll@dundee.ac.uk
     * @since 5.2.3
     */
    private static class PropertyUpdateInaccessible extends PropertyUpdate {
        protected final IObject original;
        protected final Method reader, writer;
        protected final boolean isOrdered;

        private Set<IObject> written = new HashSet<IObject>();

        /**
         * Create a note regarding how to update a specific model object property.
         * @param original the original object holding the property
         * @param duplicate the duplicate object holding the property
         * @param property the name of the property
         * @param reader the {@code iterate} method for the property
         * @param writer the {@code add} method for the property
         * @param isOrdered if the property value is a collection whose order must be preserved on duplication
         */
        PropertyUpdateInaccessible(IObject original, IObject duplicate, String property, Method reader, Method writer,
                boolean isOrdered) {
            super(duplicate, property);
            this.original = original;
            this.reader = reader;
            this.writer = writer;
            this.isOrdered = isOrdered;
        }

        @Override
        void execute(Function<Object, IObject> mapping) throws GraphException {
            try {
                @SuppressWarnings("unchecked")
                final Iterator<IObject> linkedTos = (Iterator<IObject>) reader.invoke(original);
                boolean stillWriting = true;
                while (linkedTos.hasNext()) {
                    final IObject linkedTo = linkedTos.next();
                    final IObject duplicateOfLinkedTo = mapping.apply(linkedTo);
                    if (stillWriting && duplicateOfLinkedTo != null) {
                        if (written.add(duplicateOfLinkedTo)) {
                            writer.invoke(duplicate, duplicateOfLinkedTo);
                        }
                    } else if (isOrdered) {
                        /* cannot easily update other than by appending so skip the rest for now */
                        stillWriting = false;
                    }
                }
            } catch (NestedNullException | ReflectiveOperationException e) {
                throw new GraphException(
                        "cannot set property " + property + " on duplicate " + duplicate.getClass().getName());
            }
        }
    }

    /**
     * Copy simple property values to the duplicate model object.
     * @throws GraphException if duplication failed
     */
    private void copySimpleProperties() throws GraphException {
        for (final Entry<IObject, IObject> originalAndDuplicate : originalsToDuplicates.entrySet()) {
            final IObject original = originalAndDuplicate.getKey();
            final IObject duplicate = originalAndDuplicate.getValue();
            final String originalClass = Hibernate.getClass(original).getName();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("copying properties from " + originalClass + ":" + original.getId());
            }
            try {
                /* process property values for a given object that is duplicated */
                for (final String superclassName : graphPathBean.getSuperclassesOfReflexive(originalClass)) {
                    /* process property values that do not relate to edges in the model object graph */
                    for (final String property : graphPathBean.getSimpleProperties(superclassName)) {
                        /* ignore inaccessible properties */
                        if (!graphPathBean.isPropertyAccessible(superclassName, property)) {
                            continue;
                        }
                        /* copy original property value to duplicate, creating new instances of collections */
                        final Object value = PropertyUtils.getProperty(original, property);
                        final Object duplicateValue = GraphUtil.copyComplexValue(x -> null, value);
                        PropertyUtils.setProperty(duplicate, property, duplicateValue);
                    }
                }
            } catch (NestedNullException | ReflectiveOperationException e) {
                throw new GraphException("failed to duplicate " + originalClass + ':' + original.getId());
            }
        }
    }

    /**
     * Note in which order to persist the duplicate model objects and how to copy their properties.
     * @throws GraphException if duplication failed
     */
    private void noteNewPropertyValuesForDuplicates() throws GraphException {
        /* allow lookup regardless of if original is actually a Hibernate proxy object */
        final Function<Object, IObject> duplicateLookup = new Function<Object, IObject>() {
            @Override
            public IObject apply(Object original) {
                if (original instanceof IObject) {
                    final String originalClass;
                    if (original instanceof HibernateProxy) {
                        originalClass = Hibernate.getClass(original).getName();
                    } else {
                        originalClass = original.getClass().getName();
                    }
                    final Long originalId = ((IObject) original).getId();
                    return originalClassIdToDuplicates.get(new AbstractMap.SimpleImmutableEntry<>(originalClass, originalId));
                } else {
                    return null;
                }
            }
        };
        /* note how to copy property values into duplicates and link with other model objects */
        for (final Entry<IObject, IObject> originalAndDuplicate : originalsToDuplicates.entrySet()) {
            final IObject original = originalAndDuplicate.getKey();
            final IObject duplicate = originalAndDuplicate.getValue();
            final String originalClass = Hibernate.getClass(original).getName();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("noting how to copy properties from " + originalClass + ":" + original.getId());
            }
            try {
                /* process property values for a given object that is duplicated */
                for (final String superclassName : graphPathBean.getSuperclassesOfReflexive(originalClass)) {
                    /* process property values that link from the duplicate to other model objects */
                    for (final Entry<String, String> forwardLink : graphPathBean.getLinkedTo(superclassName)) {
                        /* next forward link */
                        final String linkedClassName = forwardLink.getKey();
                        final String property = forwardLink.getValue();
                        /* ignore details for now, duplicates never preserve original ownership */
                        if (property.startsWith("details.") && !property.equals("details.externalInfo")) {
                            continue;
                        }
                        /* note which of the objects to which the original links should be ignored */
                        final Set<Long> linkedToIdsToIgnore = new HashSet<Long>();
                        for (final Entry<String, Collection<Long>> linkedToClassIds :
                            graphTraversal.getLinkeds(superclassName, property, original.getId()).asMap().entrySet()) {
                            final String linkedToClass = linkedToClassIds.getKey();
                            final Collection<Long> linkedToIds = linkedToClassIds.getValue();
                            if (classifier.getClass(Class.forName(linkedToClass).asSubclass(IObject.class)) == Inclusion.IGNORE) {
                                linkedToIdsToIgnore.addAll(linkedToIds);
                            }
                        }
                        /* check for another accessor for inaccessible properties */
                        Object value;
                        if (graphPathBean.isPropertyAccessible(superclassName, property)) {
                            /* copy the linking from the original's property over to the duplicate's */
                            try {
                                value = PropertyUtils.getNestedProperty(original, property);
                            } catch (NestedNullException e) {
                                continue;
                            }
                            if (value instanceof Collection) {
                                /* if a collection property, include only the objects that aren't to be ignored */
                                final Collection<IObject> valueCollection = (Collection<IObject>) value;
                                final Collection<IObject> valueToCopy;
                                if (value instanceof List) {
                                    valueToCopy = new ArrayList<IObject>();
                                } else if (value instanceof Set) {
                                    valueToCopy = new HashSet<IObject>();
                                } else {
                                    throw new GraphException("unexpected collection type: " + value.getClass());
                                }
                                for (final IObject linkedTo : valueCollection) {
                                    if (!linkedToIdsToIgnore.contains(linkedTo.getId())) {
                                        valueToCopy.add(linkedTo);
                                    }
                                }
                                value = valueToCopy;
                            } else if (value instanceof IObject) {
                                /* if the property value is to be ignored then null it */
                                if (linkedToIdsToIgnore.contains(((IObject) value).getId())) {
                                    value = null;
                                }
                            }
                            /* note how to copy the property value, replacing originals with corresponding duplicates */
                            if (value != null) {
                                propertiesToUpdate.put(duplicate, new PropertyUpdateAccessible(duplicate, property, value));
                            }
                        } else {
                            /* this could be a one-to-many property with direct accessors protected */
                            final Class<? extends IObject> linkerClass = Class.forName(superclassName).asSubclass(IObject.class);
                            final Class<? extends IObject> linkedClass = Class.forName(linkedClassName).asSubclass(IObject.class);
                            final Method reader, writer;
                            try {
                                reader = linkerClass.getMethod("iterate" + StringUtils.capitalize(property));
                                writer = linkerClass.getMethod("add" + linkedClass.getSimpleName(), linkedClass);
                            } catch (NoSuchMethodException | SecurityException e) {
                                /* no luck, so ignore this property */
                                continue;
                            }
                            boolean isOrdered;
                            try {
                                linkerClass.getMethod("getPrimary" + linkedClass.getSimpleName());
                                isOrdered = true;
                            } catch (NoSuchMethodException | SecurityException e) {
                                isOrdered = false;
                            }
                            value = reader.invoke(original);
                            /* note how to copy the linking from the original's property over to the duplicate's */
                            propertiesToUpdate.put(duplicate,
                                    new PropertyUpdateInaccessible(original, duplicate, property, reader, writer, isOrdered));
                            if (isOrdered) {
                                /* ensure that the values are written in order */
                                IObject previousDuplicate = null;
                                final Iterator<IObject> valueIterator = (Iterator<IObject>) reader.invoke(original);
                                while (valueIterator.hasNext()) {
                                    final IObject nextDuplicate = originalsToDuplicates.get(valueIterator.next());
                                    if (nextDuplicate != null) {
                                        if (previousDuplicate != null) {
                                            blockedBy.put(nextDuplicate, previousDuplicate);
                                        }
                                        previousDuplicate = nextDuplicate;
                                    }
                                }
                            }
                        }
                        /* persist the property values before persisting the holder */
                        final Set<IObject> duplicatesInValue = GraphUtil.filterComplexValue(duplicateLookup, value);
                        blockedBy.putAll(duplicate, duplicatesInValue);
                    }
                }
            } catch (NestedNullException | ReflectiveOperationException e) {
                throw new GraphException("failed to duplicate " + originalClass + ':' + original.getId());
            }
        }
    }

    /**
     * Duplicate model object properties, linking them as appropriate with each other and with other model objects.
     * @throws GraphException if duplication failed
     */
    private void persistDuplicatesWithNewPropertyValues() throws GraphException {
        /* copy property values into duplicates and link with other model objects */
        final Session session = helper.getSession();
        final ListMultimap<IObject, IObject> propertyUpdateTriggers = ArrayListMultimap.create();
        final Set<IObject> persisted = new HashSet<IObject>();
        final Set<IObject> remainingTransient = new HashSet<IObject>(originalsToDuplicates.values());
        while (!remainingTransient.isEmpty()) {
            boolean isProgress = false;
            final Iterator<IObject> remainingTransientIterator = remainingTransient.iterator();
            while (remainingTransientIterator.hasNext()) {
                final IObject duplicate = remainingTransientIterator.next();
                final Set<IObject> blockers = blockedBy.get(duplicate);
                blockers.retainAll(remainingTransient);  /* changes to the view affect the underlying map */
                if (blockers.isEmpty()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("duplicating an instance of " + duplicate.getClass().getName());
                    }
                    /* before persisting an object, fill in its links to other objects */
                    for (final PropertyUpdate update : propertiesToUpdate.get(duplicate)) {
                        final Function<Object, IObject> duplicateProxyLookup = new Function<Object, IObject>() {
                            @Override
                            public IObject apply(Object original) {
                                if (original instanceof IObject) {
                                    final String originalClass;
                                    if (original instanceof HibernateProxy) {
                                        originalClass = Hibernate.getClass(original).getName();
                                    } else {
                                        originalClass = original.getClass().getName();
                                    }
                                    final Long originalId = ((IObject) original).getId();
                                    final IObject duplicate =
                                            originalClassIdToDuplicates.get(new AbstractMap.SimpleImmutableEntry<>(originalClass, originalId));
                                    if (duplicate == null) {
                                        return null;
                                    }
                                    if (persisted.contains(duplicate)) {
                                        return duplicate;
                                    } else {
                                        /* this value is omitted from the object's property value when it is persisted so we note
                                         * to update this property when the value is persisted */
                                        propertyUpdateTriggers.put(duplicate, update.duplicate);
                                    }
                                }
                                return null;
                            }
                        };
                        update.execute(duplicateProxyLookup);
                    }
                    /* fill in other objects' links to the object to be persisted, such as back-references */
                    persisted.add(duplicate);
                    final Function<Object, IObject> duplicateProxyLookup = new Function<Object, IObject>() {
                        @Override
                        public IObject apply(Object original) {
                            if (original instanceof IObject) {
                                final String originalClass;
                                if (original instanceof HibernateProxy) {
                                    originalClass = Hibernate.getClass(original).getName();
                                } else {
                                    originalClass = original.getClass().getName();
                                }
                                final Long originalId = ((IObject) original).getId();
                                final IObject duplicate =
                                        originalClassIdToDuplicates.get(new AbstractMap.SimpleImmutableEntry<>(originalClass, originalId));
                                if (duplicate == null) {
                                    return null;
                                }
                                if (persisted.contains(duplicate)) {
                                    return duplicate;
                                }
                            }
                            return null;
                        }
                    };
                    for (final IObject objectToUpdate : propertyUpdateTriggers.get(duplicate)) {
                        for (final PropertyUpdate update : propertiesToUpdate.get(objectToUpdate)) {
                            update.execute(duplicateProxyLookup);
                        }
                    }
                    propertyUpdateTriggers.removeAll(duplicate);
                    final Collection<PropertyUpdate> propertiesToUpdateForDuplicate =
                            new ArrayList<PropertyUpdate>(propertiesToUpdate.get(duplicate));
                    Consumer<? super Long> idProcessor = id -> {};
                    if (duplicate instanceof OriginalFile) {
                        final OriginalFile duplicateFile = (OriginalFile) duplicate;
                        final String duplicateFileRepo = duplicateFile.getRepo();
                        if (duplicateFileRepo != null) {
                            /* Cannot naively set repo property via ORM so note for later. */
                            idProcessor = id -> originalFileRepos.put(id, duplicateFileRepo);
                            duplicateFile.setRepo(null);
                        }
                    }
                    /* when an object is persisted its hash changes, so move key in persisted and in propertiesToUpdate */
                    propertiesToUpdate.removeAll(duplicate);
                    persisted.remove(duplicate);
                    session.persist(duplicate);
                    propertiesToUpdate.putAll(duplicate, propertiesToUpdateForDuplicate);
                    persisted.add(duplicate);
                    idProcessor.accept(duplicate.getId());
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("persisted " + duplicate.getClass().getName() + ":" + duplicate.getId());
                    }
                    remainingTransientIterator.remove();
                    isProgress = true;
                }
            }
            if (!isProgress) {
                throw new GraphException("internal duplication error: cyclic model graph");
            }
        }
    }

    /**
     * Link other model objects to the duplicates.
     * @throws GraphException if duplication failed
     */
    private void linkToNewDuplicates() throws GraphException {
        /* copy property values into duplicates and link with other model objects */
        final Session session = helper.getSession();
        for (final IObject original : originalsToDuplicates.keySet()) {
            final String originalClass = Hibernate.getClass(original).getName();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("adjusting properties of " + originalClass + ":" + original.getId());
            }
            try {
                /* process property values for a given object that is duplicated */
                for (final String superclassName : graphPathBean.getSuperclassesOfReflexive(originalClass)) {
                    /* process property values that link to the duplicate from other model objects */
                    for (final Entry<String, String> backwardLink : graphPathBean.getLinkedBy(superclassName)) {
                        /* next backward link */
                        final String linkingClass = backwardLink.getKey();
                        final String property = backwardLink.getValue();
                        /* ignore inaccessible properties */
                        if (!graphPathBean.isPropertyAccessible(linkingClass, property)) {
                            continue;
                        }
                        for (final Entry<String, Collection<Long>> linkedFromClassIds :
                            graphTraversal.getLinkers(linkingClass, property, original.getId()).asMap().entrySet()) {
                            final String linkedFromClass = linkedFromClassIds.getKey();
                            final Collection<Long> linkedFromIds = linkedFromClassIds.getValue();
                            if (classifier.getClass(Class.forName(linkedFromClass).asSubclass(IObject.class)) == Inclusion.IGNORE) {
                                /* these linkers are to be ignored */
                                continue;
                            }
                            /* load the instances that link to the original */
                            final String rootQuery = "FROM " + linkedFromClass + " WHERE id IN (:ids)";
                            for (final List<Long> idsBatch : Iterables.partition(linkedFromIds, BATCH_SIZE)) {
                                final List<IObject> linkers =
                                        session.createQuery(rootQuery).setParameterList("ids", idsBatch).list();
                                for (final IObject linker : linkers) {
                                    if (originalsToDuplicates.containsKey(linker)) {
                                        /* ignore linkers that are to be duplicated, those have already been handled */
                                        continue;
                                    }
                                    /* copy the linking from the original's property over to the duplicate's */
                                    final Object value;
                                    try {
                                        value = PropertyUtils.getNestedProperty(linker, property);
                                    } catch (NestedNullException e) {
                                        continue;
                                    }
                                    /* for linkers adjust only the collection properties */
                                    if (value instanceof Collection) {
                                        final Collection<IObject> valueCollection = (Collection<IObject>) value;
                                        final Collection<IObject> newDuplicates = new ArrayList<IObject>();
                                        for (final IObject originalLinker : valueCollection) {
                                            IObject duplicateOfValue = originalsToDuplicates.get(originalLinker);
                                            if (duplicateOfValue != null) {
                                                /* previous had just original, now include duplicate too */
                                                newDuplicates.add(duplicateOfValue);
                                            }
                                        }
                                        valueCollection.addAll(newDuplicates);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (NestedNullException | ReflectiveOperationException e) {
                throw new GraphException("failed to adjust " + originalClass + ':' + original.getId());
            }
        }
    }

    /**
     * Handles duplication of a fileset, including creating filesystem directories for the new template path and
     * adjusting the paths of the duplicate to match.
     * @param originalFileset the fileset being duplicated
     * @param duplicateFileset the persisted duplicate of the fileset
     * @throws GraphException if the fileset could not be duplicated
     */
    private void duplicateFileset(Fileset originalFileset, Fileset duplicateFileset) throws GraphException {
        final Map<OriginalFile, OriginalFile> originalsToDuplicatesUsedFiles = new HashMap<>();
        for (final FilesetEntry entry : (Iterable<FilesetEntry>) () -> originalFileset.iterateUsedFiles()) {
            final OriginalFile originalUsedFile = entry.getOriginalFile();
            final OriginalFile duplicateUsedFile = (OriginalFile) originalsToDuplicates.get(originalUsedFile);
            if (duplicateUsedFile != null) {
                originalsToDuplicatesUsedFiles.put(originalUsedFile, duplicateUsedFile);
            }
        }
        if (originalsToDuplicatesUsedFiles.isEmpty()) {
            /* There are no files to be duplicated. */
            return;
        }
        /* Construct a consistent basis for any hash in the template prefix. */
        final SortedMap<Long, Long> idMap = originalsToDuplicatesUsedFiles.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getId(), entry -> entry.getValue().getId(),
                        (v1, v2) -> { throw new IllegalStateException(); }, TreeMap::new));
        /* Create a new template path for the duplicate files. */
        final EventContext ec = helper.getEventContext();
        final ServiceFactory sf = helper.getServiceFactory();
        final Session session = helper.getSession();
        final SqlAction sql = helper.getSql();
        final FsFile originalTemplatePrefix, duplicateTemplatePrefix;
        try {
            originalTemplatePrefix = new FsFile(originalFileset.getTemplatePrefix());
            duplicateTemplatePrefix = managedRepository.createTemplatePath(idMap, ec, sf, session, sql);
        } catch (ServerError se) {
            final String message = "failed to create managed repository directory for duplicate fileset";
            LOGGER.warn(message, se);
            throw new GraphException(message + ": " + se);
        }
        duplicateFileset.setTemplatePrefix(duplicateTemplatePrefix.toString() + FsFile.separatorChar);
        managedRepositoryRewrites.put(originalTemplatePrefix, duplicateTemplatePrefix);
        LOGGER.debug("will duplicate Fileset:{} as Fileset:{} into {}",
                originalFileset.getId(), duplicateFileset.getId(), duplicateTemplatePrefix);
    }

    /**
     * Note the references to underlying files that need fixing in duplicates.
     */
    private void noteWrongPaths() throws GraphException {
        final SqlAction sql = helper.getSql();
        final String repoUuid = managedRepository.getRepoUuid();
        for (final Map.Entry<IObject, IObject> originalDuplicate : originalsToDuplicates.entrySet()) {
            final IObject original = originalDuplicate.getKey();
            final IObject duplicate = originalDuplicate.getValue();
            if (original instanceof Fileset) {
                duplicateFileset((Fileset) original, (Fileset) duplicate);
            } else if (original instanceof OriginalFile) {
                if (getPath(original) == Location.MANAGED) {
                    wrongPathDuplicateFiles.add((OriginalFile) duplicate);
                }
            } else if (original instanceof Pixels) {
                final List<String> pixelInfo = sql.getPixelsNamePathRepo(original.getId());
                if (pixelInfo != null) {
                    unmappedPropertiesPixels.put(duplicate.getId(), pixelInfo);
                    if (repoUuid.equals(pixelInfo.get(2))) {
                        wrongPathDuplicatePixels.add(duplicate.getId());
                    }
                }
            }
        }
    }

    /**
     * Fix the path set for duplicated files in the managed repository such that they reference their new fileset.
     * @throws GraphException if the paths for the files could not be created
     */
    private void fixPathDuplicateFiles() throws GraphException {
        final EventContext ec = helper.getEventContext();
        final ServiceFactory sf = helper.getServiceFactory();
        final Session session = helper.getSession();
        final SqlAction sql = helper.getSql();
        for (final OriginalFile wrongPathDuplicateFile : wrongPathDuplicateFiles) {
            String fromPathName = wrongPathDuplicateFile.getPath() + wrongPathDuplicateFile.getName();
            final boolean isLog = fromPathName.endsWith(".log");
            if (isLog) {
                /* An awkward hack for import logs which are not contained within the template prefix directory. */
                fromPathName = fromPathName.substring(0, fromPathName.length() - 4) + FsFile.separatorChar + "log";
            }
            String toPathName = managedRepositoryRewrites.apply(new FsFile(fromPathName)).toString();
            if (fromPathName.equals(toPathName)) {
                continue;
            }
            if (isLog) {
                toPathName = toPathName.substring(0, toPathName.length() - 4) + ".log";
            }
            final int namePosition = toPathName.lastIndexOf(FsFile.separatorChar) + 1;
            wrongPathDuplicateFile.setPath(toPathName.substring(0, namePosition));
            wrongPathDuplicateFile.setName(toPathName.substring(namePosition));
            try {
                managedRepository.makeDir(wrongPathDuplicateFile.getPath(), true, session, sf, sql, ec);
            } catch (ServerError se) {
                final String message = "failed to create managed repository directory " + wrongPathDuplicateFile.getPath();
                LOGGER.warn(message, se);
                throw new GraphException(message + ": " + se);
            }
        }
    }

    /**
     * Duplicate files on the filesystem.
     * @throws GraphException if the files cannot be duplicated
     */
    private void duplicateUnderlyingFiles() throws GraphException {
        for (final Map.Entry<IObject, IObject> originalDuplicate : originalsToDuplicates.entrySet()) {
            final IObject original = originalDuplicate.getKey();
            final IObject duplicate = originalDuplicate.getValue();
            final Location originalLocation = getPath(original);
            if (originalLocation == null) {
                continue;
            }
            final Path originalPath = pathServices.get(originalLocation).apply(original);
            if (!Files.exists(originalPath)) {
                continue;
            }
            final Location duplicateLocation = getPath(duplicate);
            if (duplicateLocation != null && originalLocation != duplicateLocation) {
                throw new GraphException("file duplicating " + originalPath + " is in a different repository");
            }
            final Path duplicatePath = pathServices.get(originalLocation).apply(duplicate);
            if (originalPath.equals(duplicatePath)) {
                throw new GraphException("failed to generate different path for file duplicating " + originalPath);
            }
            try {
                duplicateFile(originalLocation, originalPath, duplicatePath);
                filesCreated.add(duplicatePath);
            } catch (IOException ioe) {
                final String message = "failed to duplicate file from " + originalPath + " to " + duplicatePath;
                LOGGER.warn(message, ioe);
                throw new GraphException(message + ": " + ioe);
            }
        }
    }

    /**
     * Use SQL to effect the required {@link OriginalFile#setRepo(String)} operations
     * since the property was nulled for persisting the duplicate instances.
     */
    public void setOriginalFileRepos() {
        final SqlAction sql = helper.getSql();
        for (final Map.Entry<Long, String> originalFileRepo : originalFileRepos.entrySet()) {
            sql.setFileRepo(Collections.singleton(originalFileRepo.getKey()), originalFileRepo.getValue());
        }
    }

    /**
     * Use SQL to set the unmapped {@code repo}, {@code path}, {@code name} property for pixels,
     * referencing the path to their new fileset where necessary.
     */
    private void setPixelsInfo() {
        final SqlAction sql = helper.getSql();
        for (final Map.Entry<Long, List<String>> unmappedPropertiesPixel : unmappedPropertiesPixels.entrySet()) {
            final Long pixelsId = unmappedPropertiesPixel.getKey();
            final String name = unmappedPropertiesPixel.getValue().get(0);
            String path = unmappedPropertiesPixel.getValue().get(1);
            final String repo = unmappedPropertiesPixel.getValue().get(2);
            if (wrongPathDuplicatePixels.contains(pixelsId)) {
                path = managedRepositoryRewrites.apply(new FsFile(path)).toString();
            }
            sql.setPixelsNamePathRepo(pixelsId, name, path, repo);
        }
    }

    @Override
    public Object step(int step) throws Cancel {
        helper.assertStep(step);
        try {
            switch (step) {
            case 0:
                final SetMultimap<String, Long> targetMultimap = graphHelper.getTargetMultimap(targetClasses, targetObjects);
                targetObjectCount += targetMultimap.size();
                final Entry<SetMultimap<String, Long>, SetMultimap<String, Long>> plan =
                        graphTraversal.planOperation(targetMultimap, true, true);
                if (plan.getValue().isEmpty()) {
                    graphTraversal.assertNoUnlinking();
                } else {
                    final Exception e = new IllegalArgumentException("duplication plan unexpectedly includes deletion");
                    throw helper.cancel(new ERR(), e, "bad-plan");
                }
                return plan.getKey();
            case 1:
                graphTraversal.assertNoPolicyViolations();
                return null;
            case 2:
                processor = graphTraversal.processTargets();
                return null;
            case 3:
                processor.execute();
                return null;
            case 4:
                copySimpleProperties();
                return null;
            case 5:
                noteNewPropertyValuesForDuplicates();
                return null;
            case 6:
                persistDuplicatesWithNewPropertyValues();
                return null;
            case 7:
                linkToNewDuplicates();
                return null;
            case 8:
                noteWrongPaths();
                return null;
            case 9:
                fixPathDuplicateFiles();
                return null;
            case 10:
                duplicateUnderlyingFiles();
                return null;
            case 11:
                /* Subsequent steps write via SQL instead of via Hibernate. */
                helper.getSession().flush();
                return null;
            case 12:
                setOriginalFileRepos();
                return null;
            case 13:
                setPixelsInfo();
                return null;
            default:
                final Exception e = new IllegalArgumentException("model object graph operation has no step " + step);
                throw helper.cancel(new ERR(), e, "bad-step");
            }
        } catch (Cancel c) {
            abort();
            throw c;
        } catch (GraphException ge) {
            abort();
            final omero.cmd.GraphException graphERR = new omero.cmd.GraphException();
            graphERR.message = ge.message;
            throw helper.cancel(graphERR, ge, "graph-fail");
        } catch (Throwable t) {
            abort();
            throw helper.cancel(new ERR(), t, "graph-fail");
        }
    }

    /**
     * Clean up after duplication is found to have failed.
     */
    private void abort() {
        LOGGER.debug("aborting duplication process");
        managedRepository.clearFileCreationListener();
        Collections.reverse(filesCreated);
        for (final Path fileCreated : filesCreated) {
            try {
                Files.deleteIfExists(fileCreated);
            } catch (IOException ioe) {
                LOGGER.warn("failed to delete {}", fileCreated, ioe);
            }
        }
    }

    @Override
    public void finish() {
        managedRepository.clearFileCreationListener();
        if (diskUsage.isEmpty()) {
            LOGGER.debug("no file bytes copied");
        } else {
            LOGGER.info("file bytes copied: {}", Joiner.on(", ").join(diskUsage.entrySet().stream()
                    .map(entry -> String.format("%,d into %s", entry.getValue(), entry.getKey())).iterator()));
        }
    }

    @Override
    public void buildResponse(int step, Object object) {
        helper.assertResponse(step);
        /* if the results object were in terms of IObjectList then this would need IceMapper.map */
        if (dryRun && step == 0) {
            final SetMultimap<String, Long> result = (SetMultimap<String, Long>) object;
            final Map<String, List<Long>> duplicatedObjects = GraphUtil.copyMultimapForResponse(result);
            duplicatedObjectCount += result.size();
            final DuplicateResponse response = new DuplicateResponse(duplicatedObjects);
            helper.setResponseIfNull(response);
            helper.info("in mock duplication of " + targetObjectCount + ", duplicated " + duplicatedObjectCount + " in total");
        } else if (!dryRun && step == 6) {
            final Map<String, List<Long>> duplicatedObjects = new HashMap<String, List<Long>>();
            for (final IObject duplicate : originalsToDuplicates.values()) {
                final String className = duplicate.getClass().getName();
                List<Long> ids = duplicatedObjects.get(className);
                if (ids == null) {
                    ids = new ArrayList<Long>();
                    duplicatedObjects.put(className, ids);
                }
                ids.add(duplicate.getId());
                duplicatedObjectCount++;
            }
            final DuplicateResponse response = new DuplicateResponse(duplicatedObjects);
            helper.setResponseIfNull(response);
            helper.info("in duplication of " + targetObjectCount + ", duplicated " + duplicatedObjectCount + " in total");

            if (LOGGER.isDebugEnabled()) {
                final GraphUtil.ParameterReporter arguments = new GraphUtil.ParameterReporter();
                arguments.addParameter("duplicates", response.duplicates);
                LOGGER.debug("response: " + arguments);
            }
        }
    }

    @Override
    public Response getResponse() {
        return helper.getResponse();
    }

    @Override
    public void copyFieldsTo(Duplicate request) {
        GraphUtil.copyFields(this, request);
        request.typesToDuplicate = new ArrayList<String>(typesToDuplicate);
        request.typesToReference = new ArrayList<String>(typesToReference);
        request.typesToIgnore = new ArrayList<String>(typesToIgnore);
    }

    @Override
    public void adjustGraphPolicy(Function<GraphPolicy, GraphPolicy> adjuster) {
        if (graphPolicyAdjusters == null) {
            throw new IllegalStateException("request is already initialized");
        } else {
            graphPolicyAdjusters.add(adjuster);
        }
    }

    @Override
    public int getStepProvidingCompleteResponse() {
        return dryRun ? 0 : 6;
    }

    @Override
    public GraphPolicy.Action getActionForStarting() {
        return GraphPolicy.Action.INCLUDE;
    }

    @Override
    public Map<String, List<Long>> getStartFrom(Response response) {
        return ((DuplicateResponse) response).duplicates;
    }

    @Override
    public boolean isReadOnly(ReadOnlyStatus readOnly) {
        return dryRun;
    }

    /**
     * A <q>duplicate</q> processor that assists with duplicating model objects.
     * This processor merely reads from the database and initializes the duplicate objects.
     * The updates to the objects' property values occur in a later step.
     * @author m.t.b.carroll@dundee.ac.uk
     * @since 5.2.1
     */
    private final class InternalProcessor extends BaseGraphTraversalProcessor {

        public InternalProcessor() {
            super(helper.getSession());
        }

        @Override
        public void processInstances(String className, Collection<Long> ids) throws GraphException {
            final String rootQuery = "FROM " + className + " WHERE id IN (:ids)";
            for (final List<Long> idsBatch : Iterables.partition(ids, BATCH_SIZE)) {
                final List<IObject> originals = session.createQuery(rootQuery).setParameterList("ids", idsBatch).list();
                for (final IObject original : originals) {
                    final IObject duplicate;
                    try {
                        duplicate = (IObject) Hibernate.getClass(original).newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new GraphException("cannot create a duplicate of " + original);
                    }
                    final String originalClass = Hibernate.getClass(original).getName();
                    final Long originalId = original.getId();
                    originalClassIdToDuplicates.put(new AbstractMap.SimpleImmutableEntry<>(originalClass, originalId), duplicate);
                    originalsToDuplicates.put(original, duplicate);
                }
            }
        }

        @Override
        public Set<GraphPolicy.Ability> getRequiredPermissions() {
            return REQUIRED_ABILITIES;
        }
    }
}
