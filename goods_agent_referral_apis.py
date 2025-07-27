import string
from decimal import Decimal
import json
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from mobile_app.views import select_query, insert_query, update_query
import logging

from django.views.decorators.csrf import csrf_exempt
import random
import time

def generate_unique_goods_agent_referral_code():
    """Generate a unique 6-character alphanumeric referral code for goods agents"""
    while True:
        code = ''.join(random.choices(string.ascii_uppercase + string.digits, k=6))
        # Check if code already exists
        check_query = "SELECT COUNT(*) FROM vtpartner.goods_agent_referral_code_tbl WHERE referral_code = %s"
        result = select_query(check_query, [code])
        
        if not result:
            # If query fails, assume code doesn't exist
            return code
        elif result[0][0] == 0:
            # Code doesn't exist, safe to use
            return code

def get_control_value(controller_name):
    query = """
        SELECT values 
        FROM vtpartner.control_settings_tbl 
        WHERE controller_name = %s
    """
    result = select_query(query, [controller_name])
    if result:
        return result[0][0]  # Return the 'values' column
    return None


@csrf_exempt
def generate_goods_agent_referral_code(request):
    """Generate or get existing referral code for a goods agent"""
    if request.method == "POST":
        earnings_result = None
        completed_referrals_result = None
        total_referrals_result = None
        try:
            data = json.loads(request.body)
            goods_driver_id = data.get('goods_driver_id')
            
            if not goods_driver_id:
                return JsonResponse({
                    "message": "Goods Driver ID is required",
                    "status": "error"
                }, status=400)
            
            # Check if goods driver already has a referral code
            existing_query = """
                SELECT referral_code 
                FROM vtpartner.goods_agent_referral_code_tbl 
                WHERE goods_driver_id = %s
            """
            existing_result = select_query(existing_query, [goods_driver_id])
            
            if existing_result:
                referral_code = existing_result[0][0]
            else:
                # Generate new referral code
                referral_code = generate_unique_goods_agent_referral_code()
                
                # Insert new referral code
                insert_query_text = """
                    INSERT INTO vtpartner.goods_agent_referral_code_tbl (goods_driver_id, referral_code)
                    VALUES (%s, %s)
                """
                insert_query(insert_query_text, [goods_driver_id, referral_code])
            
            # Get goods driver details for sharing
            driver_query = """
                SELECT driver_first_name, mobile_no 
                FROM vtpartner.goods_drivers_tbl 
                WHERE goods_driver_id = %s
            """
            driver_result = select_query(driver_query, [goods_driver_id])
            
            if not driver_result:
                driver_name = "Driver"
            else:
                driver_name = driver_result[0][0]
            
            # Get referral statistics - using simple queries that work with your DB
            # First get total referrals count
            total_referrals_query = """
                SELECT COUNT(*) 
                FROM vtpartner.goods_agent_referral_usage_tbl 
                WHERE referred_by_code = %s
            """
            total_referrals_result = select_query(total_referrals_query, [referral_code])
            
            if not total_referrals_result or len(total_referrals_result) == 0:
                total_referrals = 0
            else:
                total_referrals = total_referrals_result[0][0]
            
            # Get completed referrals count (count of referral bonus transactions)
            completed_referrals_query = """
                SELECT COUNT(*) 
                FROM vtpartner.goods_driver_wallet_transactions 
                WHERE goods_driver_id = %s 
                AND remarks LIKE '%Referral bonus%'
                AND status = 'SUCCESS'
            """
            completed_referrals_result = select_query(completed_referrals_query, [goods_driver_id])
            
            if not completed_referrals_result or len(completed_referrals_result) == 0 or completed_referrals_result[0][0] is None:
                completed_referrals = 0
            else:
                completed_referrals = completed_referrals_result[0][0]
            
            # Calculate total earnings - using simple SUM query
            earnings_query = """
                SELECT COALESCE(SUM(amount), 0.0) 
                FROM vtpartner.goods_driver_wallet_transactions 
                WHERE goods_driver_id = %s 
                AND remarks LIKE '%Referral bonus%'
                AND status = 'SUCCESS'
            """
            earnings_result = select_query(earnings_query, [goods_driver_id])
            
            if not earnings_result or earnings_result[0][0] is None:
                total_earnings = 0
            else:
                # Handle NULL from SUM when no rows match
                amount = earnings_result[0][0]
                total_earnings = float(amount) if amount is not None else 0
            # Fetch bonus amount from control settings
            bonus_amount = get_control_value("SIGN_UP_BONUS_GOODS_AGENT")
            bonus_amount = int(bonus_amount) if bonus_amount and bonus_amount.isdigit() else 10  # fallback to 10

            # Create share message dynamically
            share_message = (
                f"Join KAPS as a Goods Agent using my referral code {referral_code} and get ₹{bonus_amount} bonus! "
                "Start earning with reliable goods transportation services. "
                "Download: https://play.google.com/store/apps/details?id=com.kapstranspvtltd.kaps_partner&hl=en_IN"
            )

            return JsonResponse({
                "status": "success",
                "referral_code": referral_code,
                "driver_name": driver_name,
                "share_message": share_message,
                "statistics": {
                    "total_referrals": total_referrals,
                    "completed_referrals": completed_referrals,
                    "pending_referrals": total_referrals - completed_referrals,
                    "total_earnings": total_earnings
                }
            })
            
        except json.JSONDecodeError:
            return JsonResponse({
                "message": "Invalid JSON in request body",
                "status": "error"
            }, status=400)
        except Exception as err:
            print(f"earnings_result: {earnings_result}")
            print(f"completed_referrals_result: {completed_referrals_result}")
            print(f"total_referrals_result: {total_referrals_result}")
            
            print("Error in generate_goods_agent_referral_code:", err)
            return JsonResponse({
                "message": "Internal Server Error",
                "status": "error",
                "error": str(err)
            }, status=500)
    
    return JsonResponse({
        "message": "Method not allowed",
        "status": "error"
    }, status=405)

@csrf_exempt
def apply_goods_agent_referral_code(request):
    """Apply referral code when new goods agent signs up"""
    if request.method == "POST":
        try:
            data = json.loads(request.body)
            goods_driver_id = data.get('goods_driver_id')
            referral_code = data.get('referral_code', '').upper().strip()
            
            if not goods_driver_id or not referral_code:
                return JsonResponse({
                    "message": "Goods Driver ID and referral code are required",
                    "status": "error"
                }, status=400)
            
            # Validate referral code format
            if len(referral_code) != 6 or not referral_code.isalnum():
                return JsonResponse({
                    "message": "Invalid referral code format",
                    "status": "error"
                }, status=400)
            
            # Check if goods driver has already used a referral code
            existing_usage_query = """
                SELECT COUNT(*) 
                FROM vtpartner.goods_agent_referral_usage_tbl 
                WHERE used_by_goods_driver = %s
            """
            existing_usage = select_query(existing_usage_query, [goods_driver_id])
            
            if not existing_usage:
                has_used_referral = False
            else:
                has_used_referral = existing_usage[0][0] > 0
                
            if has_used_referral:
                return JsonResponse({
                    "message": "You have already used a referral code",
                    "status": "error"
                }, status=200)
            
            # Check if referral code exists and get referrer details
            referrer_query = """
                SELECT garc.goods_driver_id, gdt.driver_first_name 
                FROM vtpartner.goods_agent_referral_code_tbl garc
                JOIN vtpartner.goods_drivers_tbl gdt ON garc.goods_driver_id = gdt.goods_driver_id
                WHERE garc.referral_code = %s
            """
            referrer_result = select_query(referrer_query, [referral_code])
            
            if not referrer_result:
                return JsonResponse({
                    "message": "Invalid referral code",
                    "status": "error"
                }, status=200)
            
            referrer_id = referrer_result[0][0]
            referrer_name = referrer_result[0][1]
            
            # Check if goods driver is trying to use their own referral code
            if referrer_id == goods_driver_id:
                return JsonResponse({
                    "message": "You cannot use your own referral code",
                    "status": "error"
                }, status=400)
            
            # Record referral usage
            usage_insert_query = """
                INSERT INTO vtpartner.goods_agent_referral_usage_tbl (referred_by_code, used_by_goods_driver)
                VALUES (%s, %s)
            """
            insert_query(usage_insert_query, [referral_code, goods_driver_id])
            
            # Referral bonus amounts
            referee_bonus = Decimal('10.00')  # New agent gets ₹10
            referrer_bonus = Decimal('10.00')  # Referrer gets ₹10
            
            current_time = time.time()
            
            # Create or update wallet for referee (new goods driver)
            referee_wallet_query = """
                SELECT wallet_id 
                FROM vtpartner.goods_driver_wallet 
                WHERE goods_driver_id = %s
            """
            referee_wallet_result = select_query(referee_wallet_query, [goods_driver_id])
            
            if not referee_wallet_result:
                # Create new wallet for referee
                create_wallet_query = """
                    INSERT INTO vtpartner.goods_driver_wallet (goods_driver_id, current_balance, last_updated)
                    VALUES (%s, %s, %s)
                    RETURNING wallet_id
                """
                wallet_result = insert_query(create_wallet_query, [goods_driver_id, referee_bonus, current_time])
                referee_wallet_id = wallet_result[0][0]
            else:
                referee_wallet_id = referee_wallet_result[0][0]
                # Update existing wallet
                update_wallet_query = """
                    UPDATE vtpartner.goods_driver_wallet 
                    SET current_balance = current_balance + %s, last_updated = %s
                    WHERE goods_driver_id = %s
                """
                update_query(update_wallet_query, [referee_bonus, current_time, goods_driver_id])
            
            # Add transaction for referee
            referee_transaction_query = """
                INSERT INTO vtpartner.goods_driver_wallet_transactions 
                (wallet_id, goods_driver_id, transaction_type, amount, status, transaction_time, 
                 transaction_date, payment_mode, remarks)
                VALUES (%s, %s, 'CREDIT', %s, 'SUCCESS', %s, CURRENT_DATE, 'Referral', 
                        'Referral bonus for joining with code: ' || %s)
            """
            insert_query(referee_transaction_query, [
                referee_wallet_id, goods_driver_id, referee_bonus, current_time, referral_code
            ])
            
            # Create or update wallet for referrer
            referrer_wallet_query = """
                SELECT wallet_id 
                FROM vtpartner.goods_driver_wallet 
                WHERE goods_driver_id = %s
            """
            referrer_wallet_result = select_query(referrer_wallet_query, [referrer_id])
            
            if not referrer_wallet_result:
                # Create new wallet for referrer
                create_wallet_query = """
                    INSERT INTO vtpartner.goods_driver_wallet (goods_driver_id, current_balance, last_updated)
                    VALUES (%s, %s, %s)
                    RETURNING wallet_id
                """
                wallet_result = insert_query(create_wallet_query, [referrer_id, referrer_bonus, current_time])
                referrer_wallet_id = wallet_result[0][0]
            else:
                referrer_wallet_id = referrer_wallet_result[0][0]
                # Update existing wallet
                update_wallet_query = """
                    UPDATE vtpartner.goods_driver_wallet 
                    SET current_balance = current_balance + %s, last_updated = %s
                    WHERE goods_driver_id = %s
                """
                update_query(update_wallet_query, [referrer_bonus, current_time, referrer_id])
            
            # Add transaction for referrer
            referrer_transaction_query = """
                INSERT INTO vtpartner.goods_driver_wallet_transactions 
                (wallet_id, goods_driver_id, transaction_type, amount, status, transaction_time, 
                 transaction_date, payment_mode, remarks)
                VALUES (%s, %s, 'CREDIT', %s, 'SUCCESS', %s, CURRENT_DATE, 'Referral', 
                        'Referral bonus for referring new goods agent')
            """
            insert_query(referrer_transaction_query, [
                referrer_wallet_id, referrer_id, referrer_bonus, current_time
            ])
            
            return JsonResponse({
                "status": "success",
                "message": f"Referral code applied successfully! You earned ₹{referee_bonus}",
                "bonus_amount": float(referee_bonus),
                "referrer_name": referrer_name,
                "referrer_bonus": float(referrer_bonus)
            })
            
        except json.JSONDecodeError:
            return JsonResponse({
                "message": "Invalid JSON in request body",
                "status": "error"
            }, status=400)
        except Exception as err:
            print("Error in apply_goods_agent_referral_code:", err)
            return JsonResponse({
                "message": "Internal Server Error",
                "status": "error",
                "error": str(err)
            }, status=500)
    
    return JsonResponse({
        "message": "Method not allowed",
        "status": "error"
    }, status=405)

@csrf_exempt
def get_goods_agent_referral_details(request):
    """Get referral statistics and history for a goods agent"""
    if request.method == "POST":
        try:
            data = json.loads(request.body)
            goods_driver_id = data.get('goods_driver_id')
            
            if not goods_driver_id:
                return JsonResponse({
                    "message": "Goods Driver ID is required",
                    "status": "error"
                }, status=400)
            
            # Get referral code
            code_query = """
                SELECT referral_code 
                FROM vtpartner.goods_agent_referral_code_tbl 
                WHERE goods_driver_id = %s
            """
            code_result = select_query(code_query, [goods_driver_id])
            
            if not code_result:
                referral_code = None
            else:
                referral_code = code_result[0][0]
            
            if not referral_code:
                return JsonResponse({
                    "status": "success",
                    "referral_code": None,
                    "total_earnings": 0,
                    "referrals": [],
                    "statistics": {
                        "total_referrals": 0,
                        "completed_referrals": 0,
                        "pending_referrals": 0
                    }
                })
            
            # Get referral list with goods driver details - using simple query
            referrals_query = """
                SELECT 
                    gdt.driver_first_name,
                    garu.used_at,
                    garu.used_by_goods_driver
                FROM vtpartner.goods_agent_referral_usage_tbl garu
                JOIN vtpartner.goods_drivers_tbl gdt ON garu.used_by_goods_driver = gdt.goods_driver_id
                WHERE garu.referred_by_code = %s
                ORDER BY garu.used_at DESC
            """
            referrals_result = select_query(referrals_query, [referral_code])
            
            referrals = []
            completed_count = 0
            
            if not referrals_result:
                # No referrals found
                pass
            else:
                for row in referrals_result:
                    driver_name = row[0]
                    used_at = row[1]
                    referred_driver_id = row[2]
                    
                    # Check if this referral has been completed (wallet transaction exists)
                    status_query = """
                        SELECT COUNT(*) 
                        FROM vtpartner.goods_driver_wallet_transactions 
                        WHERE goods_driver_id = %s 
                        AND remarks LIKE '%Referral bonus%'
                        AND status = 'SUCCESS'
                    """
                    status_result = select_query(status_query, [referred_driver_id])
                    
                    # Determine status based on wallet transaction
                    if status_result and len(status_result) > 0 and status_result[0][0] > 0:
                        status = 'Completed'
                        amount = 10.0
                    else:
                        status = 'Pending'
                        amount = 0.0
                    
                    referral_data = {
                        "driver_name": driver_name,
                        "used_at": str(used_at),
                        "status": status,
                        "amount": amount
                    }
                    referrals.append(referral_data)
                    if status == 'Completed':
                        completed_count += 1
            
            # Calculate total earnings - using simple SUM query
            earnings_query = """
                SELECT COALESCE(SUM(amount), 0.0) 
                FROM vtpartner.goods_driver_wallet_transactions 
                WHERE goods_driver_id = %s 
                AND remarks LIKE '%Referral bonus%'
                AND status = 'SUCCESS'
            """
            earnings_result = select_query(earnings_query, [goods_driver_id])
            
            if not earnings_result:
                total_earnings = 0
            else:
                # Handle NULL from SUM when no rows match
                amount = earnings_result[0][0]
                total_earnings = float(amount) if amount is not None else 0
            
            return JsonResponse({
                "status": "success",
                "referral_code": referral_code,
                "total_earnings": total_earnings,
                "referrals": referrals,
                "statistics": {
                    "total_referrals": len(referrals),
                    "completed_referrals": completed_count,
                    "pending_referrals": len(referrals) - completed_count
                }
            })
            
        except json.JSONDecodeError:
            return JsonResponse({
                "message": "Invalid JSON in request body",
                "status": "error"
            }, status=400)
        except Exception as err:
            print("Error in get_goods_agent_referral_details:", err)
            return JsonResponse({
                "message": "Internal Server Error",
                "status": "error",
                "error": str(err)
            }, status=500)
    
    return JsonResponse({
        "message": "Method not allowed",
        "status": "error"
    }, status=405)

@csrf_exempt
def validate_goods_agent_referral_code(request):
    """Validate if a goods agent referral code exists and is valid"""
    if request.method == "POST":
        try:
            data = json.loads(request.body)
            referral_code = data.get('referral_code', '').upper().strip()
            goods_driver_id = data.get('goods_driver_id')  # Optional, to check self-referral
            
            if not referral_code:
                return JsonResponse({
                    "message": "Referral code is required",
                    "status": "error"
                }, status=400)
            
            if len(referral_code) != 6 or not referral_code.isalnum():
                return JsonResponse({
                    "message": "Invalid referral code format",
                    "status": "error",
                    "valid": False
                }, status=400)
            
            # Check if referral code exists
            check_query = """
                SELECT garc.goods_driver_id, gdt.driver_first_name 
                FROM vtpartner.goods_agent_referral_code_tbl garc
                JOIN vtpartner.goods_drivers_tbl gdt ON garc.goods_driver_id = gdt.goods_driver_id
                WHERE garc.referral_code = %s
            """
            result = select_query(check_query, [referral_code])
            
            if not result:
                return JsonResponse({
                    "status": "error",
                    "message": "Invalid referral code",
                    "valid": False
                })
            
            referrer_id = result[0][0]
            referrer_name = result[0][1]
            
            # Check if goods driver is trying to use their own code
            if goods_driver_id and str(referrer_id) == str(goods_driver_id):
                return JsonResponse({
                    "status": "error",
                    "message": "You cannot use your own referral code",
                    "valid": False
                })
            
            return JsonResponse({
                "status": "success",
                "message": "Valid referral code",
                "valid": True,
                "referrer_name": referrer_name,
                "bonus_amount": 10.0
            })
            
        except json.JSONDecodeError:
            return JsonResponse({
                "message": "Invalid JSON in request body",
                "status": "error"
            }, status=400)
        except Exception as err:
            print("Error in validate_goods_agent_referral_code:", err)
            return JsonResponse({
                "message": "Internal Server Error",
                "status": "error",
                "error": str(err)
            }, status=500)
    
    return JsonResponse({
        "message": "Method not allowed",
        "status": "error"
    }, status=405) 