package com.capstone.productcatalog.clients;


import com.capstone.productcatalog.dtos.FakeStoreProductDto;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class FakeStoreApiClient {
    private final RestTemplate restTemplate;

    public  FakeStoreApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public <T> ResponseEntity<T> putForEntity(String url, @Nullable Object request, Class<T> responseType, @Nullable Object... uriVariables) throws RestClientException {
        RequestCallback requestCallback = restTemplate.httpEntityCallback(request, responseType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = restTemplate.responseEntityExtractor(responseType);
        return restTemplate.execute(url, HttpMethod.PUT, requestCallback, responseExtractor, uriVariables);
    }

    public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables) throws RestClientException {
        RequestCallback requestCallback = restTemplate.acceptHeaderRequestCallback(responseType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = restTemplate.responseEntityExtractor(responseType);
        return restTemplate.execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
    }

    public Boolean validateResponse(ResponseEntity<FakeStoreProductDto> fakeStoreProductDtoResponseEntity) {
        return fakeStoreProductDtoResponseEntity.hasBody() &&
                fakeStoreProductDtoResponseEntity.getStatusCode().
                        equals(HttpStatusCode.valueOf(200));
    }

    public Boolean validateListResponse(ResponseEntity<FakeStoreProductDto[]> fakeStoreProductDtoResponseEntity) {
        return fakeStoreProductDtoResponseEntity.hasBody() &&
                fakeStoreProductDtoResponseEntity.getStatusCode().
                        equals(HttpStatusCode.valueOf(200));
    }

}
