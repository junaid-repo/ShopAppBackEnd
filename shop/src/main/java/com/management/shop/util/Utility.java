package com.management.shop.util;

import com.management.shop.dto.InvoiceData;
import com.management.shop.dto.InvoiceDetails;
import com.management.shop.dto.ShopDetails;
import com.management.shop.dto.UpdateUserDTO;
import com.management.shop.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Utility {


    private ShopService shopService;

    Utility(ShopService shopService) {
       this.shopService= shopService;
    }

    public  InvoiceData getShopDetails(String username, String orderId) {
        UpdateUserDTO userProfile= shopService.getUserProfile(username);


        System.out.println(orderId);
        InvoiceDetails order = shopService.getOrderDetails(orderId);
        LocalDate orderedDate = LocalDate.parse(order.getOrderedDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String shopEmail="";
        String gstNumber="";
        String shopAddress="";
        String shopPhone="";
        String shopName="";
        if(userProfile!=null){
            gstNumber = userProfile.getGstNumber() != null ? userProfile.getGstNumber() : "sample gst number";
            shopEmail=userProfile.getShopEmail()!=null?userProfile.getShopEmail() : "sample shop email";
            shopPhone= userProfile.getShopPhone() != null?userProfile.getShopPhone() : "sample shop phone";
            shopAddress= userProfile.getShopLocation()!=null?userProfile.getShopLocation() : "sample shop address";
            shopName=userProfile.getShopName()!=null?userProfile.getShopName() : "sample shop name";
        }

// Format to new pattern
        String formattedDate = orderedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));




        return null;

    }


}
