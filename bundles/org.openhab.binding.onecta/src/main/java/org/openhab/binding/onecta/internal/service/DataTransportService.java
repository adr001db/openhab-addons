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
package org.openhab.binding.onecta.internal.service;

import java.time.ZonedDateTime;

import org.openhab.binding.onecta.internal.api.Enums;
import org.openhab.binding.onecta.internal.api.OnectaConnectionClient;
import org.openhab.binding.onecta.internal.api.dto.units.ManagementPoint;
import org.openhab.binding.onecta.internal.api.dto.units.Unit;

import com.google.gson.JsonObject;

/**
 *
 * @author Alexander Drent - Initial contribution
 *
 */
public class DataTransportService {

    private String unitId;
    private Enums.ManagementPoint managementPointType;
    private Unit unit;
    private JsonObject rawData;
    private final OnectaConnectionClient onectaConnectionClient = new OnectaConnectionClient();

    public DataTransportService(String unitId, Enums.ManagementPoint managementPointType) {
        this.unitId = unitId;
        this.managementPointType = managementPointType;
    }

    public void refreshUnit() {
        this.unit = onectaConnectionClient.getUnit(unitId);
        this.rawData = onectaConnectionClient.getRawData(unitId);
    }

    public JsonObject getRawData() {
        return rawData;
    }

    public Boolean isAvailable() {
        return this.unit != null && getManagementPoint(this.managementPointType) != null;
    }

    public ManagementPoint getManagementPoint(Enums.ManagementPoint managementPoint) {
        return unit.findManagementPointsByType(managementPoint.getValue());
    }

    public String getEmbeddedId() {
        return getManagementPoint(this.managementPointType).getEmbeddedId();
    }

    public Enums.OperationMode getCurrentOperationMode() {
        return Enums.OperationMode
                .fromValue(getManagementPoint(this.managementPointType).getOperationMode().getValue());
    }

    public Enums.SetpointMode getSetpointMode() {
        return Enums.SetpointMode.fromValue(getManagementPoint(this.managementPointType).getSetpointMode().getValue());
    }

    public Enums.HeatupMode getHeatupMode() {
        return Enums.HeatupMode.fromValue(getManagementPoint(this.managementPointType).getHeatupMode().getValue());
    }

    public void setCurrentOperationMode(Enums.OperationMode value) {
        onectaConnectionClient.setCurrentOperationMode(unitId, managementPointType, value);
    }

    public Enums.FanSpeed getCurrentFanspeed() {
        String fanMode = getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getFanControl().getValue()
                .getOperationModes().getFanOperationMode(getCurrentOperationMode()).getFanSpeed().getCurrentMode()
                .getValue();
        if (Enums.FanSpeedMode.FIXED.getValue().equals(fanMode)) {
            Integer fanSpeed = getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getFanControl().getValue()
                    .getOperationModes().getFanOperationMode(getCurrentOperationMode()).getFanSpeed().getModes()
                    .getFixed().getValue();
            return Enums.FanSpeed.fromValue(String.format("%s_%s", fanMode, fanSpeed.toString()));
        }
        return Enums.FanSpeed.fromValue(fanMode);
    }

    public void setFanSpeed(Enums.FanSpeed value) {
        onectaConnectionClient.setFanSpeed(unitId, getEmbeddedId(), getCurrentOperationMode(), value);
    }

    public Enums.FanMovementHor getCurrentFanDirectionHor() {
        try {
            String fanMode = getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getFanControl().getValue()
                    .getOperationModes().getFanOperationMode(getCurrentOperationMode()).getFanDirection()
                    .getHorizontal().getCurrentMode().getValue();
            return Enums.FanMovementHor.fromValue(fanMode);
        } catch (Exception e) {
            return Enums.FanMovementHor.NOTAVAILABLE;
        }
    }

    public Enums.FanMovementVer getCurrentFanDirectionVer() {
        try {
            String fanMode = getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getFanControl().getValue()
                    .getOperationModes().getFanOperationMode(getCurrentOperationMode()).getFanDirection().getVertical()
                    .getCurrentMode().getValue();
            return Enums.FanMovementVer.fromValue(fanMode);
        } catch (Exception e) {
            return Enums.FanMovementVer.NOTAVAILABLE;
        }
    }

    public Enums.FanMovement getCurrentFanDirection() {
        try {
            String setting = String.format("%s_%s", getCurrentFanDirectionHor().toString(),
                    getCurrentFanDirectionVer().toString());
            switch (setting) {
                case "STOPPED_STOPPED":
                    return Enums.FanMovement.STOPPED;
                case "NOTAVAILABLE_STOPPED":
                    return Enums.FanMovement.STOPPED;
                case "SWING_STOPPED":
                    return Enums.FanMovement.HORIZONTAL;
                case "STOPPED_SWING":
                    return Enums.FanMovement.VERTICAL;
                case "NOTAVAILABLE_SWING":
                    return Enums.FanMovement.VERTICAL;
                case "SWING_SWING":
                    return Enums.FanMovement.VERTICAL_AND_HORIZONTAL;
                case "STOPPED_WINDNICE":
                    return Enums.FanMovement.VERTICAL_EXTRA;
                case "NOTAVAILABLE_WINDNICE":
                    return Enums.FanMovement.VERTICAL_EXTRA;
                case "SWING_WINDNICE":
                    return Enums.FanMovement.VERTICAL_AND_HORIZONTAL_EXTRA;
                default:
                    throw new IllegalArgumentException("Invalid fan direc" + "tion: ");
            }
        } catch (Exception e) {
            return Enums.FanMovement.UNKNOWN;
        }
    }

    public void setCurrentFanDirection(Enums.FanMovement value) {
        onectaConnectionClient.setCurrentFanDirection(unitId, getEmbeddedId(), getCurrentOperationMode(), value);
    }

    public void setCurrentFanDirectionHor(Enums.FanMovementHor value) {
        onectaConnectionClient.setCurrentFanDirectionHor(unitId, getEmbeddedId(), getCurrentOperationMode(), value);
    }

    public void setCurrentFanDirectionVer(Enums.FanMovementVer value) {
        onectaConnectionClient.setCurrentFanDirectionVer(unitId, getEmbeddedId(), getCurrentOperationMode(), value);
    }

    public String getPowerOnOff() {
        try {
            return getManagementPoint(managementPointType).getOnOffMode().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getPowerFulModeOnOff() {
        try {
            return getManagementPoint(this.managementPointType).getPowerfulMode().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public void setPowerOnOff(Enums.OnOff value) {
        onectaConnectionClient.setPowerOnOff(unitId, managementPointType, value);
    }

    public void setPowerFulModeOnOff(Enums.OnOff value) {
        onectaConnectionClient.setPowerFulModeOnOff(unitId, managementPointType, value);
    }

    public String getEconoMode() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getEconoMode().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public void setEconoMode(Enums.OnOff value) {
        onectaConnectionClient.setEconoMode(unitId, managementPointType, value);
    }

    public String getUnitName() {
        try {
            return getManagementPoint(managementPointType).getNameValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getCurrentTemperatureSet() {
        try {
            return getManagementPoint(this.managementPointType).getTemperatureControl().getValue().getOperationModes()
                    .getOperationMode(getCurrentOperationMode()).getSetpoints().getRoomTemperature().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public void setCurrentTemperatureSet(float value) {
        if (value <= getCurrentTemperatureSetMax().floatValue())
            onectaConnectionClient.setCurrentTemperatureRoomSet(unitId, getEmbeddedId(), getCurrentOperationMode(),
                    value);
    }

    public Number getCurrentTemperatureSetMin() {
        try {
            return getManagementPoint(this.managementPointType).getTemperatureControl().getValue().getOperationModes()
                    .getOperationMode(getCurrentOperationMode()).getSetpoints().getRoomTemperature().getMinValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getCurrentTemperatureSetMax() {
        try {
            return getManagementPoint(this.managementPointType).getTemperatureControl().getValue().getOperationModes()
                    .getOperationMode(getCurrentOperationMode()).getSetpoints().getRoomTemperature().getMaxValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getCurrentTemperatureSetStep() {
        try {
            return getManagementPoint(this.managementPointType).getTemperatureControl().getValue().getOperationModes()
                    .getOperationMode(getCurrentOperationMode()).getSetpoints().getRoomTemperature().getStepValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getCurrentTankTemperatureSet() {
        try {
            return getManagementPoint(this.managementPointType).getTemperatureControl().getValue().getOperationModes()
                    .getOperationMode(getCurrentOperationMode()).getSetpoints().getdomesticHotWaterTemperature()
                    .getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public void setCurrentTankTemperatureSet(float value) {
        if (value <= getCurrentTankTemperatureSetMax().floatValue())
            onectaConnectionClient.setCurrentTemperatureHotWaterSet(unitId, getEmbeddedId(), getCurrentOperationMode(),
                    value);
    }

    public Number getCurrentTankTemperatureSetMin() {
        try {
            return getManagementPoint(this.managementPointType).getTemperatureControl().getValue().getOperationModes()
                    .getOperationMode(getCurrentOperationMode()).getSetpoints().getdomesticHotWaterTemperature()
                    .getMinValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getCurrentTankTemperatureSetMax() {
        try {
            return getManagementPoint(this.managementPointType).getTemperatureControl().getValue().getOperationModes()
                    .getOperationMode(getCurrentOperationMode()).getSetpoints().getdomesticHotWaterTemperature()
                    .getMaxValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getCurrentTankTemperatureSetStep() {
        try {
            return getManagementPoint(this.managementPointType).getTemperatureControl().getValue().getOperationModes()
                    .getOperationMode(getCurrentOperationMode()).getSetpoints().getdomesticHotWaterTemperature()
                    .getStepValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getSetpointLeavingWaterTemperature() {
        try {
            return getManagementPoint(this.managementPointType).getTemperatureControl().getValue().getOperationModes()
                    .getOperationMode(getCurrentOperationMode()).getSetpoints().getLeavingWaterTemperature().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public void setSetpointLeavingWaterTemperature(float value) {
        onectaConnectionClient.setSetpointLeavingWaterTemperature(unitId, getEmbeddedId(), getCurrentOperationMode(),
                value);
    }

    public Number getSetpointLeavingWaterOffset() {
        try {
            return getManagementPoint(this.managementPointType).getTemperatureControl().getValue().getOperationModes()
                    .getOperationMode(getCurrentOperationMode()).getSetpoints().getLeavingWaterOffset().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public void setSetpointLeavingWaterOffset(float value) {
        onectaConnectionClient.setSetpointLeavingWaterOffset(unitId, getEmbeddedId(), getCurrentOperationMode(), value);
    }

    public Number getIndoorTemperature() {
        try {
            return getManagementPoint(this.managementPointType).getSensoryData().getValue().getRoomTemperature()
                    .getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getLeavingWaterTemperature() {
        try {
            return getManagementPoint(this.managementPointType).getSensoryData().getValue().getLeavingWaterTemperature()
                    .getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getTankTemperature() {
        try {
            return getManagementPoint(this.managementPointType).getSensoryData().getValue().getTankTemperature()
                    .getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getIndoorHumidity() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getSensoryData().getValue()
                    .getRoomHumidity().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public ZonedDateTime getTimeStamp() {
        try {
            return ZonedDateTime.parse(unit.getTimestamp());
        } catch (Exception e) {
            return null;
        }
    }

    public Number getOutdoorTemperature() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getSensoryData().getValue()
                    .getOutdoorTemperature().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getTargetTemperatur() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getTargetTemperature().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public void setTargetTemperatur(float value) {
        onectaConnectionClient.setTargetTemperatur(unitId, getEmbeddedId(), value);
    }

    public Number getTargetTemperaturMax() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getTargetTemperature().getMaxValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getTargetTemperaturMin() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getTargetTemperature().getMinValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getTargetTemperaturStep() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getTargetTemperature().getStepValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getStreamerMode() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getStreamerMode().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public void setStreamerMode(Enums.OnOff value) {
        onectaConnectionClient.setStreamerMode(unitId, getEmbeddedId(), value);
    }

    public String getHolidayMode() {
        try {
            return getManagementPoint(this.managementPointType).getHolidayMode().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean getIsHolidayModeActive() {
        try {
            return getManagementPoint(this.managementPointType).getisHolidayModeActive().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean getIsPowerfulModeActive() {
        try {
            return getManagementPoint(this.managementPointType).getIsPowerfulModeActive().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public void setHolidayMode(Enums.OnOff value) {
        onectaConnectionClient.setHolidayMode(unitId, getEmbeddedId(), value);
    }

    public Enums.DemandControl getDemandControl() {
        try {
            return Enums.DemandControl.fromValue(getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL)
                    .getDemandControl().getValue().getCurrentMode().getValue());
        } catch (Exception e) {
            return null;
        }
    }

    public void setDemandControl(Enums.DemandControl value) {
        onectaConnectionClient.setDemandControl(unitId, getEmbeddedId(), value);
    }

    public Integer getDemandControlFixedValue() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getDemandControl().getValue().getModes()
                    .getFixedValues().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Integer getDemandControlFixedStepValue() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getDemandControl().getValue().getModes()
                    .getFixedValues().getStepValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Integer getDemandControlFixedMinValue() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getDemandControl().getValue().getModes()
                    .getFixedValues().getMinValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Integer getDemandControlFixedMaxValue() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getDemandControl().getValue().getModes()
                    .getFixedValues().getMaxValue();
        } catch (Exception e) {
            return null;
        }
    }

    public void setDemandControlFixedValue(Integer value) {
        onectaConnectionClient.setDemandControlFixedValue(unitId, getEmbeddedId(), value);
    }

    public Float[] getConsumptionCoolingDay() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getConsumptionData().getValue()
                    .getElectrical().getCooling().getDay();
        } catch (Exception e) {
            return null;
        }
    }

    public Float[] getConsumptionCoolingWeek() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getConsumptionData().getValue()
                    .getElectrical().getCooling().getWeek();
        } catch (Exception e) {
            return null;
        }
    }

    public Float[] getConsumptionCoolingMonth() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getConsumptionData().getValue()
                    .getElectrical().getCooling().getMonth();
        } catch (Exception e) {
            return null;
        }
    }

    public Float[] getConsumptionHeatingDay() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getConsumptionData().getValue()
                    .getElectrical().getHeating().getDay();
        } catch (Exception e) {
            return null;
        }
    }

    public Float[] getConsumptionHeatingWeek() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getConsumptionData().getValue()
                    .getElectrical().getHeating().getWeek();
        } catch (Exception e) {
            return null;
        }
    }

    public Float[] getConsumptionHeatingMonth() {
        try {
            return getManagementPoint(Enums.ManagementPoint.CLIMATECONTROL).getConsumptionData().getValue()
                    .getElectrical().getHeating().getMonth();
        } catch (Exception e) {
            return null;
        }
    }

    /* GateWay data */

    public Boolean getDaylightSavingTimeEnabled() {
        try {
            return getManagementPoint(this.managementPointType).getDaylightSavingTimeEnabled().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getFirmwareVerion() {
        try {
            return getManagementPoint(this.managementPointType).getFirmwareVersion().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean getIsFirmwareUpdateSupported() {
        try {
            return getManagementPoint(this.managementPointType).getIsFirmwareUpdateSupported().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean getIsInErrorState() {
        try {
            return getManagementPoint(this.managementPointType).getIsInErrorState().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getErrorCode() {
        try {
            return getManagementPoint(this.managementPointType).getErrorCode().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean getIsInEmergencyState() {
        try {
            return getManagementPoint(this.managementPointType).getIsInEmergencyState().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean getIsInInstallerState() {
        try {
            return getManagementPoint(this.managementPointType).getIsInInstallerState().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean getIsInWarningState() {
        try {
            return getManagementPoint(this.managementPointType).getIsInWarningState().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean getIsLedEnabled() {
        try {
            return getManagementPoint(this.managementPointType).getIsLedEnabled().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getRegionCode() {
        try {
            return getManagementPoint(this.managementPointType).getRegionCode().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getSerialNumber() {
        try {
            return getManagementPoint(this.managementPointType).getSerialNumber().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getSsid() {
        try {
            return getManagementPoint(this.managementPointType).getSsid().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getTimeZone() {
        try {
            return getManagementPoint(this.managementPointType).getTimeZone().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getWifiConectionSSid() {
        try {
            return getManagementPoint(this.managementPointType).getWifiConnectionSSID().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Integer getWifiConectionStrength() {
        try {
            return getManagementPoint(this.managementPointType).getWifiConnectionStrength().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getModelInfo() {
        try {
            return getManagementPoint(this.managementPointType).getModelInfo().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getSoftwareVersion() {
        try {
            return getManagementPoint(this.managementPointType).getSoftwareVersion().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getEepromVerion() {
        try {
            return getManagementPoint(this.managementPointType).getEepromVersion().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getDryKeepSetting() {
        try {
            return getManagementPoint(this.managementPointType).getDryKeepSetting().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getFanMotorRotationSpeed() {
        try {
            return getManagementPoint(this.managementPointType).getSensoryData().getValue().getFanMotorRotationSpeed()
                    .getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getDeltaD() {
        try {
            return getManagementPoint(this.managementPointType).getSensoryData().getValue().getDeltaD().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getHeatExchangerTemperature() {
        try {
            return getManagementPoint(this.managementPointType).getSensoryData().getValue()
                    .getHeatExchangerTemperature().getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public Number getSuctionTemperature() {
        try {
            return getManagementPoint(this.managementPointType).getSensoryData().getValue().getSuctionTemperature()
                    .getValue();
        } catch (Exception e) {
            return null;
        }
    }
}
