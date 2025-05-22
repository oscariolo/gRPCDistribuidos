package grpc_trabajo.impl;
import com.example.proto.services.*;

import io.grpc.stub.StreamObserver;

public class ProductServiceImpl extends ProductServiceGrpc.ProductServiceImplBase {
    @Override
    public void getProduct(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {

       

        // Simulate a product lookup
        String productName = request.getName();
        String productdescription = "";
        double productPrice = 19.99;

         if(productName.equals("Cebolla")){
            productdescription = "Las cebollas son como los ogros, tienen capas";
            productPrice = 1.99;
         }else{
            productdescription = "No se ha encontrado el producto pedido" + productName;
            productPrice = 0.00;
         }

        // Create the response
        ProductResponse response = ProductResponse.newBuilder()
                .setName(productName)
                .setDescription(productdescription)
                .setPrice(productPrice)
                .build();

        // Send the response
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
}
