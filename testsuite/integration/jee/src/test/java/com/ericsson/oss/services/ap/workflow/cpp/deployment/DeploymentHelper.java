/*
 ------------------------------------------------------------------------------
  *******************************************************************************
  * COPYRIGHT Ericsson 2017
  *
  * The copyright to the computer program(s) herein is the property of
  * Ericsson Inc. The programs may be used and/or copied only with written
  * permission from Ericsson Inc. or in accordance with the terms and
  * conditions stipulated in the agreement/contract under which the
  * program(s) have been supplied.
  *******************************************************************************
  *----------------------------------------------------------------------------
 */

package com.ericsson.oss.services.ap.workflow.cpp.deployment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Assignable;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 * Generic helper class that provides methods to help package the test deployment archive.
 */
public class DeploymentHelper {

    private static final String ARQUILLIAN_XML = "/arquillian.xml";
    private static final String CONTENT_ROOT = "/META-INF/content";
    private static final String PROPERTIES_DEFAULT_PREFIX = "arq.";
    private static final String PROPERTIES_FILE_NAME = "/arq_system.properties";

    private static class Resource {
        private final ArchivePath path;
        private final Asset asset;

        public Resource(final Asset asset, final ArchivePath path) {
            this.asset = asset;
            this.path = path;
        }

        public ArchivePath getPath() {
            return path;
        }

        public Asset getAsset() {
            return asset;
        }
    }

    private DeploymentHelper() {
    }

    /**
     * Get the dependencies that are identified in the given list of GAVs.
     * GAV is GroupId, ArtifactId and Version as used in the maven pom.
     *
     * @param gavs
     * @return
     */
    protected static File[] getGavsFiles(final String... gavs) {
        final List<File> result = new ArrayList<>();
        for (final String gav : gavs) {
            result.addAll(Arrays.<File>asList(getGavFiles(gav)));
        }

        return result.toArray(new File[result.size()]);
    }

    /**
     * Get the dependency that is identified by the given GAV.
     * GAV is GroupId, ArtifactId and Version as used in the maven pom.
     *
     * @param gav
     * @return
     */
    protected static File[] getGavFiles(final String gav) {
        return Maven
            .resolver()
            .loadPomFromFile("pom.xml")
            .resolve(gav)
            .withTransitivity()
            .asFile();
    }

    /**
     * Create and return a WAR Archive for the given name and GAV.
     * @param name
     * @param gav
     * @return
     */
    protected static WebArchive getArchive(final String name, final String gav) {
        return ShrinkWrap
            .create(WebArchive.class, name)
            .as(ZipImporter.class)
            .importFrom(fromMaven(gav))
            .as(WebArchive.class);
    }

    /**
     * Create and return an archive for the given archive type and GAV.
     *
     * @param clazz
     * @param gav
     * @param <T>
     * @return
     */
    protected static <T extends Assignable> T getArchiveByType(final Class<T> clazz, final String gav) {
        return ShrinkWrap.createFromZipFile(clazz, fromMaven(gav));
    }


    protected static File fromMaven(final String gav) {
        return Maven.resolver()
            .loadPomFromFile("pom.xml")
            .resolve(gav)
            .withoutTransitivity()
            .asSingleFile();
    }

    protected static JavaArchive createContentsArchive() {
        final JavaArchive result = ShrinkWrap.create(JavaArchive.class, "ear-content.jar");
        final Collection<Resource> resources = getResources();

        for (final Resource resource : resources) {
            result.addAsResource(resource.getAsset(), resource.getPath());
        }

        return result;
    }

    private static Collection<Resource> getResources() {
        final Collection<Resource> data = new ArrayList<>();

        final URL base = WorkflowDeployments.class.getResource("/content");
        appendFiles(CONTENT_ROOT, new File(base.getFile()), data);

        final URL arquillianXml = WorkflowDeployments.class.getResource(ARQUILLIAN_XML);
        appendFiles(CONTENT_ROOT + ARQUILLIAN_XML, new File(arquillianXml.getFile()), data);

        return appendSystemProperties(data);
    }

    private static Collection<Resource> appendSystemProperties(final Collection<Resource> resources) {
        resources.add(new Resource(
            new StringAsset(toString(filterSystemProperties(PROPERTIES_DEFAULT_PREFIX))),
            ArchivePaths.create(CONTENT_ROOT + PROPERTIES_FILE_NAME)));
        return resources;
    }

    private static Properties filterSystemProperties(final String prefix) {
        final Properties filteredProps = new Properties();
        final Properties sysProps = System.getProperties();

        for (final Map.Entry<Object, Object> entry : sysProps.entrySet()) {
            if (entry.getKey().toString().startsWith(prefix)) {
                final String newKey = entry.getKey().toString().replaceFirst(prefix, "");
                filteredProps.setProperty(newKey, (String) entry.getValue());
            }
        }

        return filteredProps;
    }

    private static String toString(final Properties props) {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            props.store(out, "Arquillian SystemProperties Extension");
            return out.toString();
        } catch (final Exception e) {
            throw new IllegalStateException("Could not store properties", e);
        }
    }

    private static void appendFiles(final String path, final File file, final Collection<Resource> data) {
        if (file.isDirectory()) {
            final File[] files = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(final File pathname) {
                    return pathname.isFile() || !pathname.getName().startsWith(".");
                }
            });

            if (files != null) {
                appendFiles(path, files, data);
            }
        } else {
            appendAsset(path, file, data);
        }
    }

    private static void appendFiles(final String path, final File[] files, final Collection<Resource> data) {
        for (final File file : files) {
            final String name = file.getName();
            final String nextPath = path + "/" + name;
            appendFiles(nextPath, file, data);
        }
    }

    private static void appendAsset(final String path, final File file, final Collection<Resource> data) {
        final Asset fileAsset = new FileAsset(file);
        final ArchivePath archivePath = ArchivePaths.create(path);
        data.add(new Resource(fileAsset, archivePath));
    }
}
