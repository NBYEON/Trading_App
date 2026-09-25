CREATE TABLE instruments (
  symbol varchar(12) PRIMARY KEY,
  name varchar(80) NOT NULL,
  sector varchar(80) NOT NULL,
  price_cents bigint NOT NULL CHECK (price_cents > 0),
  change_percent numeric(6,2) NOT NULL
);
CREATE TABLE accounts (
  id bigint PRIMARY KEY,
  cash_cents bigint NOT NULL CHECK (cash_cents >= 0)
);
CREATE TABLE positions (
  account_id bigint NOT NULL REFERENCES accounts(id),
  symbol varchar(12) NOT NULL REFERENCES instruments(symbol),
  quantity integer NOT NULL CHECK (quantity > 0),
  cost_cents bigint NOT NULL CHECK (cost_cents >= 0),
  PRIMARY KEY (account_id, symbol)
);
CREATE TABLE orders (
  request_id uuid PRIMARY KEY,
  account_id bigint NOT NULL REFERENCES accounts(id),
  symbol varchar(12) NOT NULL REFERENCES instruments(symbol),
  side varchar(4) NOT NULL CHECK (side IN ('BUY', 'SELL')),
  quantity integer NOT NULL CHECK (quantity > 0),
  total_cents bigint NOT NULL CHECK (total_cents > 0),
  filled_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX orders_account_time_idx ON orders(account_id, filled_at DESC);

INSERT INTO instruments VALUES
 ('NVDA','NVIDIA','Semiconductors',12784,2.34),
 ('AAPL','Apple','Consumer technology',22852,0.87),
 ('TSLA','Tesla','Electric vehicles',24850,-1.26),
 ('MSFT','Microsoft','Software & cloud',42876,1.12),
 ('AMZN','Amazon','Commerce & cloud',18649,-0.43),
 ('GOOGL','Alphabet','Internet services',16585,1.68);
INSERT INTO accounts VALUES (1,2475000);
INSERT INTO positions VALUES
 (1,'NVDA',12,139200),
 (1,'AAPL',8,176000),
 (1,'MSFT',5,205000);
