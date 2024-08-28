/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.deployment;

import java.io.File;

import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.path.BasicPath;

import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.OrderErbsWorkflowTest;

/**
 * Main arquillian test deployment class that is instantiated when arquillian test is invoked.
 */
@ArquillianSuiteDeployment
public class WorkflowDeployments {

    /**
     * ERBS war is packaged by ap-testware-integration as the main deployment. Other node specific wars should then be resolved and merged with this
     * 'main' deployment archive.
     **/
    private static final String ERBS_DEFAULT_WAR = "com.ericsson.oss.services.autoprovisioning:ap-workflow-erbs-war:war:?";

    private static final String APS_API_JAR = "com.ericsson.oss.services.autoprovisioning:aps-api:jar:?";

    /** ERBS jars to be added as libraries to the deployment archive **/
    private static final String CPP_EJB_JAR = "com.ericsson.oss.services.autoprovisioning:ap-workflow-cpp-ejb:jar:?";
    private static final String CPP_COMMON_JAR = "com.ericsson.oss.services.autoprovisioning:ap-workflow-cpp-jar:jar:?";
    private static final String ERBS_JAR = "com.ericsson.oss.services.autoprovisioning:ap-workflow-erbs-jar:jar:?";

    private static final String AP_TESTWARE_INTEGRATION_ALLURE_JAR = "com.ericsson.oss.services.autoprovisioning:ap-testware-integration-allure:jar:?";
    private static final String AP_TESTWARE_INTEGRATION_ASPECTS_JAR = "com.ericsson.oss.services.autoprovisioning:ap-testware-integration-aspects:jar:?";
    private static final String AP_TESTWARE_INTEGRATION_UTIL_JAR = "com.ericsson.oss.services.autoprovisioning:ap-testware-integration-util:jar:?";
    private static final String AP_TESTWARE_INTEGRATION_TESTNG_JAR = "com.ericsson.oss.services.autoprovisioning:ap-testware-integration-testng:jar:?";

    private static final String TEST_DEPENDENCY_FREEMARKER = "org.freemarker:freemarker:?";
    private static final String TEST_DEPENDENCY_ASSERTJ = "org.assertj:assertj-core:?";
    private static final String TEST_DEPENDENCY_ZIP4J = "net.lingala.zip4j:zip4j:?";
    private static final String TEST_DEPENDENCY_SHRINKWRAP_DESCRIPTORS = "org.jboss.shrinkwrap.descriptors:shrinkwrap-descriptors-spi:?";

    private static final String SLF4J_API_JAR = "/WEB-INF/lib/slf4j-api-1.7.21.jar";
    private static final String GUAVA_JAR = "/WEB-INF/lib/guava-18.0.jar";

    private WorkflowDeployments() {
    }

    @Deployment(testable = true)
    public static Archive<?> generate() {
        final WebArchive archiveWithErbsWar = getWorkflowErbsWar();
        final WebArchive finalArchive = updateResourceFilesForTest(archiveWithErbsWar);
        return finalArchive.addAsLibrary(createTestJar());
    }

    private static JavaArchive createTestJar() {
        return ShrinkWrap.create(JavaArchive.class, "arquillian-test.jar").addPackages(true, OrderErbsWorkflowTest.class.getPackage())
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .merge(DeploymentHelper.getArchiveByType(JavaArchive.class, AP_TESTWARE_INTEGRATION_UTIL_JAR));
    }

    /**
     * Uses ap testware integration to get the ap-workflow-erbs war. This war is configured as the 'main' deployment artifact in the
     * dependencies.properties. All dependencies are retrieved and added to the test archive.
     */
    private static WebArchive getWorkflowErbsWar() {
        return DeploymentHelper.getArchive("test.war", ERBS_DEFAULT_WAR)
            .addAsLibraries(DeploymentHelper.getGavsFiles(CPP_EJB_JAR, CPP_COMMON_JAR, ERBS_JAR)).

            addAsLibrary(DeploymentHelper.fromMaven(AP_TESTWARE_INTEGRATION_TESTNG_JAR))
            .addAsLibrary(DeploymentHelper.fromMaven(AP_TESTWARE_INTEGRATION_ALLURE_JAR))
            .addAsLibrary(DeploymentHelper.fromMaven(AP_TESTWARE_INTEGRATION_ASPECTS_JAR))
            .addAsLibrary(DeploymentHelper.fromMaven("com.ericsson.oss.services.shm:backupservice-remote-api:?"))
            .addAsWebInfResource("jboss-deployment-structure.xml").addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsLibrary(DeploymentHelper.createContentsArchive()).

            addAsWebInfResource("processes.xml", "classes/META-INF/processes.xml")
            .addAsWebInfResource("ServiceFrameworkConfiguration.properties", "classes/ServiceFrameworkConfiguration.properties");
    }

    /**
     * Replace any test resources where needed in the given archive, e.g. Manifest, config files, etc..
     *
     * @param archive
     * @return
     */
    private static WebArchive updateResourceFilesForTest(final WebArchive archive) {
        // First add the common test libraries.
        archive.addAsLibraries(DeploymentHelper.getGavsFiles(TEST_DEPENDENCY_FREEMARKER, TEST_DEPENDENCY_ASSERTJ, TEST_DEPENDENCY_ZIP4J,
            TEST_DEPENDENCY_SHRINKWRAP_DESCRIPTORS));

        // Other provided 3pp dependencies to be removed from the test war library, because they are exposed as jboss modules
        archive.delete(new BasicPath(SLF4J_API_JAR));
        archive.delete(new BasicPath(GUAVA_JAR));

        final File apsApi = DeploymentHelper.fromMaven(APS_API_JAR);
        archive.delete(new BasicPath("/WEB-INF/lib/" + apsApi.getName()));

        // Processes.xml to be replaced
        return addTestProcessesXml(archive);
    }

    /**
     * Replace the production processes.xml file with our test file. The production processes.xml contains filters on bpmn resource path for its own
     * specific war, so the camunda process definition scan will only pick up process defs at the filtered location. Our test processes.xml will allow
     * all bpmn definitions (i.e. for ERBS and RBS) to be picked up.
     */
    private static WebArchive addTestProcessesXml(final WebArchive war) {
        final File processesXml = new File(Thread.currentThread().getContextClassLoader().getResource("processes.xml").getFile());
        // Now remove the production processes.xml
        final ArchivePath path = new BasicPath("/WEB-INF/classes/META-INF/processes.xml");
        war.delete(path);
        // Add in our test processes.xml. Allows camunda scan to pick up all workflow defs
        war.addAsWebInfResource(processesXml, "classes/META-INF/processes.xml");
        return war;
    }
}
