/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.binding.onecta.internal.handler;

import static org.openhab.binding.onecta.internal.constants.OnectaBridgeConstants.*;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.onecta.internal.OnectaTranslationProvider;
import org.openhab.binding.onecta.internal.api.OnectaConnectionClient;
import org.openhab.binding.onecta.internal.api.dto.units.Unit;
import org.openhab.binding.onecta.internal.api.dto.units.Units;
import org.openhab.binding.onecta.internal.exception.DaikinCommunicationException;
import org.openhab.binding.onecta.internal.oauth2.auth.OAuthTokenRefresher;
import org.openhab.binding.onecta.internal.service.DeviceDiscoveryService;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OnectaBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Drent - Initial contribution
 */
@NonNullByDefault
public class OnectaBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(OnectaBridgeHandler.class);

    private @Nullable ScheduledFuture<?> pollingJob;

    private @Nullable DeviceDiscoveryService deviceDiscoveryService;

    private Units units = new Units();
    private OnectaConnectionClient onectaConnectionClient;
    private @Nullable OnectaTranslationProvider onectaTranslationProvider;

    public OnectaBridgeHandler(Bridge bridge, OAuthTokenRefresher oAuthTokenRefresher,
            HttpClientFactory httpClientFactory, OnectaTranslationProvider onectaTranslationProvider) {
        super(bridge);
        onectaConnectionClient = new OnectaConnectionClient(oAuthTokenRefresher, httpClientFactory);
        this.onectaTranslationProvider = onectaTranslationProvider;
    }

    public List<Unit> getUnits() {
        return onectaConnectionClient.getUnits().getAll();
    }

    public OnectaTranslationProvider getOnectaTranslationProvider() {
        Optional<OnectaTranslationProvider> optionalTranslation = Optional.ofNullable(onectaTranslationProvider);
        return optionalTranslation.orElseThrow(() -> new RuntimeException("Translation provider is not available"));
    }

    /**
     * Defines a runnable for a discovery
     */
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (deviceDiscoveryService != null) {
                deviceDiscoveryService.startScan();
            }
        }
    };

    // ToDo remove code
    /*
     * public OnectaBridgeHandler(Bridge bridge, OnectaConfiguration onectaConfiguration) {
     * super(bridge);
     * this.onectaConfiguration = onectaConfiguration;
     * onectaConnectionClient = onectaConfiguration.getOnectaConnectionClient();
     * }
     */

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        logger.debug("initialize.");
        syncStatus();

        pollingJob = scheduler.scheduleWithFixedDelay(this::pollDevices, 0,
                Integer.parseInt(thing.getConfiguration().get(CONFIG_PAR_REFRESHINTERVAL).toString()),
                TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        logger.debug("Handler disposed.");
        ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null) {
            pollingJob.cancel(true);
            this.pollingJob = null;
        }
    }

    private void syncStatus() {
        updateStatus(ThingStatus.UNKNOWN);
        if (onectaConnectionClient.isOnline()) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            onectaConnectionClient.openConnecttion();
            updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    "@text/offline.communication-error");
        }
    }

    private void pollDevices() {
        logger.debug("pollDevices.");

        if (!onectaConnectionClient.isOnline()) {
            syncStatus();
        }

        if (getThing().getStatus().equals(ThingStatus.ONLINE) || getThing().getStatus().equals(ThingStatus.UNKNOWN)) {
            try {
                onectaConnectionClient.refreshUnitsData();
                updateStatus(ThingStatus.ONLINE);

                List<Thing> things = getThing().getThings();
                for (Thing t : things) {
                    if (t.isEnabled()) {
                        ((AbstractOnectaHandler) Objects.requireNonNull(t.getHandler())).refreshDevice();
                    }
                }
            } catch (DaikinCommunicationException e) {
                logger.debug("DaikinCommunicationException: {}", e.getMessage());
                updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            } catch (NullPointerException e) {
                logger.debug("NullPointerException: {}", e.getMessage());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_MISSING_ERROR, e.getMessage());
            }
        }
    }

    public void setDiscoveryService(DeviceDiscoveryService deviceDiscoveryService) {
        this.deviceDiscoveryService = deviceDiscoveryService;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(DeviceDiscoveryService.class);
    }
}
