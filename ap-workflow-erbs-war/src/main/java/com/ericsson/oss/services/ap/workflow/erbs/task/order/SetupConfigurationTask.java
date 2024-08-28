/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.erbs.task.order;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.common.model.MoType;
import com.ericsson.oss.services.ap.common.model.NodeAttribute;
import com.ericsson.oss.services.ap.common.model.NodeDhcpAttribute;
import com.ericsson.oss.services.ap.common.model.SupervisionMoType;
import com.ericsson.oss.services.ap.common.usecase.CommandLogName;
import com.ericsson.oss.services.ap.common.util.log.DdpTimer;
import com.ericsson.oss.services.ap.common.workflow.AbstractWorkflowVariables;
import com.ericsson.oss.services.ap.workflow.cpp.model.LicenseAttribute;
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;
import com.ericsson.oss.services.wfs.internal.WorkflowInternalConstants;
import com.ericsson.oss.services.wfs.task.api.AbstractServiceTask;
import com.ericsson.oss.services.wfs.task.api.TaskExecution;

/**
 * This service task is used to configure the workflow variables.
 */
public class SetupConfigurationTask extends AbstractServiceTask {

    private static final String USE_CASE_START_TIME_KEY = "useCaseStartTime";
    private static final String FDN_KEY = "fdn";
    private static final String AI_UPLOAD_CV_AFTER_INTEGRATION_ATTRIBUTE = "uploadCVAfterIntegration";
    private static final String LICENSE_INSTALL_LICENSE_ATTRIBUTE = "installLicense";
    private static final String LICENSE_ACTIVATE_LICENSE_ATTRIBUTE = "activateLicense";
    private static final String AI_UNLOCK_CELLS_ATTRIBUTE = "unlockCells";
    private static final String AI_ENABLE_FM_SUPERVISION = "fm";
    private static final String AI_ENABLE_PM_SUPERVISION = "pm";
    private static final String AI_ENABLE_INV_SUPERVISION = "inventory";
    private static final String ENABLED = "enabled";

    private final DdpTimer ddpTimer = new DdpTimer();
    private final ServiceFinderBean serviceFinder = new ServiceFinderBean();

    @Override
    public void executeTask(final TaskExecution execution) {
        ddpTimer.start(CommandLogName.SETUP_CONFIGURATION.toString());
        final ErbsWorkflowVariables workflowVariables = createErbsWorkflowVariables(execution);
        execution.setVariable(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY, workflowVariables);
        ddpTimer.end(workflowVariables.getApNodeFdn());
    }

    @SuppressWarnings("unchecked")
    private ErbsWorkflowVariables createErbsWorkflowVariables(final TaskExecution execution) {
        final Map<String, Object> processVariables = execution.getVariables();
        final ErbsWorkflowVariables workflowVariables = new ErbsWorkflowVariables();
        final String apNodeFdn = processVariables.get(FDN_KEY).toString();
        workflowVariables.setApNodeFdn(apNodeFdn);

        final Map<String, Serializable> wfsContext = (Map<String, Serializable>) processVariables.get(WorkflowInternalConstants.WFS_CONTEXT);
        workflowVariables.setUserId((String) wfsContext.get(WorkflowInternalConstants.USERNAME_KEY));

        if (processVariables.containsKey(USE_CASE_START_TIME_KEY)) {
            workflowVariables.setOrderStartTime((long) processVariables.get(USE_CASE_START_TIME_KEY));
        }

        final ManagedObject apNodeMo = getApNodeMo(apNodeFdn);
        final ManagedObject aiOptionsMo = findAutoIntegrationOptionsMo(apNodeMo);
        final ManagedObject supervisionMo = findSupervisionMo(apNodeMo);
        final ManagedObject licenseOptionsMo = findLicenseOptionsMo(apNodeMo);

        workflowVariables.setNodeType(getNodeType(apNodeMo));
        workflowVariables.setCreateUserCredentials(nodeUserCredentialsExist(apNodeMo));
        workflowVariables.setUnlockCells(isUnlockCellsEnabled(aiOptionsMo));
        workflowVariables.setUploadCvAfterIntegrationEnabled(isUploadCvAfterIntegrationEnabled(aiOptionsMo));

        setSupervisionOptions(supervisionMo, workflowVariables);
        setDhcpOptions(processVariables, getNodeDhcpMo(apNodeMo), workflowVariables);

        setImportLicenseKeyFile(getLicenseMo(apNodeMo), workflowVariables);

        final boolean installLicense = isInstallLicenseEnabled(licenseOptionsMo);
        workflowVariables.setInstallLicense(installLicense);
        if (installLicense) {
            workflowVariables.setActivateOptionalFeatures(isActivateLicenseEnabled(licenseOptionsMo));
        }
        workflowVariables.setSecurityEnabled(isNodeSecurityEnabled(apNodeMo));

        final String hardwareSerialNumber = getHardwareSerialNumber(apNodeMo);
        if (StringUtils.isNotBlank(hardwareSerialNumber)) {
            workflowVariables.setHardwareSerialNumber(hardwareSerialNumber);
        }

        final String ossPrefix = getOssPrefix(apNodeMo);
        if (StringUtils.isNotBlank(ossPrefix)) {
            workflowVariables.setOssPrefix(ossPrefix);
        }

        return workflowVariables;
    }

    private static ManagedObject findLicenseOptionsMo(final ManagedObject apNodeMo) {
        return apNodeMo.getChild(MoType.LICENSE_OPTIONS.toString() + "=1");
    }

    private static ManagedObject findAutoIntegrationOptionsMo(final ManagedObject apNodeMo) {
        return apNodeMo.getChild(MoType.AI_OPTIONS.toString() + "=1");
    }

    private static ManagedObject findSupervisionMo(final ManagedObject apNodeMo) {
        return apNodeMo.getChild(MoType.SUPERVISION_OPTIONS.toString() + "=1");
    }

    private ManagedObject getApNodeMo(final String apNodeFdn) {
        final DataPersistenceService dps = serviceFinder.find(DataPersistenceService.class);
        final DataBucket liveBucket = dps.getLiveBucket();
        return liveBucket.findMoByFdn(apNodeFdn);
    }

    private static boolean isUnlockCellsEnabled(final ManagedObject aiOptionsMo) {
        return (boolean) (aiOptionsMo.getAttribute(AI_UNLOCK_CELLS_ATTRIBUTE));
    }

    private static boolean isNodeSecurityEnabled(final ManagedObject apNodeMo) {
        final ManagedObject securityMoFdn = apNodeMo.getChild(MoType.SECURITY.toString() + "=1");
        return securityMoFdn != null;
    }

    private static boolean isUploadCvAfterIntegrationEnabled(final ManagedObject aiOptionsMo) {
        return (boolean) (aiOptionsMo.getAttribute(AI_UPLOAD_CV_AFTER_INTEGRATION_ATTRIBUTE));
    }

    private static void setSupervisionOptions(final ManagedObject supervisionMo, final ErbsWorkflowVariables workflowVariables) {
        if (supervisionMo != null) {
            workflowVariables.setEnableSupervision(SupervisionMoType.FM, isEnableFmSupervision(supervisionMo));
            workflowVariables.setEnableSupervision(SupervisionMoType.PM, isEnablePmSupervision(supervisionMo));
            workflowVariables.setEnableSupervision(SupervisionMoType.INVENTORY, isEnableInvSupervision(supervisionMo));
        }
    }

    private static void setDhcpOptions(final Map<String, Object> processVariables, final ManagedObject nodeDhcpMo,
                                       final ErbsWorkflowVariables workflowVariables) {

        if (processVariables.containsKey(AbstractWorkflowVariables.DHCP_CLIENT_ID_TO_REMOVE_KEY)) {
            final String dhcpClientId = processVariables.get(AbstractWorkflowVariables.DHCP_CLIENT_ID_TO_REMOVE_KEY).toString();
            if (isNotBlank(dhcpClientId)) {
                workflowVariables.setDhcpConfiguration(true);
                workflowVariables.setOldHardwareSerialNumber(dhcpClientId);
            }
        } else if (nodeDhcpMo != null) {
            workflowVariables.setDhcpConfiguration(true);
            workflowVariables.setInitialIpAddress(getInitialIpAddress(nodeDhcpMo));
            workflowVariables.setDefaultRouter(getDefaultRouter(nodeDhcpMo));
            workflowVariables.setNtpServers(getNtpServers(nodeDhcpMo));
            workflowVariables.setDnsServers(getDnsServers(nodeDhcpMo));
        }
    }

    private static boolean isEnableFmSupervision(final ManagedObject supervisionMo) {
        return ENABLED.equalsIgnoreCase((String) supervisionMo.getAttribute(AI_ENABLE_FM_SUPERVISION));
    }

    private static boolean isEnablePmSupervision(final ManagedObject supervisionMo) {
        return ENABLED.equalsIgnoreCase((String) (supervisionMo.getAttribute(AI_ENABLE_PM_SUPERVISION)));
    }

    private static boolean isEnableInvSupervision(final ManagedObject supervisionMo) {
        return ENABLED.equalsIgnoreCase((String) (supervisionMo.getAttribute(AI_ENABLE_INV_SUPERVISION)));
    }

    private static ManagedObject getLicenseMo(final ManagedObject apNodeMo) {
        return apNodeMo.getChild(MoType.LICENSE_OPTIONS.toString() + "=1");
    }

    private static void setImportLicenseKeyFile(final ManagedObject licenseMo, final ErbsWorkflowVariables workflowVariables) {
        if (licenseMo != null) {
            workflowVariables.setImportLicenseKeyFile(licenseMo.getAttribute(LicenseAttribute.LICENSE_KEY_FILE.toString()) != null);
        }
    }

    private static boolean isInstallLicenseEnabled(final ManagedObject licenseOptionsMo) {
        return (boolean) licenseOptionsMo.getAttribute(LICENSE_INSTALL_LICENSE_ATTRIBUTE);
    }

    private static boolean isActivateLicenseEnabled(final ManagedObject licenseOptionsMo) {
        return (boolean) licenseOptionsMo.getAttribute(LICENSE_ACTIVATE_LICENSE_ATTRIBUTE);
    }

    private static boolean nodeUserCredentialsExist(final ManagedObject apNodeMo) {
        final ManagedObject nodeUserCredapNodeMo = apNodeMo.getChild(MoType.NODE_USER_CREDENTIALS.toString() + "=1");
        return nodeUserCredapNodeMo != null;
    }

    private static String getHardwareSerialNumber(final ManagedObject apNodeMo) {
        return apNodeMo.getAttribute(NodeAttribute.HARDWARE_SERIAL_NUMBER.toString());
    }

    private static ManagedObject getNodeDhcpMo(final ManagedObject apNodeMo) {
        return apNodeMo.getChild(MoType.NODE_DHCP.toString() + "=1");
    }

    private static String getInitialIpAddress(final ManagedObject nodeDhcpMo) {
        return nodeDhcpMo.getAttribute(NodeDhcpAttribute.INITIAL_IP_ADDRESS.toString());
    }

    private static String getDefaultRouter(final ManagedObject nodeDhcpMo) {
        return nodeDhcpMo.getAttribute(NodeDhcpAttribute.DEFAULT_ROUTER.toString());
    }

    private static List<String> getNtpServers(final ManagedObject nodeDhcpMo) {
        return nodeDhcpMo.getAttribute(NodeDhcpAttribute.NTP_SERVER.toString());
    }

    private static List<String> getDnsServers(final ManagedObject nodeDhcpMo) {
        return nodeDhcpMo.getAttribute(NodeDhcpAttribute.DNS_SERVER.toString());
    }

    private static String getOssPrefix(final ManagedObject apNodeMo) {
        return apNodeMo.getAttribute(NodeAttribute.OSS_PREFIX.toString());
    }

    private static String getNodeType(final ManagedObject apNodeMo) {
        return apNodeMo.getAttribute(NodeAttribute.NODE_TYPE.toString());
    }
}
