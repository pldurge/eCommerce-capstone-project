package com.capstone.productcatalog.services;


import com.capstone.productcatalog.clients.FakeStoreApiClient;
import com.capstone.productcatalog.dtos.FakeStoreProductDto;
import com.capstone.productcatalog.models.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FakeStoreProductService implements IProductService{

    private final FakeStoreApiClient fakeStoreApiClient;
    public FakeStoreProductService(FakeStoreApiClient fakeStoreApiClient) {
        this.fakeStoreApiClient = fakeStoreApiClient;
    }


    @Override
    public Product getProductById(Long id) {
        ResponseEntity<FakeStoreProductDto> responseEntity = fakeStoreApiClient.getForEntity(
                "https://fakestoreapi.com/products/{id}",
                FakeStoreProductDto.class,
                id
        );

        if(fakeStoreApiClient.validateResponse(responseEntity)) {
            return responseEntity.getBody().convertToProduct();
        }
        return null;
    }

    @Override
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        ResponseEntity<FakeStoreProductDto[]> fakeStoreProductDtos = fakeStoreApiClient.getForEntity(
                "https://fakestoreapi.com/products",
                FakeStoreProductDto[].class
        );
        if(fakeStoreApiClient.validateListResponse(fakeStoreProductDtos)){
            FakeStoreProductDto[] dtos = fakeStoreProductDtos.getBody();
            for(FakeStoreProductDto dto : dtos){
                products.add(dto.convertToProduct());
            }
            return products;
        }
        return null;
    }

    @Override
    public Product createProduct(Product input) {
        return null;
    }

    @Override
    public Product replaceProduct(Product product, Long productId) {
        FakeStoreProductDto fakestoreProductDto = product.convertToFakeStoreProduct();


        ResponseEntity<FakeStoreProductDto> response = fakeStoreApiClient.putForEntity(
                "https://fakestoreapi.com/products/{id}",
                fakestoreProductDto,
                FakeStoreProductDto.class,
                productId
        );

        if(fakeStoreApiClient.validateResponse(response)) {
            return product;
        }

        return  null;
    }
}
