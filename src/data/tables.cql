DROP TABLE positivewords;

CREATE TABLE positivewords (
  key int PRIMARY KEY,
  word text
) WITH comment='Good sentiment words'
  AND COMPACT STORAGE
  AND read_repair_chance = 1.0;

DROP TABLE negativewords;

CREATE TABLE negativewords (
  key int PRIMARY KEY,
  word text
) WITH comment='Bad sentiment words'
  AND COMPACT STORAGE
  AND read_repair_chance = 1.0;

DROP TABLE tweets;

CREATE TABLE tweets (
  key text PRIMARY KEY,
  user_user varchar,
  text text,
  createdat timestamp
  ) WITH comment='Tweets'
    AND COMPACT STORAGE
    AND read_repair_chance = 1.0;