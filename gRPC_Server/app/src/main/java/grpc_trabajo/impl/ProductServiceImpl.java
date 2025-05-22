package grpc_trabajo.impl;
import com.example.proto.services.*;

import io.grpc.stub.StreamObserver;

public class ProductServiceImpl extends ProductServiceGrpc.ProductServiceImplBase {
    @Override
    public void getProduct(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        // Simulate a product lookup
        String productName = request.getName();
        String productdescription = "Product " + productName;
        double productPrice = 19.99;

        // Create the response
        ProductResponse response = ProductResponse.newBuilder()
                .setName(productName)
                .setDescription("Descripcion de ejemplo " + productdescription)
                .setPrice(productPrice)
                .build();

        // Send the response
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
}
