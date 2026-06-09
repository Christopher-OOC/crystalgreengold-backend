package com.topnivo.backend.mapper;

import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.InsufficientProductQuantity;
import com.topnivo.backend.model.entity.Package;
import com.topnivo.backend.model.entity.Product;
import com.topnivo.backend.model.request.PackageCreateRequest;
import com.topnivo.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PackageMapper {

    private final ProductService productService;
    private final ModelMapper modelMapper;

    public Package requestToPackage(PackageCreateRequest request) {
        Package _package = modelMapper.map(request, Package.class);

        return _package;
    }

}
