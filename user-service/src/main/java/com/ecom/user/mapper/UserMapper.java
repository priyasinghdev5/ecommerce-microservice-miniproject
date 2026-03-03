package com.ecom.user.mapper;

import com.ecom.user.dto.*;
import com.ecom.user.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "fullName", expression = "java(profile.getFullName())")
    UserProfileResponse toResponse(UserProfile profile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authUserId", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "preferences", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toEntity(UserProfileRequest request);

    AddressResponse toResponse(Address address);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Address toEntity(AddressRequest request);
}
