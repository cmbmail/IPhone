#!/bin/bash
export DB_USER=phonebiz_app
export DB_PASSWORD=Ph0neBizApp2024Secure
export JWT_SECRET='PhoneBizJwtSecret2024SecureKey!!@@@@'
export DEFAULT_PASSWORD=PhoneBiz2024!
cd /home/data/apps/phone_ip/backend
java -jar build/libs/phonebiz-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
