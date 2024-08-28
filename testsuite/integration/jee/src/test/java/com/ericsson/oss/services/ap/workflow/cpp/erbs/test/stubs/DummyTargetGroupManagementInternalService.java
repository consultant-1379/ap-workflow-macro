/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.stubs;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.services.security.genericidentitymgmtserviceapi.exceptions.EntityNotFoundException;
import com.ericsson.oss.services.security.genericidentitymgmtserviceapi.exceptions.InternalLogicException;
import com.ericsson.oss.services.security.genericidentitymgmtserviceapi.exceptions.InternalUnexpectedException;
import com.ericsson.oss.services.security.genericidentitymgmtserviceapi.exceptions.InvalidArgumentException;
import com.ericsson.oss.services.security.genericidentitymgmtserviceapi.targetgroup.TargetGroupManagementInternalService;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import java.util.List;

@Stateless
@EService
@Remote
public class DummyTargetGroupManagementInternalService implements TargetGroupManagementInternalService {

    public static boolean throwExceptionOnAddTargetsToTargetGroup = false; //NOSONAR
    public static boolean throwNonRetriableException = false; //NOSONAR
    public static int throwExceptionOnAddTargetsToTargetGroupRetryCount = 0; //NOSONAR

    public static final String EXCEPTION_MESSAGE="Cannot add Target Group";

    @Override
    public void addTargetsToTargetGroup(List<String> list, String s)
        throws EntityNotFoundException, InternalLogicException, InternalUnexpectedException, InvalidArgumentException { //NOSONAR
        if (throwExceptionOnAddTargetsToTargetGroup) {
            if (--throwExceptionOnAddTargetsToTargetGroupRetryCount <= 0) {
                throwExceptionOnAddTargetsToTargetGroup = false; //NOSONAR
            }
            if (throwNonRetriableException) {
                throw new NullPointerException(EXCEPTION_MESSAGE);
            }
            throw new InternalLogicException(EXCEPTION_MESSAGE);
        }
    }

    @Override
    public void addTargetsToTargetGroups(List<String> list, List<String> list1) throws EntityNotFoundException, InternalLogicException, InternalUnexpectedException, InvalidArgumentException {
        if (throwExceptionOnAddTargetsToTargetGroup) {
            if (--throwExceptionOnAddTargetsToTargetGroupRetryCount <= 0) {
                throwExceptionOnAddTargetsToTargetGroup = false; //NOSONAR
            }
            if (throwNonRetriableException) {
                throw new NullPointerException(EXCEPTION_MESSAGE);
            }
            throw new InternalLogicException(EXCEPTION_MESSAGE);
        }
    }
}