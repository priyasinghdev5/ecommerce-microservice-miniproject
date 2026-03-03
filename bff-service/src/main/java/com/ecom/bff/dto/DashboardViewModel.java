package com.ecom.bff.dto;
import java.util.List;

public record DashboardViewModel(
        UserProfileDto profile,
        List<OrderSummaryDto> recentOrders,
        List<ProductSummaryDto> recommendations,
        boolean profileLoaded,
        boolean ordersLoaded,
        boolean recommendationsLoaded) {

    public static DashboardViewModel partial(UserProfileDto p, List<OrderSummaryDto> o,
            List<ProductSummaryDto> r, boolean pOk, boolean oOk, boolean rOk) {
        return new DashboardViewModel(p, o, r, pOk, oOk, rOk);
    }
}
