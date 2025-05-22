package main.java.grpc_trabajo.impl;

public class ProductServiceImpl extends ProductServiceGrpc.ProductServiceImplBase {
    @Override
    public void getProduct(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        // Simulate a product lookup
        String productId = request.getProductId();
        String productName = "Product " + productId;
        double productPrice = 19.99;

        // Create the response
        ProductResponse response = ProductResponse.newBuilder()
                .setProductId(productId)
                .setProductName(productName)
                .setProductPrice(productPrice)
                .build();

        // Send the response
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
}
