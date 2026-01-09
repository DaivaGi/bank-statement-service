ALTER TABLE bank_operation
add constraint uq_bank_operation_unique
unique (
    account_number,
    operation_time,
    beneficiary,
    amount,
    currency,
    operation_comment
);
