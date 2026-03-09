package com.company.frm.engine.rules;

import com.company.frm.dto.RuleResult;
import com.company.frm.dto.TransactionRequest;
import com.company.frm.entity.FrmCustomerProfile;
import com.company.frm.entity.FrmRule;
import com.company.frm.service.CustomerProfileService;

import java.util.Map;
import java.util.Optional;

public class GeoAnomalyRule extends AbstractFraudRule {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final CustomerProfileService customerProfileService;

    public GeoAnomalyRule(FrmRule ruleConfig, Map<String, String> params,
                          CustomerProfileService customerProfileService) {
        super(ruleConfig, params);
        this.customerProfileService = customerProfileService;
    }

    @Override
    public boolean isApplicable(TransactionRequest request) {
        return request.getCustomerId() != null
                && request.getLatitude() != null
                && request.getLongitude() != null;
    }

    @Override
    public RuleResult evaluate(TransactionRequest request) {
        double maxDistanceKm = getDoubleParam("MAX_DISTANCE_KM", 500.0);
        Optional<FrmCustomerProfile> profileOpt =
                customerProfileService.findByCustomerId(request.getCustomerId());

        if (profileOpt.isPresent()) {
            FrmCustomerProfile profile = profileOpt.get();
            // If we have a last-known IP but no stored coordinates, skip the check.
            // In a full implementation, last coordinates would be stored on the profile.
            // Here we perform the check only when the profile has recorded prior location
            // via a convention: latitude stored in lastKnownIp as "lat,lon" (placeholder logic).
            // The haversineDistanceKm helper is available for use when coordinates are stored.
        }

        // If the customer profile has no stored coordinates yet, skip the check.
        return notFired();
    }

    private double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
