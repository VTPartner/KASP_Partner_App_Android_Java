package com.kapstranspvtltd.kaps_partner.network;


/*public class APIClient {


    public static final String RAZORPAY_ID = "rzp_test_61op4YoSkMBW6u"; //Test key rzp_test_crEnVFpHxMh7sZ;
//    public static String baseUrl = "http://77.37.47.156:8000/api/vt_partner/";

//    public static String baseUrl = "https://www.vtpartner.org/api/vt_partner/";
//    public static String baseImageUrl = "http://77.37.47.156:8000/api/vt_partner";
//    public static String baseImageUrl = "https://www.vtpartner.org/api/vt_partner";
    int devMode = 0; // If Dev Mode is 1 then development server is on
    if(devMode == 1){
        public static String baseUrl = "http://100.24.44.74:8000/api/vt_partner/";
        public static String baseImageUrl = "http://100.24.44.74:8000/api/vt_partner";
    }else{
        public static String baseUrl = "http://100.24.44.74/api/vt_partner/";
        public static String baseImageUrl = "http://100.24.44.74/api/vt_partner";
    }

}*/

/**
 * Here all endpoint and constant values are defined
 */

public class APIClient {
    public static final String RAZORPAY_ID = "rzp_live_2A74eAF3LgxMVM"; //Test key rzp_test_crEnVFpHxMBW6u //Live Key rzp_live_2A74eAF3LgxMVM

    private static final int DEV_MODE = 0; // If Dev Mode is 1 then development server is on and if 0 then production server is on

    public static final String baseUrl = DEV_MODE == 1
            ? "http://100.24.44.74:8000/api/vt_partner/"
            : "https://www.kaps9.in/api/vt_partner/";

    public static final String baseImageUrl = DEV_MODE == 1
            ? "http://100.24.44.74:8000/api/vt_partner"
            : "https://www.kaps9.in/api/vt_partner";
}
