package com.kapstranspvtltd.kaps_partner.models;

public class VehiclesModel {
    private final String vehicleId;
    private final String vehicleName;

    public VehiclesModel(String vehicleId, String vehicleName) {
        this.vehicleId = vehicleId;
        this.vehicleName = vehicleName;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VehiclesModel that = (VehiclesModel) o;

        if (!vehicleId.equals(that.vehicleId)) return false;
        return vehicleName.equals(that.vehicleName);
    }

    @Override
    public int hashCode() {
        int result = vehicleId.hashCode();
        result = 31 * result + vehicleName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "VehiclesModel{" +
                "vehicleId='" + vehicleId + '\'' +
                ", vehicleName='" + vehicleName + '\'' +
                '}';
    }
}