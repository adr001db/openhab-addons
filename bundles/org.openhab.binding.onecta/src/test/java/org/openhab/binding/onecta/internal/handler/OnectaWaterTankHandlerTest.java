package org.openhab.binding.onecta.internal.handler;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.openhab.binding.onecta.internal.OnectaDeviceConstants.CHANNEL_AC_OPERATIONMODE;
import static org.openhab.binding.onecta.internal.OnectaWaterTankConstants.*;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.binding.onecta.internal.api.Enums;
import org.openhab.binding.onecta.internal.service.ChannelsRefreshDelay;
import org.openhab.binding.onecta.internal.service.DataTransportService;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

@ExtendWith(MockitoExtension.class)
public class OnectaWaterTankHandlerTest {

    private OnectaWaterTankHandler handler;

    @Mock
    private ThingHandlerCallback callbackMock;

    @Mock
    private Thing thingMock;

    @Mock
    private DataTransportService dataTransServiceMock;

    @Mock
    private ChannelsRefreshDelay channelsRefreshDelayMock;

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        Configuration thingConfiguration = new Configuration();
        thingConfiguration.setProperties(Map.of("unitID", "ThisIsDummyID", "refreshDelay", "10"));
        when(thingMock.getConfiguration()).thenReturn(thingConfiguration);
        handler = new OnectaWaterTankHandler(thingMock);
        handler.setCallback(callbackMock);

        // add Mock dataTransServiceMock to handler
        Field privateDataTransServiceField = OnectaWaterTankHandler.class.getDeclaredField("dataTransService");
        privateDataTransServiceField.setAccessible(true);
        privateDataTransServiceField.set(handler, dataTransServiceMock);

        // add Mock channelsRefreshDelayMock to handler
        Field privateChannelsRefreshDelayField = OnectaWaterTankHandler.class.getDeclaredField("channelsRefreshDelay");
        privateChannelsRefreshDelayField.setAccessible(true);
        privateChannelsRefreshDelayField.set(handler, channelsRefreshDelayMock);

        lenient().when(thingMock.getUID()).thenReturn(new ThingUID("onecta", "domesticHotWaterTank", "bridge"));
    }

    @AfterEach
    public void tearDown() {
        // Free any resources, like open database connections, files etc.
        handler.dispose();
    }

    @Test
    public void initializeShouldCallTheCallback() {
        // we expect the handler#initialize method to call the callbackMock during execution and
        // pass it the thingMock and a ThingStatusInfo object containing the ThingStatus of the thingMock.
        handler.initialize();
        verify(callbackMock).statusUpdated(eq(thingMock), argThat(arg -> arg.getStatus().equals(ThingStatus.ONLINE)));
    }

    @Test
    public void refreshDeviceNotAvailTest() {
        when(dataTransServiceMock.isAvailable()).thenReturn(false);
        handler.refreshDevice();
        verify(callbackMock).statusUpdated(eq(thingMock), argThat(arg -> arg.getStatus().equals(ThingStatus.UNKNOWN)));
    }

    @Test
    public void refreshDeviceOkTest() {

        when(dataTransServiceMock.isAvailable()).thenReturn(true);
        when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_POWER)).thenReturn(true);
        when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_AC_OPERATIONMODE)).thenReturn(true);
        when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_SETTEMP)).thenReturn(true);
        when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_SETTEMP_MIN)).thenReturn(true);
        when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_SETTEMP_MAX)).thenReturn(true);
        when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_SETTEMP_STEP)).thenReturn(true);

        when(dataTransServiceMock.getPowerOnOff()).thenReturn("ON");
        when(dataTransServiceMock.getCurrentOperationMode()).thenReturn(Enums.OperationMode.HEAT);
        when(dataTransServiceMock.getErrorCode()).thenReturn("Error");
        when(dataTransServiceMock.getIsInEmergencyState()).thenReturn(true);
        when(dataTransServiceMock.getIsInErrorState()).thenReturn(true);
        when(dataTransServiceMock.getIsInInstallerState()).thenReturn(true);
        when(dataTransServiceMock.getIsInWarningState()).thenReturn(true);
        when(dataTransServiceMock.getIsHolidayModeActive()).thenReturn(true);
        when(dataTransServiceMock.getPowerFulModeOnOff()).thenReturn("ON");
        when(dataTransServiceMock.getHeatupMode()).thenReturn(Enums.HeatupMode.REHEATSCHEDULE);
        when(dataTransServiceMock.getTankTemperature()).thenReturn(19.2);
        when(dataTransServiceMock.getCurrentTankTemperatureSet()).thenReturn(20.2);
        when(dataTransServiceMock.getCurrentTankTemperatureSetMin()).thenReturn(21.2);
        when(dataTransServiceMock.getCurrentTankTemperatureSetMax()).thenReturn(22.2);
        when(dataTransServiceMock.getCurrentTankTemperatureSetStep()).thenReturn(0.5);

        handler.refreshDevice();

        verify(callbackMock, times(0)).statusUpdated(eq(thingMock),
                argThat(arg -> arg.getStatus().equals(ThingStatus.UNKNOWN)));
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_POWER),
                OnOffType.from("ON"));
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_AC_OPERATIONMODE),
                new StringType("HEAT"));
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_ERRORCODE),
                new StringType("Error"));
        verify(callbackMock, times(1)).stateUpdated(
                new ChannelUID(thingMock.getUID(), CHANNEL_HWT_IS_IN_EMERGENCY_STATE), OnOffType.from("ON"));
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_IS_IN_ERROR_STATE),
                OnOffType.from("ON"));
        verify(callbackMock, times(1)).stateUpdated(
                new ChannelUID(thingMock.getUID(), CHANNEL_HWT_IS_IN_INSTALLER_STATE), OnOffType.from("ON"));
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_IS_IN_WARNING_STATE),
                OnOffType.from("ON"));
        verify(callbackMock, times(1)).stateUpdated(
                new ChannelUID(thingMock.getUID(), CHANNEL_HWT_IS_HOLIDAY_MODE_ACTIVE), OnOffType.from("ON"));
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_POWERFUL_MODE),
                OnOffType.from("ON"));
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_HEATUP_MODE),
                new StringType("REHEATSCHEDULE"));
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_TANK_TEMPERATURE),
                new DecimalType(19.2));
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_SETTEMP),
                new DecimalType(20.2));
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_SETTEMP_MIN),
                new DecimalType(21.2));
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_SETTEMP_MAX),
                new DecimalType(22.2));
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_SETTEMP_STEP),
                new DecimalType(0.5));
    }

    @Test
    public void refreshDeviceUndefTest() {
        when(dataTransServiceMock.isAvailable()).thenReturn(true);
        when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_POWER)).thenReturn(true);
        when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_AC_OPERATIONMODE)).thenReturn(true);
        when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_SETTEMP)).thenReturn(true);
        when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_SETTEMP_MIN)).thenReturn(true);
        when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_SETTEMP_MAX)).thenReturn(true);
        when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_SETTEMP_STEP)).thenReturn(true);

        when(dataTransServiceMock.getPowerOnOff()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getCurrentOperationMode()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getErrorCode()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getIsInEmergencyState()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getIsInErrorState()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getIsInInstallerState()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getIsInWarningState()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getIsHolidayModeActive()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getPowerFulModeOnOff()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getHeatupMode()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getTankTemperature()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getCurrentTankTemperatureSet()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getCurrentTankTemperatureSetMin()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getCurrentTankTemperatureSetMax()).thenThrow(new RuntimeException("Simulating exception"));
        when(dataTransServiceMock.getCurrentTankTemperatureSetStep()).thenThrow(new RuntimeException("Simulating exception"));

        handler.refreshDevice();

        verify(callbackMock, times(0)).statusUpdated(eq(thingMock),
                argThat(arg -> arg.getStatus().equals(ThingStatus.UNKNOWN)));
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_POWER),
                UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_AC_OPERATIONMODE),
                UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_ERRORCODE),
                UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(
                new ChannelUID(thingMock.getUID(), CHANNEL_HWT_IS_IN_EMERGENCY_STATE), UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_IS_IN_ERROR_STATE),
                UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(
                new ChannelUID(thingMock.getUID(), CHANNEL_HWT_IS_IN_INSTALLER_STATE), UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_IS_IN_WARNING_STATE),
                UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(
                new ChannelUID(thingMock.getUID(), CHANNEL_HWT_IS_HOLIDAY_MODE_ACTIVE), UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_POWERFUL_MODE),
                UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_HEATUP_MODE),
                UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_TANK_TEMPERATURE),
                UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_SETTEMP),
                UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_SETTEMP_MIN),
                UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_SETTEMP_MAX),
                UnDefType.UNDEF);
        verify(callbackMock, times(1)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_SETTEMP_STEP),
                UnDefType.UNDEF);
    }

    @Test
    public void refreshDeviceDelayNotPassedTest() {

        lenient().when(dataTransServiceMock.isAvailable()).thenReturn(true);
        lenient().when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_POWER)).thenReturn(false);
        lenient().when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_AC_OPERATIONMODE)).thenReturn(false);
        lenient().when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_SETTEMP)).thenReturn(false);
        lenient().when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_SETTEMP_MIN)).thenReturn(false);
        lenient().when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_SETTEMP_MAX)).thenReturn(false);
        lenient().when(channelsRefreshDelayMock.isDelayPassed(CHANNEL_HWT_SETTEMP_STEP)).thenReturn(false);

        handler.refreshDevice();

        verify(callbackMock, times(0)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_POWER),
                UnDefType.UNDEF);
        verify(callbackMock, times(0)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_AC_OPERATIONMODE),
                UnDefType.UNDEF);
        verify(callbackMock, times(0)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_SETTEMP),
                UnDefType.UNDEF);
        verify(callbackMock, times(0)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_SETTEMP_MIN),
                UnDefType.UNDEF);
        verify(callbackMock, times(0)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_SETTEMP_MAX),
                UnDefType.UNDEF);
        verify(callbackMock, times(0)).stateUpdated(new ChannelUID(thingMock.getUID(), CHANNEL_HWT_SETTEMP_STEP),
                UnDefType.UNDEF);
    }

    @Test
    public void handleCommandTest() {
        ArgumentCaptor<State> stateCaptor = ArgumentCaptor.forClass(State.class);

        handler.handleCommand(new ChannelUID(new ThingUID("1:2:3"), CHANNEL_HWT_POWER),  OnOffType.ON);
        verify(dataTransServiceMock).setPowerOnOff(Enums.OnOff.ON);

        handler.handleCommand(new ChannelUID(new ThingUID("1:2:3"), CHANNEL_HWT_POWERFUL_MODE),  OnOffType.ON);
        verify(dataTransServiceMock).setPowerFulModeOnOff(Enums.OnOff.ON);

        handler.handleCommand(new ChannelUID(new ThingUID("1:2:3"), CHANNEL_HWT_SETTEMP),  new QuantityType<>("25.0") );
        verify(dataTransServiceMock).setCurrentTankTemperatureSet(25.0f);

        verify(callbackMock,times(3)).statusUpdated(eq(thingMock), argThat(arg -> arg.getStatus().equals(ThingStatus.ONLINE)));
    }
}
