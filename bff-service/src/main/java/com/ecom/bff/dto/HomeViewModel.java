package com.ecom.bff.dto;
import java.util.List;

public record HomeViewModel(
        List<ProductSummaryDto> featuredProducts,
        List<String> categories,
        List<BannerDto> banners,
        boolean fromCache) {
    public record BannerDto(String title, String subtitle, String imageUrl, String ctaUrl) {}
}
