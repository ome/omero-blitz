/*
 * Copyright (C) 2019 University of Dundee & Open Microscopy Environment.
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

package ome.services.blitz.util;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

/**
 * Utility class for working with the file-system.
 * @author m.t.b.carroll@dundee.ac.uk
 * @since 5.5.0
 */
public class FileSystemUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(FileSystemUtil.class);

    private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

    private static final Set<PosixFilePermission> FILE_PERMISSIONS_WRITE = ImmutableSet.of(
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.OTHERS_WRITE);

    /**
     * Remove write permissions from the given file.
     * @param file the file to set to read-only
     * @return if the file is now a read-only regular file
     */
    public static boolean setReadOnly(String file) {
            final Path path = FILE_SYSTEM.getPath(file);
            try {
                if (Files.isSymbolicLink(path)) {
                    LOGGER.debug("ignoring symbolic link: {}", path);
                } else if (Files.isRegularFile(path)) {
                    final Set<PosixFilePermission> origPerms = Files.getPosixFilePermissions(path);
                    final Set<PosixFilePermission> newPerms = new HashSet<>(origPerms);
                    newPerms.removeAll(FILE_PERMISSIONS_WRITE);
                    if (origPerms.equals(newPerms)) {
                        LOGGER.debug("already read-only: {}", path);
                    } else {
                        Files.setPosixFilePermissions(path, newPerms);
                        LOGGER.info("set to read-only: {}", path);
                    }
                    return true;
                }
            } catch (IOException | UnsupportedOperationException e) {
                LOGGER.warn("failed to set to read-only: {}", path, e);
            }
            return false;
    }
}
