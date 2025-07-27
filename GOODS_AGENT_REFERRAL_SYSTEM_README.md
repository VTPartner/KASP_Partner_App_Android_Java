# Goods Agent Referral System

This document describes the complete referral system implementation for KAPS Goods Agents, similar to the customer referral system but adapted for goods drivers.

## Overview

The Goods Agent Referral System allows existing goods agents to refer new agents and earn rewards. Both the referrer and referee receive bonus amounts when a new agent joins using a referral code.

## Features

- **Referral Code Generation**: Unique 6-character alphanumeric codes for each goods agent
- **Referral Tracking**: Track who referred whom and when
- **Bonus Distribution**: Automatic wallet credits for both referrer and referee
- **Statistics**: View total earnings, referral count, and referral history
- **Validation**: Prevent self-referrals and duplicate usage
- **Real-time Updates**: Live statistics and referral list updates

## Database Tables

### 1. `goods_agent_referral_code_tbl`
Stores referral codes generated for goods agents.

```sql
CREATE TABLE vtpartner.goods_agent_referral_code_tbl (
    id SERIAL PRIMARY KEY,
    goods_driver_id INTEGER NOT NULL,
    referral_code VARCHAR(6) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (goods_driver_id) REFERENCES vtpartner.goods_drivers_tbl(goods_driver_id)
);
```

### 2. `goods_agent_referral_usage_tbl`
Tracks when referral codes are used by new goods agents.

```sql
CREATE TABLE vtpartner.goods_agent_referral_usage_tbl (
    id SERIAL PRIMARY KEY,
    referred_by_code VARCHAR(6) NOT NULL,
    used_by_goods_driver INTEGER NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (referred_by_code) REFERENCES vtpartner.goods_agent_referral_code_tbl(referral_code),
    FOREIGN KEY (used_by_goods_driver) REFERENCES vtpartner.goods_drivers_tbl(goods_driver_id),
    UNIQUE(used_by_goods_driver)
);
```

## API Endpoints

### 1. Generate Referral Code
**Endpoint**: `POST /api/vt_partner/generate_goods_agent_referral_code`

**Request Body**:
```json
{
    "goods_driver_id": "123"
}
```

**Response**:
```json
{
    "status": "success",
    "referral_code": "ABC123",
    "driver_name": "John Doe",
    "share_message": "Join KAPS as a Goods Agent using my referral code ABC123 and get ₹10 bonus!...",
    "statistics": {
        "total_referrals": 5,
        "completed_referrals": 3,
        "pending_referrals": 2,
        "total_earnings": 30.0
    }
}
```

### 2. Apply Referral Code
**Endpoint**: `POST /api/vt_partner/apply_goods_agent_referral_code`

**Request Body**:
```json
{
    "goods_driver_id": "456",
    "referral_code": "ABC123"
}
```

**Response**:
```json
{
    "status": "success",
    "message": "Referral code applied successfully! You earned ₹10",
    "bonus_amount": 10.0,
    "referrer_name": "John Doe",
    "referrer_bonus": 10.0
}
```

### 3. Get Referral Details
**Endpoint**: `POST /api/vt_partner/get_goods_agent_referral_details`

**Request Body**:
```json
{
    "goods_driver_id": "123"
}
```

**Response**:
```json
{
    "status": "success",
    "referral_code": "ABC123",
    "total_earnings": 30.0,
    "referrals": [
        {
            "driver_name": "Jane Smith",
            "used_at": "2024-01-15 10:30:00",
            "status": "Completed",
            "amount": 10.0
        }
    ],
    "statistics": {
        "total_referrals": 5,
        "completed_referrals": 3,
        "pending_referrals": 2
    }
}
```

### 4. Validate Referral Code
**Endpoint**: `POST /api/vt_partner/validate_goods_agent_referral_code`

**Request Body**:
```json
{
    "referral_code": "ABC123",
    "goods_driver_id": "456"
}
```

**Response**:
```json
{
    "status": "success",
    "message": "Valid referral code",
    "valid": true,
    "referrer_name": "John Doe",
    "bonus_amount": 10.0
}
```

## Android Implementation

### Activities

#### 1. GoodsAgentInviteEarnActivity
- **Location**: `KapsAgentApp/app/src/main/java/com/kapstranspvtltd/kaps_partner/goods_driver_activities/GoodsAgentInviteEarnActivity.java`
- **Layout**: `activity_goods_agent_invite_earn.xml`
- **Purpose**: Display referral code, statistics, and referral history

#### 2. GoodsAgentEnterReferralActivity
- **Location**: `KapsAgentApp/app/src/main/java/com/kapstranspvtltd/kaps_partner/goods_driver_activities/GoodsAgentEnterReferralActivity.java`
- **Layout**: `activity_goods_agent_enter_referral.xml`
- **Purpose**: Allow new agents to enter referral codes during registration

### Models

#### GoodsAgentReferralModel
- **Location**: `KapsAgentApp/app/src/main/java/com/kapstranspvtltd/kaps_partner/models/GoodsAgentReferralModel.java`
- **Purpose**: Data model for referral information

### Adapters

#### GoodsAgentReferralsAdapter
- **Location**: `KapsAgentApp/app/src/main/java/com/kapstranspvtltd/kaps_partner/adapters/GoodsAgentReferralsAdapter.java`
- **Purpose**: RecyclerView adapter for displaying referral list

### Layouts

#### 1. activity_goods_agent_invite_earn.xml
- Referral code display
- Share functionality
- Statistics section
- Referral list with expand/collapse

#### 2. activity_goods_agent_enter_referral.xml
- Referral code input
- Validation messages
- Success confirmation
- Benefits explanation

#### 3. item_goods_agent_referral.xml
- Individual referral item layout
- Status indicators
- Amount display

## Configuration

### Control Settings
The system uses the following control setting:
- **Key**: `SIGN_UP_BONUS_GOODS_AGENT`
- **Value**: `10` (default bonus amount in rupees)
- **Description**: Referral bonus amount for goods agents

### Integration Points

#### 1. Registration Flow
- Add referral code input during goods agent registration
- Call `GoodsAgentEnterReferralActivity.startActivity()` after successful registration

#### 2. Menu Integration
- Add "Invite & Earn" option to goods agent menu
- Launch `GoodsAgentInviteEarnActivity` when selected

#### 3. Wallet Integration
- Referral bonuses are automatically credited to goods agent wallets
- Transactions are recorded in `goods_driver_wallet_transactions` table

## Security Features

1. **Unique Referral Codes**: Each goods agent gets a unique 6-character code
2. **Self-Referral Prevention**: Agents cannot use their own referral codes
3. **Single Usage**: Each agent can only use one referral code
4. **Code Validation**: Real-time validation of referral codes
5. **Transaction Tracking**: All bonus transactions are logged with proper remarks

## Error Handling

### Common Error Scenarios
1. **Invalid Referral Code**: Code doesn't exist or is malformed
2. **Self-Referral**: Agent tries to use their own code
3. **Already Used**: Agent has already used a referral code
4. **Network Errors**: Connection issues during API calls

### Error Responses
All APIs return consistent error responses:
```json
{
    "status": "error",
    "message": "Descriptive error message",
    "error": "Technical error details (optional)"
}
```

## Testing

### API Testing
1. Test referral code generation
2. Test referral code validation
3. Test referral code application
4. Test statistics calculation
5. Test error scenarios

### UI Testing
1. Test referral code display
2. Test copy functionality
3. Test share functionality
4. Test referral list expansion
5. Test validation messages

## Deployment

### Database Setup
1. Run the SQL scripts in `goods_agent_referral_tables.sql`
2. Verify table creation and indexes
3. Insert control settings

### Android Deployment
1. Add new activities to AndroidManifest.xml
2. Update navigation menus
3. Test on different Android versions
4. Verify API integration

## Monitoring

### Key Metrics
1. **Referral Conversion Rate**: Percentage of referrals that complete registration
2. **Average Referrals per Agent**: How many referrals each agent makes
3. **Bonus Distribution**: Total bonuses paid out
4. **Code Usage**: How often referral codes are used

### Logging
- All API calls are logged with request/response data
- Error scenarios are logged with stack traces
- Performance metrics are tracked

## Future Enhancements

1. **Tiered Rewards**: Different bonus amounts based on referral count
2. **Referral Campaigns**: Time-limited bonus increases
3. **Social Sharing**: Enhanced sharing with referral tracking
4. **Analytics Dashboard**: Detailed referral analytics
5. **Multi-level Referrals**: Referral chains with multiple levels

## Support

For technical support or questions about the referral system:
1. Check API documentation
2. Review error logs
3. Test with sample data
4. Contact development team

## Changelog

### Version 1.0.0 (Initial Release)
- Basic referral code generation and usage
- Referral statistics and history
- Android UI implementation
- API endpoints for all operations
- Database schema and indexes 