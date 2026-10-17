# ISTAFA GO Security Specification & Threat Model

## 1. Core Data Invariants & Zero-Trust Rules
1. **User Identity Isolation**: A customer cannot read or write another user's private data or PII in `/users/{userId}`.
2. **Role Elevation Prevention**: Users can only self-register as `CUSTOMER` or `DRIVER`. The `ADMIN` role is strictly restricted to verified records in `/admins/{adminId}` or hardcoded project owner bootstrap verification.
3. **Driver Approval Gate**: Newly registered drivers are locked in `status: 'PENDING_VERIFICATION'`. Only verified admins can transition status to `'APPROVED'`.
4. **Order State Consistency**: Order state transitions follow a strict directed graph:
   - `SEARCHING` (Customer creates)
   - `ACCEPTED` (Driver accepts, claiming order with immutable fare)
   - `PICKING_UP` -> `ON_THE_WAY` -> `COMPLETED` (Only assigned driver)
   - `CANCELLED` (Customer while `SEARCHING`/`ACCEPTED`, or Driver)
5. **Fare & Financial Tampering Protection**: Fares are computed and locked upon order creation. Neither customer, stranger, nor driver can alter the `fare` during status transitions.
6. **Audit & Settings Lock**: `audit_logs` and `app_settings` are strictly restricted to Super Admins.

## 2. The Dirty Dozen Payloads Evaluated
1. **Unauthenticated Read**: Attempting to read `/users` or `/orders` without auth token -> REJECTED (403 Permission Denied).
2. **Cross-User Snooping**: Alice queries `/users/bob_uid` -> REJECTED.
3. **Self-Admin Escalation**: Alice sends `role: 'ADMIN'` during registration -> REJECTED by role whitelist.
4. **Driver Self-Approval**: Unverified driver sends `status: 'APPROVED'` -> REJECTED.
5. **Fare Manipulation**: Hacker updates `fare: 100` on existing trip -> REJECTED by `affectedKeys().hasOnly(['status', ...])`.
6. **Order Hijacking**: Driver B modifies an order already accepted by Driver A -> REJECTED (`existing().driverId == request.auth.uid`).
7. **Junk ID Injection**: Client attempts to create document with 2000 character ID -> REJECTED by `isValidId()` boundary check.
8. **Malicious Negative Fares**: Client attempts to create order with `fare: -50000` -> REJECTED (`data.fare >= 0`).
9. **Fake Payment Status**: Customer sets `paymentStatus: 'PAID'` during order creation -> REJECTED (`paymentStatus in ['PENDING']` enforced).
10. **Admin Bypass Attack**: Non-admin attempts to purge `audit_logs` -> REJECTED.
11. **Timestamp Clock Spoof**: Client submits timestamp in year 2099 -> REJECTED (`data.createdAt <= request.time`).
12. **Driver Submission Forgery**: User attempts to update approval status in `driver_submissions` -> REJECTED (only admin can update).
