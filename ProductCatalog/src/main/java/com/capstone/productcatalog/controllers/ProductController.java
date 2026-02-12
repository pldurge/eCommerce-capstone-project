package com.capstone.productcatalog.controllers;


import com.capstone.productcatalog.dtos.ProductDto;
import com.capstone.productcatalog.models.Product;
import com.capstone.productcatalog.services.IProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ProductController {

    private final IProductService productService;

    public ProductController(IProductService productService){
        this.productService = productService;
    }

    /*
    1. Create product
    2. Get product by id
    3. Get all products
     */


    /*
    Create product ("/products"), POST
    Get product by id ("/products/{id}"), GET
    Get all products ("/products"), GET
    @RequestMapping("/products")
    @GetMapping
    @PostMapping
     */

    @PutMapping("/products/{productId}")
    ProductDto updateProduct(@PathVariable("productId") Long productId,
                             @RequestBody ProductDto productDTO){

        /*
        productDto to product
        pass productDTO and get the product back
         */
        ProductDto productReponseDTO = new ProductDto();
        /*
        call the service layer to update the product
         */

        Product product = productService.replaceProduct(productDTO.convertToProduct(), productId);

        if(product != null){
            return product.convertToProductDto();
        }
        return null;
    }

    /*
    Test in isolation
    a() {
        b(); //we need mock of b
    }
     */


    @PostMapping("/products")
    ProductDto createProduct(@RequestBody ProductDto product){

        ProductDto productReponseDTO = new ProductDto();
        /*
        call the service layer to save the product
         */

        //productService.createProduct(product);

        Product product1 = productService.createProduct(product.convertToProduct());
        if(product1 != null){
            return product1.convertToProductDto();
        }
        return productReponseDTO;
    }
    /*
    "name": "iphone",
    "descr": "apple",

     */
    @GetMapping("/products/{id}")
    ResponseEntity<ProductDto> getProductById(@PathVariable Long id){

        if (id < 0) {
            throw new IllegalArgumentException("Product Id not found");
        } else if(id == 0) {
            throw new IllegalArgumentException("Products exist with positive id");
        }

        //RestTemplate
        /*
        call the service layer to get the product by id
         */

//        if(id < 1){
//            throw new IllegalArgumentException("Invalid Product ID(zero or negative)");
//        }



        Product product = productService.getProductById(id);


        if(product == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }



        /*
        product
        to productDTO

        obj.from(obj) -> newObj
         */

        ProductDto productDTO = product.convertToProductDto();

        return new ResponseEntity<>(productDTO,HttpStatus.OK);
    }

    @GetMapping("/products")
    List<ProductDto> getAllProducts(){
        List<ProductDto> productDtos = new ArrayList<>();
        /*
        call the service layer to get all products
         */

        List<Product> products = productService.getAllProducts();

        if(products != null){
            for(Product product : products){
                productDtos.add(product.convertToProductDto());
            }
        }

        return productDtos;
    }



}
/*
path variable /id/
request body { "" : ""
Query params ?category=electronics


2 ways to solve ambiguity
1. Primary implementation
2. Qualifer
 */
/*
Primary
Qualifer
 */