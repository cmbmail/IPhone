-- V60: Normalize bill_raw.bill_month from "yyyyMM" to "yyyy-MM" format
UPDATE bill_raw SET bill_month = CONCAT(SUBSTRING(bill_month, 1, 4), '-', SUBSTRING(bill_month, 5, 2))
WHERE bill_month REGEXP '^[0-9]{6}$';
