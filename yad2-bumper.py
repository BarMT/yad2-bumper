import requests
import json
import time
from datetime import datetime, timezone, timedelta
import os
import pytz
import sys

default_sleep = 7200  # 2 hours in seconds
PATTERN_FORMAT = "%d.%m.%Y %H:%M:%S"
YAD2_DATE_FORMAT = "%Y-%m-%dT%H:%M:%S.%fZ"
jerusalem_tz = pytz.timezone('Asia/Jerusalem')


def main():
    print("Yad2 Bumper. ver 1.0")

    email = os.getenv("EMAIL")
    password = os.getenv("PASSWORD")
    
    if not email or not password:
        print("ERROR:required parameters: <email> <password>")
        exit(1)

    while True:
        response = login(email, password)
        if response.status_code != 200:
            print("ERROR:login failed.")
            exit(1)

        auth_cookie = response.cookies
        print("logged in")

        items = get_all_items(auth_cookie)
        print(f"queried {len(items)} items")

        if not items:
            print("Nothing to bump. sleeping.")
            time.sleep(default_sleep)
        else:
            next_bump_at = max(promote_item(item_id, auth_cookie) for item_id in items)
            print(f"bumped {len(items)} items. next bump at: {next_bump_at.strftime(PATTERN_FORMAT)}")

            jerusalem_now = datetime.now(jerusalem_tz)
            sleep_duration = (next_bump_at - jerusalem_now).total_seconds()
            sys.stdout.flush()
            time.sleep(max(sleep_duration, default_sleep))


def login(email, password):
    login_creds = {
        "email": email,
        "password": password
    }

    response = requests.post(
        "https://gw.yad2.co.il/auth/login",
        headers={"Content-Type": "application/json"},
        data=json.dumps(login_creds)
    )

    return response


def get_all_items(auth_cookie):
    response = requests.get(
        "https://gw.yad2.co.il/my-ads/?filterByStatuses[]=APPROVED&filterByStatuses[]=DEACTIVATED&filterByStatuses[]=REVIEWING&filterByStatuses[]=REFUSED&filterByStatuses[]=EXPIRED&filterByStatuses[]=REFUSED_DUE_TO_BUSINESS_AD&filterByStatuses[]=AWAITING_CONFIRMATION_BY_PHONE&filterByStatuses[]=REFUSED_DUE_TO_SUSPECTED_BUSINESS_CUSTOMER&filterByStatuses[]=AWAITING_CONFIRMATION_BY_KENNEL_CLUB&filterByStatuses[]=IN_PROGRESS_BEFORE_FINISH&filterByStatuses[]=AWAITING_PAYMENT_BY_PHONE&filterByStatuses[]=NEW_AD_NOT_PUBLISHED&filterByStatuses[]=AWAITING_COMMERCIAL_PAYMENT&filterByStatuses[]=WAITING_FOR_PHONE_APPROVAL&filterByStatuses[]=BUSINESS_TREATMENT&page=1",
        headers={"Content-Type": "application/json"},
        cookies=auth_cookie
    )

    items = json.loads(response.text)["data"]["items"]
    return [item["id"] for item in items]


def promote_item(item_id, auth_cookie):
    response = requests.put(
        f"https://gw.yad2.co.il/my-ads/{item_id}/promotion",
        headers={"Content-Type": "application/json"},
        cookies=auth_cookie
    )

    date_txt = json.loads(response.text)["data"]["allowManualPromotionAfter"]
    return datetime.strptime(date_txt, YAD2_DATE_FORMAT).replace(tzinfo=pytz.utc).astimezone(jerusalem_tz)


if __name__ == "__main__":
    main()
