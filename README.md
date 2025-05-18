# Kaps Agent App

## Overview
Kaps Agent App is a comprehensive mobile application for managing various types of service providers including cab drivers, goods drivers, handyman agents, and JCB/crane operators. The app facilitates real-time location tracking, booking management, and service delivery.

## App Flow

### 1. Authentication Flow
- User registration and login
- OTP verification system
- Document verification process for different service types

### 2. Service Provider Types
- Cab Drivers
- Goods Drivers
- Handyman Agents
- JCB/Crane Operators

### 3. Main Features
- Real-time location tracking
- Booking acceptance and management
- Payment processing (Razorpay integration)
- Multiple drop location support
- Rating system
- Recharge history
- Settings management

### 4. Background Services
- Location update service
- Booking notification service
- Floating window service for new bookings

## API Integration

### Base URLs
- Production: `https://www.kaps9.in/api/vt_partner/`
- Development: `http://100.24.44.74:8000/api/vt_partner/`

### Authentication APIs
- `send_otp`: Send OTP for verification
- `verify_otp`: Verify OTP for authentication

### Service Provider APIs
- `get_all_sub_services`: Fetch available services
- `update_goods_drivers_current_location`: Update driver location
- Various booking management endpoints for different service types

### Payment Integration
- Razorpay integration for payment processing
- Test Key: `rzp_test_61op4YoSkMBW6u`

## Technical Stack

### Dependencies
- AndroidX Lifecycle Components
- Google Places API
- Google Maps API
- Firebase Cloud Messaging
- OneSignal for Push Notifications
- Retrofit for API calls
- Volley for Network Requests
- GSON for JSON parsing

### Key Features
1. **Location Services**
   - Real-time location tracking
   - Distance matrix calculation
   - Route navigation

2. **Booking Management**
   - Booking acceptance
   - Status updates
   - Multiple drop location support
   - OTP verification for deliveries

3. **Payment Processing**
   - Razorpay integration
   - Payment status tracking
   - Transaction history

4. **Background Services**
   - Location update service
   - Booking notification service
   - Floating window service

## Security Features
- Token-based authentication
- Secure API communication
- OTP verification
- Document verification system

## Development Setup
1. Clone the repository
2. Configure API keys in `local.properties`
3. Set up Firebase project and add `google-services.json`
4. Configure Razorpay test key
5. Build and run the application

## Environment Configuration
The app supports both development and production environments:
- Development Mode: Set `DEV_MODE = 1`
- Production Mode: Set `DEV_MODE = 0`

## API Headers
All API requests include the following headers:
```json
{
    "Content-Type": "application/json",
    "Authorization": "Bearer <token>"
}
```

## Error Handling
- Retry mechanism for failed API calls
- Default timeout: 30 seconds
- Maximum retries: 2
- Backoff multiplier for retries

## Location Services
- Uses Google's FusedLocationProviderClient
- Location updates every 15 seconds
- Background location tracking
- Geofencing support

## Push Notifications
- Firebase Cloud Messaging integration
- OneSignal for enhanced push notification support
- Custom notification channels for different service types

## Contributing
Please read the contribution guidelines before submitting pull requests.

## License
[Add your license information here]

## API Documentation

### Authentication APIs
- `send_otp`: Send OTP for verification
  - Method: POST
  - Parameters: `mobile_no`
- `verify_otp`: Verify OTP for authentication
  - Method: POST
  - Parameters: `mobile_no`, `otp`

### Service Provider Registration APIs
- `cab_driver_registration`: Register cab driver
  - Method: POST
  - Parameters: Driver details, vehicle info, location
- `jcb_crane_driver_registration`: Register JCB/crane operator
  - Method: POST
  - Parameters: Driver details, vehicle info, location
- `get_all_sub_services`: Get available services
  - Method: POST
  - Parameters: Service type

### Booking Management APIs
- `booking_details_live_track`: Get live booking details
  - Method: POST
  - Parameters: `booking_id`
- `jcb_crane_driver_booking_details_live_track`: Get JCB/crane booking details
  - Method: POST
  - Parameters: `booking_id`
- `get_handyman_current_booking_detail`: Get handyman booking details
  - Method: POST
  - Parameters: `handyman_id`
- `get_other_driver_current_booking_detail`: Get other driver booking details
  - Method: POST
  - Parameters: `other_driver_id`

### Location Tracking APIs
- `goods_driver_online_status`: Update goods driver status
  - Method: POST
  - Parameters: `goods_driver_id`
- `jcb_crane_driver_update_online_status`: Update JCB/crane driver status
  - Method: POST
  - Parameters: `jcb_crane_driver_id`, `status`, `lat`, `lng`, `recent_online_pic`
- `add_new_active_jcb_crane_driver`: Add JCB/crane driver to active list
  - Method: POST
  - Parameters: `jcb_crane_driver_id`, `status`, `current_lat`, `current_lng`

### Recharge and Payment APIs
- `get_cab_driver_new_recharge_plan_history_list`: Get cab driver recharge history
  - Method: POST
  - Parameters: `driver_id`
- `get_jcb_crane_driver_new_recharge_plan_history_list`: Get JCB/crane driver recharge history
  - Method: POST
  - Parameters: `driver_id`
- `get_handyman_new_recharge_plan_history_list`: Get handyman recharge history
  - Method: POST
  - Parameters: `driver_id`
- `other_driver_current_new_recharge_details`: Get other driver recharge details
  - Method: POST
  - Parameters: `driver_id`

### FAQ and Support APIs
- `get_faqs_by_category`: Get FAQs by category
  - Method: POST
  - Parameters: `category_id`
  - Categories:
    - 2: Cab Driver FAQs
    - 4: Other Driver FAQs

### Common API Headers
All API requests include:
```json
{
    "Content-Type": "application/json",
    "Authorization": "Bearer <token>"  // When authentication is required
}
```

### API Response Format
Standard response format:
```json
{
    "message": "Success/Error message",
    "results": [], // Array of results when applicable
    "status": "success/error"
}
```

### Error Handling
- Default timeout: 30 seconds
- Maximum retries: 2
- Backoff multiplier for retries
- Standard error response format:
```json
{
    "message": "Error message",
    "status": "error"
}
```

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/kapstranspvtltd/kaps_partner/
│   │   │   ├── adapters/                 # Custom adapters for RecyclerViews
│   │   │   ├── cab_driver_activities/    # Cab driver related activities
│   │   │   │   ├── documents/
│   │   │   │   │   └── main_documents/
│   │   │   │   └── settings_pages/
│   │   │   ├── common_activities/        # Shared activities
│   │   │   ├── driver_app_activities/    # Other driver activities
│   │   │   │   ├── driver_agent_main_documents/
│   │   │   │   └── settings_pages/
│   │   │   ├── fcm/                      # Firebase Cloud Messaging
│   │   │   ├── goods_driver_activities/  # Goods driver activities
│   │   │   │   ├── fragments/
│   │   │   │   └── settings_pages/
│   │   │   ├── handyman_agent_activities/# Handyman activities
│   │   │   │   ├── documents/
│   │   │   │   │   └── handyman_main_documents/
│   │   │   │   └── settings_pages/
│   │   │   ├── jcb_crane_agent_activities/# JCB/Crane activities
│   │   │   │   ├── jcb_crane_main_documents/
│   │   │   │   └── settings_pages/
│   │   │   ├── models/                   # Data models
│   │   │   ├── network/                  # Network related classes
│   │   │   ├── services/                 # Background services
│   │   │   ├── utils/                    # Utility classes
│   │   │   ├── BackgroundService.java
│   │   │   ├── BootReceiver.java
│   │   │   ├── GoodsNewBookingFloatingWindowService.java
│   │   │   ├── MainActivity.java
│   │   │   ├── MyApplication.java
│   │   │   └── SplashScreenActivity.java
│   │   ├── res/                          # Resources directory
│   │   └── AndroidManifest.xml
│   ├── androidTest/                      # Android instrumentation tests
│   └── test/                             # Unit tests
├── build.gradle.kts                      # App level build configuration
├── google-services.json                   # Firebase configuration
├── proguard-rules.pro                    # ProGuard rules
└── libs/                                 # External libraries
```

## API Documentation by Module

### 1. Authentication Module
Location: `common_activities/`
- `send_otp`: Send OTP for verification
- `verify_otp`: Verify OTP for authentication

### 2. Cab Driver Module
Location: `cab_driver_activities/`
- `cab_driver_registration`: Register cab driver
- `get_cab_driver_new_recharge_plan_history_list`: Get recharge history
- `get_faqs_by_category`: Get cab driver FAQs (category_id: 2)

### 3. Goods Driver Module
Location: `goods_driver_activities/`
- `goods_driver_online_status`: Update driver status
- `booking_details_live_track`: Get live booking details
- `update_goods_drivers_current_location`: Update location

### 4. Handyman Module
Location: `handyman_agent_activities/`
- `get_handyman_current_booking_detail`: Get booking details
- `get_handyman_new_recharge_plan_history_list`: Get recharge history

### 5. JCB/Crane Module
Location: `jcb_crane_agent_activities/`
- `jcb_crane_driver_registration`: Register operator
- `jcb_crane_driver_update_online_status`: Update status
- `jcb_crane_driver_booking_details_live_track`: Get booking details
- `get_jcb_crane_driver_new_recharge_plan_history_list`: Get recharge history
- `add_new_active_jcb_crane_driver`: Add to active drivers

### 6. Other Driver Module
Location: `driver_app_activities/`
- `get_other_driver_current_booking_detail`: Get booking details
- `other_driver_current_new_recharge_details`: Get recharge details
- `get_other_driver_new_recharge_plan_history_list`: Get recharge history

### 7. Common Services
Location: `services/`
- Location tracking services
- Background services
- Floating window services

### 8. Network Layer
Location: `network/`
- `APIClient`: Base URL and constants
- `APIHelper`: API utility methods
- `VolleySingleton`: Network request queue

### 9. Firebase Integration
Location: `fcm/`
- Firebase Cloud Messaging setup
- Token management
- Push notification handling

## Key Files and Their Purposes

1. **MyApplication.java**
   - Application initialization
   - Firebase setup
   - Token management

2. **BackgroundService.java**
   - Background location tracking
   - Status updates
   - Connection management

3. **GoodsNewBookingFloatingWindowService.java**
   - New booking notifications
   - Floating window management
   - Booking acceptance flow

4. **SplashScreenActivity.java**
   - Initial app loading
   - Authentication check
   - Service initialization

## Complete API List by Service Type

### 1. Cab Driver APIs

#### Authentication & Registration
- `cab_driver_login`: Login for cab drivers
- `cab_driver_registration`: Register new cab driver
- `update_firebase_cab_driver_token`: Update FCM token

#### Location & Status
- `update_cab_drivers_current_location`: Update driver location
- `cab_driver_online_status`: Update online status
- `add_new_active_cab_driver`: Add to active drivers
- `delete_active_cab_driver`: Remove from active drivers

#### Booking Management
- `cab_booking_details_for_ride_acceptance`: Get booking details
- `cab_driver_booking_accepted`: Accept booking
- `cab_booking_details_live_track`: Track live booking
- `get_cab_driver_current_booking_detail`: Get current booking
- `update_booking_status_cab_driver`: Update booking status

#### Profile & Settings
- `get_cab_driver_details`: Get driver profile
- `update_cab_driver_details`: Update driver profile

#### Earnings & Orders
- `cab_driver_todays_earnings`: Get today's earnings
- `cab_driver_all_orders`: Get all orders

#### Recharge & Payments
- `cab_driver_current_new_recharge_details`: Get recharge details
- `get_cab_driver_new_recharge_plans_list`: Get recharge plans
- `new_cab_driver_new_recharge_plan`: Create recharge plan
- `get_cab_driver_new_recharge_plan_history_list`: Get recharge history
- `generate_order_id_for_booking_id_cab_driver`: Generate order ID

### 2. JCB/Crane Operator APIs

#### Authentication & Registration
- `jcb_crane_driver_login`: Login for JCB/crane operators
- `jcb_crane_driver_registration`: Register new operator
- `update_firebase_jcb_crane_driver_token`: Update FCM token

#### Location & Status
- `update_jcb_crane_drivers_current_location`: Update location
- `jcb_crane_driver_online_status`: Update online status
- `add_new_active_jcb_crane_driver`: Add to active operators
- `delete_active_jcb_crane_driver`: Remove from active operators

#### Booking Management
- `jcb_crane_booking_details_for_ride_acceptance`: Get booking details
- `jcb_crane_driver_booking_accepted`: Accept booking
- `jcb_crane_driver_booking_details_live_track`: Track live booking
- `get_jcb_crane_driver_current_booking_detail`: Get current booking
- `update_booking_status_jcb_crane_driver`: Update booking status

#### Profile & Settings
- `get_jcb_crane_driver_details`: Get operator profile
- `update_jcb_crane_driver_details`: Update operator profile

#### Earnings & Orders
- `jcb_crane_driver_todays_earnings`: Get today's earnings
- `jcb_crane_driver_whole_year_earnings`: Get yearly earnings
- `jcb_crane_driver_all_orders`: Get all orders

#### Recharge & Payments
- `jcb_crane_current_new_recharge_details`: Get recharge details
- `jcb_crane_driver_new_recharge_plan`: Create recharge plan
- `get_jcb_crane_driver_new_recharge_plan_history_list`: Get recharge history
- `generate_order_id_for_booking_id_jcb_crane_driver`: Generate order ID

### 3. Other Driver APIs

#### Authentication & Registration
- `other_driver_login`: Login for other drivers
- `other_driver_registration`: Register new driver
- `update_firebase_other_driver_token`: Update FCM token

#### Location & Status
- `update_other_drivers_current_location`: Update location
- `other_driver_online_status`: Update online status
- `add_new_active_other_driver`: Add to active drivers
- `delete_active_other_driver`: Remove from active drivers

#### Booking Management
- `other_driver_booking_details_for_ride_acceptance`: Get booking details
- `other_driver_booking_accepted`: Accept booking
- `other_driver_booking_details_live_track`: Track live booking
- `get_other_driver_current_booking_detail`: Get current booking
- `update_booking_status_other_driver`: Update booking status

#### Profile & Settings
- `get_other_driver_details`: Get driver profile
- `update_other_driver_details`: Update driver profile

#### Earnings & Orders
- `other_driver_todays_earnings`: Get today's earnings
- `other_driver_whole_year_earnings`: Get yearly earnings
- `other_driver_all_orders`: Get all orders

#### Recharge & Payments
- `other_driver_current_new_recharge_details`: Get recharge details
- `other_driver_new_recharge_plan`: Create recharge plan
- `get_other_driver_new_recharge_plan_history_list`: Get recharge history
- `generate_order_id_for_booking_id_other_driver`: Generate order ID

### 4. Handyman APIs

#### Authentication & Registration
- `handyman_login`: Login for handyman
- `handyman_registration`: Register new handyman
- `update_firebase_handyman_token`: Update FCM token

#### Location & Status
- `update_handymans_current_location`: Update location

#### Booking Management
- `handyman_agent_booking_details_for_ride_acceptance`: Get booking details
- `handyman_booking_accepted`: Accept booking
- `handyman_agent_booking_details_live_track`: Track live booking
- `get_handyman_current_booking_detail`: Get current booking
- `update_booking_status_handyman`: Update booking status

#### Profile & Settings
- `get_handyman_details`: Get handyman profile
- `update_handyman_details`: Update handyman profile

#### Earnings & Orders
- `handyman_whole_year_earnings`: Get yearly earnings
- `handyman_all_orders`: Get all orders

#### Recharge & Payments
- `handyman_current_new_recharge_details`: Get recharge details
- `get_handyman_new_recharge_plan_history_list`: Get recharge history
- `generate_order_id_for_booking_id_handyman`: Generate order ID

### Common API Headers
All API requests include:
```json
{
    "Content-Type": "application/json",
    "Authorization": "Bearer <token>"  // When authentication is required
}
```

### API Response Format
Standard response format:
```json
{
    "message": "Success/Error message",
    "results": [], // Array of results when applicable
    "status": "success/error"
}
```

### Error Handling
- Default timeout: 30 seconds
- Maximum retries: 2
- Backoff multiplier for retries
- Standard error response format:
```json
{
    "message": "Error message",
    "status": "error"
}
```

## Postman Collection

### 1. Cab Driver APIs

#### Authentication & Registration

##### Login
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_login
{
    "mobile_no": "+918296565588",
    "otp": 123456
}
```

##### Registration
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_registration
{
    "name": "John Doe",
    "mobile_no": "+918296565588",
    "email": "john@example.com",
    "vehicle_type": "sedan",
    "vehicle_number": "KA01AB1234",
    "vehicle_model": "Toyota Camry",
    "vehicle_color": "Black",
    "license_number": "DL123456789",
    "license_expiry": "2025-12-31",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image",
    "license_pic": "base64_encoded_image",
    "rc_book_pic": "base64_encoded_image",
    "insurance_pic": "base64_encoded_image",
    "r_lat": 12.9716,
    "r_lng": 77.5946,
    "current_lat": 12.9716,
    "current_lng": 77.5946,
    "recent_selfie_photo_url": "base64_encoded_image"
}
```

##### Update FCM Token
```json
POST https://www.kaps9.in/api/vt_partner/update_firebase_cab_driver_token
{
    "driver_id": 123,
    "fcm_token": "fcm_token_string"
}
```

#### Location & Status

##### Update Location
```json
POST https://www.kaps9.in/api/vt_partner/update_cab_drivers_current_location
{
    "driver_id": 123,
    "latitude": 12.9716,
    "longitude": 77.5946,
    "status": "online"
}
```

##### Update Online Status
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_online_status
{
    "driver_id": 123,
    "status": 1,
    "lat": 12.9716,
    "lng": 77.5946,
    "recent_online_pic": "base64_encoded_image"
}
```

##### Add to Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/add_new_active_cab_driver
{
    "driver_id": 123,
    "status": 1,
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

##### Delete from Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/delete_active_cab_driver
{
    "cab_driver_id": 123
}
```

#### Booking Management

##### Get Booking Details
```json
POST https://www.kaps9.in/api/vt_partner/cab_booking_details_for_ride_acceptance
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Accept Booking
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_booking_accepted
{
    "booking_id": 123,
    "driver_id": 123,
    "acceptance_status": "accepted"
}
```

##### Track Live Booking
```json
POST https://www.kaps9.in/api/vt_partner/cab_booking_details_live_track
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Update Booking Status
```json
POST https://www.kaps9.in/api/vt_partner/update_booking_status_cab_driver
{
    "booking_id": 123,
    "driver_id": 123,
    "status": "arrived",
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

#### Profile & Settings

##### Get Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/get_cab_driver_details
{
    "driver_id": 123
}
```

##### Update Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/update_cab_driver_details
{
    "driver_id": 123,
    "name": "John Doe",
    "email": "john@example.com",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image"
}
```

#### Earnings & Orders

##### Get Today's Earnings
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_todays_earnings
{
    "driver_id": 123,
    "date": "2024-03-20"
}
```

##### Get All Orders
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_all_orders
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

#### Recharge & Payments

##### Get Recharge Details
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_current_new_recharge_details
{
    "driver_id": 123
}
```

##### Create Recharge Plan
```json
POST https://www.kaps9.in/api/vt_partner/new_cab_driver_new_recharge_plan
{
    "driver_id": 123,
    "plan_id": 123,
    "amount": 1000,
    "payment_method": "razorpay"
}
```

##### Get Recharge History
```json
POST https://www.kaps9.in/api/vt_partner/get_cab_driver_new_recharge_plan_history_list
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

### 2. JCB/Crane Operator APIs

#### Authentication & Registration

##### Login
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_login
{
    "mobile_no": "+918296565588",
    "otp": 123456
}
```

##### Registration
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_registration
{
    "name": "John Doe",
    "mobile_no": "+918296565588",
    "email": "john@example.com",
    "vehicle_type": "jcb",
    "vehicle_number": "KA01AB1234",
    "vehicle_model": "JCB 3DX",
    "vehicle_color": "Yellow",
    "license_number": "DL123456789",
    "license_expiry": "2025-12-31",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image",
    "license_pic": "base64_encoded_image",
    "rc_book_pic": "base64_encoded_image",
    "insurance_pic": "base64_encoded_image",
    "r_lat": 12.9716,
    "r_lng": 77.5946,
    "current_lat": 12.9716,
    "current_lng": 77.5946,
    "recent_selfie_photo_url": "base64_encoded_image"
}
```

##### Update FCM Token
```json
POST https://www.kaps9.in/api/vt_partner/update_firebase_jcb_crane_driver_token
{
    "driver_id": 123,
    "fcm_token": "fcm_token_string"
}
```

#### Location & Status

##### Update Location
```json
POST https://www.kaps9.in/api/vt_partner/update_jcb_crane_drivers_current_location
{
    "driver_id": 123,
    "latitude": 12.9716,
    "longitude": 77.5946,
    "status": "online"
}
```

##### Update Online Status
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_update_online_status
{
    "jcb_crane_driver_id": 123,
    "status": 1,
    "lat": 12.9716,
    "lng": 77.5946,
    "recent_online_pic": "base64_encoded_image"
}
```

##### Add to Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/add_new_active_jcb_crane_driver
{
    "jcb_crane_driver_id": 123,
    "status": 1,
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

##### Delete from Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/delete_active_jcb_crane_driver
{
    "jcb_crane_driver_id": 123
}
```

#### Booking Management

##### Get Booking Details
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_booking_details_for_ride_acceptance
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Accept Booking
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_booking_accepted
{
    "booking_id": 123,
    "driver_id": 123,
    "acceptance_status": "accepted"
}
```

##### Track Live Booking
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_booking_details_live_track
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Update Booking Status
```json
POST https://www.kaps9.in/api/vt_partner/update_booking_status_jcb_crane_driver
{
    "booking_id": 123,
    "driver_id": 123,
    "status": "arrived",
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

#### Profile & Settings

##### Get Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/get_jcb_crane_driver_details
{
    "driver_id": 123
}
```

##### Update Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/update_jcb_crane_driver_details
{
    "driver_id": 123,
    "name": "John Doe",
    "email": "john@example.com",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image"
}
```

#### Earnings & Orders

##### Get Today's Earnings
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_todays_earnings
{
    "driver_id": 123,
    "date": "2024-03-20"
}
```

##### Get All Orders
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_all_orders
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

#### Recharge & Payments

##### Get Recharge Details
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_current_new_recharge_details
{
    "driver_id": 123
}
```

##### Create Recharge Plan
```json
POST https://www.kaps9.in/api/vt_partner/new_jcb_crane_driver_new_recharge_plan
{
    "driver_id": 123,
    "plan_id": 123,
    "amount": 1000,
    "payment_method": "razorpay"
}
```

##### Get Recharge History
```json
POST https://www.kaps9.in/api/vt_partner/get_jcb_crane_driver_new_recharge_plan_history_list
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

### 4. Other Driver APIs

#### Authentication & Registration

##### Login
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_login
{
    "mobile_no": "+918296565588",
    "otp": 123456
}
```

##### Registration
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_registration
{
    "name": "John Doe",
    "mobile_no": "+918296565588",
    "email": "john@example.com",
    "vehicle_type": "truck",
    "vehicle_number": "KA01AB1234",
    "vehicle_model": "Tata 407",
    "vehicle_color": "White",
    "license_number": "DL123456789",
    "license_expiry": "2025-12-31",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image",
    "license_pic": "base64_encoded_image",
    "rc_book_pic": "base64_encoded_image",
    "insurance_pic": "base64_encoded_image",
    "r_lat": 12.9716,
    "r_lng": 77.5946,
    "current_lat": 12.9716,
    "current_lng": 77.5946,
    "recent_selfie_photo_url": "base64_encoded_image"
}
```

##### Update FCM Token
```json
POST https://www.kaps9.in/api/vt_partner/update_firebase_other_driver_token
{
    "driver_id": 123,
    "fcm_token": "fcm_token_string"
}
```

#### Location & Status

##### Update Location
```json
POST https://www.kaps9.in/api/vt_partner/update_other_drivers_current_location
{
    "driver_id": 123,
    "latitude": 12.9716,
    "longitude": 77.5946,
    "status": "online"
}
```

##### Update Online Status
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_online_status
{
    "other_driver_id": 123,
    "status": 1,
    "lat": 12.9716,
    "lng": 77.5946,
    "recent_online_pic": "base64_encoded_image"
}
```

##### Add to Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/add_new_active_other_driver
{
    "other_driver_id": 123,
    "status": 1,
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

##### Delete from Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/delete_active_other_driver
{
    "other_driver_id": 123
}
```

#### Booking Management

##### Get Booking Details
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_booking_details_for_ride_acceptance
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Accept Booking
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_booking_accepted
{
    "booking_id": 123,
    "driver_id": 123,
    "acceptance_status": "accepted"
}
```

##### Track Live Booking
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_booking_details_live_track
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Update Booking Status
```json
POST https://www.kaps9.in/api/vt_partner/update_booking_status_other_driver
{
    "booking_id": 123,
    "driver_id": 123,
    "status": "arrived",
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

#### Profile & Settings

##### Get Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/get_other_driver_details
{
    "driver_id": 123
}
```

##### Update Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/update_other_driver_details
{
    "driver_id": 123,
    "name": "John Doe",
    "email": "john@example.com",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image"
}
```

#### Earnings & Orders

##### Get Today's Earnings
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_todays_earnings
{
    "driver_id": 123,
    "date": "2024-03-20"
}
```

##### Get All Orders
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_all_orders
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

#### Recharge & Payments

##### Get Recharge Details
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_current_new_recharge_details
{
    "driver_id": 123
}
```

##### Create Recharge Plan
```json
POST https://www.kaps9.in/api/vt_partner/new_other_driver_new_recharge_plan
{
    "driver_id": 123,
    "plan_id": 123,
    "amount": 1000,
    "payment_method": "razorpay"
}
```

##### Get Recharge History
```json
POST https://www.kaps9.in/api/vt_partner/get_other_driver_new_recharge_plan_history_list
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

### 5. Handyman APIs

#### Authentication & Registration

##### Login
```json
POST https://www.kaps9.in/api/vt_partner/handyman_login
{
    "mobile_no": "+918296565588",
    "otp": 123456
}
```

##### Registration
```json
POST https://www.kaps9.in/api/vt_partner/handyman_registration
{
    "name": "John Doe",
    "mobile_no": "+918296565588",
    "email": "john@example.com",
    "service_type": "plumber",
    "experience": "5 years",
    "skills": ["plumbing", "electrical"],
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image",
    "id_proof": "base64_encoded_image",
    "certificate": "base64_encoded_image",
    "r_lat": 12.9716,
    "r_lng": 77.5946,
    "current_lat": 12.9716,
    "current_lng": 77.5946,
    "recent_selfie_photo_url": "base64_encoded_image"
}
```

##### Update FCM Token
```json
POST https://www.kaps9.in/api/vt_partner/update_firebase_handyman_token
{
    "handyman_id": 123,
    "fcm_token": "fcm_token_string"
}
```

#### Location & Status

##### Update Location
```json
POST https://www.kaps9.in/api/vt_partner/update_handymans_current_location
{
    "handyman_id": 123,
    "latitude": 12.9716,
    "longitude": 77.5946,
    "status": "online"
}
```

##### Update Online Status
```json
POST https://www.kaps9.in/api/vt_partner/handyman_online_status
{
    "handyman_id": 123,
    "status": 1,
    "lat": 12.9716,
    "lng": 77.5946,
    "recent_online_pic": "base64_encoded_image"
}
```

##### Add to Active Handymen
```json
POST https://www.kaps9.in/api/vt_partner/add_new_active_handyman
{
    "handyman_id": 123,
    "status": 1,
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

##### Delete from Active Handymen
```json
POST https://www.kaps9.in/api/vt_partner/delete_active_handyman
{
    "handyman_id": 123
}
```

#### Booking Management

##### Get Booking Details
```json
POST https://www.kaps9.in/api/vt_partner/handyman_agent_booking_details_for_ride_acceptance
{
    "booking_id": 123,
    "handyman_id": 123
}
```

##### Accept Booking
```json
POST https://www.kaps9.in/api/vt_partner/handyman_booking_accepted
{
    "booking_id": 123,
    "handyman_id": 123,
    "acceptance_status": "accepted"
}
```

##### Track Live Booking
```json
POST https://www.kaps9.in/api/vt_partner/handyman_agent_booking_details_live_track
{
    "booking_id": 123,
    "handyman_id": 123
}
```

##### Update Booking Status
```json
POST https://www.kaps9.in/api/vt_partner/update_booking_status_handyman
{
    "booking_id": 123,
    "handyman_id": 123,
    "status": "arrived",
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

#### Profile & Settings

##### Get Handyman Profile
```json
POST https://www.kaps9.in/api/vt_partner/get_handyman_details
{
    "handyman_id": 123
}
```

##### Update Handyman Profile
```json
POST https://www.kaps9.in/api/vt_partner/update_handyman_details
{
    "handyman_id": 123,
    "name": "John Doe",
    "email": "john@example.com",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image"
}
```

#### Earnings & Orders

##### Get Today's Earnings
```json
POST https://www.kaps9.in/api/vt_partner/handyman_todays_earnings
{
    "handyman_id": 123,
    "date": "2024-03-20"
}
```

##### Get All Orders
```json
POST https://www.kaps9.in/api/vt_partner/handyman_all_orders
{
    "handyman_id": 123,
    "page": 1,
    "limit": 10
}
```

#### Recharge & Payments

##### Get Recharge Details
```json
POST https://www.kaps9.in/api/vt_partner/handyman_current_new_recharge_details
{
    "handyman_id": 123
}
```

##### Create Recharge Plan
```json
POST https://www.kaps9.in/api/vt_partner/new_handyman_new_recharge_plan
{
    "handyman_id": 123,
    "plan_id": 123,
    "amount": 1000,
    "payment_method": "razorpay"
}
```

##### Get Recharge History
```json
POST https://www.kaps9.in/api/vt_partner/get_handyman_new_recharge_plan_history_list
{
    "handyman_id": 123,
    "page": 1,
    "limit": 10
}
```

### Common Response Format

#### Success Response
```json
{
    "status": "success",
    "message": "Operation successful",
    "results": {
        // Response data specific to the API
    }
}
```

#### Error Response
```json
{
    "status": "error",
    "message": "Error message describing what went wrong",
    "error_code": "ERROR_CODE"
}
```

### Environment Variables
```json
{
    "base_url": "https://www.kaps9.in/api/vt_partner",
    "dev_base_url": "http://100.24.44.74:8000/api/vt_partner",
    "razorpay_key": "rzp_test_61op4YoSkMBW6u"
}
```

## Complete API Documentation with Request Bodies

### 1. Goods Driver APIs

#### Authentication & Registration

##### Login
```json
POST https://www.kaps9.in/api/vt_partner/goods_driver_login
{
    "mobile_no": "+918296565588",
    "otp": 123456
}
```

##### Registration
```json
POST https://www.kaps9.in/api/vt_partner/goods_driver_registration
{
    "name": "John Doe",
    "mobile_no": "+918296565588",
    "email": "john@example.com",
    "vehicle_type": "truck",
    "vehicle_number": "KA01AB1234",
    "vehicle_model": "Tata 407",
    "vehicle_color": "White",
    "license_number": "DL123456789",
    "license_expiry": "2025-12-31",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image",
    "license_pic": "base64_encoded_image",
    "rc_book_pic": "base64_encoded_image",
    "insurance_pic": "base64_encoded_image",
    "r_lat": 12.9716,
    "r_lng": 77.5946,
    "current_lat": 12.9716,
    "current_lng": 77.5946,
    "recent_selfie_photo_url": "base64_encoded_image"
}
```

##### Update FCM Token
```json
POST https://www.kaps9.in/api/vt_partner/update_firebase_goods_driver_token
{
    "driver_id": 123,
    "fcm_token": "fcm_token_string"
}
```

#### Location & Status

##### Update Location
```json
POST https://www.kaps9.in/api/vt_partner/update_goods_drivers_current_location
{
    "driver_id": 123,
    "latitude": 12.9716,
    "longitude": 77.5946,
    "status": "online"
}
```

##### Update Online Status
```json
POST https://www.kaps9.in/api/vt_partner/goods_driver_online_status
{
    "driver_id": 123,
    "status": 1,
    "lat": 12.9716,
    "lng": 77.5946,
    "recent_online_pic": "base64_encoded_image"
}
```

##### Add to Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/add_new_active_goods_driver
{
    "driver_id": 123,
    "status": 1,
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

##### Delete from Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/delete_active_goods_driver
{
    "driver_id": 123
}
```

#### Booking Management

##### Get Booking Details
```json
POST https://www.kaps9.in/api/vt_partner/goods_booking_details_for_ride_acceptance
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Accept Booking
```json
POST https://www.kaps9.in/api/vt_partner/goods_driver_booking_accepted
{
    "booking_id": 123,
    "driver_id": 123,
    "acceptance_status": "accepted"
}
```

##### Track Live Booking
```json
POST https://www.kaps9.in/api/vt_partner/goods_booking_details_live_track
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Update Booking Status
```json
POST https://www.kaps9.in/api/vt_partner/update_booking_status_goods_driver
{
    "booking_id": 123,
    "driver_id": 123,
    "status": "arrived",
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

#### Profile & Settings

##### Get Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/get_goods_driver_details
{
    "driver_id": 123
}
```

##### Update Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/update_goods_driver_details
{
    "driver_id": 123,
    "name": "John Doe",
    "email": "john@example.com",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image"
}
```

#### Earnings & Orders

##### Get Today's Earnings
```json
POST https://www.kaps9.in/api/vt_partner/goods_driver_todays_earnings
{
    "driver_id": 123,
    "date": "2024-03-20"
}
```

##### Get All Orders
```json
POST https://www.kaps9.in/api/vt_partner/goods_driver_all_orders
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

#### Recharge & Payments

##### Get Recharge Details
```json
POST https://www.kaps9.in/api/vt_partner/goods_driver_current_new_recharge_details
{
    "driver_id": 123
}
```

##### Create Recharge Plan
```json
POST https://www.kaps9.in/api/vt_partner/new_goods_driver_new_recharge_plan
{
    "driver_id": 123,
    "plan_id": 123,
    "amount": 1000,
    "payment_method": "razorpay"
}
```

##### Get Recharge History
```json
POST https://www.kaps9.in/api/vt_partner/get_goods_driver_new_recharge_plan_history_list
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

### 2. Cab Driver APIs

#### Authentication & Registration

##### Login
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_login
{
    "mobile_no": "+918296565588",
    "otp": 123456
}
```

##### Registration
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_registration
{
    "name": "John Doe",
    "mobile_no": "+918296565588",
    "email": "john@example.com",
    "vehicle_type": "sedan",
    "vehicle_number": "KA01AB1234",
    "vehicle_model": "Toyota Camry",
    "vehicle_color": "Black",
    "license_number": "DL123456789",
    "license_expiry": "2025-12-31",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image",
    "license_pic": "base64_encoded_image",
    "rc_book_pic": "base64_encoded_image",
    "insurance_pic": "base64_encoded_image",
    "r_lat": 12.9716,
    "r_lng": 77.5946,
    "current_lat": 12.9716,
    "current_lng": 77.5946,
    "recent_selfie_photo_url": "base64_encoded_image"
}
```

##### Update FCM Token
```json
POST https://www.kaps9.in/api/vt_partner/update_firebase_cab_driver_token
{
    "driver_id": 123,
    "fcm_token": "fcm_token_string"
}
```

#### Location & Status

##### Update Location
```json
POST https://www.kaps9.in/api/vt_partner/update_cab_drivers_current_location
{
    "driver_id": 123,
    "latitude": 12.9716,
    "longitude": 77.5946,
    "status": "online"
}
```

##### Update Online Status
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_online_status
{
    "cab_driver_id": 123,
    "status": 1,
    "lat": 12.9716,
    "lng": 77.5946,
    "recent_online_pic": "base64_encoded_image"
}
```

##### Add to Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/add_new_active_cab_driver
{
    "cab_driver_id": 123,
    "status": 1,
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

##### Delete from Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/delete_active_cab_driver
{
    "cab_driver_id": 123
}
```

#### Booking Management

##### Get Booking Details
```json
POST https://www.kaps9.in/api/vt_partner/cab_booking_details_for_ride_acceptance
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Accept Booking
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_booking_accepted
{
    "booking_id": 123,
    "driver_id": 123,
    "acceptance_status": "accepted"
}
```

##### Track Live Booking
```json
POST https://www.kaps9.in/api/vt_partner/cab_booking_details_live_track
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Update Booking Status
```json
POST https://www.kaps9.in/api/vt_partner/update_booking_status_cab_driver
{
    "booking_id": 123,
    "driver_id": 123,
    "status": "arrived",
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

#### Profile & Settings

##### Get Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/get_cab_driver_details
{
    "driver_id": 123
}
```

##### Update Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/update_cab_driver_details
{
    "driver_id": 123,
    "name": "John Doe",
    "email": "john@example.com",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image"
}
```

#### Earnings & Orders

##### Get Today's Earnings
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_todays_earnings
{
    "driver_id": 123,
    "date": "2024-03-20"
}
```

##### Get All Orders
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_all_orders
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

#### Recharge & Payments

##### Get Recharge Details
```json
POST https://www.kaps9.in/api/vt_partner/cab_driver_current_new_recharge_details
{
    "driver_id": 123
}
```

##### Create Recharge Plan
```json
POST https://www.kaps9.in/api/vt_partner/new_cab_driver_new_recharge_plan
{
    "driver_id": 123,
    "plan_id": 123,
    "amount": 1000,
    "payment_method": "razorpay"
}
```

##### Get Recharge History
```json
POST https://www.kaps9.in/api/vt_partner/get_cab_driver_new_recharge_plan_history_list
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

### 3. JCB/Crane Operator APIs

#### Authentication & Registration

##### Login
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_login
{
    "mobile_no": "+918296565588",
    "otp": 123456
}
```

##### Registration
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_registration
{
    "name": "John Doe",
    "mobile_no": "+918296565588",
    "email": "john@example.com",
    "vehicle_type": "jcb",
    "vehicle_number": "KA01AB1234",
    "vehicle_model": "JCB 3DX",
    "vehicle_color": "Yellow",
    "license_number": "DL123456789",
    "license_expiry": "2025-12-31",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image",
    "license_pic": "base64_encoded_image",
    "rc_book_pic": "base64_encoded_image",
    "insurance_pic": "base64_encoded_image",
    "r_lat": 12.9716,
    "r_lng": 77.5946,
    "current_lat": 12.9716,
    "current_lng": 77.5946,
    "recent_selfie_photo_url": "base64_encoded_image"
}
```

##### Update FCM Token
```json
POST https://www.kaps9.in/api/vt_partner/update_firebase_jcb_crane_driver_token
{
    "driver_id": 123,
    "fcm_token": "fcm_token_string"
}
```

#### Location & Status

##### Update Location
```json
POST https://www.kaps9.in/api/vt_partner/update_jcb_crane_drivers_current_location
{
    "driver_id": 123,
    "latitude": 12.9716,
    "longitude": 77.5946,
    "status": "online"
}
```

##### Update Online Status
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_update_online_status
{
    "jcb_crane_driver_id": 123,
    "status": 1,
    "lat": 12.9716,
    "lng": 77.5946,
    "recent_online_pic": "base64_encoded_image"
}
```

##### Add to Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/add_new_active_jcb_crane_driver
{
    "jcb_crane_driver_id": 123,
    "status": 1,
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

##### Delete from Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/delete_active_jcb_crane_driver
{
    "jcb_crane_driver_id": 123
}
```

#### Booking Management

##### Get Booking Details
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_booking_details_for_ride_acceptance
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Accept Booking
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_booking_accepted
{
    "booking_id": 123,
    "driver_id": 123,
    "acceptance_status": "accepted"
}
```

##### Track Live Booking
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_booking_details_live_track
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Update Booking Status
```json
POST https://www.kaps9.in/api/vt_partner/update_booking_status_jcb_crane_driver
{
    "booking_id": 123,
    "driver_id": 123,
    "status": "arrived",
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

#### Profile & Settings

##### Get Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/get_jcb_crane_driver_details
{
    "driver_id": 123
}
```

##### Update Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/update_jcb_crane_driver_details
{
    "driver_id": 123,
    "name": "John Doe",
    "email": "john@example.com",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image"
}
```

#### Earnings & Orders

##### Get Today's Earnings
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_todays_earnings
{
    "driver_id": 123,
    "date": "2024-03-20"
}
```

##### Get All Orders
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_driver_all_orders
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

#### Recharge & Payments

##### Get Recharge Details
```json
POST https://www.kaps9.in/api/vt_partner/jcb_crane_current_new_recharge_details
{
    "driver_id": 123
}
```

##### Create Recharge Plan
```json
POST https://www.kaps9.in/api/vt_partner/new_jcb_crane_driver_new_recharge_plan
{
    "driver_id": 123,
    "plan_id": 123,
    "amount": 1000,
    "payment_method": "razorpay"
}
```

##### Get Recharge History
```json
POST https://www.kaps9.in/api/vt_partner/get_jcb_crane_driver_new_recharge_plan_history_list
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

### 4. Other Driver APIs

#### Authentication & Registration

##### Login
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_login
{
    "mobile_no": "+918296565588",
    "otp": 123456
}
```

##### Registration
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_registration
{
    "name": "John Doe",
    "mobile_no": "+918296565588",
    "email": "john@example.com",
    "vehicle_type": "truck",
    "vehicle_number": "KA01AB1234",
    "vehicle_model": "Tata 407",
    "vehicle_color": "White",
    "license_number": "DL123456789",
    "license_expiry": "2025-12-31",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image",
    "license_pic": "base64_encoded_image",
    "rc_book_pic": "base64_encoded_image",
    "insurance_pic": "base64_encoded_image",
    "r_lat": 12.9716,
    "r_lng": 77.5946,
    "current_lat": 12.9716,
    "current_lng": 77.5946,
    "recent_selfie_photo_url": "base64_encoded_image"
}
```

##### Update FCM Token
```json
POST https://www.kaps9.in/api/vt_partner/update_firebase_other_driver_token
{
    "driver_id": 123,
    "fcm_token": "fcm_token_string"
}
```

#### Location & Status

##### Update Location
```json
POST https://www.kaps9.in/api/vt_partner/update_other_drivers_current_location
{
    "driver_id": 123,
    "latitude": 12.9716,
    "longitude": 77.5946,
    "status": "online"
}
```

##### Update Online Status
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_online_status
{
    "other_driver_id": 123,
    "status": 1,
    "lat": 12.9716,
    "lng": 77.5946,
    "recent_online_pic": "base64_encoded_image"
}
```

##### Add to Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/add_new_active_other_driver
{
    "other_driver_id": 123,
    "status": 1,
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

##### Delete from Active Drivers
```json
POST https://www.kaps9.in/api/vt_partner/delete_active_other_driver
{
    "other_driver_id": 123
}
```

#### Booking Management

##### Get Booking Details
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_booking_details_for_ride_acceptance
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Accept Booking
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_booking_accepted
{
    "booking_id": 123,
    "driver_id": 123,
    "acceptance_status": "accepted"
}
```

##### Track Live Booking
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_booking_details_live_track
{
    "booking_id": 123,
    "driver_id": 123
}
```

##### Update Booking Status
```json
POST https://www.kaps9.in/api/vt_partner/update_booking_status_other_driver
{
    "booking_id": 123,
    "driver_id": 123,
    "status": "arrived",
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

#### Profile & Settings

##### Get Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/get_other_driver_details
{
    "driver_id": 123
}
```

##### Update Driver Profile
```json
POST https://www.kaps9.in/api/vt_partner/update_other_driver_details
{
    "driver_id": 123,
    "name": "John Doe",
    "email": "john@example.com",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image"
}
```

#### Earnings & Orders

##### Get Today's Earnings
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_todays_earnings
{
    "driver_id": 123,
    "date": "2024-03-20"
}
```

##### Get All Orders
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_all_orders
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

#### Recharge & Payments

##### Get Recharge Details
```json
POST https://www.kaps9.in/api/vt_partner/other_driver_current_new_recharge_details
{
    "driver_id": 123
}
```

##### Create Recharge Plan
```json
POST https://www.kaps9.in/api/vt_partner/new_other_driver_new_recharge_plan
{
    "driver_id": 123,
    "plan_id": 123,
    "amount": 1000,
    "payment_method": "razorpay"
}
```

##### Get Recharge History
```json
POST https://www.kaps9.in/api/vt_partner/get_other_driver_new_recharge_plan_history_list
{
    "driver_id": 123,
    "page": 1,
    "limit": 10
}
```

### 5. Handyman APIs

#### Authentication & Registration

##### Login
```json
POST https://www.kaps9.in/api/vt_partner/handyman_login
{
    "mobile_no": "+918296565588",
    "otp": 123456
}
```

##### Registration
```json
POST https://www.kaps9.in/api/vt_partner/handyman_registration
{
    "name": "John Doe",
    "mobile_no": "+918296565588",
    "email": "john@example.com",
    "service_type": "plumber",
    "experience": "5 years",
    "skills": ["plumbing", "electrical"],
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image",
    "id_proof": "base64_encoded_image",
    "certificate": "base64_encoded_image",
    "r_lat": 12.9716,
    "r_lng": 77.5946,
    "current_lat": 12.9716,
    "current_lng": 77.5946,
    "recent_selfie_photo_url": "base64_encoded_image"
}
```

##### Update FCM Token
```json
POST https://www.kaps9.in/api/vt_partner/update_firebase_handyman_token
{
    "handyman_id": 123,
    "fcm_token": "fcm_token_string"
}
```

#### Location & Status

##### Update Location
```json
POST https://www.kaps9.in/api/vt_partner/update_handymans_current_location
{
    "handyman_id": 123,
    "latitude": 12.9716,
    "longitude": 77.5946,
    "status": "online"
}
```

##### Update Online Status
```json
POST https://www.kaps9.in/api/vt_partner/handyman_online_status
{
    "handyman_id": 123,
    "status": 1,
    "lat": 12.9716,
    "lng": 77.5946,
    "recent_online_pic": "base64_encoded_image"
}
```

##### Add to Active Handymen
```json
POST https://www.kaps9.in/api/vt_partner/add_new_active_handyman
{
    "handyman_id": 123,
    "status": 1,
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

##### Delete from Active Handymen
```json
POST https://www.kaps9.in/api/vt_partner/delete_active_handyman
{
    "handyman_id": 123
}
```

#### Booking Management

##### Get Booking Details
```json
POST https://www.kaps9.in/api/vt_partner/handyman_agent_booking_details_for_ride_acceptance
{
    "booking_id": 123,
    "handyman_id": 123
}
```

##### Accept Booking
```json
POST https://www.kaps9.in/api/vt_partner/handyman_booking_accepted
{
    "booking_id": 123,
    "handyman_id": 123,
    "acceptance_status": "accepted"
}
```

##### Track Live Booking
```json
POST https://www.kaps9.in/api/vt_partner/handyman_agent_booking_details_live_track
{
    "booking_id": 123,
    "handyman_id": 123
}
```

##### Update Booking Status
```json
POST https://www.kaps9.in/api/vt_partner/update_booking_status_handyman
{
    "booking_id": 123,
    "handyman_id": 123,
    "status": "arrived",
    "current_lat": 12.9716,
    "current_lng": 77.5946
}
```

#### Profile & Settings

##### Get Handyman Profile
```json
POST https://www.kaps9.in/api/vt_partner/get_handyman_details
{
    "handyman_id": 123
}
```

##### Update Handyman Profile
```json
POST https://www.kaps9.in/api/vt_partner/update_handyman_details
{
    "handyman_id": 123,
    "name": "John Doe",
    "email": "john@example.com",
    "address": "123 Main St",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": 560001,
    "profile_pic": "base64_encoded_image"
}
```

#### Earnings & Orders

##### Get Today's Earnings
```json
POST https://www.kaps9.in/api/vt_partner/handyman_todays_earnings
{
    "handyman_id": 123,
    "date": "2024-03-20"
}
```

##### Get All Orders
```json
POST https://www.kaps9.in/api/vt_partner/handyman_all_orders
{
    "handyman_id": 123,
    "page": 1,
    "limit": 10
}
```

#### Recharge & Payments

##### Get Recharge Details
```json
POST https://www.kaps9.in/api/vt_partner/handyman_current_new_recharge_details
{
    "handyman_id": 123
}
```

##### Create Recharge Plan
```json
POST https://www.kaps9.in/api/vt_partner/new_handyman_new_recharge_plan
{
    "handyman_id": 123,
    "plan_id": 123,
    "amount": 1000,
    "payment_method": "razorpay"
}
```

##### Get Recharge History
```json
POST https://www.kaps9.in/api/vt_partner/get_handyman_new_recharge_plan_history_list
{
    "handyman_id": 123,
    "page": 1,
    "limit": 10
}
```

### Common Response Format

#### Success Response
```json
{
    "status": "success",
    "message": "Operation successful",
    "results": {
        // Response data specific to the API
    }
}
```

#### Error Response
```json
{
    "status": "error",
    "message": "Error message describing what went wrong",
    "error_code": "ERROR_CODE"
}
```

### Environment Variables
```json
{
    "base_url": "https://www.kaps9.in/api/vt_partner",
    "dev_base_url": "http://100.24.44.74:8000/api/vt_partner",
    "razorpay_key": "rzp_test_61op4YoSkMBW6u"
}
```

