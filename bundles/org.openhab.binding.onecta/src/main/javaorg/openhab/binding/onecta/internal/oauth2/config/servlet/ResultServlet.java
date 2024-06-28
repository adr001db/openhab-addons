/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.onecta.internal.oauth2.config.servlet;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.onecta.internal.oauth2.auth.OAuthException;
import org.openhab.binding.onecta.internal.oauth2.config.OAuthAuthorizationHandler;
import org.openhab.binding.onecta.internal.oauth2.config.exception.NoOngoingAuthorizationException;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet processing the response by the Onecta service after a login. This servlet is called as a result of a
 * completed login to the Onecta service and assumes that the OAuth 2 parameters are passed. Depending on the parameters
 * and whether the token response can be fetched either the browser is redirected to the success or the failure page.
 *
 * @author Björn Lange - Initial Contribution
 */
@NonNullByDefault
public final class ResultServlet extends AbstractRedirectionServlet {
    private static final long serialVersionUID = 2157912755568949550L;

    public static final String CODE_PARAMETER_NAME = "code";
    public static final String STATE_PARAMETER_NAME = "state";
    public static final String ERROR_PARAMETER_NAME = "error";

    private final Logger logger = LoggerFactory.getLogger(ResultServlet.class);

    private final OAuthAuthorizationHandler authorizationHandler;

    /**
     * Creates a new {@link ResultServlet}.
     *
     * @param authorizationHandler Handler implementing the OAuth authorization.
     */
    public ResultServlet(OAuthAuthorizationHandler authorizationHandler) {
        this.authorizationHandler = authorizationHandler;
    }

    @Override
    protected String getRedirectionDestination(HttpServletRequest request) {
        String error = request.getParameter(ERROR_PARAMETER_NAME);
        if (error != null) {
            logger.warn("Received error response: {}", error);
            return "/onecta/failure?" + FailureServlet.OAUTH2_ERROR_PARAMETER_NAME + "=" + error;
        }

        String code = request.getParameter(CODE_PARAMETER_NAME);
        if (code == null) {
            logger.warn("Code is null");
            return "/onecta/failure?" + FailureServlet.ILLEGAL_RESPONSE_PARAMETER_NAME + "=true";
        }
        String state = request.getParameter(STATE_PARAMETER_NAME);
        if (state == null) {
            logger.warn("State is null");
            return "/onecta/failure?" + FailureServlet.ILLEGAL_RESPONSE_PARAMETER_NAME + "=true";
        }

        try {
           
            StringBuffer requestUrl = request.getRequestURL();
            if (requestUrl == null) {
                return "/onecta/failure?" + FailureServlet.MISSING_REQUEST_URL_PARAMETER_NAME + "=true";
            }

            try {
                authorizationHandler.completeAuthorization(requestUrl.toString() + "?" + request.getQueryString());
            } catch (OAuthException e) {
                logger.warn("Failed to complete authorization.", e);
                return "/onecta/failure?" + FailureServlet.FAILED_TO_COMPLETE_AUTHORIZATION_PARAMETER_NAME + "=true";
            }

            return "/onecta/success";
        } catch (NoOngoingAuthorizationException e) {
            logger.warn("Failed to complete authorization: There is no ongoing authorization or it timed out");
            return "/onecta/failure?" + FailureServlet.NO_ONGOING_AUTHORIZATION_PARAMETER_NAME + "=true";
        }
    }
}