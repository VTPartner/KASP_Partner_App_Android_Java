-- Goods Agent Referral System Tables

-- Table to store referral codes for goods agents
CREATE TABLE IF NOT EXISTS vtpartner.goods_agent_referral_code_tbl (
    id SERIAL PRIMARY KEY,
    goods_driver_id INTEGER NOT NULL,
    referral_code VARCHAR(6) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (goods_driver_id) REFERENCES vtpartner.goods_driverstbl(goods_driver_id) ON DELETE CASCADE
);

-- Table to track referral usage
CREATE TABLE IF NOT EXISTS vtpartner.goods_agent_referral_usage_tbl (
    id SERIAL PRIMARY KEY,
    referred_by_code VARCHAR(6) NOT NULL,
    used_by_goods_driver INTEGER NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (referred_by_code) REFERENCES vtpartner.goods_agent_referral_code_tbl(referral_code) ON DELETE CASCADE,
    FOREIGN KEY (used_by_goods_driver) REFERENCES vtpartner.goods_driverstbl(goods_driver_id) ON DELETE CASCADE,
    UNIQUE(used_by_goods_driver) -- Ensure each goods driver can only use one referral code
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_goods_agent_referral_code_driver_id ON vtpartner.goods_agent_referral_code_tbl(goods_driver_id);
CREATE INDEX IF NOT EXISTS idx_goods_agent_referral_code_code ON vtpartner.goods_agent_referral_code_tbl(referral_code);
CREATE INDEX IF NOT EXISTS idx_goods_agent_referral_usage_code ON vtpartner.goods_agent_referral_usage_tbl(referred_by_code);
CREATE INDEX IF NOT EXISTS idx_goods_agent_referral_usage_driver ON vtpartner.goods_agent_referral_usage_tbl(used_by_goods_driver);

-- Add comments for documentation
COMMENT ON TABLE vtpartner.goods_agent_referral_code_tbl IS 'Stores referral codes generated for goods agents';
COMMENT ON TABLE vtpartner.goods_agent_referral_usage_tbl IS 'Tracks when referral codes are used by new goods agents';

-- Insert control setting for goods agent referral bonus amount
INSERT INTO vtpartner.control_settings_tbl (controller_name, values, description) 
VALUES ('SIGN_UP_BONUS_GOODS_AGENT', '10', 'Referral bonus amount for goods agents in rupees')
ON CONFLICT (controller_name) DO UPDATE SET 
    values = EXCLUDED.values,
    description = EXCLUDED.description; 