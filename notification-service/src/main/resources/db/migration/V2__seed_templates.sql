INSERT INTO notification_templates (name, channel, subject, body) VALUES
('order_confirmed', 'EMAIL',
 'Your order has been confirmed!',
 'Dear customer, your order has been confirmed and payment processed successfully.'),

('payment_failed', 'EMAIL',
 'Payment issue with your order',
 'Dear customer, we could not process your payment. Please try again.'),

('order_shipped', 'EMAIL',
 'Your order is on its way!',
 'Dear customer, your order has been shipped and is on the way.'),

('order_confirmed_sms', 'SMS',
 NULL,
 'Your eCommerce order has been confirmed! Total: [[${amount}]] [[${currency}]]'),

('payment_failed_sms', 'SMS',
 NULL,
 'Payment for your order failed. Reason: [[${reason}]]. Please retry.')

ON CONFLICT (name) DO NOTHING;
