ALTER TABLE accounts ADD COLUMN user_id BIGINT;

-- Kimlik dogrulamadan once acilmis hesaplarin sahibi yok. Ilk kayitli kullaniciya atanir.
-- Hic kullanici yoksa bu hesaplar sahipsiz test verisidir; islemleriyle birlikte silinir.
UPDATE accounts SET user_id = (SELECT MIN(id) FROM users) WHERE user_id IS NULL;
DELETE FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE user_id IS NULL);
DELETE FROM accounts WHERE user_id IS NULL;

ALTER TABLE accounts ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_accounts_user ON accounts (user_id);
