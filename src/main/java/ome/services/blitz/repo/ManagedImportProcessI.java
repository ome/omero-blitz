/*
 * Copyright (C) 2012-2014 Glencoe Software, Inc. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package ome.services.blitz.repo;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.framework.Advised;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;

import Ice.Current;
import ome.services.blitz.impl.AbstractCloseableAmdServant;
import ome.services.blitz.impl.ServiceFactoryI;
import ome.services.blitz.repo.PublicRepositoryI.AMD_submit;
import ome.services.blitz.repo.path.FsFile;
import ome.services.blitz.util.ServiceFactoryAware;
import ome.services.util.Executor;
import ome.system.Login;
import omero.RString;
import omero.RType;
import omero.SecurityViolation;
import omero.ServerError;
import omero.api.IQueryPrx;
import omero.api.RawFileStorePrx;
import omero.cmd.CallContext;
import omero.cmd.HandlePrx;
import omero.grid.ImportLocation;
import omero.grid.ImportProcessPrx;
import omero.grid.ImportProcessPrxHelper;
import omero.grid.ImportRequest;
import omero.grid.ImportSettings;
import omero.grid._ImportProcessOperations;
import omero.grid._ImportProcessTie;
import omero.model.Fileset;
import omero.model.FilesetJobLink;
import omero.sys.Parameters;
import omero.sys.ParametersI;

/**
 * Represents a single import within a defined-session
 * all running server-side.
 *
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 5.0.0
 */
public class ManagedImportProcessI extends AbstractCloseableAmdServant
    implements _ImportProcessOperations, ServiceFactoryAware,
                ProcessContainer.Process {

    private final static Logger log = LoggerFactory.getLogger(ManagedImportProcessI.class);

    static class UploadState {
        final RawFileStorePrx prx;
        /** Next byte which should be written */
        long offset = 0;

        UploadState(RawFileStorePrx prx) {
            if (prx == null) {
                throw new RuntimeException("Null not allowed!");
            }
            this.prx = prx;
        }

        void setOffset(long offset) {
            this.offset = offset;
        }
    }

    /**
     * Current which created this instance.
     */
    private final Ice.Current current;

    /**
     * The managed repo instance which created (and ultimately is responsible
     * for) this import process.
     */
    private final ManagedRepositoryI repo;

    /**
     * A proxy to this servant which can be given to clients to monitor the
     * import process.
     */
    private final ImportProcessPrx proxy;

    /**
     * The model object as originally passed in by the client and then
     * modified and saved by the managed repository.
     */
    private final Fileset fs;

    /**
     * The settings as passed in by the user. Never null.
     */
    private final ImportSettings settings;

    /**
     * The import location as defined by the managed repository during
     * importFileset. Never null.
     */
    private final ImportLocation location;

    /**
     * SessionI/ServiceFactoryI that this process is running in.
     */
    private/* final */ServiceFactoryI sf;

    /**
     * A sparse and often empty map of the {@link UploadState} instances which
     * this import process is aware of. In a single-threaded model, this map
     * will likely only have at most one element, but depending on threads,
     * pauses, and restarts, this may contain more elements. After close
     * is called on each of the proxies, {@link #closeCalled(int)} will be
     * invoked with the integer lookup to this map, in which case the instance
     * will be purged.
     */
    private final Cache<Integer, UploadState> uploaders = CacheBuilder.newBuilder().build();

    /**
     * Handle which is the initial first step of import.
     */
    private HandlePrx handle;

    /**
     * Additional logfile for this process. Must be set in {@link MDC} with key
     * "fileset" to be activated.
     */
    private String logFilename;

    /**
     * Root session UUID token that can be used for authentication.
     *
     * Be careful with this.
     */
    private String rootToken;

    /**
     * Create and register a servant for servicing the import process
     * within a managed repository.
     */
    public ManagedImportProcessI(ManagedRepositoryI repo, Fileset fs,
            ImportLocation location, ImportSettings settings, Current __current,
            String rootToken)
                throws ServerError {
        super(null, null);
        this.repo = repo;
        this.fs = fs;
        this.settings = settings;
        this.location = location;
        this.current = __current;
        this.rootToken = rootToken;
        this.proxy = registerProxy(__current);
        setApplicationContext(repo.context);
        // TODO: The above could be moved to SessionI.internalServantConfig as
        // long as we're careful to remove all other, redundant calls to setAC.
        CheckedPath logFile = ((ManagedImportLocationI) location).getLogFile();
        logFilename = logFile.getFullFsPath();
    }

    public void setServiceFactory(ServiceFactoryI sf) throws ServerError {
        this.sf = sf;
    }

    /**
     * Adds this instance to the current session so that clients can communicate
     * with it. Once we move to opening a new session for this import, care
     * must be taken to guarantee that these instances don't leak:
     * i.e. who's responsible for closing them and removing them from the
     * adapter.
     */
    protected ImportProcessPrx registerProxy(Ice.Current ignore) throws ServerError {
        _ImportProcessTie tie = new _ImportProcessTie(this);
        Ice.Current adjustedCurr = repo.makeAdjustedCurrent(current);
        Ice.ObjectPrx prx = repo.registerServant(tie, this, adjustedCurr);
        return ImportProcessPrxHelper.uncheckedCast(prx);
   }

    public ImportProcessPrx getProxy() {
        return this.proxy;
    }

    public Fileset getFileset() {
        return this.fs;
    }

    public ImportSettings getImportSettings(Current __current) {
        return this.settings;
    }

    //
    // ProcessContainer INTERFACE METHODS
    //

    public long getGroup() {
        return fs.getDetails().getGroup().getId().getValue();
    }

    public void ping() {
        throw new RuntimeException("NYI");
    }

    public void shutdown() {
        throw new RuntimeException("NYI");
    }
    //
    // ICE INTERFACE METHODS
    //

    public RawFileStorePrx getUploader(final int i, Current current)
            throws ServerError {

        StopWatch sw1 = new Slf4JStopWatch();
        try {
            MDC.put("fileset", logFilename);

            String mode = null;
            if (current != null && current.ctx != null) {
                mode = current.ctx.get("omero.fs.mode");
                if (mode == null) {
                    mode = "rw";
                }
            }

            final String applicableMode = mode;
            final Callable<UploadState> rfsOpener = new Callable<UploadState>() {
                @Override
                public UploadState call() throws ServerError {
                    final StopWatch sw2 = new Slf4JStopWatch();
                    final String path = location.sharedPath + FsFile.separatorChar + location.usedFiles.get(i);
                    final Ice.Current adjustedCurr = repo.makeAdjustedCurrent(ManagedImportProcessI.this.current);
                    adjustedCurr.ctx.put(CallContext.FILENAME_KEY, logFilename);
                    adjustedCurr.ctx.put(CallContext.TOKEN_KEY, rootToken);
                    final RawFileStorePrx prx = repo.file(path, applicableMode,  adjustedCurr);
                    try {
                        registerCallback(prx, i);
                    } catch (RuntimeException re) {
                        try {
                            prx.close();  // close if anything happens
                        } catch (Exception e) {
                            log.error("Failed to close RawFileStorePrx", e);
                        }
                        throw re;
                    } finally {
                        sw2.stop("omero.import.process.opener");
                    }
                    return new UploadState(prx);
                }
            };

            try {
                return uploaders.get(i, rfsOpener).prx;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    /* there are no checked exceptions to worry about, so this cannot happen */
                    return null;
                }
            }
        } finally {
            sw1.stop("omero.import.process.uploader");
            MDC.clear();
        }
    }

    protected void registerCallback(RawFileStorePrx prx, final int idx) {
        Object servant = this.sf.getServant(prx.ice_getIdentity());
        if (servant instanceof Advised) {
            try {
                servant = ((Advised) servant).getTargetSource().getTarget();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        RepoRawFileStoreI store = (RepoRawFileStoreI) servant;

        final ManagedImportProcessI proc = this;
        store.setCallback(new RepoRawFileStoreI.NoOpCallback() {

            @Override
            public void onWrite(byte[] buf, long position, long length) {
                proc.setOffset(idx, position+length);
            }

            /**
             * During the close process, remove this instance from the
             * "uploaders" hash map in order to prevent concurrent access
             * issues.
             */
            @Override
            public void onPreClose() {
                proc.closeCalled(idx);
            }
        });
    }

    public HandlePrx verifyUpload(List<String> hashes, Current __current)
            throws ServerError {

        try {
            MDC.put("fileset", logFilename);
            final int size = fs.sizeOfUsedFiles();
            if (hashes == null) {
                throw new omero.ApiUsageException(null, null,
                        "hashes list cannot be null");
            } else if (hashes.size() != size) {
                throw new omero.ApiUsageException(null, null,
                        String.format("hashes size should be %s not %s", size,
                                hashes.size()));
            }

            Map<Integer, String> failingChecksums = new HashMap<Integer, String>();
            Map<String, String> groupContext = ImmutableMap.of(Login.OMERO_GROUP, "-1");
            final IQueryPrx iQuery = sf.getQueryService(__current);
            final String hql = "SELECT originalFile.hash, originalFile.path || originalFile.name FROM FilesetEntry "
                    + "WHERE fileset.id = :id";
            final Parameters params = new ParametersI().addId(fs.getId());
            List<List<RType>> results;
            try {
                results = iQuery.projection(hql, params, groupContext);
            } catch (SecurityViolation sv) {
                if (groupContext == null) {
                    /* No other workaround to try. */
                    throw sv;
                }
                /* The permissions context probably locks us to only certain groups. */
                log.debug("all-groups query for file checksum failed, retrying with current group context", sv);
                results = iQuery.projection(hql, params);
                groupContext = null;
            }
            Map<String, String> serverHashes = new HashMap<String, String>();
            for (int i = 0; i < results.size(); i++) {
                serverHashes.put(
                    ((RString) results.get(i).get(1)).getValue(),
                    ((RString) results.get(i).get(0)).getValue());
            }
            for (int i = 0; i < size; i++) {
                StopWatch sw1 = new Slf4JStopWatch();
                String usedFile = location.sharedPath + FsFile.separatorChar + location.usedFiles.get(i);
                final String clientHash = hashes.get(i);
                String serverHash = serverHashes.getOrDefault(usedFile, null);
                if (serverHash == null) {
                    log.error("no server checksum on uploaded file {}", usedFile);
                    failingChecksums.put(i, "");
                } else if (!clientHash.equals(serverHash)) {
                    failingChecksums.put(i, serverHash);
                }

                sw1.stop("omero.import.process.checksum");
            }

            if (!failingChecksums.isEmpty()) {
                throw new omero.ChecksumValidationException(null,
                        omero.ChecksumValidationException.class.toString(),
                        "A checksum mismatch has occurred.",
                        failingChecksums);
            }

            StopWatch sw2 = new Slf4JStopWatch();
            // i==0 is the upload job which is implicit.
            FilesetJobLink link = fs.getFilesetJobLink(0);
            repo.repositoryDao.updateJob(link.getChild(),
                    "Finished", "Finished", this.current);

            // Now move on to the metadata import.
            link = fs.getFilesetJobLink(1);
            CheckedPath checkedPath = ((ManagedImportLocationI) location).getLogFile();
            final omero.model.OriginalFile logFile = repo.findInDb(checkedPath, "r", __current);

            final String reqId = ImportRequest.ice_staticId();
            final ImportRequest req = (ImportRequest)
                    repo.getFactory(reqId, this.current).create(reqId);
            // TODO: Should eventually be from a new omero.client
            req.clientUuid = UUID.randomUUID().toString();
            req.repoUuid = repo.getRepoUuid();
            req.process = this.proxy;
            req.activity = link;
            req.location = location;
            req.settings = settings;
            req.logFile = logFile;
            if (req instanceof ManagedImportRequestI && current.ctx != null) {
                /* propagate this process' call context to the new import request */
                ((ManagedImportRequestI) req).setCallContext(current.ctx);
            }
            final AMD_submit submit = repo.submitRequest(sf, req, this.current, Executor.Priority.BACKGROUND);
            this.handle = submit.ret;
            // TODO: in 5.1 this should be added to the request object
            ((ManagedImportRequestI) req).handle = submit.ret;
            sw2.stop("omero.import.process.submit");
            return submit.ret;
        } finally {
            MDC.clear();
        }
    }

    //
    // GETTERS
    //

    public long getUploadOffset(int idx, Current ignore) throws ServerError {
        final UploadState state = uploaders.getIfPresent(idx);
        if (state == null) {
            return 0;
        }
        return state.offset;
    }

    public HandlePrx getHandle(Ice.Current ignore) {
        return handle;
    }

    //
    // OTHER LOCAL INVOCATIONS
    //

    public void setOffset(int idx, long offset) {
        final UploadState state = uploaders.getIfPresent(idx);
        if (state == null) {
            log.warn(String.format("setOffset(%s, %s) - no such object", idx, offset));
        } else {
            state.setOffset(offset);
            log.debug(String.format("setOffset(%s, %s) successfully", idx, offset));
        }
    }

    public void closeCalled(int idx) {
        final UploadState state = uploaders.getIfPresent(idx);
        if (state == null) {
            log.warn(String.format("closeCalled(%s) - no such object", idx));
        } else {
            uploaders.invalidate(idx);
            log.debug(String.format("closeCalled(%s) successfully", idx));
        }
    }

    //
    // CLOSE LOGIC
    //

    @Override
    protected void preClose(Current current) throws Throwable {
        // no-op
    }

    @Override
    protected void postClose(Current current) {
        // no-op
    }
}
