package com.modusbox.client.jaxrs;

import com.modusbox.client.api.HealthApi;

import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;

//@CrossOriginResourceSharing(
//        allowAllOrigins = true,
////        allowOrigins = {
////                "http://area51.mil:31415"
////        },
//        allowCredentials = true,
////        maxAge = 1,
//        allowHeaders = {
//                "*"
//        }
////        exposeHeaders = {
////                "X-custom-3", "X-custom-4"
////        }
//)
public class HealthApiImpl implements HealthApi {

    @Override
    public String getHealth() {
        return null;
    }
}
